package org.dalquist.photos.survey.model;

import java.util.Map;

public abstract class FirebaseObject extends JsonObject {
  public abstract Map<String, Object> getFirebaseRepresentation();
}
