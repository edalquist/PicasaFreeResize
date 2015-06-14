package org.dalquist.photos.survey.model;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

public class MediaId extends JsonObject implements Comparable<MediaId> {
  private SourceId sourceId;
  private String id;

  @Override
  public int compareTo(MediaId o) {
    return ComparisonChain.start()
        .compare(sourceId, o.sourceId, Ordering.natural().nullsLast())
        .compare(id, o.id, Ordering.natural().nullsLast())
        .result();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public SourceId getSourceId() {
    return sourceId;
  }

  public void setSourceId(SourceId sourceId) {
    this.sourceId = sourceId;
  }
}
