package org.dalquist.photos.survey.picasa;

import java.io.IOException;

import org.dalquist.photos.survey.PhotoOrganizer;
import org.dalquist.photos.survey.PhotosDatabase;
import org.dalquist.photos.survey.model.Image;
import org.dalquist.photos.survey.model.MediaId;
import org.dalquist.photos.survey.model.Source;
import org.dalquist.photos.survey.model.SourceId;

import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

public class PicasaPhotoOrganizer implements PhotoOrganizer {
  public static final String SOURCE = "Picasa";

  private final SimplePicasaServiceImpl picasaService;
  private final SourceId sourceId;

  public PicasaPhotoOrganizer(Source config) throws AuthenticationException {
    this.sourceId = config.getSourceId();

    String username = config.get("username");
    String password = config.get("password");
    this.picasaService = new SimplePicasaServiceImpl(username, password);
  }

  @Override
  public void loadPhotoEntries(PhotosDatabase pdb) throws IOException {
    try {
      for (AlbumEntry albumEntry : this.picasaService.getAlbums()) {
        String albumId = albumEntry.getGphotoId();
        String albumTitle = albumEntry.getTitle().getPlainText();
        for (PhotoEntry photoEntry : this.picasaService.getPhotos(albumEntry)) {
          Image np = convert(photoEntry, albumId, albumTitle);
          pdb.writeImage(np);
        }
      }
    } catch (ServiceException e) {
      throw new IOException(e);
    }
  }

  Image convert(PhotoEntry photoEntry, String albumId, String albumTitle)
      throws ServiceException {
    MediaId mId = new MediaId();
    mId.setId(photoEntry.getGphotoId());
    mId.setSourceId(sourceId);

    Image m = new Image();
    m.setMediaId(mId);

//    Album album = new Album();
//    album.setAlbumId(albumId);
//    album.setAlbumName(albumTitle);
//    m.getAlbums().add(album);
//
//    m.setBytes(photoEntry.getSize());
//    m.setCreated(new DateTime(photoEntry.getTimestamp()));
//    m.setFilename(photoEntry.getTitle().getPlainText());
//    ExifTags exifTags = photoEntry.getExifTags();
//    if (exifTags != null) {
//      m.setExifUniqueId(exifTags.getImageUniqueID());
//    }
//    // np.setPhash(phash); TODO calculate this via IM
//    
//    // Find largest thumbnail
//    long tWidth = 0, tHeight = 0;
//    for (MediaThumbnail mediaThumbnail : photoEntry.getMediaThumbnails()) {
//      if (mediaThumbnail.getWidth() > tWidth || mediaThumbnail.getHeight() > tHeight) {
//        tWidth = mediaThumbnail.getWidth();
//        tHeight = mediaThumbnail.getHeight();
//        m.setThumbUrl(mediaThumbnail.getUrl());
//      }
//    }
//    
//    // Find largest image/video to set as the primary
//    for (MediaContent content : photoEntry.getMediaContents()) {
//      if (content.getWidth() > m.getWidth() || content.getHeight() > m.getHeight()) {
//        m.setWidth(content.getWidth());
//        m.setHeight(content.getHeight());
//        m.setMimeType(content.getType());
//        m.setUrl(content.getUrl());
//      }
//    }

    return m;
  }
}
