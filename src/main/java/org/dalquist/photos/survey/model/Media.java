package org.dalquist.photos.survey.model;

import java.util.LinkedHashSet;
import java.util.Set;

import org.joda.time.DateTime;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

public class Media extends JsonObject implements Comparable<Media> {
  private MediaId mediaId;
  private Set<Album> albums;
  private Set<Resource> resources;
  private String title;
  private DateTime created;
  private String mimeType;
  
  @Override
  public int compareTo(Media o) {
    return ComparisonChain.start()
        .compare(mediaId, o.mediaId, Ordering.natural().nullsLast())
        .result();
  }
  
  public Set<Album> getAlbums() {
    if (albums == null) {
      albums = new LinkedHashSet<>();
    }
    return albums;
  }

  public void setAlbums(Set<Album> albums) {
    this.albums = albums;
  }

  public Set<Resource> getResources() {
    if (resources == null) {
      resources = new LinkedHashSet<>();
    }
    return resources;
  }

  public void setResources(Set<Resource> resources) {
    this.resources = resources;
  }

  public MediaId getMediaId() {
    return mediaId;
  }

  public void setMediaId(MediaId mediaId) {
    this.mediaId = mediaId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public DateTime getCreated() {
    return created;
  }

  public void setCreated(DateTime created) {
    this.created = created;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }
}
