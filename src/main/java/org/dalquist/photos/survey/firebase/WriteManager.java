package org.dalquist.photos.survey.firebase;

import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.firebase.client.Firebase;
import com.firebase.client.Firebase.CompletionListener;
import com.firebase.client.FirebaseError;

/**
 * Utility that uses a Phaser to wait for all of the created CompletionListeners to have onComplete
 * called. Useful to make sure the app doesn't exit until everything is written to firebase.
 */
public class WriteManager {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private final Semaphore writeLimiter = new Semaphore(10000);
  private final Phaser lock = new Phaser(1);
  private volatile boolean terminated = false;

  public CompletionListener getCompletionListener() {
    if (terminated) {
      throw new RuntimeException("waitForCompletion has been called");
    }
    try {
      writeLimiter.acquire();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
    return new SingleUseCompletionListener();
  }

  public void waitForCompletion() {
    terminated = true;
    int phase = lock.arrive();

    while (true) {
      // TODO turn this into general logging (every 5 seconds if getUnarrivedParties>0)
      logger.info("Waiting on {} out of {} updates", lock.getUnarrivedParties(),
          lock.getRegisteredParties() - 1);
      try {
        lock.awaitAdvanceInterruptibly(phase, 5, TimeUnit.SECONDS);
        return;
      } catch (InterruptedException | TimeoutException e) {
      }
    }
  }

  private class SingleUseCompletionListener implements CompletionListener {
    private final AtomicBoolean complete = new AtomicBoolean(false);
    private final Phaser childLock = new Phaser(lock, 1);

    @Override
    public void onComplete(FirebaseError error, Firebase ref) {
      if (complete.compareAndSet(false, true)) {
        logger.debug("update complete: {}", ref);
        childLock.arrive();
        writeLimiter.release();
      }
    }
  }
}
