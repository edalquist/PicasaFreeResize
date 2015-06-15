package org.dalquist.photos.survey;

import java.util.Map;

import javax.annotation.PreDestroy;

import org.dalquist.photos.survey.firebase.BlockingAuthResultHandler;
import org.dalquist.photos.survey.firebase.OverallCompletionListener;
import org.dalquist.photos.survey.model.Album;
import org.dalquist.photos.survey.model.Config;
import org.dalquist.photos.survey.model.Credentials;
import org.dalquist.photos.survey.model.Image;
import org.dalquist.photos.survey.model.SourceId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.firebase.client.Firebase;
import com.google.common.collect.ImmutableMap;

@Service
public final class PhotosDatabase {
  private final OverallCompletionListener overallCompletionListener =
      new OverallCompletionListener();
  private final Firebase firebase;

  @Autowired
  public PhotosDatabase(Config config) {
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
    System.out.println("Waiting for Firebase writes to flush");
    overallCompletionListener.waitForCompletion();
  }

  public void writeAlbum(SourceId sourceId, Album album) {
    Map<String, Object> albumData = ImmutableMap.of("name", album.getName());

    firebase
      .child("sources")
      .child(sourceId.getId())
      .child("albums")
      .child(album.getId())
      .setValue(albumData, overallCompletionListener.getCompletionListener());
  }

  public void writeImage(Image image) {
    ImmutableMap.Builder<String, Object> mediaDataBuilder = ImmutableMap.builder();
    if (image.getOriginal() != null) {
      mediaDataBuilder.put("original", image.getOriginal());
    }
    if (image.getModified() != null) {
      mediaDataBuilder.put("modified", image.getModified());
    }
    if (image.getThumb() != null) {
      mediaDataBuilder.put("thumb", image.getThumb());
    }

    firebase
      .child("sources")
      .child(image.getMediaId().getSourceId().getId())
      .child("images")
      .child(image.getMediaId().getId())
      .setValue(mediaDataBuilder.build(), overallCompletionListener.getCompletionListener());
  }
  
  public void putImageInAlbum(Image image, Album album) {
    // TODO
  }
}
