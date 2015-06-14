package org.dalquist.photos.survey.model;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;


public class Resource extends JsonObject implements Comparable<Resource> {
  private String description;
  private String url;
  private boolean primary;
  private long width;
  private long height;
  private long bytes;
  private String sha;
  private String phash;
  // TODO store the JSON from ImageMagick here
  
  @Override
  public int compareTo(Resource o) {
    return ComparisonChain.start()
        .compare(url, o.url, Ordering.natural().nullsLast())
        .result();
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public boolean isPrimary() {
    return primary;
  }

  public void setPrimary(boolean primary) {
    this.primary = primary;
  }

  public long getWidth() {
    return width;
  }

  public void setWidth(long width) {
    this.width = width;
  }

  public long getHeight() {
    return height;
  }

  public void setHeight(long height) {
    this.height = height;
  }

  public long getBytes() {
    return bytes;
  }

  public void setBytes(long bytes) {
    this.bytes = bytes;
  }

  public String getSha() {
    return sha;
  }

  public void setSha(String sha) {
    this.sha = sha;
  }

  public String getPhash() {
    return phash;
  }

  public void setPhash(String phash) {
    this.phash = phash;
  }
}
