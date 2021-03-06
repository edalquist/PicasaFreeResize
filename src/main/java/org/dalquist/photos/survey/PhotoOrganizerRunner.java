package org.dalquist.photos.survey;

import java.io.IOException;
import java.util.Set;

import org.dalquist.photos.survey.config.Config;
import org.dalquist.photos.survey.config.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gdata.util.ServiceException;

@Service
public class PhotoOrganizerRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(PhotoOrganizerRunner.class);

  public static void main(String[] args) throws Exception {
    try (ConfigurableApplicationContext ctx =
        new AnnotationConfigApplicationContext(AppConfig.class)) {
      try {
        PhotoOrganizerRunner runner = ctx.getBean(PhotoOrganizerRunner.class);
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
  private final PhotoProcessor photoProcessor;
  private final Set<ConfigurablePhotoOrganizer> organizers;

  @Autowired
  public PhotoOrganizerRunner(Config config, PhotoProcessor photoProcessor,
      Set<ConfigurablePhotoOrganizer> organizers) {
    this.config = config;
    this.photoProcessor = photoProcessor;
    this.organizers = organizers;
  }

  private void run() throws JsonProcessingException, IOException, ServiceException {
    // Iterate through sources
    for (Source source : config.getSources()) {
      if (new Boolean(String.valueOf(source.get("skip")))) {
        LOGGER.info("Skipping source: " + source.getId() + ":" + source.getType());
        continue;
      }

      PhotoOrganizer sourceOrganizer = createOrganizer(source);
      LOGGER.info("Organizing photos from: " + source.getId() + ":" + source.getType());

      sourceOrganizer.organizePhotos(photoProcessor);

      LOGGER.info("Organized photos from: " + source.getId() + ":" + source.getType());
    }
  }

  private PhotoOrganizer createOrganizer(Source source) {
    for (ConfigurablePhotoOrganizer organizer : organizers) {
      if (organizer.getType().equals(source.getType())) {
        return organizer.configure(source);
      }
    }
    throw new IllegalStateException("No organizer registered for: " + source.getType());
  }
}
