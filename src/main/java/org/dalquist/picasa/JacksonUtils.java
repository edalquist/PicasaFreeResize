package org.dalquist.picasa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public class JacksonUtils {
  private JacksonUtils() { }

  public static ObjectMapper getObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    registerModules(mapper);
    return mapper;
  }
  
  public static CsvMapper getCsvMapper() {
    CsvMapper mapper = new CsvMapper();
    registerModules(mapper);
    return mapper;
  }

  private static void registerModules(ObjectMapper mapper) {
    mapper.registerModule(new JodaModule());
  }
}
