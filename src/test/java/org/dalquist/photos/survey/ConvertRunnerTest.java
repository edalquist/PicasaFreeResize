package org.dalquist.photos.survey;

import static com.google.common.truth.Truth.assertThat;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.concurrent.Future;

import org.dalquist.photos.survey.config.Config;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class ConvertRunnerTest {
  private ConvertRunner runner;

  @Before
  public void setup() {
    Config config = new Config();
    config.setConvertBinary("/opt/local/bin/convert");
    runner = new ConvertRunner(config);
  }

  @Test
  public void testConvert() throws Exception {
    String file = "/Users/edalquist/Pictures/iPhoto Library.photolibrary/Masters/2015/01/13/20150113-160614/IMG_9457.JPG";
    try (FileInputStream fis = new FileInputStream(file)) {
//      Future<JsonNode> jsonFuture = runner.generateImageJson(new BufferedInputStream(fis));
//      JsonNode json = jsonFuture.get();
//      assertThat(json).isNotNull();
//      System.out.println(json);
    }
  }
}
