package org.mltooling.core.lab;

import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.api.format.StatusMessageFormat;
import org.mltooling.core.api.format.ValueListFormat;
import org.mltooling.core.lab.model.LabUser;

import javax.annotation.Nullable;
import java.util.List;


public interface LabAuthApi {

    // ================ Constants =========================================== //
    final String ENDPOINT_PATH = LabApi.ENDPOINT_PATH + "/auth";

    // REST Params
    String PARAM_USER = "user";
    String PARAM_PASSWORD = "password";
    String PARAM_ADMIN = "admin";
    String PARAM_JWT_SECRET = "jwtSecret";
    String PARAM_DEACTIVATE_TOKEN = "deactivateToken";

    // REST method paths
    String METHOD_GET_USERS = "/users";
    String METHOD_CREATE_USER = "/users";
    String METHOD_DEACTIVATE_USERS = "/users/deactivate";
    String METHOD_GET_ME = "/users/me";
    String METHOD_GET_USER = "/users/{" + PARAM_USER + "}";
    String METHOD_DELETE_USER = "/users/{" + PARAM_USER + "}";
    String METHOD_ADD_USER_TO_PROJECT = "/users/{" + PARAM_USER + "}/projects/{" + LabApi.PARAM_PROJECT + "}";
    String METHOD_REMOVE_USER_FROM_PROJECT = "/users/{" + PARAM_USER + "}/projects/{" + LabApi.PARAM_PROJECT + "}";
    String METHOD_UPDATE_USER_PERMISSIONS = "/users/{" + PARAM_USER + "}/permissions";
    String METHOD_UPDATE_USER_PASSWORD = "/users/{" + PARAM_USER + "}/password";
    String METHOD_GET_API_TOKEN = "/users/{" + PARAM_USER + "}/token";

    String METHOD_GET_LOGIN_TOKEN = "/login";
    String METHOD_LOGOUT = "/logout";
    String METHOD_REFRESH_TOKEN = "/refresh";

    // ================ Methods ============================================= //

    SingleValueFormat<LabUser> createAdminUser(String user, String password, String jwtSecret);

    SingleValueFormat<LabUser> createUser(String user, String password);

    SingleValueFormat<String> loginUser(String user, String password); // -> returns short-term application token

    SingleValueFormat<String> logoutUser();

    SingleValueFormat<String> refreshToken(); //-> returns short-term application token

    SingleValueFormat<String> createApiToken(String user); //-> returns long-term API token

    SingleValueFormat<LabUser> getMe();

    SingleValueFormat<LabUser> getUser(String user);

    ValueListFormat<LabUser> getUsers();

    StatusMessageFormat deactivateUsers(List<String> users);

    StatusMessageFormat deleteUser(String user);

    SingleValueFormat<String> updatePermissions(String user, List<String> permissions, @Nullable Boolean deactivateToken); // -> return updated short-term application token

    SingleValueFormat<String> updateUserPassword(String user, String password); // -> return updated short-term application token

    SingleValueFormat<String> addUserToProject(String user, String project); // -> return updated short-term application token

    SingleValueFormat<String> removeUserFromProject(String user, String project); // -> return updated short-term application token
}
