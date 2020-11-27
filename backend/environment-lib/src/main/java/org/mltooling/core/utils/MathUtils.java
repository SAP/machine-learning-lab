package org.mltooling.core.utils;

import java.util.ArrayList;
import java.util.List;

public final class MathUtils {
  // ================ Constants =========================================== //

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //
  private MathUtils() {}

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //
  public static double randomInRange(double min, double max) {
    return Math.random() < 0.5
        ? ((1 - Math.random()) * (max - min) + min)
        : (Math.random() * (max - min) + min);
  }

  public static int randomInIntRange(int Min, int Max) {
    return Min + (int) (Math.random() * ((Max - Min) + 1));
  }

  /** Compares two doubles within a given epsilon */
  public static boolean equals(double a, double b, double eps) {
    if (a == b) {
      return true;
    }
    // If the difference is less than epsilon, treat as equal.
    return Math.abs(a - b) < eps;
  }

  public static List<Integer> getRowOfNumbers(int number, int offset) {
    List<Integer> precedingNumbers = new ArrayList<>();
    for (int i = 0; i < number; i++) {
      precedingNumbers.add(offset + i);
    }

    return precedingNumbers;
  }

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
