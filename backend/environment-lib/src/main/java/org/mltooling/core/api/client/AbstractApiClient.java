package org.mltooling.core.api.client;

import com.google.gson.JsonSyntaxException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.http.HttpStatus;
import org.mltooling.core.api.basics.SystemApi;
import org.mltooling.core.api.basics.SystemApiClient;
import org.mltooling.core.api.format.UnifiedFormat;
import org.mltooling.core.api.format.parser.JsonFormatParser;
import org.mltooling.core.api.utils.ApiUtils;
import org.mltooling.core.api.utils.DefaultRequestParams;
import org.mltooling.core.utils.PerfLogger;
import org.mltooling.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractApiClient<T extends AbstractApiClient<T>> {

  // ================ Constants =========================================== //
  protected final Logger log = LoggerFactory.getLogger(getClass());

  // ================ Members ============================================= //
  protected Map<String, Object> queryParams = new HashMap<>();
  protected Map<String, String> headers = new HashMap<>();
  protected String serviceUrl;
  protected String authToken;

  // ================ Constructors & Main ================================= //
  public AbstractApiClient(String serviceUrl, @Nullable String authToken) {
    init(serviceUrl, authToken);
  }

  private void init(String serviceUrl, String authToken) {
    this.serviceUrl = serviceUrl;

    if (!StringUtils.isNullOrEmpty(authToken)) {
      this.setAuthToken(authToken);
    }
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //
  public abstract String getEndpointUrl();

  // ================ Public Methods ====================================== //
  public boolean isAvailable() {
    SystemApi systemApi = new SystemApiClient(serviceUrl, authToken);
    return systemApi.isHealthy().isSuccessful();
  }

  // ================ Private Methods ===================================== //
  protected <F extends UnifiedFormat> F executeRequest(HttpRequest request, Type type) {
    F unifiedFormat = null;

    Class<F> responseClass = null;

    if (type instanceof Class) {
      responseClass = (Class<F>) type;
    } else if (type instanceof ParameterizedType) {
      responseClass = (Class<F>) ((ParameterizedType) type).getRawType();
    } else {
      log.error("Unable to detect response class.");
      return null;
    }

    try {
      unifiedFormat = responseClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      log.error("Unable to create response class instance.");
      return null;
    }

    try {
      HttpResponse<String> response = executeRequest(request);
      String responseBody = getResponseBody(request.getUrl(), response);
      if (!StringUtils.isNullOrEmpty(responseBody)) {
        unifiedFormat = JsonFormatParser.INSTANCE.fromJson(responseBody, type);
      } else {
        unifiedFormat.setErrorStatus(response.getStatusText(), response.getStatus());
      }
    } catch (UnirestException | JsonSyntaxException e) {
      log.warn("Exception while requesting data: " + e.getMessage()); // Don't log stacktrace here
      unifiedFormat.setErrorStatus(e);
    }
    return unifiedFormat;
  }

  protected <F extends UnifiedFormat> F executeRequest(HttpRequest request, Class<F> formatClass) {
    F unifiedFormat;
    try {
      unifiedFormat = formatClass.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      return null;
    }

    try {
      HttpResponse<String> response = executeRequest(request);
      String responseBody = getResponseBody(request.getUrl(), response);
      if (!StringUtils.isNullOrEmpty(responseBody)) {
        unifiedFormat = JsonFormatParser.INSTANCE.fromJson(responseBody, formatClass);
      } else {
        unifiedFormat.setErrorStatus(response.getStatusText(), response.getStatus());
      }
    } catch (UnirestException | JsonSyntaxException e) {
      log.warn("Exception while requesting data: " + e.getMessage()); // Don't log stacktrace here
      unifiedFormat.setErrorStatus(e);
    }

    return unifiedFormat;
  }

  protected HttpResponse<String> executeRequest(HttpRequest request) throws UnirestException {
    // always reset the auth token -> headers are cleaned after every call
    if (!StringUtils.isNullOrEmpty(this.authToken)
        && StringUtils.isNullOrEmpty(headers.get(ApiUtils.AUTHORIZATION_HEADER))) {
      // set auth token if not already provided
      this.setAuthToken(authToken);
    }

    request = request.queryString(queryParams).headers(headers);

    // clean all query parameter & header after every request
    queryParams.clear();
    headers.clear();

    PerfLogger pl = new PerfLogger();
    pl.start();
    log.debug("Execute request: " + request.getHttpMethod().name() + " " + request.getUrl());
    HttpResponse<String> response = request.asString();

    log.debug(
        "Executed request: "
            + request.getHttpMethod().name()
            + " "
            + request.getUrl()
            + " ["
            + pl.end()
            + " ms]");
    return response;
  }

  private String getResponseBody(String requestUrl, HttpResponse<String> response) {
    if (response.getStatus() == HttpStatus.SC_BAD_GATEWAY) {
      log.info(
          "Request for "
              + requestUrl
              + " was unsuccessful with status: "
              + response.getStatusText()
              + " The service is probably not started yet.");
    } else if (response.getStatus() < HttpStatus.SC_OK
        || response.getStatus() >= HttpStatus.SC_MULTIPLE_CHOICES) {
      log.info(
          "Request for "
              + requestUrl
              + " was unsuccessful with status: "
              + response.getStatusText()
              + "("
              + response.getStatus()
              + "). Body: "
              + StringUtils.abbreviate(response.getBody(), 500));
    }

    String responseBody = response.getBody();

    if (StringUtils.isNullOrEmpty(responseBody)) {
      log.info("Response body for " + requestUrl + " is null or empty");
    }

    return responseBody;
  }

  public String getServiceUrl() {
    return serviceUrl;
  }

  public T setServiceUrl(String serviceUrl) {
    this.serviceUrl = serviceUrl;
    return (T) this;
  }

  public T setDefaultParams(DefaultRequestParams defaultRequestParams) {
    queryParams.putAll(defaultRequestParams.getParams());
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public T setQueryParam(String name, Object value) {
    if (StringUtils.isNullOrEmpty(name)) {
      return (T) this;
    }

    queryParams.put(name.trim(), value);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public T setHeader(String name, String value) {
    if (StringUtils.isNullOrEmpty(name)) {
      return (T) this;
    }

    headers.put(name.trim(), value);
    return (T) this;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public String getAuthToken() {
    return authToken;
  }

  // Default parameters
  @SuppressWarnings("unchecked")
  public T setAuthToken(String token) {
    this.authToken = token;

    if (StringUtils.isNullOrEmpty(token)) {
      return (T) this;
    }

    setHeader(ApiUtils.AUTHORIZATION_HEADER, token);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public T setLimit(Integer limit) {
    if (limit == null) {
      return (T) this;
    }

    setQueryParam(DefaultRequestParams.LIMIT, limit);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public T setSelect(String types) {
    if (StringUtils.isNullOrEmpty(types)) {
      return (T) this;
    }

    setQueryParam(DefaultRequestParams.SELECT, types);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public T setExclude(String types) {
    if (StringUtils.isNullOrEmpty(types)) {
      return (T) this;
    }

    setQueryParam(DefaultRequestParams.EXCLUDE, types);
    return (T) this;
  }

  @SuppressWarnings("unchecked")
  public T setFields(String fields) {
    if (StringUtils.isNullOrEmpty(fields)) {
      return (T) this;
    }

    setQueryParam(DefaultRequestParams.FIELDS, fields);
    return (T) this;
  }

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
