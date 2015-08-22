package org.dalquist.photos.survey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ObjectMapperHolder {
  private static class LazyHolder {
    private static final ObjectMapperHolder INSTANCE = new ObjectMapperHolder();
  }
  
  public static ObjectMapper getObjectMapper() {
    return LazyHolder.INSTANCE.objectMapper;
  }

  private final ObjectMapper objectMapper;

  private ObjectMapperHolder() {
    objectMapper = new ObjectMapper();
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
//    objectMapper.registerModule(new JodaModule());
  }
}
