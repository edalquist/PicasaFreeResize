package org.dalquist.photos.survey;

import org.dalquist.photos.survey.model.Source;


public interface ConfigurablePhotoOrganizer {
  PhotoOrganizer configure(Source source);

  String getType();
}
