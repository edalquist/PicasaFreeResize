package org.dalquist.photos.survey;

import com.fasterxml.jackson.core.JsonProcessingException;

public class Album {
  private String albumId;
  private String albumName;

  public String getAlbumId() {
    return albumId;
  }
  public void setAlbumId(String albumId) {
    this.albumId = albumId;
  }
  public String getAlbumName() {
    return albumName;
  }
  public void setAlbumName(String albumName) {
    this.albumName = albumName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((albumId == null) ? 0 : albumId.hashCode());
    result = prime * result + ((albumName == null) ? 0 : albumName.hashCode());
    return result;
  }
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Album other = (Album) obj;
    if (albumId == null) {
      if (other.albumId != null)
        return false;
    } else if (!albumId.equals(other.albumId))
      return false;
    if (albumName == null) {
      if (other.albumName != null)
        return false;
    } else if (!albumName.equals(other.albumName))
      return false;
    return true;
  }

  @Override
  public String toString() {
    try {
      return JacksonUtils.getObjectMapper().writer().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      return super.toString();
    }
  }
}
