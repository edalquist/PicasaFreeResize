package org.dalquist.photos.survey.model;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

public class Album extends JsonObject implements Comparable<Album> {
  private static final long serialVersionUID = 1L;

  private String id;
  private String name;
  private SortedSet<String> images = new TreeSet<>();

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

  public void addImage(Image image) {
    images.add(image.getId());
  }

  public Set<String> getImageIds() {
    return images;
  }
}
