package org.dalquist.photos.survey.model;

import java.util.List;

public class Config extends JsonObject {
  private String photoDbFile;
  private Credentials firebaseCredentials;
  private List<Source> sources;

  public String getPhotoDbFile() {
    return photoDbFile;
  }

  public void setPhotoDbFile(String photoDbFile) {
    this.photoDbFile = photoDbFile;
  }

  public Credentials getFirebaseCredentials() {
    return firebaseCredentials;
  }

  public void setFirebaseCredentials(Credentials firebaseCredentials) {
    this.firebaseCredentials = firebaseCredentials;
  }

  public List<Source> getSources() {
    return sources;
  }

  public void setSources(List<Source> sources) {
    this.sources = sources;
  }
}
