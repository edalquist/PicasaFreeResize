package org.dalquist.photos.survey.model;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;

public class Image extends FirebaseObject implements Comparable<Image> {
  private String id;
  private Resource original;
  private Resource modified;
  private Resource thumb;
  private SortedMap<String, Boolean> albums = new TreeMap<>();

  @Override
  public int compareTo(Image o) {
    return ComparisonChain.start()
        .compare(id, o.id, Ordering.natural().nullsLast())
        .result();
  }

  @Override
  public Map<String, Object> getFirebaseRepresentation() {
    ImmutableMap.Builder<String, Object> mediaDataBuilder = ImmutableMap.builder();
    if (original != null) {
      mediaDataBuilder.put("original", original.getFirebaseRepresentation());
    }
    if (modified != null) {
      mediaDataBuilder.put("modified", modified.getFirebaseRepresentation());
    }
    if (modified != null) {
      mediaDataBuilder.put("thumb", modified.getFirebaseRepresentation());
    }
    mediaDataBuilder.put("albums", albums);

    return mediaDataBuilder.build();
  }

  public Resource getOriginal() {
    return original;
  }

  public void setOriginal(Resource original) {
    this.original = original;
  }

  public Resource getModified() {
    return modified;
  }

  public void setModified(Resource modified) {
    this.modified = modified;
  }

  public Resource getThumb() {
    return thumb;
  }

  public void setThumb(Resource thumb) {
    this.thumb = thumb;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Set<String> getAlbumIds() {
    return albums.keySet();
  }

  public void addAlbum(Album album) {
    albums.put(album.getId(), Boolean.TRUE);
  }
}
