package org.mltooling.core.lab;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.HttpRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.mltooling.core.api.client.AbstractApiClient;
import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.api.format.StatusMessageFormat;
import org.mltooling.core.api.format.ValueListFormat;
import org.mltooling.core.lab.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LabAdminApiClient extends AbstractApiClient<LabAdminApiClient> implements LabAdminApi {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(LabAdminApiClient.class);

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //
  public LabAdminApiClient(String serviceUrl) {
    super(serviceUrl, null);
  }

  public LabAdminApiClient(String serviceUrl, String authToken) {
    super(serviceUrl, authToken);
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //
  @Override
  public String getEndpointUrl() {
    try {
      return new URI(getServiceUrl()).resolve(ENDPOINT_PATH).toString();
    } catch (URISyntaxException e) {
      log.error("Failed to resolve endpoint URL", e);
      return "";
    }
  }

  @Override
  public StatusMessageFormat checkWorkspace(String workspaceId) {
    return executeRequest(
        Unirest.get(getEndpointUrl() + METHOD_CHECK_WORKSPACE)
            .getHttpRequest()
            .queryString(PARAM_WORKSPACE_ID, workspaceId),
        new TypeToken<StatusMessageFormat>() {}.getType());
  }

  @Override
  public StatusMessageFormat resetAllWorkspaces() {
    return executeRequest(
        Unirest.put(getEndpointUrl() + METHOD_RESET_ALL_WORKSPACES),
        new TypeToken<StatusMessageFormat>() {}.getType());
  }

  @Override
  public ValueListFormat<LabUser> shutdownUnusedWorkspaces(
      boolean dryRun, int daysThreshold, @Nullable List<String> includedIds) {
    if (includedIds == null) {
      includedIds = new ArrayList<>();
    }

    return executeRequest(
        Unirest.put(getEndpointUrl() + METHOD_SHUTDOWN_UNUSED_WORKSPACES)
            .queryString(PARAM_DRY_RUN, dryRun)
            .queryString(PARAM_DAYS_THRESHOLD, daysThreshold)
            .body(new Gson().toJson(includedIds))
            .getHttpRequest(),
        new TypeToken<ValueListFormat<LabUser>>() {}.getType());
  }

  @Override
  public ValueListFormat<String> shutdownDiskExceedingContainers(boolean dryRun) {
    return executeRequest(
        Unirest.put(getEndpointUrl() + METHOD_SHUTDOWN_DISK_EXCEEDING_CONTAINERS)
            .queryString(PARAM_DRY_RUN, dryRun)
            .getHttpRequest(),
        new TypeToken<ValueListFormat<String>>() {}.getType());
  }

  @Override
  public SingleValueFormat<LabService> resetWorkspace(String workspaceId) {
    return executeRequest(
        Unirest.get(getEndpointUrl() + METHOD_RESET_WORKSPACE)
            .getHttpRequest()
            .queryString(PARAM_WORKSPACE_ID, workspaceId),
        new TypeToken<SingleValueFormat<LabService>>() {}.getType());
  }

  @Override
  public SingleValueFormat<LabInfo> getLabInfo() {
    return executeRequest(
        Unirest.get(getEndpointUrl() + METHOD_GET_INFO).getHttpRequest(),
        new TypeToken<SingleValueFormat<LabInfo>>() {}.getType());
  }

  @Override
  public SingleValueFormat<LabProjectsStatistics> getStatistics() {
    return executeRequest(
        Unirest.get(getEndpointUrl() + METHOD_GET_STATISTICS).getHttpRequest(),
        new TypeToken<SingleValueFormat<LabProjectsStatistics>>() {}.getType());
  }

  @Override
  public ValueListFormat<LabEvent> getEvents(@Nullable String event) {
    HttpRequest request = Unirest.get(getEndpointUrl() + METHOD_GET_EVENTS);

    if (event != null) {
      request = request.queryString(PARAM_EVENT, event);
    }

    return executeRequest(request, new TypeToken<ValueListFormat<LabEvent>>() {}.getType());
  }

  // ================ Constants =========================================== //

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
