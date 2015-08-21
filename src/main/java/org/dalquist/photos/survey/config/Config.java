package org.dalquist.photos.survey.config;

import java.util.List;

import org.dalquist.photos.survey.model.JsonObject;
import org.dalquist.photos.survey.model.SourceId;

public class Config extends JsonObject {
  private static final long serialVersionUID = 1L;

  private String photoDbFile;
  private String convertBinary;
  private Credentials firebaseCredentials;
  private String destDir;
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

  public String getConvertBinary() {
    return convertBinary;
  }

  public void setConvertBinary(String convertBinary) {
    this.convertBinary = convertBinary;
  }

  public String getDestDir() {
    return destDir;
  }

  public void setDestDir(String destDir) {
    this.destDir = destDir;
  }

  public List<Source> getSources() {
    return sources;
  }

  public void setSources(List<Source> sources) {
    this.sources = sources;
  }

  public Source getSource(SourceId sourceId) {
    for (Source source : sources) {
      if (sourceId.equals(source.getSourceId())) {
        return source;
      }
    }
    return null;
  }
}
