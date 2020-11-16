package org.mltooling.lab.authorization;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.mltooling.core.lab.LabAuthApi;
import org.mltooling.core.utils.ListUtils;
import org.mltooling.core.utils.StringUtils;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;

public class UserAuthorizer<U extends CommonProfile> extends AdminAuthorizer<U> {

  // ================ Constants =========================================== //

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //

  // ================ Methods for/from SuperClass / Interfaces ============ //

  @Override
  public boolean isAuthorized(WebContext context, List<U> profiles) {
    if (super.isAuthorized(context, profiles)) {
      // if admin -> allow access
      return true;
    }

    if (ListUtils.isNullOrEmpty(profiles)) {
      return false;
    }

    U profile = profiles.get(0);

    // match path parameter /users/{user}/
    Pattern pattern = Pattern.compile("/users/([^/]+)");
    Matcher matcher = pattern.matcher(context.getPath());
    if (matcher.find()) {
      return matcher.group(1).equalsIgnoreCase(profile.getId());
    }

    // match query parameter ?user={user}
    String userParam = context.getRequestParameter(LabAuthApi.PARAM_USER);
    if (!StringUtils.isNullOrEmpty(userParam)) {
      try {
        userParam = AuthorizationManager.resolveUserName(userParam);
      } catch (Exception e) {
        return false;
      }
      return userParam.equalsIgnoreCase(profile.getId());
    }

    return false;
  }
  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
