package org.dalquist.photos.survey;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.atomic.AtomicInteger;

import org.dalquist.photos.survey.config.Config;
import org.dalquist.photos.survey.config.PathReplacement;
import org.dalquist.photos.survey.config.Source;
import org.dalquist.photos.survey.model.Image;
import org.dalquist.photos.survey.model.Resource;
import org.dalquist.photos.survey.model.SourceId;
import org.joda.time.Period;
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
  private final PeriodicExecutor statusLoggingExecutor = new PeriodicExecutor(Period.seconds(5));

  @Autowired
  public PhotoHashingRunner(Config config, PhotosDatabase photosDatabase,
      ListeningExecutorService execService) {
    this.config = config;
    this.photosDatabase = photosDatabase;
    this.execService = execService;
  }

  private void run() throws JsonProcessingException, IOException {
    final AtomicInteger imageCounter = new AtomicInteger();
    final int totalImages = photosDatabase.getImageCount();

    Collection<ListenableFuture<List<Boolean>>> futures = new LinkedList<>();

    for (Entry<Pair<SourceId, String>, Image> imageEntry : photosDatabase.listImages()) {
      final SourceId sourceId = imageEntry.getKey().first;
      Source source = config.getSource(sourceId);
      PathReplacement pathReplacement = source.getPathReplacement();

      Image image = imageEntry.getValue();

      // Trigger photo persist once both resources have been processed
      @SuppressWarnings("unchecked")
      ListenableFuture<List<Boolean>> imageFutures = Futures.allAsList(
          submit(pathReplacement, image.getOriginal()),
          submit(pathReplacement, image.getModified()));

      Futures.addCallback(imageFutures, new FutureCallback<List<Boolean>>() {
        @Override
        public void onSuccess(List<Boolean> result) {
          // If at least one of the tasks modified a resource write the modified image
          if (result.contains(true)) {
            write();
          }
          log();
        }

        @Override
        public void onFailure(Throwable t) {
          // Be safe and write
          write();
          log();
        }

        private void write() {
          photosDatabase.writeImage(sourceId, image);
        }

        private void log() {
          statusLoggingExecutor.run(new Runnable() {
            @Override
            public void run() {
              int processedImages = imageCounter.incrementAndGet();
              int percent = (processedImages / totalImages) * 100;
              LOGGER.info(percent + "% complete (" + processedImages + "/" + totalImages + ")");
            }
          });
        }
      });


      // Keep the list size down
      for (Iterator<ListenableFuture<List<Boolean>>> futureItr = futures.iterator(); futureItr
          .hasNext();) {
        ListenableFuture<List<Boolean>> future = futureItr.next();
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

      ProcessBuilder pb = new ProcessBuilder(convertBinary, url, "-moments", "json:-");
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

    private void readOutputs(Process proc, StringBuilder stdoutBuilder, StringBuilder stderrBuilder)
        throws IOException {
      char[] readBuff = new char[1024];
      try (InputStream stdout = proc.getInputStream(); InputStream stderr = proc.getErrorStream();) {
        Reader stdoutR = new BufferedReader(new InputStreamReader(stdout));
        Reader stderrR = new BufferedReader(new InputStreamReader(stderr));

        while (proc.isAlive()) {
          readInto(stdoutR, readBuff, stdoutBuilder);
          readInto(stderrR, readBuff, stderrBuilder);
          try {
            Thread.sleep(1);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
          }
        }

        // Read one last time
        readInto(stdoutR, readBuff, stdoutBuilder);
        readInto(stderrR, readBuff, stderrBuilder);
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
