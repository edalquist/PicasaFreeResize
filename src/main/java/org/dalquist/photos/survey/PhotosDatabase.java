package org.dalquist.photos.survey;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;

import org.dalquist.photos.survey.config.Config;
import org.dalquist.photos.survey.model.Album;
import org.dalquist.photos.survey.model.Image;
import org.dalquist.photos.survey.model.SourceId;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public final class PhotosDatabase {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final DB db;
  private final Map<String, Object> dbMap;
  private final AtomicInteger commitCounter = new AtomicInteger();

  @Autowired
  public PhotosDatabase(Config config) {
    db = DBMaker.newFileDB(new File(config.getPhotoDbFile()))
        .cacheLRUEnable()
        .checksumEnable()
        .asyncWriteEnable()
        .make();
    dbMap = db.getHashMap("photos");
  }

  @PreDestroy
  public void stop() {
    // Ensure commit is complete
    db.commit();

    logger.info("Compacting DB");
    try {
      db.compact();
    } catch (RuntimeException e) {
      logger.error("Compact failed", e);
    }
    logger.info("Closing DB");
    try {
      db.close();
    } catch (RuntimeException e) {
      logger.error("Close failed", e);
    }
    logger.info("Closed DB");
  }

  public void writeImage(SourceId sourceId, Image image) {
    String imageKey = "/sources/" + sourceId.getId() + "/images/" + image.getId();
    dbMap.put(imageKey, image.getCollectionRepresentation());
    commit();
  }

  public void writeImages(SourceId sourceId, Iterable<Image> images) {
    for (Image image : images) {
      writeImage(sourceId, image);
    }
  }

  public void writeAlbum(SourceId sourceId, Album album) {
    String albumKey = "/sources/" + sourceId.getId() + "/albums/" + album.getId();
    dbMap.put(albumKey, album.getCollectionRepresentation());
    commit();
  }

  private void commit() {
    if (commitCounter.incrementAndGet() % 1000 == 0) {
      db.commit();
    }
  }
}
