package org.dalquist.photos.survey.model;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

public class Album extends JsonObject implements Comparable<Album> {
  private String id;
  private String name;
  
  @Override
  public int compareTo(Album o) {
    return ComparisonChain.start()
        .compare(id, o.id, Ordering.natural().nullsLast())
        .compare(name, o.name, Ordering.natural().nullsLast())
        .result();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
