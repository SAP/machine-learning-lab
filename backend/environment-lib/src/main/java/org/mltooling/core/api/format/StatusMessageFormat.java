package org.mltooling.core.api.format;

import org.mltooling.core.api.format.parser.JsonFormatParser;


public class StatusMessageFormat extends UnifiedFormat {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //

    // ================ Constructors & Main ================================= //
    public StatusMessageFormat() {
    }

    public StatusMessageFormat(String message) {
        getMetadata().setMessage(message);
    }

    public StatusMessageFormat(int statusCode) {
        this.setStatus(statusCode);
    }

    public StatusMessageFormat(String message, int statusCode) {
        this.setStatus(statusCode);
        getMetadata().setMessage(message);
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Private Methods ===================================== //
    public static StatusMessageFormat fromJson(String json) {
        return JsonFormatParser.INSTANCE.fromJson(json, StatusMessageFormat.class);
    }

    // ================ Public Methods ====================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
