package org.mltooling.core.lab.model;

import io.swagger.annotations.ApiModelProperty;
import java.util.Date;
import java.util.Map;

public class LabScheduledJob {
  // ================ Constants =========================================== //

  // ================ Members ============================================= //
  private String id;
  private String jobName;
  private String dockerImage;
  private Map<String, String> configuration;
  private String schedule;

  @ApiModelProperty(dataType = "java.lang.Long")
  private Date addedAt;

  @ApiModelProperty(dataType = "java.lang.Long")
  private Date lastExecution;

  // ================ Constructors & Main ================================= //
  public LabScheduledJob() {}

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //
  public String getId() {
    return id;
  }

  public LabScheduledJob setId(String id) {
    this.id = id;
    return this;
  }

  public String getJobName() {
    return jobName;
  }

  public LabScheduledJob setJobName(String jobName) {
    this.jobName = jobName;
    return this;
  }

  public String getDockerImage() {
    return dockerImage;
  }

  public LabScheduledJob setDockerImage(String dockerImage) {
    this.dockerImage = dockerImage;
    return this;
  }

  public Map<String, String> getConfiguration() {
    return configuration;
  }

  public LabScheduledJob setConfiguration(Map<String, String> configuration) {
    this.configuration = configuration;
    return this;
  }

  public String getSchedule() {
    return schedule;
  }

  public LabScheduledJob setSchedule(String schedule) {
    this.schedule = schedule;
    return this;
  }

  public Date getAddedAt() {
    return addedAt;
  }

  public LabScheduledJob setAddedAt(Date addedAt) {
    this.addedAt = addedAt;
    return this;
  }

  public Date getLastExecution() {
    return lastExecution;
  }

  public LabScheduledJob setLastExecution(Date lastExecution) {
    this.lastExecution = lastExecution;
    return this;
  }

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
