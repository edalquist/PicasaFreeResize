package org.dalquist.photos.survey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

public final class JacksonUtils {
  private static class LazyHolder {
    private static final JacksonUtils INSTANCE = new JacksonUtils();
  }
  
  public static ObjectMapper getObjectMapper() {
    return LazyHolder.INSTANCE.objectMapper;
  }

  private final ObjectMapper objectMapper;

  private JacksonUtils() {
    objectMapper = new ObjectMapper();
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    objectMapper.registerModule(new JodaModule());
  }
}
