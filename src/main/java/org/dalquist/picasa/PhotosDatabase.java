package org.dalquist.picasa;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class PhotosDatabase {
  private final List<MediaEntry> photos = new LinkedList<>();

  public void add(MediaEntry entry) {
    photos.add(entry);
  }
  
  public void writeAsCsv(Writer out) throws IOException {
    CsvMapper csvMapper = JacksonUtils.getCsvMapper();
    CsvSchema mediaEntrySchema = csvMapper.schemaFor(MediaEntry.class).withHeader();
    csvMapper.writer(mediaEntrySchema).writeValue(out, photos);
  }
}
