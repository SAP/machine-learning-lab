package org.mltooling.core.api.format.metadata;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;

// when more metadata is required -> just extend from this class
public class UnifiedFormatMetadata {

  // ================ Constants =========================================== //
  private static final String TIME = "time"; // execution time
  private static final String MESSAGE = "message";
  private static final String QUERY = "query";
  private static final String STATUS = "status";

  // ================ Members ============================================= //
  private HashMap<String, Object> metadata;

  // ================ Constructors & Main ================================= //

  public UnifiedFormatMetadata(HashMap<String, Object> metadata) {
    this.metadata = metadata;
  }

  public UnifiedFormatMetadata() {}

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Private Methods ===================================== //

  // ================ Public Methods ====================================== //

  // ================ Getter & Setter ===================================== //
  public void addMetadata(String key, Object value) {
    getMetadataMap().put(key, value);
  }

  public Object getMetadata(String key) {
    return getMetadataMap().get(key);
  }

  public Long getTime() {
    return (Long) getMetadata(TIME);
  }

  public void setTime(Long executionTime) {
    addMetadata(TIME, executionTime);
  }

  public String getMessage() {
    return (String) getMetadata(MESSAGE);
  }

  public void setMessage(String message) {
    addMetadata(MESSAGE, message);
  }

  public String getQuery() {
    return (String) getMetadata(QUERY);
  }

  public void setQuery(String query) {
    addMetadata(QUERY, query);
  }

  public Integer getStatus() {
    Object status = getMetadata(STATUS);
    if (status == null) {
      return null;
    }

    if (status instanceof Double) {
      return ((Double) status).intValue();
    }
    return (Integer) status;
  }

  public void setStatus(Integer status) {
    addMetadata(STATUS, status);
  }

  @ApiModelProperty(hidden = true)
  public HashMap<String, Object> getMetadataMap() {
    if (metadata == null) {
      metadata = new HashMap<>();
    }
    return metadata;
  }

  // ================ Builder Pattern ===================================== //

  public UnifiedFormatMetadata withMetadata(String key, Object value) {
    addMetadata(key, value);
    return this;
  }

  // ================ Inner & Anonymous Classes =========================== //
}
