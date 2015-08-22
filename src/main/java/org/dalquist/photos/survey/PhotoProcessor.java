package org.dalquist.photos.survey;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FilenameUtils;
import org.dalquist.photos.survey.config.Config;
import org.dalquist.photos.survey.model.Image;
import org.dalquist.photos.survey.model.Resource;
import org.dalquist.photos.survey.model.SourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListeningExecutorService;

@Service
public class PhotoProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(PhotoProcessor.class);

  private static final DateTimeFormatter EXIF_DATE_TIME_PATTERN = DateTimeFormatter
      .ofPattern("yyyy:MM:DD HH:mm:ss");
  private static final DateTimeFormatter YMD_PATTERN = DateTimeFormatter
      .ofPattern("yyyy_MM_DD");

  private final Config config;
  private final PhotosDatabase photosDatabase;
  private final ListeningExecutorService execService;
  private final ResourceLoader resourceLoader;
  private final ConvertRunner convertRunner;
  private final Path destDirBase;

  @Autowired
  public PhotoProcessor(Config config, PhotosDatabase photosDatabase,
      ListeningExecutorService execService, ResourceLoader resourceLoader,
      ConvertRunner convertRunner) {
    this.config = config;
    this.photosDatabase = photosDatabase;
    this.execService = execService;
    this.resourceLoader = resourceLoader;
    this.convertRunner = convertRunner;

    destDirBase = Paths.get(config.getDestDir());
  }

  public void processImage(SourceId sourceId, Image image) {
    if (image == null) {
      return;
    }

    execService.submit(new ImageProcessor(sourceId, image));
  }

  private class ImageProcessor implements Callable<Void> {
    private final SourceId sourceId;
    private final Image image;

    public ImageProcessor(SourceId sourceId, Image image) {
      this.sourceId = sourceId;
      this.image = image;
    }

    @Override
    public Void call() throws Exception {
      try {
        LOGGER.info("Processing: " + image);
        Path dest = processResource(image.getOriginal(), "original", null);
        processResource(image.getModified(), "modified", dest);
      } catch (Exception e) {
        LOGGER.error("Failed processing:", e);
        throw e;
      }

      return null;
    }

    private UrlChannelWrapper openChannel(String url) throws IOException {
      if (url.startsWith("file:")) {
        Path path = Paths.get(url.substring("file:".length()));
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        return new UrlChannelWrapper(Files.newByteChannel(path), attrs);
      }
      InputStream urlStream = resourceLoader.getResource(url).getInputStream();
      return new UrlChannelWrapper(Channels.newChannel(urlStream));
    }

    private Path processResource(Resource r, String resourceType, Path dest) throws IOException,
        InterruptedException, ExecutionException {
      if (r == null) {
        return null;
      }

      // Copy file to temp directory
      String url = r.getUrl();
      String fileName = resourceType + "_" + FilenameUtils.getName(url);

      Path destFile;
      Path tempFile = null;
      if (dest == null) {
        tempFile = Files.createTempFile("pp_", "_" + fileName);
        destFile = tempFile;
      } else {
        destFile = dest.resolve(fileName);
      }

      // TODO when processing local files don't do copy+process / copy do buffer+process / write

      try (FileChannel destChannel = openFile(destFile);
          UrlChannelWrapper urlChannelWrapper = openChannel(url)) {
        LOGGER.info("Copying " + url + " to: " + destFile);

        // Run convert to generate the JSON and copy to the destFile
        TeeReadableByteChannel teeChannel =
            new TeeReadableByteChannel(urlChannelWrapper.getChannel(), destChannel);
        Future<JsonNode> jsonFuture = convertRunner.generateImageJson(teeChannel);

        // Wait for copy and convert to return the JSON data
        JsonNode jsonNode = jsonFuture.get();
        LOGGER.info("Copied " + url + " to: " + destFile);
        // LOGGER.info("JSON: " + jsonNode);

        // Copy file attributes if the source was a file
        if (urlChannelWrapper.getFileAttrs() != null) {
          FileUtils.setAttributes(destFile, urlChannelWrapper.getFileAttrs());
        }

        if (dest == null) {
          LocalDateTime imageDateTime = getImageDateTime(jsonNode);

          dest = destDirBase
            .resolve(sourceId.getType())
            .resolve(sourceId.getId())
            .resolve(imageDateTime.format(YMD_PATTERN));
          LOGGER.info("Image date: " + dest);

          // Ensure directory exists
          if (!Files.exists(dest)) {
            Files.createDirectories(dest);
          }

          destFile = dest.resolve(fileName);

          LOGGER.info("Moving " + tempFile + " to: " + destFile);
          // Move tmp file to dest
          Path result = Files.move(tempFile, destFile);
          if (urlChannelWrapper.getFileAttrs() != null) {
            FileUtils.setAttributes(result, urlChannelWrapper.getFileAttrs());
          }
          LOGGER.info("Result: " + result);
        }

        // move temp file to dest path
        // write JSON to dest path

        // TODO retain filesystem date/time or http lastModified?
        // Files.move(destFile, destFile)

        // TODO write mapping data to Firebase
      } catch (Exception e) {
        LOGGER.error("Copy " + url + " to: " + destFile + " FAILED", e);
        throw e;
      } finally {
        try {
          Files.deleteIfExists(tempFile);
        } catch (IOException e) {
        }
      }

      // return dest path
      return dest;
    }

    private LocalDateTime getImageDateTime(JsonNode jsonNode) {
      jsonNode = jsonNode.get("image");
      if (jsonNode == null) {
        return null;
      }

      jsonNode = jsonNode.get("properties");
      if (jsonNode == null) {
        return null;
      }

      LocalDateTime dateTime =
          getDateTime(jsonNode, "exif:DateTimeOriginal", "exif:SubSecTimeOriginal");
      if (dateTime != null) {
        return dateTime;
      }

      dateTime = getDateTime(jsonNode, "exif:DateTimeDigitized", "exif:SubSecTimeDigitized");
      if (dateTime != null) {
        return dateTime;
      }

      return getDateTime(jsonNode, "exif:DateTime", "exif:SubSecTime");
    }

    private LocalDateTime getDateTime(JsonNode jsonNode, String dtName, String stName) {
      JsonNode dateTimeNode = jsonNode.get(dtName);
      if (dateTimeNode != null) {
        JsonNode subsecTimeNode = jsonNode.get(stName);
        return toDateTime(dateTimeNode, subsecTimeNode);
      }

      return null;
    }

    private LocalDateTime toDateTime(JsonNode dateTimeNode, JsonNode subsecTimeNode) {
      LocalDateTime imageDateTime =
          LocalDateTime.parse(dateTimeNode.asText(), EXIF_DATE_TIME_PATTERN);
      if (subsecTimeNode == null) {
        return imageDateTime;
      }

      return imageDateTime.plusNanos(TimeUnit.MILLISECONDS.toNanos(subsecTimeNode.asInt()));
    }

    private FileChannel openFile(Path destFile) throws IOException {
      return FileChannel.open(destFile, StandardOpenOption.WRITE,
          StandardOpenOption.TRUNCATE_EXISTING);
    }
  }

  static final class TeeReadableByteChannel implements ReadableByteChannel {
    private final ReadableByteChannel input;
    private final WritableByteChannel teeOutput;
    private volatile boolean open = true;

    private TeeReadableByteChannel(ReadableByteChannel resourceInChannel,
        WritableByteChannel tmpDestChannel) {
      this.input = resourceInChannel;
      this.teeOutput = tmpDestChannel;
    }

    @Override
    public boolean isOpen() {
      return open;
    }

    @Override
    public void close() throws IOException {
      open = false;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
      if (!open) {
        throw new ClosedChannelException();
      }

      // Read bytes directly into the dst buffer
      int bytesRead = input.read(dst);
      if (bytesRead < 0) {
        open = false;
        return bytesRead;
      }

      // Create a transfer buffer of the same data and flip it
      ByteBuffer transferBuffer = dst.duplicate();
      transferBuffer.flip();

      // Write the read bytes to the tee output, this is effectively blocking I/O since we need to
      // loop until the output has accepted all of the data in the transfer buffer.
      while (transferBuffer.hasRemaining()) {
        teeOutput.write(transferBuffer);
      }

      // Return the number of bytes read into the dst buffer
      return bytesRead;
    }
  }

  static class UrlChannelWrapper implements Closeable {
    private final ReadableByteChannel channel;
    private final BasicFileAttributes fileAttrs;

    public UrlChannelWrapper(ReadableByteChannel channel) {
      this.channel = channel;
      this.fileAttrs = null;
    }

    public UrlChannelWrapper(ReadableByteChannel channel, BasicFileAttributes attrs) {
      this.channel = channel;
      this.fileAttrs = attrs;
    }

    @Override
    public void close() throws IOException {
      channel.close();
    }

    public ReadableByteChannel getChannel() {
      return channel;
    }

    public BasicFileAttributes getFileAttrs() {
      return fileAttrs;
    }
  }
}
