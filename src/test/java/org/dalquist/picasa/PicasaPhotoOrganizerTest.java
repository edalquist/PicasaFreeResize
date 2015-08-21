package org.dalquist.picasa;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.dalquist.photos.survey.PhotoOrganizer;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gdata.util.AuthenticationException;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class PicasaPhotoOrganizerTest {
  private static ImmutableMap<String, String> testConfig;
  private PhotoOrganizer organizer;

  @BeforeClass
  public static void setupStatic() throws IOException, AuthenticationException {
    Properties testProperties = new Properties();

    try (InputStream testPropertiesStream =
        PicasaPhotoOrganizerTest.class.getResourceAsStream("/test.properties")) {
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

//    organizer = new PicasaPhotoOrganizer(username, password);
  }

}
