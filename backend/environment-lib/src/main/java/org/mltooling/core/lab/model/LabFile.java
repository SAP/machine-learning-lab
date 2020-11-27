package org.mltooling.core.lab.model;

import io.swagger.annotations.ApiModelProperty;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;

public class LabFile {

  // ================ Constants =========================================== //
  public static final String META_MODIFIED_BY = "modified-by";
  public static final String META_PROJECT = "project";
  public static final String META_CONTENT_TYPE = "content-type";
  public static final String META_EXPERIMENT = "experiment";

  // ================ Members ============================================= //
  private String name;
  private String key;
  private String modifiedBy;

  @ApiModelProperty(dataType = "java.lang.Long")
  private Date modifiedAt;

  private Long size;
  private String hash;
  private Integer version;
  private String contentType;
  private String dataType;
  private String description;
  private HashMap<String, String> metadata;

  @ApiModelProperty(hidden = true)
  private transient InputStream fileStream;

  // ================ Constructors & Main ================================= //
  public LabFile() {}

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //
  public String getName() {
    return name;
  }

  public LabFile setName(String name) {
    this.name = name;
    return this;
  }

  public String getKey() {
    return key;
  }

  public LabFile setKey(String key) {
    this.key = key;
    return this;
  }

  public String getModifiedBy() {
    return modifiedBy;
  }

  public LabFile setModifiedBy(String modifiedBy) {
    this.modifiedBy = modifiedBy;
    return this;
  }

  public Date getModifiedAt() {
    return modifiedAt;
  }

  public LabFile setModifiedAt(Date modifiedAt) {
    this.modifiedAt = modifiedAt;
    return this;
  }

  public long getSize() {
    return size;
  }

  public LabFile setSize(Long size) {
    this.size = size;
    return this;
  }

  public Integer getVersion() {
    return version;
  }

  public LabFile setVersion(Integer version) {
    this.version = version;
    return this;
  }

  public String getContentType() {
    return contentType;
  }

  public LabFile setContentType(String contentType) {
    this.contentType = contentType;
    return this;
  }

  public String toString() {
    return name + (version != null ? " (v" + version + ")" : "");
  }

  @ApiModelProperty(dataType = "java.lang.String")
  public LabFileDataType getDataType() {
    return LabFileDataType.from(dataType);
  }

  public LabFile setDataType(LabFileDataType type) {
    this.dataType = type.getName();
    return this;
  }

  public HashMap<String, String> getMetadata() {
    return metadata;
  }

  public LabFile setMetadata(HashMap<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }

  public InputStream getFileStream() {
    return fileStream;
  }

  public LabFile setFileStream(InputStream fileStream) {
    this.fileStream = fileStream;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public LabFile setDescription(String description) {
    this.description = description;
    return this;
  }

  public String getHash() {
    return hash;
  }

  public LabFile setHash(String hash) {
    this.hash = hash;
    return this;
  }

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
