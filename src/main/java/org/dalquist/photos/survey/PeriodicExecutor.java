package org.dalquist.photos.survey;

import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.Period;

public final class PeriodicExecutor {
  private final AtomicLong runTimer = new AtomicLong(0);
  private final long period;

  public PeriodicExecutor(Period period) {
    this.period = period.toStandardSeconds().getSeconds() * 1000L;
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
