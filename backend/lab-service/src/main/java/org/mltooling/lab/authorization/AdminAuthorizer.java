package org.mltooling.lab.authorization;

import java.util.List;
import java.util.Set;
import org.mltooling.core.utils.ListUtils;
import org.mltooling.lab.ComponentManager;
import org.mltooling.lab.LabConfig;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;

public class AdminAuthorizer<U extends CommonProfile> implements Authorizer<U> {
  // ================ Constants =========================================== //

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //

  // ================ Methods for/from SuperClass / Interfaces ============ //
  @Override
  public boolean isAuthorized(WebContext context, List<U> profiles) {
    if (ListUtils.isNullOrEmpty(profiles)) {
      return false;
    }

    U profile = profiles.get(0);

    Set<String> permissions = profile.getPermissions();
    if (LabConfig.ALWAYS_CHECK_PERMISSIONS
        || (profile.getAttribute(AuthorizationManager.TOKEN_TYPE) != null
            && profile
                .getAttribute(AuthorizationManager.TOKEN_TYPE)
                .equals(AuthorizationManager.TOKEN_TYPES.LONG.getName()))) {
      permissions = ComponentManager.INSTANCE.getAuthManager().getDbPermissions(profile);
      profile.setPermissions(permissions);
    }

    // Either user has admin permission or is the admin user itself
    return !permissions.isEmpty()
        && (permissions.contains(AuthorizationManager.CORE_PERMISSIONS.ADMIN.getName())
            || profile.getId().equalsIgnoreCase(AuthorizationManager.DEFAULT_ADMIN_USER));
  }
  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
