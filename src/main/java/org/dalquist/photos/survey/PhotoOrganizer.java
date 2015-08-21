package org.dalquist.photos.survey;

import java.io.IOException;

public interface PhotoOrganizer {
  void loadPhotoEntries(PhotosDatabase pdb) throws IOException;

  void organizePhotos(PhotoProcessor pp) throws IOException;
}
