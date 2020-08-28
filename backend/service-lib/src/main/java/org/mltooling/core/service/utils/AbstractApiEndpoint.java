package org.mltooling.core.service.utils;

import org.mltooling.core.api.handler.AbstractApiHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.Map;


public abstract class AbstractApiEndpoint<T extends AbstractApiEndpoint<T>> {

    // ================ Constants =========================================== //
    protected final Logger log = LoggerFactory.getLogger(getClass());

    // ================ Members ============================================= //
    protected UriInfo uriInfo;
    protected HttpHeaders httpHeaders;
    // ================ Constructors & Main ================================= //

    public AbstractApiEndpoint(UriInfo uriInfo, HttpHeaders httpHeaders) {
        this.uriInfo = uriInfo;
        this.httpHeaders = httpHeaders;
    }
    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //

    // ================ Private Methods ===================================== //
    protected void registerHandler(AbstractApiHandler apiHandler) {
        apiHandler.setHeaders(getHeaders());
        apiHandler.setQueryParams(getQueryParams());
        if (uriInfo.getAbsolutePath() != null) {
            apiHandler.setRequestUrl(uriInfo.getAbsolutePath().getRawPath());
        }
    }

    protected Map<String, String> getHeaders() {
        return EndpointUtils.converHeadersToMap(httpHeaders);
    }

    protected Map<String, String> getQueryParams() {
        return EndpointUtils.convertUriInfoToMap(uriInfo);
    }

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
