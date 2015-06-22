package org.dalquist.photos.survey.model;

import java.util.Map;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;


public class Resource extends PersistedObject implements Comparable<Resource> {
  private String url;

  @Override
  public int compareTo(Resource o) {
    return ComparisonChain.start()
        .compare(url, o.url, Ordering.natural().nullsLast())
        .result();
  }

  @Override
  public Map<String, Object> getCollectionRepresentation() {
    return ImmutableMap.of("url", url);
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
