package org.dalquist.photos.survey.model;

import java.util.Map;

public abstract class PersistedObject extends JsonObject {
  public abstract Map<String, Object> getCollectionRepresentation();
}
