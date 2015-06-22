package org.dalquist.photos.survey.config;

import org.dalquist.photos.survey.model.JsonObject;

public class PathReplacement extends JsonObject {
  private String from;
  private String to;

  public String getFrom() {
    return from;
  }
  public void setFrom(String from) {
    this.from = from;
  }
  public String getTo() {
    return to;
  }
  public void setTo(String to) {
    this.to = to;
  }
}
