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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

@Configuration
@ComponentScan("org.dalquist.photos.survey")
public class AppConfig {
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Bean
  public ListeningExecutorService createListeningExecutorService() {
    int cores = Runtime.getRuntime().availableProcessors();
    logger.info("Starting with {} cores", cores);

    ThreadPoolExecutor tpe =
        new ThreadPoolExecutor(cores, cores, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(cores * 4), new ThreadPoolExecutor.CallerRunsPolicy());
    ListeningExecutorService les = MoreExecutors.listeningDecorator(tpe);

    return les;
//    return MoreExecutors.newDirectExecutorService();
  }

  @Bean
  public Config createConfig(JacksonUtils jacksonUtils) throws JsonProcessingException, IOException {
    return jacksonUtils.read("classpath:/app_config.json", Config.class);
  }
}
