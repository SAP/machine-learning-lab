package org.mltooling.lab.services;

import org.mltooling.core.utils.StringUtils;

public enum FeatureType {
  PROJECT_SERVICE("project-service"),
  CORE_SERVICE("core-service"),
  PROJECT_JOB("project-job"),
  WORKSPACE("workspace"),
  UNKNOWN("");

  private String name;

  FeatureType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public static FeatureType from(String type) {
    if (StringUtils.isNullOrEmpty(type)) {
      return UNKNOWN;
    }

    for (FeatureType mode : FeatureType.values()) {
      if (type.equalsIgnoreCase(mode.getName())) {
        return mode;
      }
    }
    return UNKNOWN;
  }

  @Override
  public String toString() {
    return getName();
  }

  public boolean isUnknown() {
    return this == UNKNOWN;
  }
}
