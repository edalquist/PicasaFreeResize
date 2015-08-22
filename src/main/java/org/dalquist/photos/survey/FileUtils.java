package org.dalquist.photos.survey;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtils {
  public static void copyAttributes(Path source, Path target) throws IOException {
    // Get attributes of source
    BasicFileAttributes attrs = Files.readAttributes(source, BasicFileAttributes.class);

    setAttributes(target, attrs);
  }

  public static void setAttributes(Path target, BasicFileAttributes attrs) throws IOException {
    // Write basic attributes to target
    BasicFileAttributeView view = Files.getFileAttributeView(target, BasicFileAttributeView.class);
    view.setTimes(attrs.lastModifiedTime(), attrs.lastAccessTime(), attrs.creationTime());
  }
}
