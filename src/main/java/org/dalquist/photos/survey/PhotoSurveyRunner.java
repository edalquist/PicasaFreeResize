package org.dalquist.photos.survey;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.dalquist.photos.survey.picasa.PicasaPhotoOrganizer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

public class PhotoSurveyRunner {
  private final static ImmutableMap<String, Class<? extends PhotoOrganizer>> SOURCES = ImmutableMap
      .of(PicasaPhotoOrganizer.SOURCE, PicasaPhotoOrganizer.class);

  public static void main(String[] args) throws Exception {
    JsonNode config = readConfig();

    // Load the photoDb
    String photoDbFile = config.get("photodbfile").textValue();
    PhotosDatabase pdb = new PhotosDatabase(photoDbFile);
    
    // Iterate through sources
    for (JsonNode source : config.get("sources")) {
      PhotoOrganizer sourceOrganizer = createOrganizer(source);
      System.out.println("Parsing photos from: " + source.get("id").textValue());
      
      sourceOrganizer.loadPhotoEntries(pdb);
      
      // Save the db after each source
      System.out.println("Parsed photos from: " + source.get("id").textValue());
      pdb.save();
    }
  }

  private static PhotoOrganizer createOrganizer(JsonNode source) throws NoSuchMethodException,
      InstantiationException, IllegalAccessException, InvocationTargetException {
    Class<? extends PhotoOrganizer> sourceParserType = SOURCES.get(source.get("type").textValue());
    Constructor<? extends PhotoOrganizer> constructor =
        sourceParserType.getConstructor(JsonNode.class);
    return constructor.newInstance(source);
  }

  private static JsonNode readConfig() throws IOException, JsonProcessingException {
    try (InputStream in = PhotoSurveyRunner.class.getResourceAsStream("/app_config.json")) {
      ObjectMapper objectMapper = JacksonUtils.getObjectMapper();
      
      return objectMapper.reader().readTree(in);
    }
  }
}
