package org.mltooling.core.lab.model;

import io.swagger.annotations.ApiModelProperty;
import java.util.Date;
import java.util.Map;
import org.mltooling.core.utils.structures.PropertyContainer;

public class LabProjectsStatistics extends PropertyContainer {

  // ================ Constants =========================================== //
  public static final String STATISTICS_METADATA_NAME = "stats";

  private static final String PROJECTS_COUNT = "projectsCount";
  private static final String INACTIVE_PROJECTS_COUNT = "inactiveProjectsCount";
  private static final String SHARED_PROJECTS_COUNT = "sharedProjectsCount";
  private static final String USER_COUNT = "userCount";
  private static final String INACTIVE_USER_COUNT = "inactiveUserCount";
  private static final String SERVICES_COUNT = "servicesCount";
  private static final String JOBS_COUNT = "jobsCount";
  private static final String FILES_COUNT = "filesCount";
  private static final String FILES_TOTAL_SIZE = "filesTotalSize";
  private static final String CONTAINER_COUNT = "containerCount";
  private static final String DATASETS_COUNT = "datasetsCount";
  private static final String DATASETS_TOTAL_SIZE = "datasetsTotalSize";
  private static final String MODELS_COUNT = "modelsCount";
  private static final String MODELS_TOTAL_SIZE = "modelsTotalSize";
  private static final String EXPERIMENTS_COUNT = "experimentsCount";
  private static final String SERVER_COUNT = "serverCount";
  private static final String DOWNLOADED_FILES = "downloadedFiles";
  private static final String LAST_MODIFIED = "lastModified";

  private static final String CACHE_UPDATE_DATE = "cacheUpdateDate";
  private static final String CACHE_UPDATE_DURATION = "cacheUpdateDuration";

  // ================ Members ============================================= //

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //
  public LabProjectsStatistics() {}

  public LabProjectsStatistics(Map<String, Object> propertyMap) {
    getProperties().clear();
    getProperties().putAll(propertyMap);
  }
  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Private Methods ===================================== //

  protected LabProjectsStatistics addValue(String key, Object value) {
    getProperties().put(key, value);
    return this;
  }

  // ================ Public Methods ====================================== //

  // ================ Getter & Setter ===================================== //
  public Integer getProjectsCount() {
    return this.getInteger(PROJECTS_COUNT);
  }

  public LabProjectsStatistics setProjectsCount(Integer count) {
    this.addValue(PROJECTS_COUNT, count);
    return this;
  }

  public Integer getInactiveProjectsCount() {
    return this.getInteger(INACTIVE_PROJECTS_COUNT);
  }

  public LabProjectsStatistics setInactiveProjectsCount(Integer count) {
    this.addValue(INACTIVE_PROJECTS_COUNT, count);
    return this;
  }

  public Integer getSharedProjectsCount() {
    return this.getInteger(SHARED_PROJECTS_COUNT);
  }

  public LabProjectsStatistics setSharedProjectsCount(Integer count) {
    this.addValue(SHARED_PROJECTS_COUNT, count);
    return this;
  }

  public Integer getUserCount() {
    return this.getInteger(USER_COUNT);
  }

  public LabProjectsStatistics setUserCount(Integer count) {
    this.addValue(USER_COUNT, count);
    return this;
  }

  public Integer getInactiveUserCount() {
    return this.getInteger(INACTIVE_USER_COUNT);
  }

  public LabProjectsStatistics setInactiveUserCount(Integer count) {
    this.addValue(INACTIVE_USER_COUNT, count);
    return this;
  }

  public Integer getJobsCount() {
    return this.getInteger(JOBS_COUNT);
  }

  public LabProjectsStatistics setJobsCount(Integer count) {
    this.addValue(JOBS_COUNT, count);
    return this;
  }

  public Integer getServicesCount() {
    return this.getInteger(SERVICES_COUNT);
  }

  public LabProjectsStatistics setServicesCount(Integer count) {
    this.addValue(SERVICES_COUNT, count);
    return this;
  }

  public Integer getFilesCount() {
    return this.getInteger(FILES_COUNT);
  }

  public LabProjectsStatistics setFilesCount(Integer count) {
    this.addValue(FILES_COUNT, count);
    return this;
  }

  public Double getFilesTotalSize() {
    return this.getDouble(FILES_TOTAL_SIZE);
  }

  public LabProjectsStatistics setFilesTotalSize(Double size) {
    this.addValue(FILES_TOTAL_SIZE, size);
    return this;
  }

  public Integer getDatasetsCount() {
    return this.getInteger(DATASETS_COUNT);
  }

  public LabProjectsStatistics setDatasetsCount(Integer count) {
    this.addValue(DATASETS_COUNT, count);
    return this;
  }

  public Double getDatasetsTotalSize() {
    return this.getDouble(DATASETS_TOTAL_SIZE);
  }

  public LabProjectsStatistics setDatasetsTotalSize(Double size) {
    this.addValue(DATASETS_TOTAL_SIZE, size);
    return this;
  }

  public Double getModelsTotalSize() {
    return this.getDouble(MODELS_TOTAL_SIZE);
  }

  public LabProjectsStatistics setModelsTotalSize(Double size) {
    this.addValue(MODELS_TOTAL_SIZE, size);
    return this;
  }

  public Integer getModelsCount() {
    return this.getInteger(MODELS_COUNT);
  }

  public LabProjectsStatistics setModelsCount(Integer count) {
    this.addValue(MODELS_COUNT, count);
    return this;
  }

  public Integer getServerCount() {
    return this.getInteger(SERVER_COUNT);
  }

  public LabProjectsStatistics setServerCount(Integer count) {
    this.addValue(SERVER_COUNT, count);
    return this;
  }

  public Integer getContainerCount() {
    return this.getInteger(CONTAINER_COUNT);
  }

  public LabProjectsStatistics setContainerCount(Integer count) {
    this.addValue(CONTAINER_COUNT, count);
    return this;
  }

  public Integer getDownloadedFiles() {
    return this.getInteger(DOWNLOADED_FILES);
  }

  public LabProjectsStatistics setDownloadedFiles(Integer count) {
    this.addValue(DOWNLOADED_FILES, count);
    return this;
  }

  public Integer getExperimentsCount() {
    return this.getInteger(EXPERIMENTS_COUNT);
  }

  public LabProjectsStatistics setExperimentsCount(Integer count) {
    this.addValue(EXPERIMENTS_COUNT, count);
    return this;
  }

  @ApiModelProperty(dataType = "java.lang.Long")
  public Date getLastModified() {
    return this.getDate(LAST_MODIFIED);
  }

  public LabProjectsStatistics setLastModified(Date lastModified) {
    this.addValue(LAST_MODIFIED, lastModified.getTime());
    return this;
  }

  @ApiModelProperty(dataType = "java.lang.Long")
  public Date getCacheUpdateDate() {
    return this.getDate(CACHE_UPDATE_DATE);
  }

  public LabProjectsStatistics setCacheUpdateDate(Date cacheUpdateDate) {
    this.addValue(CACHE_UPDATE_DATE, cacheUpdateDate.getTime());
    return this;
  }

  public Long getCacheUpdateDuration() {
    return this.getLong(CACHE_UPDATE_DURATION);
  }

  public LabProjectsStatistics setCacheUpdateDuration(long cacheUpdateDuration) {
    this.addValue(CACHE_UPDATE_DURATION, cacheUpdateDuration);
    return this;
  }
  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
