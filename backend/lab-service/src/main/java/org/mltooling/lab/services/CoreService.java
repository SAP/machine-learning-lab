package org.mltooling.lab.services;

import org.mltooling.core.utils.StringUtils;
import org.mltooling.lab.LabConfig;

public enum CoreService {
  LAB_BACKEND("backend", LabConfig.BACKEND_SERVICE_IMAGE, 8091),
  UNIFIED_MODEL_SERVICE("model-service", LabConfig.MODEL_SERVICE_IMAGE, 8091),
  WORKSPACE("workspace", LabConfig.WORKSPACE_IMAGE, 8091),
  MINIO("minio", LabConfig.MINIO_IMAGE, 9000),
  MONGO("mongo", LabConfig.MONGO_IMAGE, 27017),
  PORTAINER("service-admin", LabConfig.PORTAINER_IMAGE, 9000),
  NFS("nfs-server", LabConfig.NFS_SERVER_IMAGE, 2049),
  UNKNOWN("unknown", ""); // Fallback configuration

  // ================ Constants =========================================== //

  // ================ Members ============================================= //
  String name;
  String image;
  Integer connectionPort;

  // ================ Constructors & Main ================================= //

  CoreService(String name, String image) {
    this(name, image, AbstractServiceManager.DEFAULT_CONNECTION_PORT);
  }

  CoreService(String name, String image, Integer connectionPort) {
    this.name = name;
    this.image = image;
    this.connectionPort = connectionPort;
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //
  @Override
  public String toString() {
    return getName();
  }

  // ================ Public Methods ====================================== //
  public static CoreService from(String name) {
    if (StringUtils.isNullOrEmpty(name)) {
      return UNKNOWN;
    }

    for (CoreService service : CoreService.values()) {
      if (name.equalsIgnoreCase(service.getName())) {
        return service;
      }
      if (name.toLowerCase().contains(service.getImage().toLowerCase())) {
        return service;
      }
    }
    return UNKNOWN;
  }

  public boolean isUnknown() {
    return this.equals(UNKNOWN);
  }

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //
  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }

  public Integer getConnectionPort() {
    return connectionPort;
  }

  public void setConnectionPort(Integer connectionPort) {
    this.connectionPort = connectionPort;
  }

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
