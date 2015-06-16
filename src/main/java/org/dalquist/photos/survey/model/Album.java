package org.dalquist.photos.survey.model;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;

public class Album extends FirebaseObject implements Comparable<Album> {
  private String id;
  private String name;
  private SortedMap<String, Boolean> images = new TreeMap<>();

  @Override
  public int compareTo(Album o) {
    return ComparisonChain.start()
        .compare(id, o.id, Ordering.natural().nullsLast())
        .compare(name, o.name, Ordering.natural().nullsLast())
        .result();
  }

  @Override
  public Map<String, Object> getFirebaseRepresentation() {
    ImmutableMap.Builder<String, Object> mediaDataBuilder = ImmutableMap.builder();
    mediaDataBuilder.put("name", name);
    mediaDataBuilder.put("images", images);

    return mediaDataBuilder.build();
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
    images.put(image.getId(), Boolean.TRUE);
  }

  public Set<String> getImageIds() {
    return images.keySet();
  }
}
