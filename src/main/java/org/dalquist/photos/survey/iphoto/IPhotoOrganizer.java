package org.dalquist.photos.survey.iphoto;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.dalquist.photos.survey.Album;
import org.dalquist.photos.survey.MediaEntry;
import org.dalquist.photos.survey.PhotoOrganizer;
import org.dalquist.photos.survey.PhotoSurveyRunner.Source;
import org.dalquist.photos.survey.PhotosDatabase;
import org.xml.sax.SAXException;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Maps.EntryTransformer;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

public class IPhotoOrganizer implements PhotoOrganizer {
  public static final String SOURCE = "iPhoto";

  private final String id;
  private final String albumXml;

  public IPhotoOrganizer(Source config) throws AuthenticationException {
    this.id = Preconditions.checkNotNull(config.getId());
    this.albumXml = Preconditions.checkNotNull(config.get("album.xml"));
  }

  @Override
  public void loadPhotoEntries(PhotosDatabase pdb) throws IOException, ServiceException {
    NSDictionary rootDict;
    try (InputStream is = new BufferedInputStream(new FileInputStream(albumXml))) {
      rootDict = (NSDictionary) PropertyListParser.parse(is);
    } catch (PropertyListFormatException | ParseException | ParserConfigurationException
        | SAXException e) {
      throw new IOException(e);
    }

    NSDictionary imagesDict = (NSDictionary) rootDict.get("Master Image List");

    Map<String, MediaEntry> mediaMap =
        ImmutableMap.copyOf(Maps.transformEntries(imagesDict, new MediaEntryBuilder()));

    NSArray albums = (NSArray) rootDict.get("List of Albums");
    for (NSObject albumObj : albums.getArray()) {
      NSDictionary nsAlbum = (NSDictionary) albumObj;

      String albumType = nsAlbum.get("Album Type").toString();
      if (!"Event".equals(albumType) && !"Regular".equals(albumType)) {
        // Skip special albums
        continue;
      }

      Album album = new Album();
      album.setAlbumId(nsToInt(nsAlbum.get("AlbumId")).toString());
      album.setAlbumName(nsToString(nsAlbum.get("AlbumName")));

      NSArray imageKeys = (NSArray) nsAlbum.get("KeyList");
      for (NSObject imageKey : imageKeys.getArray()) {
        MediaEntry me = mediaMap.get(nsToString(imageKey));
        if (me == null) {
          // WTF
        } else {
          me.getAlbums().add(album);
        }
      }
    }

    NSArray rolls = (NSArray) rootDict.get("List of Rolls");
    for (NSObject rollObj : rolls.getArray()) {
      NSDictionary roll = (NSDictionary) rollObj;

      Album album = new Album();
      album.setAlbumId(nsToInt(roll.get("RollID")).toString());
      album.setAlbumName(nsToString(roll.get("RollName")));

      NSArray imageKeys = (NSArray) roll.get("KeyList");
      for (NSObject imageKey : imageKeys.getArray()) {
        MediaEntry me = mediaMap.get(nsToString(imageKey));
        if (me == null) {
          // WTF
        } else {
          me.getAlbums().add(album);
        }
      }
    }

    for (MediaEntry entry : mediaMap.values()) {
      pdb.add(entry);
    }
  }

  private static Integer nsToInt(NSObject obj) {
    return nsConvert(obj, Integer.class);
  }

  private static String nsToString(NSObject obj) {
    return nsConvert(obj, String.class);
  }

  private static <T> T nsConvert(NSObject obj, Class<T> type) {
    return obj == null ? null : type.cast(obj.toJavaObject());
  }

  private final class MediaEntryBuilder implements EntryTransformer<String, NSObject, MediaEntry> {
    @Override
    public MediaEntry transformEntry(String key, NSObject value) {
      NSDictionary imageDict = (NSDictionary) value;
      MediaEntry np = new MediaEntry();

      // np.setBytes(imageDict.);
      // np.setCreated(new DateTime(photoEntry.getTimestamp()));
      np.setId(key);
      // ExifTags exifTags = photoEntry.getExifTags();
      // if (exifTags != null) {
      // np.setExifUniqueId(exifTags.getImageUniqueID());
      // }
      // np.setPhash(phash); TODO calculate this via IM
      np.setSource(SOURCE);
      np.setAccount(id);

      np.setThumbUrl(nsToString(imageDict.get("ThumbPath")));

      // np.setWidth(content.getWidth());
      // np.setHeight(content.getHeight());

      // TODO convert to mimeType
      np.setMimeType(nsToString(imageDict.get("MediaType")));

      String path = nsToString(imageDict.get("OriginalPath"));
      if (path == null) {
        path = nsToString(imageDict.get("ImagePath"));
      }
      np.setUrl(path);
      np.setFilename(path); // TODO basename the path

      return np;
    }
  }
}
