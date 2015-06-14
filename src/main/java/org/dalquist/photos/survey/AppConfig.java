package org.dalquist.photos.survey;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.dalquist.photos.survey.model.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

@Configuration
@ComponentScan("org.dalquist.photos.survey")
public class AppConfig {

  @Bean
  public ListeningExecutorService createListeningExecutorService(CPUUsageCollector cpuUsageCollector) {
    int cores = Runtime.getRuntime().availableProcessors();
    System.out.println("Starting with " + cores + " cores");

    ThreadPoolExecutor tpe =
        new ThreadPoolExecutor(cores, cores, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
    ListeningExecutorService les = MoreExecutors.listeningDecorator(tpe);

    cpuUsageCollector.registerThreadPool(tpe);

    return les;
  }

  @Bean
  public Config createConfig(JacksonUtils jacksonUtils) throws JsonProcessingException, IOException {
    return jacksonUtils.read("classpath:/app_config.json", Config.class);
  }
}
