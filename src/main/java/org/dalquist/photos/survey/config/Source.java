package org.dalquist.photos.survey.config;

import java.util.HashMap;

import org.dalquist.photos.survey.model.SourceId;

public class Source extends HashMap<String, String> {
  private static final long serialVersionUID = 1L;

  public SourceId getSourceId() {
    return new SourceId(getType(), getId());
  }

  public String getId() {
    return get("id");
  }

  public void setId(String id) {
    put("id", id);
  }

  public String getType() {
    return get("type");
  }

  public void setType(String type) {
    put("type", type);
  }
}