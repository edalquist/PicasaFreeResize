package org.dalquist.photos.survey.model;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

public class Image extends JsonObject implements Comparable<Image> {
  private static final long serialVersionUID = 1L;

  private String id;
  private Resource original;
  private Resource modified;
  private Resource thumb;
  private SortedSet<String> albums = new TreeSet<>();

  @Override
  public int compareTo(Image o) {
    return ComparisonChain.start()
        .compare(id, o.id, Ordering.natural().nullsLast())
        .result();
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
    return albums;
  }

  public void addAlbum(Album album) {
    albums.add(album.getId());
  }
}
