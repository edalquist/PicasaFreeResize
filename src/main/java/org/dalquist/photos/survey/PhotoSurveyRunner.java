package org.dalquist.photos.survey;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;

import org.dalquist.photos.survey.picasa.PicasaPhotoOrganizer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;

public class PhotoSurveyRunner {
  private final static ImmutableMap<String, Class<? extends PhotoOrganizer>> SOURCES = ImmutableMap
      .of(PicasaPhotoOrganizer.SOURCE, PicasaPhotoOrganizer.class);

  public static void main(String[] args) throws Exception {
    Config config = readConfig();

    // Load the photoDb
    String photoDbFile = config.getPhotoDbFile();
    PhotosDatabase pdb = new PhotosDatabase(photoDbFile);
    
    // Iterate through sources
    for (Source source : config.getSources()) {
      PhotoOrganizer sourceOrganizer = createOrganizer(source);
      System.out.println("Parsing photos from: " + source.getId());
      
      sourceOrganizer.loadPhotoEntries(pdb);
      
      // Save the db after each source
      System.out.println("Parsed photos from: " + source.getId());
      pdb.save();
    }
  }

  private static PhotoOrganizer createOrganizer(Source source) throws NoSuchMethodException,
      InstantiationException, IllegalAccessException, InvocationTargetException {
    Class<? extends PhotoOrganizer> sourceParserType = SOURCES.get(source.getType());
    Constructor<? extends PhotoOrganizer> constructor =
        sourceParserType.getConstructor(Source.class);
    return constructor.newInstance(source);
  }

  static Config readConfig() throws IOException, JsonProcessingException {
    return readConfig("/app_config.json");
  }

  static Config readConfig(String configFile) throws IOException, JsonProcessingException {
    try (InputStream in = PhotoSurveyRunner.class.getResourceAsStream(configFile)) {
      ObjectMapper objectMapper = JacksonUtils.getObjectMapper();
      return objectMapper.reader(Config.class).readValue(in);
    }
  }

  static class Config {
    private String photoDbFile;
    private List<Source> sources;
    
    public String getPhotoDbFile() {
      return photoDbFile;
    }
    public void setPhotoDbFile(String photoDbFile) {
      this.photoDbFile = photoDbFile;
    }
    public List<Source> getSources() {
      return sources;
    }
    public void setSources(List<Source> sources) {
      this.sources = sources;
    }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((photoDbFile == null) ? 0 : photoDbFile.hashCode());
      result = prime * result + ((sources == null) ? 0 : sources.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Config other = (Config) obj;
      if (photoDbFile == null) {
        if (other.photoDbFile != null)
          return false;
      } else if (!photoDbFile.equals(other.photoDbFile))
        return false;
      if (sources == null) {
        if (other.sources != null)
          return false;
      } else if (!sources.equals(other.sources))
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "Config [photoDbFile=" + photoDbFile + ", sources=" + sources + "]";
    }
  }

  public static class Source extends HashMap<String, String> {
    private static final long serialVersionUID = 1L;

    public String getId() {
      return get("id");
    }
    public void setId(String id) {
      put("id", id);
    }
    public String getType() {
      return get("type");
    }
    public void setType(String type) {
      put("type", type);
    }
  }
}
