package org.dalquist.photos.survey.iphoto;

import org.dalquist.photos.survey.ConfigurablePhotoOrganizer;
import org.dalquist.photos.survey.PhotoOrganizer;
import org.dalquist.photos.survey.model.Source;
import org.springframework.stereotype.Service;

@Service
public class IPhotoConfigurablePhotoOrganizer implements ConfigurablePhotoOrganizer {
  public static final String SOURCE = "iPhoto";

  @Override
  public PhotoOrganizer configure(Source source) {
    return new IPhotoOrganizer(source);
  }

  @Override
  public String getType() {
    return SOURCE;
  }

}
