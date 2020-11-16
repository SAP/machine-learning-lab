package org.mltooling.core.lab.model;

import io.swagger.annotations.ApiModelProperty;
import java.util.Date;

public class LabJob extends LabDeployment<LabJob> {

  // ================ Constants =========================================== //
  public enum State {
    RUNNING("running"),
    SUCCEEDED("succeeded"),
    FAILED("failed");

    private String name;

    State(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return getName();
    }
  }

  // ================ Members ============================================= //
  @ApiModelProperty(dataType = "java.lang.Long")
  private Date finishedAt;

  private Integer exitCode;

  // ================ Constructors & Main ================================= //
  public LabJob() {}

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //
  public LabJob setStatus(LabJob.State status) {
    setStatus(status.getName());
    return this;
  }

  public Date getFinishedAt() {
    return finishedAt;
  }

  public LabJob setFinishedAt(Date finishedAt) {
    this.finishedAt = finishedAt;
    return this;
  }

  public Integer getExitCode() {
    return exitCode;
  }

  public LabJob setExitCode(Integer exitCode) {
    this.exitCode = exitCode;
    return this;
  }

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
