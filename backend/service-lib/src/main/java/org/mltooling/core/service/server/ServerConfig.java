package org.mltooling.core.service.server;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.UriBuilder;

public class ServerConfig {

  // ================ Constants =========================================== //
  public static final String DEFAULT_HOST = "0.0.0.0";
  public static final String DEFAULT_PROTOCOL = "http";
  public static final int DEFAULT_PORT = 8090;
  public static final String DEFAULT_API_DOCS_ENDPOINT = "/docs/";

  // ================ Members ============================================= //
  private String baseUrl = "";
  private Integer servicePort;
  private String endpointPackage;
  private ContainerRequestFilter authFilter;
  private DefaultServerFactory.ServerCallback serverCallback;
  private boolean loggingActivated = true;
  private String apiDocsEndpoint;

  // ================ Constructors & Main ================================= //
  public ServerConfig(String endpointPackage) {
    this(null, endpointPackage);
  }

  public ServerConfig(Integer servicePort, String endpointPackage) {
    this.endpointPackage = endpointPackage;
    this.servicePort = servicePort;
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  public ServerConfig setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
    return this;
  }

  public String getAddress() {
    return UriBuilder.fromPath(this.baseUrl + "/")
        .scheme(DEFAULT_PROTOCOL)
        .host(DEFAULT_HOST)
        .port(getServicePort())
        .build()
        .toString();
  }

  public String getEndpointPackage() {
    return endpointPackage;
  }

  public ServerConfig setEndpointPackage(String endpointPackage) {
    this.endpointPackage = endpointPackage;
    return this;
  }

  public ContainerRequestFilter getAuthFilter() {
    return authFilter;
  }

  public ServerConfig setAuthFilter(ContainerRequestFilter authFilter) {
    this.authFilter = authFilter;
    return this;
  }

  public DefaultServerFactory.ServerCallback getServerCallback() {
    return serverCallback;
  }

  public ServerConfig setServerCallback(DefaultServerFactory.ServerCallback serverCallback) {
    this.serverCallback = serverCallback;
    return this;
  }

  public Integer getServicePort() {
    if (this.servicePort == null) {
      return DEFAULT_PORT;
    }
    return servicePort;
  }

  public ServerConfig setServicePort(Integer servicePort) {
    this.servicePort = servicePort;
    return this;
  }

  public ServerConfig setLoggingActivated(boolean loggingActivated) {
    this.loggingActivated = loggingActivated;
    return this;
  }

  public boolean isLoggingActivated() {
    return this.loggingActivated;
  }

  public String getApiDocsEndpoint() {
    if (this.apiDocsEndpoint == null) {
      return DEFAULT_API_DOCS_ENDPOINT;
    }
    return apiDocsEndpoint;
  }

  public ServerConfig setApiDocsEndpoint(String apiDocsEndpoint) {
    this.apiDocsEndpoint = apiDocsEndpoint;
    return this;
  }

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
