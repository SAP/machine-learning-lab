package org.mltooling.core.lab.model;

import java.util.Map;


public class LabInfo {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //
    private String version;
    private String runtime;
    private String namespace;
    private Integer projectsCount;
    private Boolean isHealthy;
    private String termsOfService;
    private Map<String, String> coreServiceInfo;

    // ================ Constructors & Main ================================= //

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //
    public String getVersion() {
        return version;
    }

    public LabInfo setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getRuntime() {
        return runtime;
    }

    public LabInfo setRuntime(String runtime) {
        this.runtime = runtime;
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public LabInfo setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Integer getProjectsCount() {
        return projectsCount;
    }

    public LabInfo setProjectsCount(Integer projectsCount) {
        this.projectsCount = projectsCount;
        return this;
    }

    public Map<String, String> getCoreServiceInfo() {
        return coreServiceInfo;
    }

    public LabInfo setCoreServiceInfo(Map<String, String> coreServiceInfo) {
        this.coreServiceInfo = coreServiceInfo;
        return this;
    }

    public Boolean getHealthy() {
        return isHealthy;
    }

    public LabInfo setHealthy(Boolean healthy) {
        isHealthy = healthy;
        return this;
    }

    public String getTermsOfService() {
        return termsOfService;
    }

    public LabInfo setTermsOfService(String termsOfService) {
        this.termsOfService = termsOfService;
        return this;
    }

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
