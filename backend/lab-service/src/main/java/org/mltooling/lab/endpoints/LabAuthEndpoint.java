package org.mltooling.lab.endpoints;

import com.google.api.client.http.HttpStatusCodes;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.util.Date;
import java.util.List;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.api.format.StatusMessageFormat;
import org.mltooling.core.api.format.ValueListFormat;
import org.mltooling.core.lab.LabApi;
import org.mltooling.core.lab.LabAuthApi;
import org.mltooling.core.lab.model.LabUser;
import org.mltooling.core.service.params.DefaultHeaderFields;
import org.mltooling.core.service.utils.AbstractApiEndpoint;
import org.mltooling.core.service.utils.UnifiedResponseFactory;
import org.mltooling.core.utils.ListUtils;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.lab.ComponentManager;
import org.mltooling.lab.LabConfig;
import org.mltooling.lab.api.LabAuthApiHandler;
import org.mltooling.lab.authorization.AuthorizationManager;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import org.pac4j.mongo.profile.MongoProfile;

/** Endpoint for all authorization-relevant functionality */
@Api(value = LabAuthApi.ENDPOINT_PATH, tags = "authorization")
@Path(LabAuthApi.ENDPOINT_PATH)
@Pac4JSecurity(
    clients = {AuthorizationManager.PAC4J_CLIENT_COOKIE, AuthorizationManager.PAC4J_CLIENT_HEADER},
    authorizers = AuthorizationManager.AUTHORIZER_IS_AUTHENTICATED)
public class LabAuthEndpoint extends AbstractApiEndpoint<LabAuthEndpoint> {
  // ================ Constants =========================================== //

  // ================ Members ============================================= //
  private LabAuthApiHandler authApiHandler;

  // ================ Constructors & Main ================================= //
  public LabAuthEndpoint(@Context UriInfo uriInfo, @Context HttpHeaders httpHeaders) {
    super(uriInfo, httpHeaders);
    authApiHandler = new LabAuthApiHandler();
    registerHandler(authApiHandler);
  }
  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  /**
   * Creates a new user in the DB. Authentication / Authorization is disabled so that users can
   * register on the web app.
   *
   * @param user user id
   * @param password password for the user. Will be stored encrypted in MongoDB
   * @return response indicating whether or not the user was successfully created
   */
  @POST
  @Path(LabAuthApi.METHOD_CREATE_USER)
  @ApiOperation(value = "Create user profile.", response = LabUserResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(ignore = true)
  public Response createUser(
      @ApiParam(value = "Id/username of the profile.", required = true)
          @QueryParam(LabAuthApi.PARAM_USER)
          String user,
      @ApiParam(value = "Password of the profile.", required = true)
          @QueryParam(LabAuthApi.PARAM_PASSWORD)
          String password,
      @ApiParam(value = "Create the user with Admin permissions.", required = false)
          @QueryParam(LabAuthApi.PARAM_ADMIN)
          Boolean admin,
      @ApiParam(
              value =
                  "JWT Secret. If passed and matches the server's secret, the account will be"
                      + " created with admin credentials.",
              required = false)
          @QueryParam(LabAuthApi.PARAM_JWT_SECRET)
          String jwtSecret,
      @BeanParam DefaultHeaderFields defaultHeaders) {

    if (admin == null) {
      // Default is not to create an admin user
      admin = false;
    }

    if (!StringUtils.isNullOrEmpty(jwtSecret) && LabConfig.JWT_SECRET.equalsIgnoreCase(jwtSecret)) {
      // Access with admin permissions
      if (admin) {
        // create admin user
        return UnifiedResponseFactory.getResponse(
            authApiHandler.createAdminUser(user, password, jwtSecret));
      }
      return UnifiedResponseFactory.getResponse(authApiHandler.createUser(user, password));
    }

    if (!LabConfig.ALLOW_SELF_REGISTRATIONS) {
      // do not allow self registrations
      return UnifiedResponseFactory.getErrorResponse(
          Response.Status.UNAUTHORIZED,
          "Self registration is not allowed. Please ask an administrator for an account.");
    }

    return UnifiedResponseFactory.getResponse(authApiHandler.createUser(user, password));
  }

  @POST
  @Path(LabAuthApi.METHOD_DEACTIVATE_USERS)
  @ApiOperation(
      value = "Deactivate a list of users. This will overwrite all deactivated users (admin only).",
      response = StatusMessageFormat.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_ADMIN)
  public Response deactivateUsers(
      @ApiParam(value = "List of users to set deactivated.", required = true) List<String> users,
      @Pac4JProfile MongoProfile commonProfile,
      @BeanParam DefaultHeaderFields defaultHeaders) {

    return UnifiedResponseFactory.getResponse(authApiHandler.deactivateUsers(users));
  }

  /**
   * Use the basic auth information to check in the MongoDB whether a id:password match exists and,
   * if yes, return a short-term JWT token for the matched profile. The token will expire within
   * {@link AuthorizationManager#JWT_EXPIRATION_TIME_WEEK_IN_SECONDS}. The client can use an
   * unexpired token to call {@link LabAuthEndpoint#refreshToken(MongoProfile, DefaultHeaderFields)}
   * to get a new token. If the expiration time is exceeded, the client has to loginUser again.
   *
   * @param commonProfile automatically injected by the DirectBasicAuthClient based on the provided
   *     basic auth credentials. Corresponds to the MongoDB profile for a match user:password pair.
   * @return {@link SingleValueFormat<String>} containing the JWT token additionally to being set as
   *     a cookie
   */
  @GET
  @Path(LabAuthApi.METHOD_GET_LOGIN_TOKEN)
  @ApiOperation(
      value = "Login with basic auth and get short-term application token (JWT).",
      response = StringResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {AuthorizationManager.PAC4J_CLIENT_DIRECT_BASIC_AUTH},
      authorizers = AuthorizationManager.AUTHORIZER_IS_AUTHENTICATED)
  public Response loginUser(
      @Pac4JProfile CommonProfile commonProfile, @BeanParam DefaultHeaderFields defaultHeaders) {
    if (commonProfile == null) {
      log.warn("User profile not injected.");
      return UnifiedResponseFactory.getErrorResponse(
          Response.Status.UNAUTHORIZED, "Could not resolve profile from credentials.");
    }

    List<String> deactivatedUsers = ComponentManager.INSTANCE.getAuthManager().deactivatedUsers;
    if (!ListUtils.isNullOrEmpty(deactivatedUsers)
        && deactivatedUsers.contains(commonProfile.getId())) {
      return UnifiedResponseFactory.getErrorResponse(
          Response.Status.UNAUTHORIZED,
          "Your user is deactivated. Please ask an administrator for help.");
    }

    String jwtToken = ComponentManager.INSTANCE.getAuthManager().createAppToken(commonProfile);

    if (StringUtils.isNullOrEmpty(jwtToken)) {
      return UnifiedResponseFactory.getErrorResponse(
          Response.Status.INTERNAL_SERVER_ERROR, "Failed to retrieve token.");
    }

    Response response = UnifiedResponseFactory.getResponse(new SingleValueFormat<>(jwtToken));
    return getCookieResponse(response, jwtToken).status(HttpStatusCodes.STATUS_CODE_OK).build();
  }

  @GET
  @Path(LabAuthApi.METHOD_LOGOUT)
  @ApiOperation(
      value = "Log the user out by setting the auth cookie to a time in the past",
      response = StringResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_IS_AUTHENTICATED)
  public Response logoutUser(
      @Pac4JProfile CommonProfile commonProfile, @BeanParam DefaultHeaderFields defaultHeaders) {
    if (commonProfile == null) {
      log.warn("User profile not injected.");
      return UnifiedResponseFactory.getErrorResponse(
          Response.Status.UNAUTHORIZED, "Could not resolve profile from credentials.");
    }

    Response.ResponseBuilder responseBuilder =
        Response.fromResponse(UnifiedResponseFactory.getEmptyResponse());
    return responseBuilder
        .cookie(
            new NewCookie(
                AuthorizationManager.COOKIE_ACCESS_TOKEN,
                "logout",
                LabConfig.LAB_BASE_URL + "/",
                null,
                NewCookie.DEFAULT_VERSION,
                "Invalidate cookie",
                -1,
                new Date(),
                false,
                true))
        .build();
  }

  /**
   * Compares the incoming JWT's refresh token with the one saved in the DB for the same profile. If
   * they match, issue a new JWT access token. Using this concept, fewer requests can be made by
   * using the information in the JWT token while simultaneously, JWT tokens can be revoked by
   * changing the refresh tokens in the database and letting the JWT expire.
   *
   * @param commonProfile contained in the sent JWT token
   * @return status
   */
  @GET
  @Path(LabAuthApi.METHOD_REFRESH_TOKEN)
  @ApiOperation(
      value = "Get a new short-term application token (JWT).",
      response = StringResponse.class)
  @Produces(MediaType.TEXT_PLAIN)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_IS_AUTHENTICATED)
  public Response refreshToken(
      @Pac4JProfile MongoProfile commonProfile, @BeanParam DefaultHeaderFields defaultHeaders) {
    if (commonProfile == null) {
      log.warn("User profile not injected.");
      return UnifiedResponseFactory.getErrorResponse(
          Response.Status.UNAUTHORIZED, "Could not resolve profile from credentials.");
    }

    List<String> deactivatedUsers = ComponentManager.INSTANCE.getAuthManager().deactivatedUsers;
    if (!ListUtils.isNullOrEmpty(deactivatedUsers)
        && deactivatedUsers.contains(commonProfile.getId())) {
      return UnifiedResponseFactory.getErrorResponse(
          Response.Status.UNAUTHORIZED,
          "Your user is deactivated. Please ask an administrator for help.");
    }

    authApiHandler.setAuthProfile(commonProfile);
    SingleValueFormat<String> tokenResponse = authApiHandler.refreshToken();

    if (tokenResponse == null || StringUtils.isNullOrEmpty(tokenResponse.getData())) {
      return UnifiedResponseFactory.getErrorResponse(
          Response.Status.UNAUTHORIZED, "Refresh token is not valid anymore. Login again.");
    }

    return getCookieResponse(
            UnifiedResponseFactory.getResponse(tokenResponse), tokenResponse.getData())
        .status(HttpStatusCodes.STATUS_CODE_OK)
        .build();
  }

  /**
   * Get a long-term token based on the profile with the given {@param id}. Long-term tokens do not
   * contain permissions, but rather the permissions are looked up in the profile stored in the db.
   * A user can create a long term token for the user itself (id in the own jwt token must match the
   * passed query id); an admin user can create long-term token for any id.
   *
   * @param user Username of the profile based on which a token is generated
   * @return long-term API token
   */
  @GET
  @Path(LabAuthApi.METHOD_GET_API_TOKEN)
  @ApiOperation(
      value = "Get a long-term API token for given user.",
      response = StringResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_USER)
  public Response createApiToken(
      @ApiParam(value = "User Name", required = true) @PathParam(LabAuthApi.PARAM_USER) String user,
      @BeanParam DefaultHeaderFields defaultHeaders) {
    return UnifiedResponseFactory.getResponse(authApiHandler.createApiToken(user));
  }

  @GET
  @Path(LabAuthApi.METHOD_GET_ME)
  @ApiOperation(
      value = "Get the user profile of the current user.",
      response = LabUserResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_IS_AUTHENTICATED)
  public Response getMe(
      @Pac4JProfile MongoProfile commonProfile, @BeanParam DefaultHeaderFields defaultHeaders) {
    authApiHandler.setAuthProfile(commonProfile);
    return UnifiedResponseFactory.getResponse(authApiHandler.getMe());
  }

  /**
   * Get profile from the db with the id.
   *
   * @param user of the profile to fetch
   * @return the fetched profile
   */
  @GET
  @Path(LabAuthApi.METHOD_GET_USER)
  @ApiOperation(value = "Get the profile a user has access to.", response = LabUserResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_USER)
  public Response getUser(
      @ApiParam(value = "User Name", required = true) @PathParam(LabAuthApi.PARAM_USER) String user,
      @BeanParam DefaultHeaderFields defaultHeaders) {
    return UnifiedResponseFactory.getResponse(authApiHandler.getUser(user));
  }

  /**
   * Get all profiles stored in the db
   *
   * @return all profiles stored in the db.
   */
  @GET
  @Path(LabAuthApi.METHOD_GET_USERS)
  @ApiOperation(
      value = "Get all profiles stored in the database (admin only).",
      response = ListOfLabUsersResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_IS_AUTHENTICATED)
  public Response getUsers(
      @Pac4JProfile MongoProfile commonProfile, @BeanParam DefaultHeaderFields defaultHeaders) {
    authApiHandler.setAuthProfile(commonProfile);
    return UnifiedResponseFactory.getResponse(authApiHandler.getUsers());
  }

  @DELETE
  @Path(LabAuthApi.METHOD_DELETE_USER)
  @ApiOperation(value = "Delete a user (admin only).", response = StatusMessageFormat.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_ADMIN)
  public Response deleteUser(
      @ApiParam(value = "User Name", required = true) @PathParam(LabAuthApi.PARAM_USER) String user,
      @BeanParam DefaultHeaderFields defaultHeaders) {
    return UnifiedResponseFactory.getResponse(authApiHandler.deleteUser(user));
  }

  /**
   * Overwrites the permissions of the user. If you want to add / remove a permission, fetch all of
   * the user's permissions first and then send the modified list.
   *
   * @param user name / id of the profile
   * @param permissions to set for the user
   * @return status
   */
  @POST
  @Path(LabAuthApi.METHOD_UPDATE_USER_PERMISSIONS)
  @ApiOperation(
      value = "Update permissions of a user (admin only). Return new token.",
      response = StringResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_ADMIN)
  public Response updatePermissions(
      @ApiParam(value = "User Name", required = true) @PathParam(LabAuthApi.PARAM_USER) String user,
      @ApiParam(value = "If true, the user is forced to re-login.", required = false)
          @QueryParam(LabAuthApi.PARAM_DEACTIVATE_TOKEN)
          Boolean deactivateToken,
      @ApiParam(value = "Permission List", required = true) List<String> permissions,
      @BeanParam DefaultHeaderFields defaultHeaders) {
    return UnifiedResponseFactory.getResponse(
        authApiHandler.updatePermissions(user, permissions, deactivateToken));
  }

  @GET
  @Path(LabAuthApi.METHOD_UPDATE_USER_PASSWORD)
  @ApiOperation(value = "Update user password. Return new token.", response = StringResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_USER)
  public Response updateUserPassword(
      @ApiParam(value = "User Name", required = true) @PathParam(LabAuthApi.PARAM_USER) String user,
      @ApiParam(value = "New Password", required = true) @QueryParam(LabAuthApi.PARAM_PASSWORD)
          String password,
      @Pac4JProfile MongoProfile commonProfile,
      @BeanParam DefaultHeaderFields defaultHeaders) {
    authApiHandler.setAuthProfile(commonProfile);
    return UnifiedResponseFactory.getResponse(authApiHandler.updateUserPassword(user, password));
  }

  @GET
  @Path(LabAuthApi.METHOD_ADD_USER_TO_PROJECT)
  @ApiOperation(
      value = "Add a user to a project. Return new token.",
      response = StringResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
  public Response addUserToProject(
      @ApiParam(value = "User Name", required = true) @PathParam(LabAuthApi.PARAM_USER) String user,
      @ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT)
          String project,
      @BeanParam DefaultHeaderFields defaultHeaders) {
    return UnifiedResponseFactory.getResponse(authApiHandler.addUserToProject(user, project));
  }

  @DELETE
  @Path(LabAuthApi.METHOD_REMOVE_USER_FROM_PROJECT)
  @ApiOperation(
      value = "Remove a user from a project. Return new token.",
      response = StringResponse.class)
  @Produces(MediaType.APPLICATION_JSON)
  @Pac4JSecurity(
      clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER
      },
      authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
  public Response removeUserFromProject(
      @ApiParam(value = "User Name", required = true) @PathParam(LabAuthApi.PARAM_USER) String user,
      @ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT)
          String project,
      @BeanParam DefaultHeaderFields defaultHeaders) {
    return UnifiedResponseFactory.getResponse(authApiHandler.removeUserFromProject(user, project));
  }
  // ================ Private Methods ===================================== //

  private Response.ResponseBuilder getCookieResponse(Response response, String jwtToken) {
    Response.ResponseBuilder responseBuilder = Response.fromResponse(response);
    return responseBuilder.cookie(
        new NewCookie(
            AuthorizationManager.COOKIE_ACCESS_TOKEN,
            jwtToken,
            LabConfig.LAB_BASE_URL + "/",
            null,
            "Cookie for Webapp",
            AuthorizationManager.JWT_EXPIRATION_TIME_WEEK_IN_SECONDS,
            false,
            true)
        // new NewCookie(AuthorizationManager.COOKIE_ACCESS_TOKEN, jwtToken, LabConfig.LAB_BASE_URL
        // + "/app", null, "Cookie for Webapp",
        // AuthorizationManager.JWT_EXPIRATION_TIME_WEEK_IN_SECONDS, false, true),
        // new NewCookie(AuthorizationManager.COOKIE_ACCESS_TOKEN, jwtToken, LabConfig.LAB_BASE_URL
        // + "/lab", null, "Cookie for API",
        // AuthorizationManager.JWT_EXPIRATION_TIME_WEEK_IN_SECONDS, false, true),
        // new NewCookie(AuthorizationManager.COOKIE_ACCESS_TOKEN, jwtToken, LabConfig.LAB_BASE_URL
        // + "/workspace", null, "Cookie for Workspace",
        // AuthorizationManager.JWT_EXPIRATION_TIME_WEEK_IN_SECONDS, false, true),
        // new NewCookie(AuthorizationManager.COOKIE_ACCESS_TOKEN, jwtToken, LabConfig.LAB_BASE_URL
        // + "/service-admin", null, "Cookie for Service Admin",
        // AuthorizationManager.JWT_EXPIRATION_TIME_WEEK_IN_SECONDS, false, true),
        // new NewCookie(AuthorizationManager.COOKIE_ACCESS_TOKEN, jwtToken, LabConfig.LAB_BASE_URL
        // + "/netdata", null, "Cookie for Netdata",
        // AuthorizationManager.JWT_EXPIRATION_TIME_WEEK_IN_SECONDS, false, true)
        );
  }

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
  private static class StringResponse extends SingleValueFormat<String> {

    public String data;
  }

  private static class LabUserResponse extends SingleValueFormat<LabUser> {

    public LabUser data;
  }

  private static class ListOfLabUsersResponse extends ValueListFormat<LabUser> {

    public List<LabUser> data;
  }
}
