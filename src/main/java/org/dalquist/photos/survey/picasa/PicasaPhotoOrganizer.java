package org.dalquist.photos.survey.picasa;

import java.io.IOException;

import org.dalquist.photos.survey.Album;
import org.dalquist.photos.survey.MediaEntry;
import org.dalquist.photos.survey.PhotoOrganizer;
import org.dalquist.photos.survey.PhotoSurveyRunner.Source;
import org.dalquist.photos.survey.PhotosDatabase;
import org.joda.time.DateTime;

import com.google.common.base.Preconditions;
import com.google.gdata.data.media.mediarss.MediaContent;
import com.google.gdata.data.media.mediarss.MediaThumbnail;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.ExifTags;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

public class PicasaPhotoOrganizer implements PhotoOrganizer {
  public static final String SOURCE = "Picasa";

  private final SimplePicasaServiceImpl picasaService;
  private final String id;

  public PicasaPhotoOrganizer(Source config) throws AuthenticationException {
    this.id = Preconditions.checkNotNull(config.getId());

    String username = config.get("username");
    String password = config.get("password");
    this.picasaService = new SimplePicasaServiceImpl(username, password);
  }

  public PicasaPhotoOrganizer(String username, String password) throws AuthenticationException {
    this.id = SOURCE;
    this.picasaService = new SimplePicasaServiceImpl(username, password);
  }

  @Override
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

    Album album = new Album();
    album.setAlbumId(albumId);
    album.setAlbumName(albumTitle);
    np.getAlbums().add(album);

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
    np.setAccount(id);
    
    // Find largest thumbnail
    long tWidth = 0, tHeight = 0;
    for (MediaThumbnail mediaThumbnail : photoEntry.getMediaThumbnails()) {
      if (mediaThumbnail.getWidth() > tWidth || mediaThumbnail.getHeight() > tHeight) {
        tWidth = mediaThumbnail.getWidth();
        tHeight = mediaThumbnail.getHeight();
        np.setThumbUrl(mediaThumbnail.getUrl());
      }
    }

    // Find largest image/video to set as the primary
    for (MediaContent content : photoEntry.getMediaContents()) {
      if (content.getWidth() > np.getWidth() || content.getHeight() > np.getHeight()) {
        np.setWidth(content.getWidth());
        np.setHeight(content.getHeight());
        np.setMimeType(content.getType());
        np.setUrl(content.getUrl());
      }
    }

    return np;
  }
}
