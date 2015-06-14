package org.dalquist.photos.survey;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

@Service
public final class JacksonUtils {
  private final ResourceLoader resourceLoader;

  @Autowired
  public JacksonUtils(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public <T> T read(String configFile, Class<T> type) throws IOException,
      JsonProcessingException {
    Resource resource = resourceLoader.getResource(configFile);
    try (InputStream in = resource.getInputStream()) {
      return ObjectMapperHolder.getObjectMapper().reader(type).readValue(in);
    }
  }
}
