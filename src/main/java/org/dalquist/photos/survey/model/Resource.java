package org.dalquist.photos.survey.model;

import java.util.Map;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;


public class Resource extends JsonObject implements Comparable<Resource> {
  private static final long serialVersionUID = 1L;

  private String url;
  private String destFile;
  private String destMetadataFile;

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

  public String getDestFile() {
    return destFile;
  }

  public void setDestFile(String destFile) {
    this.destFile = destFile;
  }

  public String getDestMetadataFile() {
    return destMetadataFile;
  }

  public void setDestMetadataFile(String destMetadataFile) {
    this.destMetadataFile = destMetadataFile;
  }
}
