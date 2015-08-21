package org.dalquist.photos.survey;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.io.FilenameUtils;
import org.dalquist.photos.survey.config.Config;
import org.dalquist.photos.survey.model.Image;
import org.dalquist.photos.survey.model.Resource;
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

  private final Config config;
  private final PhotosDatabase photosDatabase;
  private final ListeningExecutorService execService;
  private final ResourceLoader resourceLoader;
  private final ConvertRunner convertRunner;

  @Autowired
  public PhotoProcessor(Config config, PhotosDatabase photosDatabase,
      ListeningExecutorService execService, ResourceLoader resourceLoader,
      ConvertRunner convertRunner) {
    this.config = config;
    this.photosDatabase = photosDatabase;
    this.execService = execService;
    this.resourceLoader = resourceLoader;
    this.convertRunner = convertRunner;
  }

  public void processImage(Image image) {
    execService.submit(new ImageProcessor(image));
  }

  // Queue task
  // copy images to dest
  // run convert on both images
  // save json to dest
  // write mapping to FireBase

  private class ImageProcessor implements Callable<Void> {
    private final Image image;

    public ImageProcessor(Image image) {
      this.image = image;
    }

    @Override
    public Void call() throws Exception {
      LOGGER.info("Processing: " + image);
      Path dest = processResource(image.getOriginal(), "original", null);
      processResource(image.getModified(), "original", dest);

      return null;
    }

    private ReadableByteChannel openChannel(String url) throws IOException {
      if (url.startsWith("file:")) {
        return Files.newByteChannel(Paths.get(url.substring("file:".length())));
      }
      return Channels.newChannel(resourceLoader.getResource(url).getInputStream());
    }

    private Path processResource(Resource r, String type, Path dest) throws IOException,
        InterruptedException, ExecutionException {
      if (r == null) {
        return null;
      }

      // Copy file to temp directory
      String url = r.getUrl();
      String ext = FilenameUtils.getExtension(url);

      boolean useTmpFile = dest != null;
      Path tmpFile = Files.createTempFile("pp_", "." + ext);
      try (FileChannel tmpDestChannel =
          FileChannel.open(tmpFile, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {

        try (ReadableByteChannel resourceInChannel = openChannel(url)) {
          LOGGER.info("Copying " + url + " to: " + tmpFile);

          Future<JsonNode> jsonFuture = convertRunner.generateImageJson(new ReadableByteChannel() {
            private boolean open = true;

            @Override
            public boolean isOpen() {
              return open;
            }

            @Override
            public void close() throws IOException {
              open = false;
              // noop
            }

            @Override
            public int read(ByteBuffer dst) throws IOException {
              if (!open) {
                throw new ClosedChannelException();
              }

              // Read image bytes into the dst buffer
              int bytesRead = resourceInChannel.read(dst);
              if (bytesRead < 0) {
                open = false;
                return bytesRead;
              }

              // Create a transfer buffer of the same data and flip it
              ByteBuffer transferBuffer = dst.duplicate();
              transferBuffer.flip();

              // Write the read bytes to the temp file
              while (transferBuffer.hasRemaining()) {
                tmpDestChannel.write(transferBuffer);
              }

              return bytesRead;
            }
          });

          JsonNode jsonNode = jsonFuture.get();
          LOGGER.info("Copied " + url + " to: " + tmpFile);
          LOGGER.info("JSON: " + jsonNode);
        } catch (IOException e) {
          LOGGER.error("Copy " + url + " to: " + tmpFile + " FAILED", e);
          throw e;
        }
      } finally {
        try {
          Files.deleteIfExists(tmpFile);
        } catch (IOException e) {
        }
      }
      // Figure out new DEST path

      // Move temp file to DEST path

      return null;
    }
  }
}
