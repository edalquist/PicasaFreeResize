package org.dalquist.photos.survey.config;

import java.util.HashMap;

import org.dalquist.photos.survey.model.SourceId;

public class Source extends HashMap<String, Object> {
  private static final long serialVersionUID = 1L;

  public SourceId getSourceId() {
    return new SourceId(getType(), getId());
  }

  public String getId() {
    return (String) get("id");
  }

  public void setId(String id) {
    put("id", id);
  }

  public String getType() {
    return (String) get("type");
  }

  public void setType(String type) {
    put("type", type);
  }
  
  public PathReplacement getPathReplacement() {
    return (PathReplacement) get("pathReplacement");
  }
  
  public void setPathReplacement(PathReplacement pathReplacement) {
    put("pathReplacement", pathReplacement);
  }
}