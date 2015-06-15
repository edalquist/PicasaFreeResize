package org.dalquist.photos.survey.iphoto;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;

import org.dalquist.photos.survey.PhotoOrganizer;
import org.dalquist.photos.survey.PhotosDatabase;
import org.dalquist.photos.survey.model.Album;
import org.dalquist.photos.survey.model.Image;
import org.dalquist.photos.survey.model.MediaId;
import org.dalquist.photos.survey.model.MediaType;
import org.dalquist.photos.survey.model.Resource;
import org.dalquist.photos.survey.model.Source;
import org.dalquist.photos.survey.model.SourceId;
import org.xml.sax.SAXException;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListFormatException;
import com.dd.plist.PropertyListParser;
import com.google.common.base.Preconditions;

public class IPhotoOrganizer implements PhotoOrganizer {
  private final SourceId sourceId;
  private final String albumXml;

  public IPhotoOrganizer(Source config) {
    this.sourceId = config.getSourceId();
    this.albumXml = Preconditions.checkNotNull(config.get("album.xml"));
  }

  @Override
  public void loadPhotoEntries(PhotosDatabase pdb) throws IOException {
    NSDictionary rootDict;
    try (InputStream is = new BufferedInputStream(new FileInputStream(albumXml))) {
      rootDict = (NSDictionary) PropertyListParser.parse(is);
    } catch (PropertyListFormatException | ParseException | ParserConfigurationException
        | SAXException e) {
      throw new IOException(e);
    }

    NSDictionary imagesDict = (NSDictionary) rootDict.get("Master Image List");

    // Convert each image to a Media
    Map<String, Image> mediaMap = new HashMap<>();
    for (final Entry<String, NSObject> mediaEntry : imagesDict.entrySet()) {
      Image media = transformEntry(mediaEntry.getKey(), mediaEntry.getValue());
      pdb.writeImage(media);
      mediaMap.put(media.getMediaId().getId(), media);
    }

    NSArray albums = (NSArray) rootDict.get("List of Albums");
    for (NSObject albumObj : albums.getArray()) {
      NSDictionary nsAlbum = (NSDictionary) albumObj;

      String albumType = nsAlbum.get("Album Type").toString();
      if (!"Event".equals(albumType) && !"Regular".equals(albumType)) {
        // Skip special albums
        continue;
      }

      Album album = new Album();
      album.setId(nsToInt(nsAlbum.get("AlbumId")).toString());
      album.setName(nsToString(nsAlbum.get("AlbumName")));

      NSArray imageKeys = (NSArray) nsAlbum.get("KeyList");
      for (NSObject imageKey : imageKeys.getArray()) {
        Image me = mediaMap.get(nsToString(imageKey));
        if (me == null) {
          throw new IllegalStateException("No image found for: " + imageKey);
        }
//        pdb.putMediaInAlbum(sourceId, album, me);
      }

      pdb.writeAlbum(sourceId, album);
    }

    NSArray rolls = (NSArray) rootDict.get("List of Rolls");
    for (NSObject rollObj : rolls.getArray()) {
      NSDictionary roll = (NSDictionary) rollObj;

      Album album = new Album();
      album.setId(nsToInt(roll.get("RollID")).toString());
      album.setName(nsToString(roll.get("RollName")));

      NSArray imageKeys = (NSArray) roll.get("KeyList");
      for (NSObject imageKey : imageKeys.getArray()) {
        Image me = mediaMap.get(nsToString(imageKey));
        if (me == null) {
          throw new IllegalStateException("No image found for: " + imageKey);
        }
//        pdb.putImageInAlbum(sourceId, album, me);

        pdb.writeAlbum(sourceId, album);
      }
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

  public Image transformEntry(String key, NSObject value) {
    NSDictionary imageDict = (NSDictionary) value;

    MediaId mId = new MediaId();
    mId.setId(key); // GUID?
    mId.setSourceId(sourceId);

    Image m = new Image();
    m.setMediaId(mId);

    MediaType mediaType =
        "Image".equals(nsToString(imageDict.get("MediaType"))) ? MediaType.IMAGE : MediaType.VIDEO;

    Resource thumb = createImageResource(nsToString(imageDict.get("ThumbPath")));
    if (thumb != null) {
      m.setThumb(thumb);
    }

    if (mediaType == MediaType.IMAGE) {
      Resource original = createImageResource(nsToString(imageDict.get("OriginalPath")));
      Resource image = createImageResource(nsToString(imageDict.get("ImagePath")));
      if (original != null) {
        // If original exists image must be a modified version
        m.setOriginal(original);
        m.setModified(image);
      } else {
        m.setOriginal(image);
      }
    } else {
      System.err.println("Unhandled MediaType: " + mediaType);
      // Assume video?
    }

    return m;
  }

  private Resource createImageResource(String file) {
    if (file == null) {
      return null;
    }
    Resource resource = new Resource();
    resource.setUrl("file:" + file);
    return resource;
  }
}
