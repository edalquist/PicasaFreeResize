package org.dalquist.photos.survey.model;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

public class MediaId extends JsonObject implements Comparable<MediaId> {
  private String id;
  private String source;
  private String account;
  
  @Override
  public int compareTo(MediaId o) {
    return ComparisonChain.start()
        .compare(source, o.source, Ordering.natural().nullsLast())
        .compare(account, o.account, Ordering.natural().nullsLast())
        .compare(id, o.id, Ordering.natural().nullsLast())
        .result();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }
}
