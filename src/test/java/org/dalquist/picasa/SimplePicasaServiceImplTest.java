package org.dalquist.picasa;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gdata.data.photos.AlbumEntry;
import com.google.gdata.data.photos.PhotoEntry;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;
import com.google.gdata.util.XmlBlob;

@RunWith(MockitoJUnitRunner.class)
public class SimplePicasaServiceImplTest {
  private static ImmutableMap<String, String> testConfig;
  private SimplePicasaServiceImpl service;

  @BeforeClass
  public static void setupStatic() throws IOException, AuthenticationException {
    Properties testProperties = new Properties();

    try (InputStream testPropertiesStream =
        SimplePicasaServiceImplTest.class.getResourceAsStream("/test.properties")) {
      if (testPropertiesStream == null) {
        fail("test.properties is missing, copy and populate test.properties.sample");
      }
      testProperties.load(testPropertiesStream);
    }

    testConfig = Maps.fromProperties(testProperties);
  }

  @Before
  public void setup() throws IOException, AuthenticationException {
    String username = testConfig.get("google.username");
    assertThat(username).isNotNull();

    String password = testConfig.get("google.password");
    assertThat(password).isNotNull();

    service = new SimplePicasaServiceImpl(username, password);
  }

  @Test
  public void testListAllPhotos() throws IOException, ServiceException {
    List<AlbumEntry> albums = service.getAlbums();
    assertThat(albums).isNotNull();

    int totalPhotos = 0;
    for (AlbumEntry albumEntry : albums) {
      System.out.println(albumEntry.getDate() + ", " + albumEntry.getName() + ", "
          + albumEntry.getTitle().getPlainText() + ", " + albumEntry.getPhotosLeft() + ", "
          + albumEntry.getPhotosUsed());

      int albumPhotos = 0;
      for (PhotoEntry photoEntry : service.getPhotos(albumEntry)) {
        // System.out.println("\t" + objectMapper.writeValueAsString(photoEntry));
        totalPhotos++;
        albumPhotos++;
      }
      System.out.println("\t" + albumPhotos);
    }
    System.out.println(totalPhotos);
  }

}
