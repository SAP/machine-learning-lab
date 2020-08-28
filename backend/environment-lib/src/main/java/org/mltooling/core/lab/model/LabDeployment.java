package org.mltooling.core.lab.model;

import io.swagger.annotations.ApiModelProperty;

import java.util.Date;
import java.util.Map;


public class LabDeployment<T extends LabDeployment<T>> {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //
    private String name;
    private String dockerId;
    private String dockerName;
    private String dockerImage;
    private String adminLink;
    @ApiModelProperty(dataType = "java.lang.Long")
    private Date startedAt;
    private String status;
    private String featureType;
    private Map<String, String> configuration;
    private Map<String, String> labels;

    // ================ Constructors & Main ================================= //

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //
    public String getName() {
        return name;
    }

    public T setName(String name) {
        this.name = name;
        return (T) this;
    }

    public String getDockerId() {
        return dockerId;
    }

    public T setDockerId(String dockerId) {
        this.dockerId = dockerId;
        return (T) this;
    }

    public String getDockerImage() {
        return dockerImage;
    }

    public T setDockerImage(String dockerImage) {
        this.dockerImage = dockerImage;
        return (T) this;
    }

    public String getAdminLink() {
        return adminLink;
    }

    public T setAdminLink(String adminLink) {
        this.adminLink = adminLink;
        return (T) this;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public T setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
        return (T) this;
    }

    public String getStatus() {
        return status;
    }

    public T setStatus(String status) {
        this.status = status;
        return (T) this;
    }

    public Map<String, String> getConfiguration() {
        return configuration;
    }

    public T setConfiguration(Map<String, String> configuration) {
        this.configuration = configuration;
        return (T) this;
    }

    public String getDockerName() {
        return dockerName;
    }

    public T setDockerName(String dockerName) {
        this.dockerName = dockerName;
        return (T) this;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public T setLabels(Map<String, String> labels) {
        this.labels = labels;
        return (T) this;
    }

    public String getFeatureType() {
        return featureType;
    }

    public T setFeatureType(String featureType) {
        this.featureType = featureType;
        return (T) this;
    }

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
