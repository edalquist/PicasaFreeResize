package org.dalquist.photos.survey;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.dalquist.photos.survey.config.Config;
import org.dalquist.photos.survey.config.PathReplacement;
import org.dalquist.photos.survey.config.Source;
import org.dalquist.photos.survey.model.Image;
import org.dalquist.photos.survey.model.Resource;
import org.dalquist.photos.survey.model.SourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gdata.util.common.base.Pair;

@Service
public class PhotoHashingRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(PhotoHashingRunner.class);

  public static void main(String[] args) throws Exception {
    try (ConfigurableApplicationContext ctx =
        new AnnotationConfigApplicationContext(AppConfig.class)) {
      try {
        PhotoHashingRunner runner = ctx.getBean(PhotoHashingRunner.class);
        runner.run();
      } catch (Throwable t) {
        LOGGER.error("Something Broke", t);
        throw t;
      }
    } finally {
      LOGGER.info("All Done!");
    }
  }

  private final Config config;
  private final PhotosDatabase photosDatabase;
  private final ListeningExecutorService execService;

  @Autowired
  public PhotoHashingRunner(Config config, PhotosDatabase photosDatabase,
      ListeningExecutorService execService) {
    this.config = config;
    this.photosDatabase = photosDatabase;
    this.execService = execService;
  }

  private void run() throws JsonProcessingException, IOException {
    Collection<ListenableFuture<?>> futures = new LinkedList<>();
    for (Entry<Pair<SourceId, String>, Image> imageEntry : photosDatabase.listImages()) {
      final SourceId sourceId = imageEntry.getKey().first;
      Source source = config.getSource(sourceId);
      PathReplacement pathReplacement = source.getPathReplacement();

      Image image = imageEntry.getValue();

      ListenableFuture<Boolean> of = submit(pathReplacement, image.getOriginal());
      ListenableFuture<Boolean> mf = submit(pathReplacement, image.getModified());
      futures.add(of);
      futures.add(mf);

      // Trigger photo persist once both resources have been processed
      Futures.addCallback(Futures.allAsList(of, mf), new FutureCallback<List<Boolean>>() {
        @Override
        public void onSuccess(List<Boolean> result) {
          if (result.contains(true)) {
            photosDatabase.writeImage(sourceId, image);
          }
        }

        @Override
        public void onFailure(Throwable t) {
          // Be safe and write
          photosDatabase.writeImage(sourceId, image);
        }
      });


      // Keep the list size down
      for (Iterator<ListenableFuture<?>> futureItr = futures.iterator(); futureItr.hasNext();) {
        ListenableFuture<?> future = futureItr.next();
        if (future.isDone()) {
          try {
            future.get();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          } catch (ExecutionException e) {
            Throwables.propagateIfPossible(e.getCause(), IOException.class);
          }
          futureItr.remove();
        }
      }
    }

    try {
      Futures.allAsList(futures).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      Throwables.propagateIfPossible(e.getCause(), IOException.class);
    }
  }

  private ListenableFuture<Boolean> submit(PathReplacement pathReplacement, Resource resource) {
    if (resource == null) {
      return Futures.immediateFuture(false);
    }
    if (resource.getMetadata() != null) {
      LOGGER.info("Skipping: " + resource.getUrl());
      return Futures.immediateFuture(false);
    }
    return execService.submit(new MetaDataExtractor(config.getConvertBinary(), pathReplacement,
        resource));
  }

  private static class MetaDataExtractor implements Callable<Boolean> {
    private final String convertBinary;
    private final PathReplacement pathReplacement;
    private final Resource resource;

    public MetaDataExtractor(String convertBinary, PathReplacement pathReplacement,
        Resource resource) {
      this.convertBinary = convertBinary;
      this.pathReplacement = pathReplacement;
      this.resource = resource;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Boolean call() throws Exception {
      String url = resource.getUrl();
      if (pathReplacement != null) {
        url = url.replace(pathReplacement.getFrom(), pathReplacement.getTo());
      }
      if (url.startsWith("file:/")) {
        url = url.substring("file:/".length() - 1);
      }

      LOGGER.info("Start processing: " + url);

      ProcessBuilder pb =
          new ProcessBuilder("/Users/edalquist/tmp/im_json_bug/im/bin/convert", url, "-moments",
              "json:-");
      Process proc = pb.start();

      StringBuilder stdoutBuilder = new StringBuilder();
      StringBuilder stderrBuilder = new StringBuilder();
      readOutputs(proc, stdoutBuilder, stderrBuilder);

      if (proc.exitValue() != 0) {
        throw new RuntimeException("Failed to execute convert on: " + url + "\n" + stderrBuilder);
      }

      ObjectMapper objectMapper = ObjectMapperHolder.getObjectMapper();
      Map<String, Object> metadata = objectMapper.readValue(stdoutBuilder.toString(), Map.class);

      metadata = (Map<String, Object>) metadata.get("image");
      resource.setMetadata(metadata);

      LOGGER.info("End processing: " + url);

      return true;
    }

    private void readOutputs(Process proc, StringBuilder stdoutBuilder, StringBuilder stderrBuilder) {
      char[] readBuff = new char[1024];
      try (Reader stdout = new BufferedReader(new InputStreamReader(proc.getInputStream()));
          Reader stderr = new BufferedReader(new InputStreamReader(proc.getErrorStream()));) {
        while (proc.isAlive()) {
          readInto(stdout, readBuff, stdoutBuilder);
          readInto(stderr, readBuff, stderrBuilder);
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }
        }

        // Read one last time
        readInto(stdout, readBuff, stdoutBuilder);
        readInto(stderr, readBuff, stderrBuilder);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private void readInto(Reader src, char[] readBuff, StringBuilder dest) throws IOException {
      while (src.ready()) {
        int chars = src.read(readBuff);
        dest.append(readBuff, 0, chars);
      }
    }
  }
}
