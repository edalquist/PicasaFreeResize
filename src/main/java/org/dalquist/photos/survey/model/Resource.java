package org.dalquist.photos.survey.model;

import java.util.Map;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;


public class Resource extends JsonObject implements Comparable<Resource> {
  private static final long serialVersionUID = 1L;

  private String url;
  private Map<String, Object> metadata;

  @Override
  public int compareTo(Resource o) {
    return ComparisonChain.start()
        .compare(url, o.url, Ordering.natural().nullsLast())
        .result();
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public synchronized Map<String, Object> getMetadata() {
    return metadata;
  }

  public synchronized void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }
}
