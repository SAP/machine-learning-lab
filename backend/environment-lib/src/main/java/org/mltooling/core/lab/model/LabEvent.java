package org.mltooling.core.lab.model;

import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.Map;


public class LabEvent {

    // ================ Constants =========================================== //
    public static final String DOWNLOADED_FILE = "downloaded-file";
    public static final String SHUTDOWN_UNUSED_WORKSPACES = "shutdown-unused-workspaces";
    public static final String LONG_REQUEST = "long-request";
    public static final String DELETE_PROJECT = "delete-project";
    public static final String DELETE_EXPERIMENT = "delete-experiment";
    public static final String DELETE_FILE = "delete-file";
    public static final String DELETE_SERVICE = "delete-service";
    public static final String DELETE_JOB = "delete-job";
    public static final String DELETE_SCHEDULED_JOB = "delete-scheduled-job";
    public static final String CREATE_ADMIN_USER = "create-admin-user";
    public static final String DELETE_USER = "delete-user";
    public static final String RESET_ALL_WORKSPACES = "reset-all-workspaces";

    // ================ Members ============================================= //
    private String name;
    private Date createdAt;
    private Map<String, Object> attributes;

    // ================ Constructors & Main ================================= //
    public LabEvent() {}

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //
    public String getName() {
        return name;
    }

    public LabEvent setName(String name) {
        this.name = name;
        return this;
    }

    @ApiModelProperty(dataType = "java.lang.Long")
    public Date getCreatedAt() {
        return createdAt;
    }

    public LabEvent setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public LabEvent setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
