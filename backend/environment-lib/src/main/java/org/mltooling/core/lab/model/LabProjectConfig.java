package org.mltooling.core.lab.model;

import io.swagger.annotations.ApiModelProperty;


public class LabProjectConfig {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //

    private String name;
    private String description;
    private String visibility;
    @ApiModelProperty(hidden = true)
    private String creator;

    // ================ Constructors & Main ================================= //

    public LabProjectConfig(String name) {
        this.name = name;
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    @ApiModelProperty(dataType = "java.lang.String")
    public LabProjectVisibility getVisibility() {
        return LabProjectVisibility.from(visibility);
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
