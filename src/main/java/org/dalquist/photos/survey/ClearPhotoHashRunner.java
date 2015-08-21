package org.dalquist.photos.survey;

import java.io.IOException;
import java.util.Map.Entry;

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
import com.google.gdata.util.common.base.Pair;

@Service
public class ClearPhotoHashRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClearPhotoHashRunner.class);

  public static void main(String[] args) throws Exception {
    try (ConfigurableApplicationContext ctx =
        new AnnotationConfigApplicationContext(AppConfig.class)) {
      try {
        ClearPhotoHashRunner runner = ctx.getBean(ClearPhotoHashRunner.class);
        runner.run();
      } catch (Throwable t) {
        LOGGER.error("Something Broke", t);
        throw t;
      }
    } finally {
      LOGGER.info("All Done!");
    }
  }

  private final PhotosDatabase photosDatabase;

  @Autowired
  public ClearPhotoHashRunner(PhotosDatabase photosDatabase) {
    this.photosDatabase = photosDatabase;
  }

  private void run() throws JsonProcessingException, IOException {
//    for (Entry<Pair<SourceId, String>, Image> imageEntry : photosDatabase.listImages()) {
//      final SourceId sourceId = imageEntry.getKey().first;
//      Image image = imageEntry.getValue();
//
//      boolean modified = clearMetadata(image.getOriginal());
//      modified = modified || clearMetadata(image.getModified());
//      modified = modified || clearMetadata(image.getThumb());
//      if (modified) {
//        LOGGER.info("Clearing: " + sourceId + ":" + image.getId());
//        photosDatabase.writeImage(sourceId, image);
//      } else {
//        LOGGER.info("Skipping: " + sourceId + ":" + image.getId());
//      }
//    }
  }

  private boolean clearMetadata(Resource r) {
//    if (r != null && r.getMetadata() != null) {
//      r.setMetadata(null);
//      return true;
//    }
    return false;
  }
}
