package org.dalquist.photos.survey.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

@JsonTypeInfo(use = Id.MINIMAL_CLASS)
public class SourceId extends JsonObject implements Comparable<SourceId> {
  private static final long serialVersionUID = 1L;

  private String type;
  private String id;

  public SourceId() {
  }

  public SourceId(String type, String id) {
    this.type = type;
    this.id = id;
  }

  @Override
  public int compareTo(SourceId o) {
    return ComparisonChain.start()
        .compare(type, o.type, Ordering.natural().nullsLast())
        .compare(id, o.id, Ordering.natural().nullsLast())
        .result();
  }

  public String getType() {
    return type;
  }

  public void setType(String source) {
    this.type = source;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
