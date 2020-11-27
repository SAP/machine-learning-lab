package org.mltooling.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemUtils {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(SystemUtils.class);

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //
  public static String getEnvVar(String name) {
    String env = System.getenv(name);
    if (env != null) {
      return env;
    }

    env = System.getenv(name.toLowerCase());
    if (env != null) {
      return env;
    }

    env = System.getenv(name.toUpperCase());
    if (env != null) {
      return env;
    }

    return null;
  }

  public static String getEnvVar(String name, String defaultValue) {
    String env = System.getenv(name);
    if (env != null) {
      return env;
    }

    env = System.getenv(name.toLowerCase());
    if (env != null) {
      return env;
    }

    env = System.getenv(name.toUpperCase());
    if (env != null) {
      return env;
    }

    return defaultValue;
  }

  public static void printMemoryInfo() {
    final long MEGABYTE = 1024L * 1024L;

    Runtime runtime = Runtime.getRuntime();
    long usedMemory = (runtime.totalMemory() - runtime.freeMemory());
    long availableMemory = (Runtime.getRuntime().maxMemory() - usedMemory);
    log.info(
        "Max memory: "
            + (runtime.maxMemory() / MEGABYTE)
            + "mb; "
            + "Available memory: "
            + (availableMemory / MEGABYTE)
            + "mb; "
            + "Free memory: "
            + (runtime.freeMemory() / MEGABYTE)
            + "mb; "
            + "Used memory: "
            + (usedMemory / MEGABYTE)
            + "mb; "
            + "Total memory: "
            + (runtime.totalMemory() / MEGABYTE)
            + "mb; ");
  }
  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
