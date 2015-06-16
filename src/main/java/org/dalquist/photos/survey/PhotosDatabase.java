package org.dalquist.photos.survey;

import java.util.Map;

import javax.annotation.PreDestroy;

import org.dalquist.photos.survey.config.Config;
import org.dalquist.photos.survey.config.Credentials;
import org.dalquist.photos.survey.firebase.BlockingAuthResultHandler;
import org.dalquist.photos.survey.firebase.WriteManager;
import org.dalquist.photos.survey.firebase.SystemOutLogger;
import org.dalquist.photos.survey.model.Album;
import org.dalquist.photos.survey.model.Image;
import org.dalquist.photos.survey.model.SourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.firebase.client.Firebase;
import com.firebase.client.Firebase.CompletionListener;
import com.firebase.client.FirebaseError;
import com.firebase.client.Logger.Level;

@Service
public final class PhotosDatabase {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final WriteManager overallCompletionListener =
      new WriteManager();
  private final Firebase firebase;

  @Autowired
  public PhotosDatabase(Config config) {
    com.firebase.client.Config fbConfig = new com.firebase.client.Config();
    fbConfig.setLogLevel(Level.DEBUG);
    fbConfig.setLogger(SystemOutLogger.INSTANCE);

    Firebase.setDefaultConfig(fbConfig);
    firebase = new Firebase("https://photosdb.firebaseio.com/");
    Credentials firebaseCredentials = config.getFirebaseCredentials();

    // Request auth and wait for it to complete
    BlockingAuthResultHandler waitingAuthResultHandler = new BlockingAuthResultHandler();
    firebase.authWithPassword(firebaseCredentials.getEmail(), firebaseCredentials.getPassword(),
        waitingAuthResultHandler);
    waitingAuthResultHandler.waitForAuth();
  }

  @PreDestroy
  public void stop() {
    logger.info("Waiting for Firebase writes to flush");
    overallCompletionListener.waitForCompletion();
  }

  public void writeImage(SourceId sourceId, Image image) {
    Firebase path = getImagesRoot(sourceId).child(image.getId());
    writeObject(path, image.getFirebaseRepresentation());
  }

  public void writeImages(SourceId sourceId, Iterable<Image> images) {
    Firebase path = getImagesRoot(sourceId);
    for (Image image : images) {
      Firebase imagePath = path.child(image.getId());
      writeObject(imagePath, image.getFirebaseRepresentation());
    }
  }

  public void writeAlbum(SourceId sourceId, Album album) {
    Firebase path = getAlbumsRoot(sourceId).child(album.getId());
    writeObject(path, album.getFirebaseRepresentation());
  }

  private void writeObject(Firebase path, Map<String, Object> obj) {
    CompletionListener completionListener = overallCompletionListener.getCompletionListener();
    try {
      path.updateChildren(obj, completionListener);
    } catch (RuntimeException e) {
      completionListener.onComplete(FirebaseError.fromException(e), path);
    }
  }

  private Firebase getSourceRoot(SourceId sourceId) {
    return firebase.child("sources").child(sourceId.getId());
  }

  private Firebase getImagesRoot(SourceId sourceId) {
    return getSourceRoot(sourceId).child("images");
  }

  private Firebase getAlbumsRoot(SourceId sourceId) {
    return getSourceRoot(sourceId).child("albums");
  }
}
