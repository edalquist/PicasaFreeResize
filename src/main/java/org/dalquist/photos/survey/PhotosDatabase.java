package org.dalquist.photos.survey;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.dalquist.photos.survey.config.Config;
import org.dalquist.photos.survey.model.Album;
import org.dalquist.photos.survey.model.Image;
import org.dalquist.photos.survey.model.SourceId;
import org.joda.time.Period;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gdata.util.common.base.Pair;

@Service("PhotosDatabase")
public final class PhotosDatabase {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final DB db;
  private final Map<Pair<SourceId, String>, Image> imagesMap;
  private final Map<Pair<SourceId, String>, Album> albumsMap;
  private final PeriodicExecutor commitExecutor = new PeriodicExecutor(Period.seconds(15));

  @Autowired
  public PhotosDatabase(Config config) {
    db = DBMaker.newFileDB(new File(config.getPhotoDbFile()))
        .cacheLRUEnable()
        .checksumEnable()
        .compressionEnable()
        .mmapFileEnablePartial()
        .make();
    imagesMap = db.getHashMap("images");
    albumsMap = db.getHashMap("albums");
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
    Pair<SourceId, String> imageKey = getImageKey(sourceId, image);
    imagesMap.put(imageKey, image);
    commit();
  }

  public void writeImages(SourceId sourceId, Iterable<Image> images) {
    for (Image image : images) {
      writeImage(sourceId, image);
    }
  }

  public void writeAlbum(SourceId sourceId, Album album) {
    Pair<SourceId, String> albumKey = getAlbumKey(sourceId, album);
    albumsMap.put(albumKey, album);
    commit();
  }

  public Iterable<Entry<Pair<SourceId, String>, Image>> listImages() {
    return imagesMap.entrySet();
  }

  public int getImageCount() {
    return imagesMap.size();
  }

  private Pair<SourceId, String> getAlbumKey(SourceId sourceId, Album album) {
    return Pair.of(sourceId, album.getId());
  }

  private Pair<SourceId, String> getImageKey(SourceId sourceId, Image image) {
    return Pair.of(sourceId, image.getId());
  }

  private void commit() {
    commitExecutor.run(new Runnable() {
      @Override
      public void run() {
        long start = System.nanoTime();
        db.commit();
        long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        logger.info("Committed DB in {}ms", duration);
      }
    });
  }
}
