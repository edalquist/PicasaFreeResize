package org.dalquist.photos.survey.iphoto;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.dalquist.photos.survey.PhotoOrganizer;
import org.dalquist.photos.survey.PhotoProcessor;
import org.dalquist.photos.survey.PhotosDatabase;
import org.dalquist.photos.survey.config.Source;
import org.dalquist.photos.survey.model.Album;
import org.dalquist.photos.survey.model.Image;
import org.dalquist.photos.survey.model.Resource;
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
    this.albumXml = Preconditions.checkNotNull((String) config.get("album.xml"));
  }

  @Override
  public void organizePhotos(PhotoProcessor pp) throws IOException {
    NSDictionary rootDict = readPhotoDict();

    NSDictionary imagesDict = (NSDictionary) rootDict.get("Master Image List");

    // organize each image
    for (final Entry<String, NSObject> mediaEntry : imagesDict.entrySet()) {
      Image media = transformEntry(mediaEntry.getKey(), mediaEntry.getValue());
      if (media != null) {
        pp.processImage(media);
      }
    }
  }

  @Override
  public void loadPhotoEntries(PhotosDatabase pdb) throws IOException {
    NSDictionary rootDict = readPhotoDict();

    NSDictionary imagesDict = (NSDictionary) rootDict.get("Master Image List");

    Map<String, Image> imageMap = new TreeMap<>();
    // Convert each image
    for (final Entry<String, NSObject> mediaEntry : imagesDict.entrySet()) {
      Image media = transformEntry(mediaEntry.getKey(), mediaEntry.getValue());
      if (media != null) {
        imageMap.put(media.getId(), media);
      }
    }

    NSArray albumsArray = (NSArray) rootDict.get("List of Albums");
    for (NSObject albumObj : albumsArray.getArray()) {
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
        Image img = imageMap.get(nsToString(imageKey));
        if (img == null) {
          continue;
//          throw new IllegalStateException("No image found for: " + imageKey);
        }
        img.addAlbum(album);
        album.addImage(img);
      }
//      pdb.writeAlbum(sourceId, album);
    }

    NSArray rollsArray = (NSArray) rootDict.get("List of Rolls");
    for (NSObject rollObj : rollsArray.getArray()) {
      NSDictionary roll = (NSDictionary) rollObj;

      Album album = new Album();
      album.setId(nsToInt(roll.get("RollID")).toString());
      album.setName(nsToString(roll.get("RollName")));

      NSArray imageKeys = (NSArray) roll.get("KeyList");
      for (NSObject imageKey : imageKeys.getArray()) {
        Image img = imageMap.get(nsToString(imageKey));
        if (img == null) {
          continue;
//          throw new IllegalStateException("No image found for: " + imageKey);
        }
        img.addAlbum(album);
        album.addImage(img);
      }
//      pdb.writeAlbum(sourceId, album);
    }

//    pdb.writeImages(sourceId, imageMap.values());
  }

  private NSDictionary readPhotoDict() throws IOException, FileNotFoundException {
    try (InputStream is = new BufferedInputStream(new FileInputStream(albumXml))) {
      return (NSDictionary) PropertyListParser.parse(is);
    } catch (PropertyListFormatException | ParseException | ParserConfigurationException
        | SAXException e) {
      throw new IOException(e);
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

    Image m = new Image();
    m.setId(key); // GUID?

    boolean isImage = "Image".equals(nsToString(imageDict.get("MediaType")));

    Resource thumb = createImageResource(nsToString(imageDict.get("ThumbPath")));
    if (thumb != null) {
      m.setThumb(thumb);
    }

    if (isImage) {
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
      // TODO videos too
      // Assume video?
      return null;
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
