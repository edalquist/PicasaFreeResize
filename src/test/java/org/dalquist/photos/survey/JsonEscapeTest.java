package org.dalquist.photos.survey;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;

public class JsonEscapeTest {
  @Test
  public void testEscape() throws JsonProcessingException {
    String str =
        ObjectMapperHolder.getObjectMapper().writeValueAsString(ImmutableMap.of("key", "%G"));
    assertThat(str).isEqualTo("");
  }
}
