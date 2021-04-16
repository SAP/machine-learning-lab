package org.mltooling.lab.api;

import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.http.HttpStatus;
import org.apache.shiro.authz.UnauthorizedException;
import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.api.format.StatusMessageFormat;
import org.mltooling.core.api.format.UnifiedFormat;
import org.mltooling.core.api.format.ValueListFormat;
import org.mltooling.core.api.format.parser.JsonFormatParser;
import org.mltooling.core.api.handler.AbstractApiHandler;
import org.mltooling.core.lab.LabAuthApi;
import org.mltooling.core.lab.model.LabEvent;
import org.mltooling.core.lab.model.LabUser;
import org.mltooling.core.utils.CryptoUtils;
import org.mltooling.core.utils.ListUtils;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.core.utils.structures.PropertyContainer;
import org.mltooling.lab.ComponentManager;
import org.mltooling.lab.LabConfig;
import org.mltooling.lab.authorization.AuthorizationManager;
import org.pac4j.mongo.profile.MongoProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class LabAuthApiHandler extends AbstractApiHandler<LabAuthApiHandler> implements LabAuthApi {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(LabAuthApiHandler.class);

  // ================ Members ============================================= //
  private AuthorizationManager authManager;
  private MongoProfile authProfile;
  private ComponentManager componentManager;

  // ================ Constructors & Main ================================= //

  public LabAuthApiHandler() {
    componentManager = ComponentManager.INSTANCE;
    authManager = ComponentManager.INSTANCE.getAuthManager();
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //
  @Override
  public SingleValueFormat<LabUser> createAdminUser(String user, String password, String jwtSecret)
      throws UnauthorizedException {
    SingleValueFormat<LabUser> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(user)) {
        response.setErrorStatus(
            "The user parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      if (StringUtils.isNullOrEmpty(password)) {
        response.setErrorStatus(
            "The password parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      if (StringUtils.isNullOrEmpty(jwtSecret)) {
        response.setErrorStatus(
            "The secret parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      // log event
      componentManager
          .getEventLogManager()
          .logEvent(
              LabEvent.CREATE_ADMIN_USER,
              new PropertyContainer()
                  .addProperty("new-user", user)
                  .addProperty(
                      LabAuthApi.PARAM_USER, authProfile != null ? authProfile.getId() : "unk"));

      MongoProfile profile = authManager.createAdminUser(user, password, jwtSecret);
      if (profile == null) {
        response.setErrorStatus(
            "Failed to create admin user profile.", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        return prepareResponse(response);
      }

      response.setData(AuthorizationManager.transformProfile(profile));

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<LabUser> createUser(String user, String password) {
    SingleValueFormat<LabUser> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(user)) {
        response.setErrorStatus(
            "The user parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      if (StringUtils.isNullOrEmpty(password)) {
        response.setErrorStatus(
            "The password parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      if (AuthorizationManager.isTechnicalUser(user)) {
        response.setErrorStatus(
            "User name is reserved for technical users.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      if (user.length() > 25) {
        // TODO: quick fix, non technical users should not be longer than 25 chars -> otherwise
        // there might be some problems
        response.setErrorStatus(
            "User name is not allowed to be longer than 25 chars.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }
      MongoProfile profile = authManager.createUser(user, password);
      if (profile == null) {
        response.setErrorStatus(
            "Failed to create user profile.", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        return prepareResponse(response);
      }

      response.setData(AuthorizationManager.transformProfile(profile));
      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<String> loginUser(String user, String password) {
    // basic authentication is done by endpoint
    throw new NotImplementedException();
  }

  @Override
  public SingleValueFormat<String> logoutUser() {
    // basic authentication is done by endpoint
    throw new NotImplementedException();
  }

  @Override
  public SingleValueFormat<Boolean> isOidcEnabled() {
    String message = null;
    if (LabConfig.EXTERNAL_OIDC_AUTH_URL == null) {
      message = "External OIDC authentication is disabled: LAB_EXTERNAL_OIDC_AUTH_URL is not set.";
    }
    if (LabConfig.EXTERNAL_OIDC_TOKEN_URL == null) {
      message = "External OIDC authentication is disabled: LAB_EXTERNAL_OIDC_TOKEN_URL is not set.";
    }
    if (LabConfig.EXTERNAL_OIDC_CLIENT_ID == null) {
      message = "External OIDC authentication is disabled: LAB_EXTERNAL_OIDC_CLIENT_ID is not set.";
    }
    if (LabConfig.EXTERNAL_OIDC_CLIENT_SECRET == null) {
      message = "External OIDC authentication is disabled: LAB_EXTERNAL_OIDC_CLIENT_SECRET is not set.";
    }
    boolean isEnabled = message == null;
    if (isEnabled) {
      message = "External OIDC authentication is enabled.";
    }
    SingleValueFormat<Boolean> result = new SingleValueFormat<>(isEnabled);
    result.getMetadata().setMessage(message);
    return result;
  }

  public URI getOidcLoginURI(String host) {
    // Request the email of the user to use it for the username
    final String OAUTH2_SCOPE = "openid email";

    /* Build callback URL which the client will be redirected to after successful authentication
    with the external OIDC provider. Use the "Host" header to figure out the complete URL at which
    this ML LAB API is deployed. */
    String scheme = LabConfig.SERVICE_SSL_ENABLED ? "https" : "http";
    String redirect_uri =
        scheme + "://" + host + LabAuthApi.ENDPOINT_PATH + LabAuthApi.METHOD_OIDC_CALLBACK;

    // Build the external OIDC url that the client will be redirected to
    String uri =
        Unirest.get(LabConfig.EXTERNAL_OIDC_AUTH_URL)
            .queryString("response_type", "code")
            .queryString("client_id", LabConfig.EXTERNAL_OIDC_CLIENT_ID)
            .queryString("scope", OAUTH2_SCOPE)
            .queryString("redirect_uri", redirect_uri)
            .getUrl();
    return URI.create(uri);
  }

  public Map<String, Object> getOidcTokenContent(String code, String host) throws IOException {
    // Redirect uri needs to be the same as the one used for the request to get the auth code
    String scheme = LabConfig.SERVICE_SSL_ENABLED ? "https" : "http";
    String redirect_uri =
        scheme + "://" + host + LabAuthApi.ENDPOINT_PATH + LabAuthApi.METHOD_OIDC_CALLBACK;

    // Use the auth code to get the OIDC token from the configured external endpoint
    HttpResponse<String> tokenResponse;
    try {
      tokenResponse =
          Unirest.post(LabConfig.EXTERNAL_OIDC_TOKEN_URL)
              .basicAuth(LabConfig.EXTERNAL_OIDC_CLIENT_ID, LabConfig.EXTERNAL_OIDC_CLIENT_SECRET)
              .field("grant_type", "authorization_code")
              .field("code", code)
              .field("redirect_uri", redirect_uri)
              .asString();
      if (tokenResponse.getStatus() != 200) {
        throw new IOException("Error while requesting OIDC token: " + tokenResponse.getBody());
      }
    } catch (UnirestException e) {
      throw new IOException("Error while requesting OIDC token: " + e.getMessage());
    }

    String tokenResponseBody = tokenResponse.getBody();
    Type stringMapType = new TypeToken<Map<String, String>>() {}.getType();
    JsonFormatParser jsonParser = JsonFormatParser.INSTANCE;
    Map<String, String> tokenResponseMap;
    try {
      tokenResponseMap = jsonParser.fromJson(tokenResponseBody, stringMapType);
    } catch (JsonParseException e){
      throw new IOException("Received invalid token response body: " + tokenResponseBody, e);
    }
    String idToken = tokenResponseMap.get("id_token");
    // The JWT token consists of 3 parts (header, content, signature) which are separated by a '.'
    // We only need the content part which needs to be decoded from base64
    String idTokenContent = new String(CryptoUtils.decode(idToken.split("\\.")[1]));
    // Parse json content of token and return it as a Map
    try{
      Type stringToObjectMapType = new TypeToken<Map<String, Object>>() {}.getType();
      return jsonParser.fromJson(idTokenContent, stringToObjectMapType);
    } catch (JsonParseException e){
      throw new IOException("Unable to parse id token content: " + idTokenContent, e);
    }
  }

  @Override
  public SingleValueFormat<String> refreshToken() {
    SingleValueFormat<String> response = new SingleValueFormat<>();

    try {
      if (this.authProfile == null) {
        response.setErrorStatus(
            "No profile found. Failed to authenticate.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      String token = authManager.refreshToken(this.authProfile);
      if (StringUtils.isNullOrEmpty(token)) {
        response.setErrorStatus(
            "Failed to refresh app token.", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        return prepareResponse(response);
      }

      response.setData(token);

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<String> createApiToken(String user) {
    SingleValueFormat<String> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(user)) {
        response.setErrorStatus(
            "The user parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }
      user = AuthorizationManager.resolveUserName(user);

      String token = authManager.createApiToken(user);
      if (StringUtils.isNullOrEmpty(token)) {
        response.setErrorStatus("Failed to create API token.", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        return prepareResponse(response);
      }

      response.setData(token);

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<LabUser> getMe() {
    SingleValueFormat<LabUser> response = new SingleValueFormat<>();

    try {
      MongoProfile profile = authManager.getUser(authProfile.getId());
      if (profile == null) {
        response.setErrorStatus(
            "Failed to get user profile for current user.", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        return prepareResponse(response);
      }
      response.setData(AuthorizationManager.transformProfile(profile));

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<LabUser> getUser(String user) {
    SingleValueFormat<LabUser> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(user)) {
        response.setErrorStatus(
            "The user parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      user = AuthorizationManager.resolveUserName(user);

      MongoProfile profile = authManager.getUser(user);
      if (profile == null) {
        response.setErrorStatus(
            "Failed to get user profile for " + user, HttpStatus.SC_INTERNAL_SERVER_ERROR);
        return prepareResponse(response);
      }
      response.setData(AuthorizationManager.transformProfile(profile));

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public ValueListFormat<LabUser> getUsers() {
    ValueListFormat<LabUser> response = new ValueListFormat<>();

    try {
      List<MongoProfile> users = authManager.getUsers();
      if (ListUtils.isNullOrEmpty(users)) {
        response.setErrorStatus("Could not find any users", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        return prepareResponse(response);
      }

      boolean isAdmin = authManager.isAdmin(authProfile);

      List<LabUser> transformedUsers = new ArrayList<>();
      for (MongoProfile profile : users) {
        LabUser user = AuthorizationManager.transformProfile(profile);
        if (!isAdmin) {
          // if not admin  -> only return user name and id
          if (AuthorizationManager.isTechnicalUser(user.getName())) {
            // if technical user -> don't return
            continue;
          }
          user = new LabUser().setId(user.getId()).setName(user.getName());
        }

        transformedUsers.add(user);
      }

      response.setData(transformedUsers);

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public StatusMessageFormat deactivateUsers(List<String> users) {
    StatusMessageFormat response = new StatusMessageFormat();

    try {
      if (users == null) {
        response.setErrorStatus(
            "The body should contain a list of users.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      // Todo temp solution to deactivate users - stored in memory only
      // always overwrite everything
      authManager.deactivatedUsers = new ArrayList<>();

      for (String user : users) {
        authManager.deactivatedUsers.add(AuthorizationManager.resolveUserName(user));
      }

      response.setSuccessfulStatus();

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public StatusMessageFormat deleteUser(String user) {
    StatusMessageFormat response = new StatusMessageFormat();

    try {
      if (StringUtils.isNullOrEmpty(user)) {
        response.setErrorStatus(
            "The user parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      // log delete user event
      componentManager
          .getEventLogManager()
          .logEvent(
              LabEvent.DELETE_USER,
              new PropertyContainer()
                  .addProperty("selected-user", user)
                  .addProperty(
                      LabAuthApi.PARAM_USER, authProfile != null ? authProfile.getId() : "unk"));

      user = AuthorizationManager.resolveUserName(user);

      // delete workspace
      try {
        componentManager.getServiceManger().deleteWorkspace(user);
      } catch (Exception e) {
        log.error("Failed to delete workspace for user: " + user, e);
      }

      // delete user bucket
      String userBucket = AuthorizationManager.resolveUserProject(user);
      componentManager.getFileManager().deleteBucket(userBucket);

      // delete profile
      authManager.deleteProfile(user);

      response.setSuccessfulStatus();

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<String> updatePermissions(
      String user, List<String> permissions, @Nullable Boolean deactivateToken) {
    SingleValueFormat<String> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(user)) {
        response.setErrorStatus(
            "The user parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      if (permissions == null) {
        // empty list is fine
        response.setErrorStatus(
            "The permission list in the body should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      user = AuthorizationManager.resolveUserName(user);

      if (AuthorizationManager.isTechnicalUser(user)) {
        // prevent technical users from updating permissions
        response.setErrorStatus(
            "It is not allowed to update permissions of technical users.",
            HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      authManager.updatePermissions(user, permissions, deactivateToken);
      response.setData(authManager.createAppToken(authManager.getUser(user)));

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<String> updateUserPassword(String user, String password) {
    SingleValueFormat<String> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(user)) {
        response.setErrorStatus(
            "The user parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      if (StringUtils.isNullOrEmpty(password)) {
        response.setErrorStatus(
            "The password parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      if (authProfile != null && AuthorizationManager.isProjectAdmin(authProfile.getId())) {
        response.setErrorStatus(
            "A project admin user is not allowed to update the user password.",
            HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      user = AuthorizationManager.resolveUserName(user);

      authManager.updatePassword(user, password, null);
      response.setData(authManager.createAppToken(authManager.getUser(user)));

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<String> addUserToProject(String user, String project) {
    SingleValueFormat<String> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(user)) {
        response.setErrorStatus(
            "The user parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      user = AuthorizationManager.resolveUserName(user);

      if (AuthorizationManager.isTechnicalUser(user)) {
        response.setErrorStatus(
            "It is not allowed to add technical users to projects.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = ComponentManager.INSTANCE.getProjectManager().resolveProjectName(project);

      authManager.addProjectPermission(user, project);
      response.setData(authManager.createAppToken(authManager.getUser(user)));

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<String> removeUserFromProject(String user, String project) {
    SingleValueFormat<String> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(user)) {
        response.setErrorStatus(
            "The user parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      user = AuthorizationManager.resolveUserName(user);

      if (AuthorizationManager.isTechnicalUser(user)) {
        response.setErrorStatus(
            "It is not allowed to remove technical users from projects.",
            HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = ComponentManager.INSTANCE.getProjectManager().resolveProjectName(project);

      authManager.removeProjectPermission(user, project);
      response.setData(authManager.createAppToken(authManager.getUser(user)));

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  // ================ Private Methods ===================================== //
  @Override
  protected <F extends UnifiedFormat> F prepareResponse(F unifiedFormat) {
    F response = super.prepareResponse(unifiedFormat);
    try {
      if (!StringUtils.isNullOrEmpty(this.getRequestUrl())
          && response.getMetadata() != null
          && response.getMetadata().getTime() != null) {
        if (response.getMetadata().getTime() > LabConfig.LOG_REQUEST_MAX_TIME) {
          // request takes more than 10 seconds -> log event
          componentManager
              .getEventLogManager()
              .logEvent(
                  LabEvent.LONG_REQUEST,
                  new PropertyContainer()
                      .addProperty("url", this.getRequestUrl())
                      .addProperty("time", response.getMetadata().getTime())
                      .addProperty("status", response.getMetadata().getStatus())
                      .addProperty("responseType", response.getClass().getSimpleName())
                      .addProperty("user", authProfile != null ? authProfile.getId() : "unknown"));
        }
      }
    } catch (Exception ex) {
      // do nothing
    }

    return response;
  }

  // ================ Getter & Setter ===================================== //
  public LabAuthApiHandler setAuthProfile(MongoProfile authProfile) {
    this.authProfile = authProfile;
    return this;
  }

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
