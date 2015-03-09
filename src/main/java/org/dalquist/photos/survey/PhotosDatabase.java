package org.dalquist.photos.survey;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import com.google.common.io.Files;

public final class PhotosDatabase {
  private SortedSet<MediaEntry> photos = new TreeSet<MediaEntry>();
  private final File dbFile;

  public PhotosDatabase(String filename) throws IOException {
    dbFile = new File(filename);
    load();
  }

  public void add(MediaEntry entry) {
    // Perform replacement of existing entries
    photos.remove(entry);
    photos.add(entry);
  }
  
  public void load() throws IOException {
    if (!dbFile.exists()) {
      return;
    }

    try (Reader in = new FileReader(dbFile)) {
      ObjectMapper objectMapper = JacksonUtils.getObjectMapper();
      
      MappingIterator<MediaEntry> mediaEntryItr =
          objectMapper.reader(MediaEntry.class).readValues(in);

      List<MediaEntry> photosBuilder = new LinkedList<>();
      Iterators.addAll(photosBuilder, mediaEntryItr);
      photos = new TreeSet<>(photosBuilder);
      
      int dupeCount = photosBuilder.size() - photos.size();
      if (dupeCount > 0) {
        throw new IllegalStateException("The photosDb file contains duplicate " + dupeCount + "entries!");
      }

      System.out.println("Loaded " + photos.size() + " photos");
    }
  }
  
  public void save() throws IOException {
    File tempFile = File.createTempFile("pdb_", "_tmp.json");
    tempFile.deleteOnExit();
    try {
      try (Writer out = new FileWriter(tempFile)) {
        ObjectMapper objectMapper = JacksonUtils.getObjectMapper();
        objectMapper.writer().writeValue(out, photos);
      }
      Files.move(tempFile, dbFile);
    } finally {
      tempFile.delete();
    }
  }
  
  static class DatabaseRoot {
    private SortedSet<MediaEntry> mediaEntries;
  }
}
