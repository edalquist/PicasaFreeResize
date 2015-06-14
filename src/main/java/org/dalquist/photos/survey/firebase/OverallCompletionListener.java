package org.dalquist.photos.survey.firebase;

import java.util.concurrent.Phaser;

import com.firebase.client.Firebase;
import com.firebase.client.Firebase.CompletionListener;
import com.firebase.client.FirebaseError;

/**
 * Utility that uses a Phaser to wait for all of the created CompletionListeners to have onComplete
 * called. Useful to make sure the app doesn't exit until everything is written to firebase.
 */
public class OverallCompletionListener {
  private final Phaser lock = new Phaser(1);
  private volatile boolean terminated = false;

  public CompletionListener getCompletionListener() {
    if (terminated) {
      throw new RuntimeException("waitForCompletion has been called");
    }
    return new SingleUseCompletionListener();
  }

  public void waitForCompletion() {
    terminated = true;
    lock.arriveAndAwaitAdvance();
  }

  private class SingleUseCompletionListener implements CompletionListener {
    private final Phaser childLock = new Phaser(lock, 1);

    @Override
    public void onComplete(FirebaseError arg0, Firebase arg1) {
      childLock.arrive();
    }
  }
}
