package org.dalquist.photos.survey;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PreDestroy;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.dalquist.photos.survey.StdinShutdownListener.ShutdownRequested;
import org.dalquist.photos.survey.config.Config;
import org.dalquist.photos.survey.config.PathReplacement;
import org.dalquist.photos.survey.config.Source;
import org.dalquist.photos.survey.model.Image;
import org.dalquist.photos.survey.model.Resource;
import org.dalquist.photos.survey.model.SourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.gdata.util.common.base.Pair;
import com.zaxxer.nuprocess.NuAbstractProcessHandler;
import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;

@Service
@DependsOn("PhotosDatabase")
public class PhotoHashingRunner implements ApplicationListener<ShutdownRequested> {
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
  private final Map<String, ListenableFuture<Long>> futures = new ConcurrentHashMap<>();
  private final DescriptiveStatistics procTimeStats = new DescriptiveStatistics(1000000);
  private final DescriptiveStatistics completeTimeStats = new DescriptiveStatistics(1000000);
  private final AtomicLong lastCompleted = new AtomicLong(System.nanoTime());
  private final AtomicInteger imageCounter = new AtomicInteger(0);
  private volatile boolean stopped = false;

  @Autowired
  public PhotoHashingRunner(Config config, PhotosDatabase photosDatabase,
      ListeningExecutorService execService) {
    this.config = config;
    this.photosDatabase = photosDatabase;
    this.execService = execService;
  }

  @Override
  public void onApplicationEvent(ShutdownRequested event) {
  }
  @PreDestroy
  public void shutdown() {
    stopped = true;
    try {
      waitForRemainingFutures();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String getLogPrefix() {
    long ptMean = TimeUnit.NANOSECONDS.toMillis((long) procTimeStats.getMean());
    long ctMean = TimeUnit.NANOSECONDS.toMillis((long) completeTimeStats.getMean());

    int processedImages = imageCounter.get();
    int totalImages = photosDatabase.getImageCount();
    double percent = (processedImages / (double) totalImages) * 100;

    return String.format("%,.2f%% (%d/%d) %,.2fs, %,.2fs - ", percent, processedImages,
        totalImages, ptMean / 1000D, ctMean / 1000D);
  }

  private void run() throws JsonProcessingException, IOException {
    for (Entry<Pair<SourceId, String>, Image> imageEntry : photosDatabase.listImages()) {
      if (stopped) {
        return;
      }

      final SourceId sourceId = imageEntry.getKey().first;
      Source source = config.getSource(sourceId);
      PathReplacement pathReplacement = source.getPathReplacement();

      Image image = imageEntry.getValue();

      // Trigger photo persist once both resources have been processed
      ListenableFuture<Long> of =
          submit(completeTimeStats, lastCompleted, pathReplacement, image.getOriginal());
      ListenableFuture<Long> mf =
          submit(completeTimeStats, lastCompleted, pathReplacement, image.getModified());

      @SuppressWarnings("unchecked")
      ListenableFuture<List<Long>> imageFutures = Futures.allAsList(of, mf);

      Futures.addCallback(imageFutures, new FutureCallback<List<Long>>() {
        @Override
        public void onSuccess(List<Long> result) {
          // If at least one of the tasks modified a resource write the modified image
          boolean write = false;
          for (Long procTime : result) {
            write = write || procTime >= 0;
          }
          if (write) {
            write();
          }
          imageCounter.incrementAndGet();
        }

        @Override
        public void onFailure(Throwable t) {
          // Be safe and write
          write();
          imageCounter.incrementAndGet();
        }

        private void write() {
          photosDatabase.writeImage(sourceId, image);
        }
      });
    }

    waitForRemainingFutures();
  }

  private void waitForRemainingFutures() throws IOException {
    LOGGER.info("Waiting on " + futures.size() + " tasks");
    try {
      Futures.allAsList(futures.values()).get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      Throwables.propagateIfPossible(e.getCause(), IOException.class);
    } finally {
      LOGGER.info(futures.size() + " tasks complete");
    }
  }

  private ListenableFuture<Long> submit(final DescriptiveStatistics completeTimeStats,
      final AtomicLong lastCompleted, PathReplacement pathReplacement, final Resource resource) {
    if (resource == null) {
      return Futures.immediateFuture(-1L);
    }
    if (resource.getMetadata() != null) {
      LOGGER.info("Skipping (alread has metadata): " + resource.getUrl());
      return Futures.immediateFuture(-1L);
    }

    MetaDataExtractor extractor =
        new MetaDataExtractor(config.getConvertBinary(), pathReplacement, resource);
    ListenableFuture<Long> f = execService.submit(extractor);
    futures.put(resource.getUrl(), f);
    Futures.addCallback(f, new FutureCallback<Long>() {
      @Override
      public void onSuccess(Long result) {
        procTimeStats.addValue(result);
        onComplete(completeTimeStats, lastCompleted);
      }

      @Override
      public void onFailure(Throwable t) {
        onComplete(completeTimeStats, lastCompleted);
      }

      private void onComplete(final DescriptiveStatistics completeTimeStats,
          final AtomicLong lastCompleted) {
        if (futures.remove(resource.getUrl()) == null) {
          LOGGER.warn("Nothing removed for: " + resource.getUrl());
        };
        long prev = lastCompleted.get();
        long now = System.nanoTime();
        long duration = now - prev;
        if (duration > 0) {
          lastCompleted.compareAndSet(prev, now);
        }
        completeTimeStats.addValue(Math.max(duration, 0));
        LOGGER.info(getLogPrefix() + " Done: " + resource.getUrl());
      }
    });
    return f;
  }

  private class MetaDataExtractor implements Callable<Long> {
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
    public Long call() throws Exception {
      if (stopped) {
        return -1L;
      }

      long start = System.nanoTime();
      final String url = getUrl();

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(getLogPrefix() + "Start: " + url);
      }

      final StringBuilder stdoutBuilder = new StringBuilder();
      final StringBuilder stderrBuilder = new StringBuilder();

      NuProcessBuilder pb = new NuProcessBuilder(convertBinary, url, "-moments", "json:-");
      pb.setProcessListener(new NuAbstractProcessHandler() {
        @Override
        public void onStdout(ByteBuffer buffer) {
          if (buffer != null) {
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer);
            stdoutBuilder.append(charBuffer);
          }
        }

        @Override
        public void onStderr(ByteBuffer buffer) {
          if (buffer != null) {
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer);
            stderrBuilder.append(charBuffer);
          }
        }

        @Override
        public void onExit(int exitCode) {
          if (exitCode != 0) {
            throw new RuntimeException("Failed to execute convert on: " + url + "\n"
                + stderrBuilder);
          }
        }
      });

      NuProcess proc = pb.start();
      proc.waitFor(30, TimeUnit.SECONDS);

      ObjectMapper objectMapper = ObjectMapperHolder.getObjectMapper();
      Map<String, Object> metadata = objectMapper.readValue(stdoutBuilder.toString(), Map.class);

      metadata = (Map<String, Object>) metadata.get("image");
      resource.setMetadata(metadata);

      return System.nanoTime() - start;
    }

    private String getUrl() {
      String url = resource.getUrl();
      if (pathReplacement != null) {
        url = url.replace(pathReplacement.getFrom(), pathReplacement.getTo());
      }
      if (url.startsWith("file:/")) {
        url = url.substring("file:/".length() - 1);
      }
      return url;
    }
  }
}
