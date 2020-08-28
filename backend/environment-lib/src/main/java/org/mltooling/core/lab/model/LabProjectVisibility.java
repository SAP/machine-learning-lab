package org.mltooling.core.lab.model;

import org.mltooling.core.utils.StringUtils;


public enum LabProjectVisibility {
    PRIVATE("private"), // private is default visibility
    PUBLIC("public");

    private String name;

    public final static String ALLOWABLE_VALUES = "private, public";

    LabProjectVisibility(String name) {
        this.name = name;
    }

    public static LabProjectVisibility from(String type) {
        if (StringUtils.isNullOrEmpty(type)) {
            return PRIVATE;
        }

        for (LabProjectVisibility availableType : LabProjectVisibility.values()) {
            if (type.equalsIgnoreCase(availableType.getName())) {
                return availableType;
            }
        }
        return PRIVATE;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }
}