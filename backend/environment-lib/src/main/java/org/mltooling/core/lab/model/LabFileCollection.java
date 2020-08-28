package org.mltooling.core.lab.model;

import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class LabFileCollection {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //
    private List<LabFile> labFiles = new ArrayList<>();

    private int fileCount = 0;
    private long totalSize = 0;
    @ApiModelProperty(dataType = "java.lang.Long")
    private Date lastModified = null;

    private boolean aggregatedVersions = false;

    // ================ Constructors & Main ================================= //
    public LabFileCollection() {}

    public LabFileCollection(List<LabFile> labFiles, int fileCount, long totalSize, Date lastModified, boolean aggregatedVersions) {
        this.labFiles = labFiles;
        this.fileCount = fileCount;
        this.totalSize = totalSize;
        this.lastModified = lastModified;
        this.aggregatedVersions = aggregatedVersions;
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //
    @Override
    public String toString() {
        return labFiles.toString();
    }

    // ================ Public Methods ====================================== //
    public void add(LabFile labFile) {
        this.labFiles.add(labFile);
        this.updateStatistics(labFile);
    }

    public List<LabFile> getLabFiles() {
        return labFiles;
    }

    public int getFileCount() {
        return fileCount;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public boolean isAggregatedVersions() {
        return aggregatedVersions;
    }

    public LabFileCollection setLabFiles(List<LabFile> labFiles) {
        this.labFiles = labFiles;
        return this;
    }

    public LabFileCollection setFileCount(int fileCount) {
        this.fileCount = fileCount;
        return this;
    }

    public LabFileCollection setTotalSize(long totalSize) {
        this.totalSize = totalSize;
        return this;
    }

    public LabFileCollection setLastModified(Date lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    public LabFileCollection setAggregatedVersions(boolean aggregatedVersions) {
        this.aggregatedVersions = aggregatedVersions;
        return this;
    }

    // ================ Private Methods ===================================== //
    private void updateStatistics(LabFile labFile) {
        fileCount = fileCount + 1;
        totalSize = totalSize + labFile.getSize();

        if (lastModified == null || lastModified.compareTo(labFile.getModifiedAt()) < 0) {
            lastModified = labFile.getModifiedAt();
        }
    }

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
