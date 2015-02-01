package org.dalquist.picasa;

import java.io.IOException;

import org.joda.time.DateTime;

import com.google.gdata.data.media.mediarss.MediaContent;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.ExifTags;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

public class PicasaPhotoOrganizer {
  public static final String SOURCE = "Picasa";
  private final SimplePicasaServiceImpl picasaService;
  private final String username;

  public PicasaPhotoOrganizer(String username, String password) throws AuthenticationException {
    this.picasaService = new SimplePicasaServiceImpl(username, password);
    this.username = username;
  }

  public void loadPhotoEntries(PhotosDatabase pdb) throws IOException, ServiceException {
    for (AlbumEntry albumEntry : this.picasaService.getAlbums()) {
      String albumId = albumEntry.getGphotoId();
      String albumTitle = albumEntry.getTitle().getPlainText();
      for (PhotoEntry photoEntry : this.picasaService.getPhotos(albumEntry)) {
        MediaEntry np = convert(photoEntry, albumId, albumTitle);
        pdb.add(np);
      }
    }
  }

  MediaEntry convert(PhotoEntry photoEntry, String albumId, String albumTitle)
      throws ServiceException {
    MediaEntry np = new MediaEntry();

    np.setAlbumId(albumId);
    np.setAlbumName(albumTitle);
    np.setBytes(photoEntry.getSize());
    np.setCreated(new DateTime(photoEntry.getTimestamp()));
    np.setFilename(photoEntry.getTitle().getPlainText());
    np.setId(photoEntry.getGphotoId());
    ExifTags exifTags = photoEntry.getExifTags();
    if (exifTags != null) {
      np.setExifUniqueId(exifTags.getImageUniqueID());
    }
    // np.setPhash(phash); TODO calculate this via IM
    np.setSource(SOURCE);
    np.setAccount(username);

    // Find largest image/video to set as the primary
    for (MediaContent content : photoEntry.getMediaContents()) {
      if (content.getWidth() > np.getWidth() || content.getHeight() > np.getHeight()) {
        np.setWidth(content.getWidth());
        np.setHeight(content.getHeight());
        np.setMimeType(content.getType());
        np.setUrl(content.getUrl());
      }
    }

    System.out.println(np);

    return np;
  }

}
