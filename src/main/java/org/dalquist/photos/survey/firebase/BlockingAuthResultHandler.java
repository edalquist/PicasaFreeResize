package org.dalquist.photos.survey.firebase;

import java.util.concurrent.CountDownLatch;

import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.Firebase.AuthResultHandler;
import com.firebase.client.FirebaseError;

public class BlockingAuthResultHandler implements AuthResultHandler {
  private final CountDownLatch latch = new CountDownLatch(1);
  private final Firebase.AuthResultHandler delegate;

  public BlockingAuthResultHandler() {
    this(null);
  }

  public BlockingAuthResultHandler(AuthResultHandler delegate) {
    this.delegate = delegate;
  }

  @Override
  public void onAuthenticated(AuthData arg0) {
    try {
      if (delegate != null) {
        delegate.onAuthenticated(arg0);
      }
    } finally {
      latch.countDown();
    }
  }

  @Override
  public void onAuthenticationError(FirebaseError arg0) {
    try {
      if (delegate != null) {
        delegate.onAuthenticationError(arg0);
      }
    } finally {
      latch.countDown();
    }
  }

  public void waitForAuth() {
    try {
      latch.await();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }
}
