package org.mltooling.lab.endpoints;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.api.format.StatusMessageFormat;
import org.mltooling.core.api.format.ValueListFormat;
import org.mltooling.core.lab.LabAdminApi;
import org.mltooling.core.lab.LabApi;
import org.mltooling.core.lab.model.*;
import org.mltooling.core.service.params.DefaultHeaderFields;
import org.mltooling.core.service.utils.AbstractApiEndpoint;
import org.mltooling.core.service.utils.UnifiedResponseFactory;
import org.mltooling.lab.api.LabAdminApiHandler;
import org.mltooling.lab.authorization.AuthorizationManager;
import org.mltooling.lab.services.CoreService;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import org.pac4j.mongo.profile.MongoProfile;

@Api(value = LabAdminApi.ENDPOINT_PATH, tags = "administration")
@Path(LabAdminApi.ENDPOINT_PATH)
@Pac4JSecurity(
    clients = {AuthorizationManager.PAC4J_CLIENT_COOKIE, AuthorizationManager.PAC4J_CLIENT_HEADER},
    authorizers = AuthorizationManager.AUTHORIZER_IS_AUTHENTICATED)
public class LabAdminEndpoint extends AbstractApiEndpoint<LabAdminEndpoint> {
  // ================ Constants =========================================== //

  // ================ Members ============================================= //

  private LabAdminApiHandler adminApiHandler;

  // ================ Constructors & Main ================================= //
  public LabAdminEndpoint(@Context UriInfo uriInfo, @Context HttpHeaders httpHeaders) {
    super(uriInfo, httpHeaders);
    adminApiHandler = new LabAdminApiHandler();
    registerHandler(adminApiHandler);
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  /**
   * Checks whether a research-workspace container for the passed id already exists. If not, a new
   * one is created & started.
   *
   * @param id
   * @param defaultHeaders
   * @return - response object containing the HTTP status
   */
  @GET
  @Path(LabAdminApi.METHOD_CHECK_WORKSPACE)
  @ApiOperation(
      value =
          "Checks whether a workspace container for the passed id already exists. If not, a new"
              + " one is created & started.",
      response = LabServiceResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
    clients = {
      AuthorizationManager.PAC4J_CLIENT_COOKIE,
      AuthorizationManager.PAC4J_CLIENT_HEADER
    },
    authorizers = AuthorizationManager.AUTHORIZER_IS_AUTHENTICATED)
  public Response checkWorkspace(
      @QueryParam(LabAdminApi.PARAM_WORKSPACE_ID) String id,
      @Pac4JProfile MongoProfile commonProfile,
      @BeanParam DefaultHeaderFields defaultHeaders) {
    adminApiHandler.setAuthProfile(commonProfile);
    return UnifiedResponseFactory.getResponse(adminApiHandler.checkWorkspace(id));
  }

  @PUT
  @Path(LabAdminApi.METHOD_SHUTDOWN_UNUSED_WORKSPACES)
  @ApiOperation(
      value = "Shutdown all unused workspaces - 15 days without activity (admin-only).",
      response = ListOfLabUsers.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_ADMIN)
  public Response shutdownUnusedWorkspaces(
      @ApiParam(
              value = "If 'true', it will only return candidates for shutdown.",
              defaultValue = "true",
              required = true)
          @QueryParam(LabAdminApi.PARAM_DRY_RUN)
          Boolean dryRun,
      @ApiParam(
              value = "Number of inactive days to consider workspace unused.",
              defaultValue = "14",
              required = false)
          @QueryParam(LabAdminApi.PARAM_DAYS_THRESHOLD)
          Integer daysThreshold,
      @ApiParam(value = "IDs to include as inactive users.", required = false)
          List<String> includedIds,
      @Pac4JProfile MongoProfile commonProfile,
      @BeanParam DefaultHeaderFields defaultHeaders) {
    if (dryRun == null) {
      dryRun = true;
    }

    if (daysThreshold == null) {
      daysThreshold = 14;
    }

    adminApiHandler.setAuthProfile(commonProfile);
    return UnifiedResponseFactory.getResponse(
        adminApiHandler.shutdownUnusedWorkspaces(dryRun, daysThreshold, includedIds));
  }

  @PUT
  @Path(LabAdminApi.METHOD_SHUTDOWN_DISK_EXCEEDING_CONTAINERS)
  @ApiOperation(
      value = "Remove all workspaces that exceed the disk storage limit (docker-local mode only).",
      response = ListOfStringsResponse.class)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_ADMIN)
  public Response shutdownDiskExceedingContainers(
      @ApiParam(
              value = "If 'true', it will only return candidates for removal.",
              defaultValue = "true",
              required = true)
          @QueryParam(LabAdminApi.PARAM_DRY_RUN)
          Boolean dryRun,
      @Pac4JProfile MongoProfile commonProfile,
      @BeanParam DefaultHeaderFields defaultHeaders) {
    if (dryRun == null) {
      dryRun = true;
    }

    adminApiHandler.setAuthProfile(commonProfile);
    return UnifiedResponseFactory.getResponse(
        adminApiHandler.shutdownDiskExceedingContainers(dryRun));
  }

  @PUT
  @Path(LabAdminApi.METHOD_RESET_ALL_WORKSPACES)
  @ApiOperation(
      value = "Resets all workspaces. Use with caution (admin-only).",
      response = StatusMessageFormat.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_ADMIN)
  public Response resetAllWorkspaces(
      @Pac4JProfile MongoProfile commonProfile, @BeanParam DefaultHeaderFields defaultHeaders) {
    adminApiHandler.setAuthProfile(commonProfile);
    return UnifiedResponseFactory.getResponse(adminApiHandler.resetAllWorkspaces());
  }

  @GET
  @Path(LabAdminApi.METHOD_RESET_WORKSPACE)
  @ApiOperation(
      value =
          "Resets a workspace. Removes the container (keeps all persisted data) and starts a new"
              + " one.  If an image is specified, it will be used instead of the default image.",
      response = LabServiceResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_IS_AUTHENTICATED)
  public Response resetWorkspace(
      @QueryParam(LabAdminApi.PARAM_WORKSPACE_ID) String id,
      @ApiParam(value = "Image Name", required = false) @QueryParam(LabApi.PARAM_DOCKER_IMAGE)
        String image,
      @Pac4JProfile MongoProfile commonProfile,
      @BeanParam DefaultHeaderFields defaultHeaders) {
    adminApiHandler.setAuthProfile(commonProfile);
    return UnifiedResponseFactory.getResponse(adminApiHandler.resetWorkspace(id, image));
  }

  @GET
  @Path(LabAdminApi.METHOD_GET_STATISTICS)
  @ApiOperation(
      value = "Returns statistics about this Lab instance (admin-only).",
      response = LabStatisticsResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_ADMIN)
  public Response getStatistics(@BeanParam DefaultHeaderFields defaultHeaders) {
    return UnifiedResponseFactory.getResponse(adminApiHandler.getStatistics());
  }

  @GET
  @Path(LabAdminApi.METHOD_GET_INFO)
  @ApiOperation(
      value = "Returns information about this Lab instance.",
      response = LabInfoResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(ignore = true)
  public Response getLabInfo(@BeanParam DefaultHeaderFields defaultHeaders) {
    return UnifiedResponseFactory.getResponse(adminApiHandler.getLabInfo());
  }

  @GET
  @Path(LabAdminApi.METHOD_GET_EVENTS)
  @ApiOperation(
      value = "Returns events filtered by a specified event type (admin-only).",
      response = ListOfLabEventsResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_ADMIN)
  public Response getEvents(
      @ApiParam(
              value = "Event Type. If not provided, all events will be returned.",
              required = false)
          @QueryParam(LabAdminApi.PARAM_EVENT)
          String event,
      @BeanParam DefaultHeaderFields defaultHeaders) {
    return UnifiedResponseFactory.getResponse(adminApiHandler.getEvents(event));
  }

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //
  private static class StringResponse extends SingleValueFormat<String> {

    public String data;
  }

  private static class LabInfoResponse extends SingleValueFormat<LabInfo> {

    public LabInfo data;
  }

  private static class LabStatisticsResponse extends SingleValueFormat<LabProjectsStatistics> {

    public LabProjectsStatistics data;
  }

  private static class LabServiceResponse extends SingleValueFormat<LabService> {

    public LabService data;
  }

  private static class ListOfStringsResponse extends ValueListFormat<String> {

    public List<String> data;
  }

  private static class ListOfLabEventsResponse extends ValueListFormat<LabEvent> {

    public List<LabEvent> data;
  }

  private static class ListOfLabUsers extends ValueListFormat<LabUser> {

    public List<LabUser> data;
  }

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
