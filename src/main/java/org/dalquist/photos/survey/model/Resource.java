package org.dalquist.photos.survey.model;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;


public class Resource extends JsonObject implements Comparable<Resource> {
  private static final long serialVersionUID = 1L;

  private String url;

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
}
