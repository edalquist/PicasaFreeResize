package org.dalquist.photos.survey;

import java.io.IOException;
import java.io.InputStreamReader;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Service;

import com.google.common.io.LineReader;
import com.google.common.util.concurrent.ListeningExecutorService;

@Service
public class StdinShutdownListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(PhotoHashingRunner.class);

  private final ConfigurableApplicationContext ctx;
  private final ListeningExecutorService execService;

  @Autowired
  public StdinShutdownListener(ConfigurableApplicationContext ctx,
      ListeningExecutorService execService) {
    this.ctx = ctx;
    this.execService = execService;
  }

  @PostConstruct
  public void startListenerThread() {
    Runnable r = new Runnable() {
      @Override
      public void run() {
        LOGGER.info("Started shutdown listener");

        LineReader reader = new LineReader(new InputStreamReader(System.in));
        while (true) {
          String line = null;
          try {
            line = reader.readLine();
          } catch (IOException e) {
            LOGGER.warn("error readin from stdin", e);
          }

          if ("stop".equalsIgnoreCase(line)) {
            shutdown();
            return;
          }
        }
      }
    };

    Thread t = new Thread(r, "stdin shutdown listener");
    t.setDaemon(true);
    t.start();
  }

  public void shutdown() {
//    LOGGER.warn("Thread Pool Shutdown");
//    execService.shutdown();
//    LOGGER.warn("Sending Stop Event");
//    ctx.publishEvent(new ShutdownRequested(StdinShutdownListener.this));
    LOGGER.warn("Closing Spring App Context");
    ctx.close();
  }

  public static final class ShutdownRequested extends ApplicationEvent {
    private static final long serialVersionUID = 1L;

    public ShutdownRequested(Object source) {
      super(source);
    }
  }
}
