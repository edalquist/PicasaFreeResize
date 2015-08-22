package org.dalquist.photos.survey;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public final class PeriodicExecutor {
  private final AtomicLong runTimer = new AtomicLong(0);
  private final long period;

  public PeriodicExecutor(Duration duration) {
    this.period = duration.toMillis();
  }

  public void run(Runnable r) {
    long now = System.currentTimeMillis();
    long last = runTimer.get();
    if ((now - last) > period) {
      if (runTimer.compareAndSet(last, now)) {
        r.run();
      }
    }
  }
}
