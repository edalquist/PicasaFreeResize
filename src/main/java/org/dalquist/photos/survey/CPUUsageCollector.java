package org.dalquist.photos.survey;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CPUUsageCollector implements Runnable {
  private final static long INTERVAL = 1000L; // polling interval in ms

  protected final Logger logger = LoggerFactory.getLogger(getClass());
  private final int cores = Runtime.getRuntime().availableProcessors();
  private final List<ThreadPoolExecutor> tpes = new CopyOnWriteArrayList<ThreadPoolExecutor>();
  private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
  private volatile boolean run = true;

  private long totalCpuTime = 0L; // total CPU time in millis
  private double load = 0d; // average load over the interval
  private int previousCores = cores;

  @PostConstruct
  public void init() {
    Thread t = new Thread(this);
    t.setDaemon(true);
    t.start();
  }

  @PreDestroy
  public void stop() {
    run = false;
  }

  public void registerThreadPool(ThreadPoolExecutor tpe) {
    tpes.add(tpe);
  }

  @Override
  public void run() {
    try {
      while (run) {
        long start = System.currentTimeMillis();
        long[] ids = threadMXBean.getAllThreadIds();
        long time = 0L;
        for (long id : ids) {
          long l = threadMXBean.getThreadCpuTime(id);
          if (l >= 0L)
            time += l;
        }
        long newCpuTime = time / 1000000L;
        long oldCpuTime = totalCpuTime;
        totalCpuTime = newCpuTime;
        // load = CPU time difference / sum of elapsed time for all CPUs
        load =
            (newCpuTime - oldCpuTime)
                / (double) (INTERVAL * Runtime.getRuntime().availableProcessors());

        int adjustedCores = (int) (cores * 2 * (1 - load));
        if (adjustedCores != previousCores) {
          logger.debug("Adjusting cores from {} to {}", previousCores, adjustedCores);
          previousCores = adjustedCores;

          for (ThreadPoolExecutor tpe : tpes) {
            tpe.setCorePoolSize(adjustedCores);
            tpe.setMaximumPoolSize(adjustedCores);
          }
        }

        long sleepTime = INTERVAL - (System.currentTimeMillis() - start);
        goToSleep(sleepTime <= 0L ? INTERVAL : sleepTime);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void goToSleep(final long time) {
    try {
      Thread.sleep(time);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
