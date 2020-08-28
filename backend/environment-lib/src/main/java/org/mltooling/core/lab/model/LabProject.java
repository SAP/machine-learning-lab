package org.mltooling.core.lab.model;

import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class LabProject {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //
    private String id; // lowercased project name
    private String name;
    private String description;
    private String creator;
    private String visibility;

    @ApiModelProperty(dataType = "java.lang.Long")
    private Date createdAt;

    private Boolean isAvailable;

    private Map<String, Object> statistics;
    private List<LabService> services;
    private List<LabExperiment> experiments;
    private LabFileCollection datasets;
    private LabFileCollection models;
    private List<String> members;

    // ================ Constructors & Main ================================= //
    public LabProject() {}

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //
    public String getName() {
        return name;
    }

    public LabProject setName(String name) {
        this.name = name;
        return this;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public LabProject setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public LabProject setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
        return this;
    }

    public LabProjectsStatistics getStatistics() {
        return new LabProjectsStatistics(statistics);
    }

    public LabProject setStatistics(LabProjectsStatistics statistics) {
        this.statistics = statistics.getProperties();
        return this;
    }

    public List<LabService> getServices() {
        return services;
    }

    public LabProject setServices(List<LabService> services) {
        this.services = services;
        return this;
    }

    public List<LabExperiment> getExperiments() {
        return experiments;
    }

    public LabProject setExperiments(List<LabExperiment> experiments) {
        this.experiments = experiments;
        return this;
    }

    public LabFileCollection getDatasets() {
        return datasets;
    }

    public LabProject setDatasets(LabFileCollection datasets) {
        this.datasets = datasets;
        return this;
    }

    public LabFileCollection getModels() {
        return models;
    }

    public LabProject setModels(LabFileCollection models) {
        this.models = models;
        return this;
    }

    public String getId() {
        return id;
    }

    public LabProject setId(String id) {
        this.id = id;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public LabProject setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getCreator() {
        return creator;
    }

    public LabProject setCreator(String creator) {
        this.creator = creator;
        return this;
    }

    @ApiModelProperty(dataType = "java.lang.String")
    public LabProjectVisibility getVisibility() {
        return LabProjectVisibility.from(visibility);
    }

    public LabProject setVisibility(String visibility) {
        this.visibility = visibility;
        return this;
    }

    public LabProject setVisibility(LabProjectVisibility visibility) {
        this.visibility = visibility.getName();
        return this;
    }

    public List<String> getMembers() {
        return members;
    }

    public LabProject setMembers(List<String> members) {
        this.members = members;
        return this;
    }

    public LabProject addMember(String user) {
        if (this.members == null) {
            members = new ArrayList<>();
        }
        this.members.add(user);
        return this;
    }

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
