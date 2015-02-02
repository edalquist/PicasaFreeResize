package org.dalquist.picasa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public class JacksonUtils {
  private JacksonUtils() { }

  public static ObjectMapper getObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    registerModules(mapper);
    return mapper;
  }
  
//  public static CsvMapper getCsvMapper() {
//    CsvMapper mapper = new CsvMapper();
//    registerModules(mapper);
//    return mapper;
//  }

  private static void registerModules(ObjectMapper mapper) {
    mapper.registerModule(new JodaModule());
  }
}
