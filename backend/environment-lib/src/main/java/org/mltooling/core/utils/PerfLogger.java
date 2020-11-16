package org.mltooling.core.utils;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;

public class PerfLogger {

  // ================ Constants ============================================== //

  // ================ Members ================================================ //
  private boolean logging;
  private long startTime = System.nanoTime();
  private Logger logger;
  private HashMap<String, Long> startTimes = new HashMap<String, Long>();

  // ================ Constructors & Main ==================================== //
  public PerfLogger() {
    this(null, false);
  }

  public PerfLogger(Logger logger) {
    this(logger, true);
  }

  public PerfLogger(Logger logger, boolean logging) {
    this.logger = logger;
    setLogging(logging);
  }
  // ================ Methods for/from SuperClass / Interfaces =============== //

  // ================ Methods ================================================ //

  public void start() {
    startTime = System.nanoTime();
  }

  public void start(String eventId) {
    if (eventId != null) {
      startTimes.put(eventId, System.nanoTime());
    }
  }

  public long end() {
    if (startTime == 0) {
      if (isLogging()) {
        logger.info(logger.getName() + " - PerfLogger wasn't started");
      }
      return 0;
    }
    long measuredTime = System.nanoTime() - startTime;
    long measuredTimeInMillis = TimeUnit.NANOSECONDS.toMillis(measuredTime);
    if (isLogging()) {
      logger.info(logger.getName() + " - Measured time: " + measuredTimeInMillis);
    }
    return measuredTimeInMillis;
  }

  public long end(String eventId) {
    if (!startTimes.containsKey(eventId)) {
      if (isLogging()) {
        logger.info(logger.getName() + " - PerfLogger wasn't started for " + eventId);
      }
      return 0;
    }
    long startTime = startTimes.remove(eventId);
    long measuredTime = System.nanoTime() - startTime;
    long measuredTimeInMillis = TimeUnit.NANOSECONDS.toMillis(measuredTime);
    if (isLogging()) {
      logger.info(
          logger.getName() + " - Measured time for " + eventId + ": " + measuredTimeInMillis);
    }
    return measuredTimeInMillis;
  }

  // ================ Getter & Setter ======================================== //
  public boolean isLogging() {
    if (logger == null) {
      return false;
    }

    return logging;
  }

  public void setLogging(boolean logging) {
    this.logging = logging;
  }

  // ================ Inner & Anonymous Classes ============================== //
}
