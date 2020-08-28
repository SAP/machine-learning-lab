package org.mltooling.core.api.handler;

import org.mltooling.core.api.format.UnifiedFormat;
import org.mltooling.core.api.utils.DefaultRequestParams;
import org.mltooling.core.utils.PerfLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public abstract class AbstractApiHandler<T extends AbstractApiHandler<T>> {

    // ================ Constants =========================================== //
    protected final Logger log = LoggerFactory.getLogger(getClass());

    // ================ Members ============================================= //
    private PerfLogger perfLogger = new PerfLogger();

    protected Map<String, String> queryParams = new HashMap<>();
    protected Map<String, String> headers = new HashMap<>();
    protected String requestUrl;

    // ================ Constructors & Main ================================= //
    public AbstractApiHandler() {
        this(new HashMap<String, String>(), new HashMap<String, String>());
    }

    public AbstractApiHandler(Map<String, String> queryParams, Map<String, String> headers) {
        perfLogger.start();

        this.queryParams = queryParams;
        if (this.queryParams == null) {
            this.queryParams = new HashMap<>();
        }

        this.headers = headers;
        if (this.headers == null) {
            this.headers = new HashMap<>();
        }
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //
    public DefaultRequestParams getRequestParams() {
        return new DefaultRequestParams(queryParams);
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    // ================ Private Methods ===================================== //
    protected long getExecutionTime() {
        return perfLogger.end();
    }

    protected <F extends UnifiedFormat> F prepareResponse(F unifiedFormat, boolean applyDefaultParameters) {
        if (getRequestParams() == null || unifiedFormat == null) {
            return unifiedFormat;
        }

        if (!applyDefaultParameters) {
            unifiedFormat.prepareResponse(new DefaultRequestParams());
        } else {
            unifiedFormat.prepareResponse(getRequestParams());
        }

        unifiedFormat.getMetadata().setTime(getExecutionTime());

        // clean all query parameter & header after every request
        queryParams.clear();
        headers.clear();

        return unifiedFormat;
    }

    protected <F extends UnifiedFormat> F prepareResponse(F unifiedFormat) {
        return prepareResponse(unifiedFormat, true);
    }

    // ================ Getter & Setter ===================================== //
    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    // ================ Builder Pattern ===================================== //
    public T setDefaultParams(DefaultRequestParams defaultRequestParams) {
        queryParams.putAll(defaultRequestParams.getParams());
        return (T) this;
    }

    // ================ Inner & Anonymous Classes =========================== //
}
