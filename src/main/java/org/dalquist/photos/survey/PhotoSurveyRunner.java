package org.dalquist.photos.survey;

import java.io.IOException;
import java.util.Set;

import org.dalquist.photos.survey.model.Config;
import org.dalquist.photos.survey.model.Source;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gdata.util.ServiceException;

@Service
public class PhotoSurveyRunner {
  public static void main(String[] args) throws Exception {
    try (ConfigurableApplicationContext ctx =
        new AnnotationConfigApplicationContext(AppConfig.class)) {
      PhotoSurveyRunner runner = ctx.getBean(PhotoSurveyRunner.class);
      runner.run();
    } finally {
      System.out.println("DONE");
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
//    // Load the photoDb
//    String photoDbFile = config.getPhotoDbFile();

    // Iterate through sources
    for (Source source : config.getSources()) {
      if (new Boolean(source.get("skip"))) {
        System.out.println("Skipping source: " + source.getId() + ":" + source.getType());
        continue;
      }

      PhotoOrganizer sourceOrganizer = createOrganizer(source);
      System.out.println("Parsing photos from: " + source.getId() + ":" + source.getType());

      sourceOrganizer.loadPhotoEntries(photosDatabase);

      // Save the db after each source
      System.out.println("Parsed photos from: " + source.getId() + ":" + source.getType());
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
