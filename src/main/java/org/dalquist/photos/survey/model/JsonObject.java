package org.dalquist.photos.survey.model;

import java.io.Serializable;

import org.dalquist.photos.survey.ObjectMapperHolder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class JsonObject implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final ObjectMapper OBJECT_MAPPER = ObjectMapperHolder.getObjectMapper();

  protected final JsonNode getJson() {
    return OBJECT_MAPPER.convertValue(this, JsonNode.class);
  }

  @Override
  public final int hashCode() {
    return getJson().hashCode();
  }

  @Override
  public final boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!obj.getClass().equals(obj.getClass())) {
      return false;
    }
    return getJson().equals(((JsonObject)obj).getJson());
  }

  @Override
  public final String toString() {
    return getJson().toString();
  }
}
