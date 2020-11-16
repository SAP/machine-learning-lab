package org.mltooling.core.lab.model;

import io.swagger.annotations.ApiModelProperty;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

public class LabUser {
  // ================ Constants =========================================== //

  // ================ Members ============================================= //
  private String id;
  private String name;

  // TODO createdAt?

  private Set<String> permissions;
  private Map<String, Object> attributes;

  @ApiModelProperty(dataType = "java.lang.Long")
  private Date lastActivity;

  // ================ Constructors & Main ================================= //
  public LabUser() {}

  // ================ Constructors & Main ================================= //

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //
  public String getName() {
    return name;
  }

  public LabUser setName(String name) {
    this.name = name;
    return this;
  }

  public Collection<String> getPermissions() {
    return permissions;
  }

  public LabUser setPermissions(Set<String> permissions) {
    this.permissions = permissions;
    return this;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public LabUser setAttributes(Map<String, Object> attributes) {
    this.attributes = attributes;
    return this;
  }

  public String getId() {
    return id;
  }

  public LabUser setId(String id) {
    this.id = id;
    return this;
  }

  public Date getLastActivity() {
    return lastActivity;
  }

  public LabUser setLastActivity(Date lastActivity) {
    this.lastActivity = lastActivity;
    return this;
  }

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
