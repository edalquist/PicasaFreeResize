package org.dalquist.photos.survey.firebase;

import org.slf4j.LoggerFactory;

import com.firebase.client.Logger;

public class SystemOutLogger implements Logger {
  public static final SystemOutLogger INSTANCE = new SystemOutLogger();

  @Override
  public Level getLogLevel() {
    return Level.DEBUG;
  }

  @Override
  public void onLogMessage(Level level, String tag, String message, long msTimestamp) {
    org.slf4j.Logger l = LoggerFactory.getLogger(tag);
    switch (level) {
      case DEBUG: {
        l.debug(message);
        break;
      }
      case ERROR: {
        l.error(message);
        break;
      }
      case INFO: {
        l.info(message);
        break;
      }
      case NONE: {
        l.trace(message);
        break;
      }
      case WARN: {
        l.warn(message);
        break;
      }
      default: {
        l.error(message);
        break;
      }
    }
  }
}
