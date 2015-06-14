package org.dalquist.photos.survey;

import static com.google.common.truth.Truth.assertThat;

import org.dalquist.photos.survey.model.Config;
import org.dalquist.photos.survey.model.Source;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;

public class PhotoSurveyRunnerTest {
  @Ignore
  @Test
  public void generateConfigTemplate() throws Exception {
    Config config = new Config();
    config.setPhotoDbFile("/Users/myuser/tmp/photos_db.json");
    
    Source s1 = new Source();
    s1.setId("user1");
    s1.setType("Picasa");
    s1.put("username", "user1@gmail.com");
    s1.put("password", "USE_AN_APP_PASSWORD");

    Source s2 = new Source();
    s2.setId("user2");
    s2.setType("Picasa");
    s2.put("username", "user2@gmail.com");
    s2.put("password", "USE_AN_APP_PASSWORD");
    
    config.setSources(ImmutableList.of(
        s1,
        s2));
    
    ObjectMapper objectMapper = ObjectMapperHolder.getObjectMapper();
    String configStr = objectMapper.writerFor(Config.class).writeValueAsString(config);
    System.out.println(configStr);
  }

//  @Test
//  public void testLoadConfig() throws Exception {
//    Config config = PhotoSurveyRunner.readConfig("/app_config.json.sample");
//    assertThat(config).isNotNull();
//    assertThat(config.getPhotoDbFile()).isEqualTo("/Users/user1/tmp/photos_db.json");
//    
//    Source s1 = new Source();
//    s1.setId("user1");
//    s1.setType("Picasa");
//    s1.put("username", "user1@gmail.com");
//    s1.put("password", "USE_AN_APP_PASSWORD");
//
//    Source s2 = new Source();
//    s2.setId("user2");
//    s2.setType("Picasa");
//    s2.put("username", "user2@gmail.com");
//    s2.put("password", "USE_AN_APP_PASSWORD");
//
//    Source s3 = new Source();
//    s3.setId("apollo:user1");
//    s3.setType("iPhoto");
//    s3.put("albumXml", "/Users/user1/Pictures/iPhoto Library.photolibrary/AlbumData.xml");
//
//    Source s4 = new Source();
//    s4.setId("apollo:user2");
//    s4.setType("iPhoto");
//    s4.put("albumXml", "/Users/user2/Pictures/iPhoto Library/AlbumData.xml");
//    
//    assertThat(config.getSources()).containsExactly(s1, s2, s3, s4);
//  }
}
