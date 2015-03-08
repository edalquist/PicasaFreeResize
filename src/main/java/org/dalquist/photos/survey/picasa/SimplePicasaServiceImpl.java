package org.dalquist.photos.survey.picasa;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.gdata.client.photos.PicasawebService;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.AlbumFeed;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.data.photos.UserFeed;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

public class SimplePicasaServiceImpl {
  // Setup the Picasa service API
  final PicasawebService picasaService;

  public SimplePicasaServiceImpl(String username, String password) throws AuthenticationException {
    Preconditions.checkNotNull(username);
    Preconditions.checkNotNull(password);
    picasaService = new PicasawebService("bulk-photo-utils");
    picasaService.setUserCredentials(username, password);
  }

  public List<AlbumEntry> getAlbums() throws IOException, ServiceException {
    // Get the album feed
    final URL feedUrl =
        new URL("https://picasaweb.google.com/data/feed/api/user/default?kind=album");
    final UserFeed myUserFeed = picasaService.getFeed(feedUrl, UserFeed.class);
    List<AlbumEntry> albumEntries = myUserFeed.getAlbumEntries();
    Collections.sort(albumEntries, new Comparator<AlbumEntry>() {
      @Override
      public int compare(AlbumEntry o1, AlbumEntry o2) {
        return o1.getDate().compareTo(o2.getDate());
      }
    });
    return albumEntries;
  }

  public List<PhotoEntry> getPhotos(AlbumEntry albumEntry) throws IOException, ServiceException {
    Integer photoCount = albumEntry.getPhotosUsed();
    List<PhotoEntry> totalEntries = new ArrayList<>(photoCount);
    AlbumFeed albumFeed;
    do {
      final URL albumFeedUrl =
          new URL("https://picasaweb.google.com/data/feed/api/user/default/albumid/"
              + albumEntry.getGphotoId() + "?imgmax=d&start-index=" + (totalEntries.size() + 1));

      albumFeed = picasaService.getFeed(albumFeedUrl, AlbumFeed.class);
      List<PhotoEntry> photoEntries = albumFeed.getPhotoEntries();
      totalEntries.addAll(photoEntries);
    } while (albumFeed.getTotalResults() == albumFeed.getItemsPerPage());

    Collections.sort(totalEntries, new Comparator<PhotoEntry>() {
      @Override
      public int compare(PhotoEntry o1, PhotoEntry o2) {
        try {
          return o1.getTimestamp().compareTo(o2.getTimestamp());
        } catch (ServiceException e) {
          throw Throwables.propagate(e);
        }
      }
    });

    return totalEntries;
  }
}
