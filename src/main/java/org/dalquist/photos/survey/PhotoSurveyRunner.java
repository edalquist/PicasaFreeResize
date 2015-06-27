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
public class PhotoSurveyRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(PhotoSurveyRunner.class);

  public static void main(String[] args) throws Exception {
    try (ConfigurableApplicationContext ctx =
        new AnnotationConfigApplicationContext(AppConfig.class)) {
      try {
        PhotoSurveyRunner runner = ctx.getBean(PhotoSurveyRunner.class);
//        runner.run();
        // TODO make this run idempotent
      } catch (Throwable t) {
        LOGGER.error("Something Broke", t);
        throw t;
      }
    } finally {
      LOGGER.info("All Done!");
    }
  }

  private final Config config;
  private final Set<ConfigurablePhotoOrganizer> organizers;
  private final PhotosDatabase photosDatabase;

  @Autowired
  public PhotoSurveyRunner(Config config, Set<ConfigurablePhotoOrganizer> organizers,
      PhotosDatabase photosDatabase) {
    this.config = config;
    this.organizers = organizers;
    this.photosDatabase = photosDatabase;
  }

  private void run() throws JsonProcessingException, IOException, ServiceException {
    // Iterate through sources
    for (Source source : config.getSources()) {
      if (new Boolean(String.valueOf(source.get("skip")))) {
        LOGGER.info("Skipping source: " + source.getId() + ":" + source.getType());
        continue;
      }

      PhotoOrganizer sourceOrganizer = createOrganizer(source);
      LOGGER.info("Parsing photos from: " + source.getId() + ":" + source.getType());

      sourceOrganizer.loadPhotoEntries(photosDatabase);

      // Save the db after each source
      LOGGER.info("Parsed photos from: " + source.getId() + ":" + source.getType());
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
