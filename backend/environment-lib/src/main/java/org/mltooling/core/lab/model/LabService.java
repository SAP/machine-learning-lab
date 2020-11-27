package org.mltooling.core.lab.model;

import io.swagger.annotations.ApiModelProperty;
import java.util.Date;
import java.util.Set;

public class LabService extends LabDeployment<LabService> {
  // ================ Constants =========================================== //

  // ================ Members ============================================= //
  private Boolean isHealthy;

  @ApiModelProperty(dataType = "java.lang.Long")
  private Date modifiedAt;

  // The main port of the service -> either the only exposed one or 8091 if exposed, or the first
  // exposed one.
  private Integer connectionPort;
  private Set<Integer> exposedPorts;

  // ================ Constructors & Main ================================= //
  public LabService() {}

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  public Integer getConnectionPort() {
    return connectionPort;
  }

  public LabService setConnectionPort(Integer connectionPort) {
    this.connectionPort = connectionPort;
    return this;
  }

  public Boolean getIsHealthy() {
    return isHealthy;
  }

  public LabService setIsHealthy(Boolean isHealthy) {
    this.isHealthy = isHealthy;
    return this;
  }

  public Date getModifiedAt() {
    return modifiedAt;
  }

  public LabService setModifiedAt(Date modifiedAt) {
    this.modifiedAt = modifiedAt;
    return this;
  }

  public Set<Integer> getExposedPorts() {
    return exposedPorts;
  }

  public LabService setExposedPorts(Set<Integer> exposedPorts) {
    this.exposedPorts = exposedPorts;
    return this;
  }

  public LabService addExposedPort(Integer exposedPort) {
    this.exposedPorts.add(exposedPort);
    return this;
  }
  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
