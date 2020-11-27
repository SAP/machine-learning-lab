package org.mltooling.core.lab;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.HttpRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.List;
import javax.annotation.Nullable;
import org.mltooling.core.api.client.AbstractApiClient;
import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.api.format.StatusMessageFormat;
import org.mltooling.core.api.format.ValueListFormat;
import org.mltooling.core.api.utils.ApiUtils;
import org.mltooling.core.lab.model.LabUser;

public class LabAuthApiClient extends AbstractApiClient<LabAuthApiClient> implements LabAuthApi {

  // ================ Constants =========================================== //

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //

  public LabAuthApiClient(String serviceUrl) {
    super(serviceUrl, null);
  }

  public LabAuthApiClient(String serviceUrl, @Nullable String authToken) {
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

  /** Creates a user profile containing no permissions. */
  @Override
  public SingleValueFormat<LabUser> createUser(String user, String password) {
    return executeRequest(
        Unirest.post(getEndpointUrl() + METHOD_CREATE_USER)
            .queryString(PARAM_USER, user)
            .queryString(PARAM_PASSWORD, password)
            .getHttpRequest(),
        new TypeToken<SingleValueFormat<LabUser>>() {}.getType());
  }

  /** Creates a user profile with admin permission. */
  @Override
  public SingleValueFormat<LabUser> createAdminUser(
      String user, String password, String jwtSecret) {
    return executeRequest(
        Unirest.post(getEndpointUrl() + METHOD_CREATE_USER)
            .queryString(PARAM_USER, user)
            .queryString(PARAM_PASSWORD, password)
            .queryString(PARAM_ADMIN, true)
            .queryString(PARAM_JWT_SECRET, jwtSecret)
            .getHttpRequest(),
        new TypeToken<SingleValueFormat<LabUser>>() {}.getType());
  }

  @Override
  public SingleValueFormat<String> loginUser(String user, String password) {
    final String auth =
        Base64.getEncoder().encodeToString((String.format("%s:%s", user, password)).getBytes());
    this.setHeader(ApiUtils.AUTHORIZATION_HEADER, String.format("Basic %s", auth));

    return executeRequest(
        Unirest.get(getEndpointUrl() + METHOD_GET_LOGIN_TOKEN).getHttpRequest(),
        new TypeToken<SingleValueFormat<String>>() {}.getType());
  }

  @Override
  public SingleValueFormat<String> logoutUser() {
    return executeRequest(
        Unirest.get(getEndpointUrl() + METHOD_LOGOUT).getHttpRequest(),
        new TypeToken<SingleValueFormat<String>>() {}.getType());
  }

  @Override
  public SingleValueFormat<String> refreshToken() {
    return executeRequest(
        Unirest.get(getEndpointUrl() + METHOD_REFRESH_TOKEN).getHttpRequest(),
        new TypeToken<SingleValueFormat<String>>() {}.getType());
  }

  @Override
  public SingleValueFormat<String> createApiToken(String user) {
    return executeRequest(
        Unirest.get(getEndpointUrl() + METHOD_GET_API_TOKEN)
            .routeParam(PARAM_USER, user)
            .header(ApiUtils.CONTENT_TYPE_HEADER, ApiUtils.CONTENT_TYPE_JSON)
            .getHttpRequest(),
        new TypeToken<SingleValueFormat<String>>() {}.getType());
  }

  @Override
  public SingleValueFormat<LabUser> getMe() {
    return executeRequest(
        Unirest.get(getEndpointUrl() + METHOD_GET_ME).getHttpRequest(),
        new TypeToken<SingleValueFormat<LabUser>>() {}.getType());
  }

  @Override
  public SingleValueFormat<LabUser> getUser(String user) {
    return executeRequest(
        Unirest.get(getEndpointUrl() + METHOD_GET_USER)
            .routeParam(PARAM_USER, user)
            .getHttpRequest(),
        new TypeToken<SingleValueFormat<LabUser>>() {}.getType());
  }

  @Override
  public ValueListFormat<LabUser> getUsers() {
    return executeRequest(
        Unirest.get(getEndpointUrl() + METHOD_GET_USERS).getHttpRequest(),
        new TypeToken<ValueListFormat<LabUser>>() {}.getType());
  }

  @Override
  public StatusMessageFormat deactivateUsers(List<String> users) {
    HttpRequest request =
        Unirest.post(getEndpointUrl() + METHOD_DEACTIVATE_USERS)
            .header(ApiUtils.CONTENT_TYPE_HEADER, ApiUtils.CONTENT_TYPE_JSON)
            .body(new Gson().toJson(users))
            .getHttpRequest();

    return executeRequest(request, new TypeToken<StatusMessageFormat>() {}.getType());
  }

  @Override
  public StatusMessageFormat deleteUser(String user) {
    return executeRequest(
        Unirest.delete(getEndpointUrl() + METHOD_DELETE_USER)
            .routeParam(PARAM_USER, user)
            .getHttpRequest(),
        new TypeToken<StatusMessageFormat>() {}.getType());
  }

  @Override
  public SingleValueFormat<String> updatePermissions(
      String user, List<String> permissions, @Nullable Boolean deactivateToken) {

    HttpRequest request =
        Unirest.post(getEndpointUrl() + METHOD_UPDATE_USER_PERMISSIONS)
            .header(ApiUtils.CONTENT_TYPE_HEADER, ApiUtils.CONTENT_TYPE_JSON)
            .routeParam(PARAM_USER, user)
            .body(new Gson().toJson(permissions))
            .getHttpRequest();

    if (deactivateToken != null) {
      request = request.queryString(PARAM_DEACTIVATE_TOKEN, deactivateToken);
    }

    return executeRequest(request, new TypeToken<SingleValueFormat<String>>() {}.getType());
  }

  @Override
  public SingleValueFormat<String> updateUserPassword(String user, String password) {
    return executeRequest(
        Unirest.get(getEndpointUrl() + METHOD_UPDATE_USER_PASSWORD)
            .routeParam(PARAM_USER, user)
            .queryString(PARAM_PASSWORD, password)
            .getHttpRequest(),
        new TypeToken<SingleValueFormat<String>>() {}.getType());
  }

  @Override
  public SingleValueFormat<String> addUserToProject(String user, String project) {
    return executeRequest(
        Unirest.get(getEndpointUrl() + METHOD_ADD_USER_TO_PROJECT)
            .routeParam(PARAM_USER, user)
            .routeParam(LabApi.PARAM_PROJECT, project)
            .getHttpRequest(),
        new TypeToken<SingleValueFormat<String>>() {}.getType());
  }

  @Override
  public SingleValueFormat<String> removeUserFromProject(String user, String project) {
    return executeRequest(
        Unirest.delete(getEndpointUrl() + METHOD_REMOVE_USER_FROM_PROJECT)
            .routeParam(PARAM_USER, user)
            .routeParam(LabApi.PARAM_PROJECT, project)
            .getHttpRequest(),
        new TypeToken<SingleValueFormat<String>>() {}.getType());
  }

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
