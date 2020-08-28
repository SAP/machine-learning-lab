package org.mltooling.core.api.format.metadata;

public class UnifiedErrorMessage {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //
    private Integer code;
    private String type;
    private String message;
    private String description;

    // ================ Constructors & Main ================================= //

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
