package org.dalquist.photos.survey.model;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

public class Image extends JsonObject implements Comparable<Image> {
  private MediaId mediaId;
  private Resource original;
  private Resource modified;
  private Resource thumb;

  @Override
  public int compareTo(Image o) {
    return ComparisonChain.start()
        .compare(mediaId, o.mediaId, Ordering.natural().nullsLast())
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

  public MediaId getMediaId() {
    return mediaId;
  }

  public void setMediaId(MediaId mediaId) {
    this.mediaId = mediaId;
  }
}
