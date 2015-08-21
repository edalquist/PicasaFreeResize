package org.dalquist.photos.survey;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.dalquist.photos.survey.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.nuprocess.NuAbstractProcessHandler;
import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;

@Service
public class ConvertRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(PhotoHashingRunner.class);

  private final Config config;

  @Autowired
  public ConvertRunner(Config config) {
    this.config = config;
  }

  public Future<JsonNode> generateImageJson(ReadableByteChannel in) throws IOException {
    final CompletableFuture<JsonNode> future = new CompletableFuture<JsonNode>();

    // Create handler that copies data from the in stream into the process
    ConvertProcessHandler processHandler = new ConvertProcessHandler(in, future);

    NuProcessBuilder pb =
        new NuProcessBuilder(config.getConvertBinary(), "-", "-moments", "json:-");
    pb.setProcessListener(processHandler);

    // Start the process
    pb.start();

    return future;
  }

  private static final class OutputBuffer {
    private final ByteArrayOutputStream byteBuffer;
    private final WritableByteChannel writeableChannel;

    public OutputBuffer(int size) {
      byteBuffer = new ByteArrayOutputStream(size);
      writeableChannel = Channels.newChannel(byteBuffer);
    }

    public void write(ByteBuffer buffer) {
      while (buffer != null && buffer.hasRemaining()) {
        try {
          writeableChannel.write(buffer);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }

    public Reader getReader() {
      return new InputStreamReader(new ByteArrayInputStream(byteBuffer.toByteArray()),
          StandardCharsets.UTF_8);
    }
  }

  private static final class ConvertProcessHandler extends NuAbstractProcessHandler {
    private final ReadableByteChannel imageChannel;
    private final CompletableFuture<JsonNode> future;

    private final OutputBuffer stdout = new OutputBuffer(10240);
    private final OutputBuffer stderr = new OutputBuffer(0);

    private NuProcess nuProcess;
    private boolean dataRead = true;

    private ConvertProcessHandler(ReadableByteChannel in, CompletableFuture<JsonNode> future) {
      this.imageChannel = in;
      this.future = future;
    }

    @Override
    public void onPreStart(NuProcess nuProcess) {
      LOGGER.info("onPreStart");
      this.nuProcess = nuProcess;
    }

    @Override
    public void onStart(NuProcess nuProcess) {
      LOGGER.info("onStart");
      nuProcess.wantWrite();
    }

    @Override
    public void onStdout(ByteBuffer buffer) {
      LOGGER.info("onStdout");
      stdout.write(buffer);
    }

    @Override
    public void onStderr(ByteBuffer buffer) {
      LOGGER.info("onStderr");
      stderr.write(buffer);
    }

    @Override
    public void onExit(int exitCode) {
      LOGGER.info("onExit");
      if (exitCode != 0) {
        // Process completed with error
        // Complete future with exception and stderr
        future.completeExceptionally(new RuntimeException("Failed to execute convert\n"
            + stderr.getReader()));
      } else {
        // Process completed successfull!
        // Complete future after parsing stdout into JSON
        ObjectMapper objectMapper = ObjectMapperHolder.getObjectMapper();
        try {
          JsonNode result = objectMapper.readTree(stdout.getReader());
          future.complete(result);
        } catch (IOException e) {
          future.completeExceptionally(e);
        }
      }
    }

    @Override
    public boolean onStdinReady(ByteBuffer buffer) {
      LOGGER.info("onStdinReady");
      if (!dataRead) {
        nuProcess.closeStdin();
        return false;
      }

      try {
        dataRead = imageChannel.read(buffer) >= 0;
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      buffer.flip();
      return true;
    }
  }
}
