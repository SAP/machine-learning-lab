package org.mltooling.core.api.format;

import org.mltooling.core.api.format.parser.JsonFormatParser;


public class ErrorMessageFormat extends UnifiedFormat {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //

    // ================ Constructors & Main ================================= //
    public ErrorMessageFormat(int statusCode, String message) {
        setErrorStatus(message, statusCode);
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Private Methods ===================================== //
    public static ErrorMessageFormat fromJson(String json) {
        return JsonFormatParser.INSTANCE.fromJson(json, ErrorMessageFormat.class);
    }

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
