package org.dalquist.picasa;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class PhotosDatabase {
  private List<MediaEntry> photos = new LinkedList<>();
  private final File dbFile;

  public PhotosDatabase(String filename) throws IOException {
    dbFile = new File(filename);
    load();
  }

  public void add(MediaEntry entry) {
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

      LinkedList<MediaEntry> photosBuilder = new LinkedList<>();
      Iterators.addAll(photosBuilder, mediaEntryItr);
      photos = photosBuilder;

      System.out.println("Loaded " + photos.size() + " photos");
    }
  }
  
  public void save() throws IOException {
    try (Writer out = new FileWriter(dbFile)) {
      ObjectMapper objectMapper = JacksonUtils.getObjectMapper();
      objectMapper.writer().writeValue(out, photos);
    }
  }

//  private CsvSchema getSchema(CsvMapper csvMapper) {
//    return csvMapper.schemaFor(MediaEntry.class).withHeader();
//  }
}
