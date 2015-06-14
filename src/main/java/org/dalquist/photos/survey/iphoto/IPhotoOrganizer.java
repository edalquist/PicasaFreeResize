package org.dalquist.photos.survey.iphoto;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.dalquist.photos.survey.PhotoOrganizer;
import org.dalquist.photos.survey.PhotosDatabase;
import org.dalquist.photos.survey.model.Album;
import org.dalquist.photos.survey.model.Media;
import org.dalquist.photos.survey.model.MediaId;
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
    Map<String, Media> mediaMap = new HashMap<>();
    for (final Entry<String, NSObject> mediaEntry : imagesDict.entrySet()) {
      Media media = transformEntry(mediaEntry.getKey(), mediaEntry.getValue());
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
        Media me = mediaMap.get(nsToString(imageKey));
        if (me == null) {
          throw new IllegalStateException("No image found for: " + imageKey);
        }
        me.getAlbums().add(album);
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
        Media me = mediaMap.get(nsToString(imageKey));
        if (me == null) {
          throw new IllegalStateException("No image found for: " + imageKey);
        }
        me.getAlbums().add(album);

        pdb.writeAlbum(sourceId, album);
      }
    }

    for (Media entry : mediaMap.values()) {
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

  public Media transformEntry(String key, NSObject value) {
    NSDictionary imageDict = (NSDictionary) value;

    MediaId mId = new MediaId();
    mId.setId(key); // GUID?
    mId.setSourceId(sourceId);

    Media m = new Media();
    m.setMediaId(mId);

    // TODO read file data from FS
    // np.setCreated(new DateTime(photoEntry.getTimestamp()));

    // ExifTags exifTags = photoEntry.getExifTags();
    // if (exifTags != null) {
    // np.setExifUniqueId(exifTags.getImageUniqueID());
    // }
    // np.setPhash(phash); TODO calculate this via IM

    // TODO convert to mimeType
    m.setMimeType(nsToString(imageDict.get("MediaType")));

    Set<Resource> resources = m.getResources();
    Resource thumb = createImageResource(nsToString(imageDict.get("ThumbPath")));
    if (thumb != null) {
      thumb.setDescription("Thumb");
      resources.add(thumb);
    }

    if ("Image".equals(m.getMimeType())) {
      Resource original = createImageResource(nsToString(imageDict.get("OriginalPath")));
      Resource image = createImageResource(nsToString(imageDict.get("ImagePath")));
      image.setDescription("Image");
      if (original != null) {
        original.setPrimary(true);
        original.setDescription("Original");
        resources.add(original);
      } else {
        image.setPrimary(true);
      }
      resources.add(image);
    } else {
      System.err.println("Unhandled MediaType: " + m.getMimeType());
      // Assume video?
    }

    return m;
  }

  private Resource createImageResource(String file) {
    if (file == null) {
      return null;
    }
    Resource resource = new Resource();
    resource.setUrl(file);

//    FileType fileType;
//    CountingInputStream countingStream;
//    HashingInputStream hashingStream;
//    Metadata metadata;
//    try (FileInputStream fis = new FileInputStream(file)) {
//      BufferedInputStream bis = new BufferedInputStream(fis);
//      fileType = FileTypeDetector.detectFileType(bis);
//
//      countingStream = new CountingInputStream(bis);
//      hashingStream = new HashingInputStream(Hashing.sha1(), countingStream);
//      metadata = ImageMetadataReader.readMetadata(hashingStream);
//
//      // Read the whole stream to ensure the count and hash data is correct
//      ByteStreams.copy(hashingStream, ByteStreams.nullOutputStream());
//    } catch (ImageProcessingException | IOException e) {
//      throw new RuntimeException("Failed to read: " + file, e);
//    }
//
//    // TODO figure out how to capture this in JSON
//    // for (Directory directory : metadata.getDirectories()) {
//    // for (Tag tag : directory.getTags()) {
//    // System.out.println(tag);
//    // }
//    // }
//
//    // ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
//    // Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
//    try {
//      // TODO do I need to capture this or just capture all metadata?
//      switch (fileType) {
//        case Jpeg: {
//          JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
//          resource.setWidth(jpegDirectory.getInt(JpegDirectory.TAG_IMAGE_WIDTH));
//          resource.setHeight(jpegDirectory.getInt(JpegDirectory.TAG_IMAGE_HEIGHT));
//          break;
//        }
//
//        case Png: {
//          PngDirectory pngDirectory = metadata.getFirstDirectoryOfType(PngDirectory.class);
//          resource.setWidth(pngDirectory.getInt(PngDirectory.TAG_IMAGE_WIDTH));
//          resource.setHeight(pngDirectory.getInt(PngDirectory.TAG_IMAGE_HEIGHT));
//          break;
//        }
//
//        default:
//          System.err.println("Unhandled FileType: " + fileType);
//          break;
//      }
//    } catch (Exception e) {
//      throw new RuntimeException("Failed to read: " + file, e);
//    }
//
//    resource.setBytes(countingStream.getByteCount());
//    // resource.setHeight(0);
//    // resource.setWidth(0);
//    resource.setSha(BaseEncoding.base64Url().encode(hashingStream.hash().asBytes()));
    return resource;
  }
}
