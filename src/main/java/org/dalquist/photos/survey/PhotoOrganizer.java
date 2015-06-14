package org.dalquist.photos.survey;

import java.io.IOException;

import com.google.gdata.util.ServiceException;

public interface PhotoOrganizer {
  void loadPhotoEntries(PhotosDatabase pdb) throws IOException;
}
