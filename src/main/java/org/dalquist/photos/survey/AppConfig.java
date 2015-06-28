package org.dalquist.photos.survey;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.dalquist.photos.survey.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.util.concurrent.ForwardingListeningExecutorService;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

@Configuration
@ComponentScan("org.dalquist.photos.survey")
public class AppConfig {
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Bean(destroyMethod = "shutdown")
  public ListeningExecutorService createListeningExecutorService() {
    int cores = (int) (Runtime.getRuntime().availableProcessors() * 1) - 1;
    logger.info("Starting with {} cores", cores);

    ThreadPoolExecutor tpe =
        new ThreadPoolExecutor(cores, cores, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(cores * 4), new ThreadPoolExecutor.CallerRunsPolicy());
    ListeningExecutorService les = MoreExecutors.listeningDecorator(tpe);

    return new ForwardingListeningExecutorService() {
      @Override
      public void shutdown() {
        logger.info("Requesting thread pool shutdown");
        super.shutdown();
        try {
          logger.info("Waiting for thread pool termination");
          super.awaitTermination(5, TimeUnit.MINUTES);
          logger.info("Thread pool terminated");
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(e);
        }
      }

      @Override
      protected ListeningExecutorService delegate() {
        return les;
      }
    };
//    return MoreExecutors.newDirectExecutorService();
  }

  @Bean
  public Config createConfig(JacksonUtils jacksonUtils) throws JsonProcessingException, IOException {
    return jacksonUtils.read("classpath:/app_config.json", Config.class);
  }
}
