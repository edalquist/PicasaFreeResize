package org.dalquist.photos.survey;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import org.dalquist.photos.survey.model.Media;
import org.dalquist.photos.survey.model.MediaId;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.common.io.Files;

public final class PhotosDatabase {
  private SortedMap<MediaId, Media> media = new TreeMap<>();
  private SortedSetMultimap<String, MediaId> mediaHashes = TreeMultimap.create();
  private final File dbFile;

  public PhotosDatabase(String filename) throws IOException {
    dbFile = new File(filename);
    load();
  }

  public void add(Media entry) {
    // Perform replacement of existing entries
    media.put(entry.getMediaId(), entry);
  }
  
  public void load() throws IOException {
    if (!dbFile.exists()) {
      return;
    }

//    try (Reader in = new FileReader(dbFile)) {
//      ObjectMapper objectMapper = JacksonUtils.getObjectMapper();
//      
//      MappingIterator<Media> mediaEntryItr =
//          objectMapper.reader(Media.class).readValues(in);
//
//      List<Media> photosBuilder = new LinkedList<>();
//      Iterators.addAll(photosBuilder, mediaEntryItr);
//      photos = new TreeSet<>(photosBuilder);
//      
//      int dupeCount = photosBuilder.size() - photos.size();
//      if (dupeCount > 0) {
//        throw new IllegalStateException("The photosDb file contains duplicate " + dupeCount + "entries!");
//      }
//
//      System.out.println("Loaded " + photos.size() + " photos");
//    }
  }
  
  public void save() throws IOException {
    Database db = new Database();
    db.media = media.values();
    
    File tempFile = File.createTempFile("pdb_", "_tmp.json");
    tempFile.deleteOnExit();
    try {
      try (Writer out = new FileWriter(tempFile)) {
        ObjectMapper objectMapper = JacksonUtils.getObjectMapper();
        objectMapper.writer().writeValue(out, db);
      }
      Files.move(tempFile, dbFile);
    } finally {
      tempFile.delete();
    }
    
//    try (OutputStream out = new FileOutputStream(new File(dbFile.getAbsolutePath() + ".cbor"))) {
//      ObjectMapper objectMapper = JacksonUtils.getCBORObjectMapper();
//      objectMapper.writer().writeValue(out, db);
//    }
  }
  
  static class Database {
    private Collection<Media> media;

    public Collection<Media> getMedia() {
      return media;
    }

    public void setMedia(Collection<Media> media) {
      this.media = media;
    }
  }
}
