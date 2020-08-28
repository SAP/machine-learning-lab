package org.mltooling.core.service.basics.handlers;

import org.mltooling.core.api.basics.SystemApi;
import org.mltooling.core.api.format.StatusMessageFormat;
import org.mltooling.core.api.handler.AbstractApiHandler;
import org.apache.http.HttpStatus;


public class SystemApiHandler extends AbstractApiHandler<SystemApiHandler> implements SystemApi {

    // ================ Constants =========================================== //

    // ================ Members ============================================= //
    private static HealthCheck healthCallback;

    // ================ Constructors & Main ================================= //

    // ================ Methods for/from SuperClass / Interfaces ============ //
    @Override
    public StatusMessageFormat isHealthy() {
        StatusMessageFormat response = new StatusMessageFormat();
        try {
            if (healthCallback == null) {
                // healthcheck not configured, return healthy as default.
                response.setSuccessfulStatus();
                return prepareResponse(response);
            }

            try {
                if (healthCallback.isHealthy()) {
                    response.setSuccessfulStatus();
                    return prepareResponse(response);
                } else {
                    response.setErrorStatus("Healthcheck failed. Service is not healthy.", HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    return prepareResponse(response);
                }
            } catch (Exception e) {
                response.setErrorStatus(e);
                return prepareResponse(response);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            response.setErrorStatus(e);
            return prepareResponse(response);
        }
    }

    // ================ Public Methods ====================================== //
    public static void setHealthCheck(HealthCheck healthCheck) {
        healthCallback = healthCheck;
    }
    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //


    // ================ Inner & Anonymous Classes =========================== //
    public interface HealthCheck {

        boolean isHealthy() throws Exception;
    }
}
