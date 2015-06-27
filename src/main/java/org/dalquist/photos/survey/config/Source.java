package org.dalquist.photos.survey.config;

import java.io.IOException;
import java.util.HashMap;

import org.dalquist.photos.survey.ObjectMapperHolder;
import org.dalquist.photos.survey.model.SourceId;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    return get("pathReplacement", PathReplacement.class);
  }

  public void setPathReplacement(PathReplacement pathReplacement) {
    put("pathReplacement", pathReplacement);
  }

  private <T> T get(String property, Class<T> type) {
    Object data = get(property);
    if (data == null) {
      return null;
    }
    if (type.isAssignableFrom(data.getClass())) {
      return type.cast(data);
    }

    ObjectMapper om = ObjectMapperHolder.getObjectMapper();
    try {
      String dataStr = om.writeValueAsString(data);
      T convertedData = om.readValue(dataStr, type);
      put(property, convertedData); // cache the converted data
      return convertedData;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}