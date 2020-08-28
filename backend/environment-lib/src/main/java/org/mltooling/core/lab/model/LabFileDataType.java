package org.mltooling.core.lab.model;

import org.mltooling.core.utils.StringUtils;


public enum LabFileDataType {
    UNKNOWN("unk", "unk"),
    DATASET("dataset", "datasets"),
    MODEL("model", "models"),
    BACKUPS("backup", "backups"),
    ARCHIVE("archive", "backups"),
    EXPERIMENT("experiment", "experiments"),
    IMPORT_DATA("import-data", "import-data"),
    IMPORT_CACHE("import-cache", "import-cache");

    private String name;
    private String defaultFolder;

    public final static String ALLOWABLE_VALUES = "dataset, model, experiment, backup, archive, import-data, import-cache";

    LabFileDataType(String name, String defaultFolder) {
        this.name = name;
        this.defaultFolder = defaultFolder;
    }

    public static LabFileDataType from(String type) {
        if (StringUtils.isNullOrEmpty(type)) {
            return UNKNOWN;
        }

        for (LabFileDataType availableType : LabFileDataType.values()) {
            if (type.equalsIgnoreCase(availableType.getName())) {
                return availableType;
            }

            if (type.equalsIgnoreCase(availableType.getDefaultFolder())) {
                return availableType;
            }
        }
        return UNKNOWN;
    }

    public static LabFileDataType fromKey(String key) {
        if (StringUtils.isNullOrEmpty(key)) {
            return UNKNOWN;
        }

        for (LabFileDataType availableType : LabFileDataType.values()) {
            if (key.toLowerCase().startsWith(availableType.defaultFolder.toLowerCase())) {
                return availableType;
            }
        }
        return UNKNOWN;
    }

    public String getName() {
        return name;
    }

    public String getDefaultFolder() {
        return defaultFolder;
    }

    public boolean isUnknown() {
        return this == UNKNOWN;
    }

    @Override
    public String toString() {
        return getName();
    }
}