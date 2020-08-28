package org.mltooling.lab.authorization;

import org.mltooling.core.utils.ListUtils;
import org.mltooling.lab.ComponentManager;
import org.mltooling.lab.LabConfig;
import org.pac4j.core.authorization.authorizer.IsAuthenticatedAuthorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;

import java.util.List;


public class CustomIsAuthenticatedAuthorizer<U extends CommonProfile> extends IsAuthenticatedAuthorizer<U> {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //

    // ================ Constructors & Main ================================= //

    // ================ Methods for/from SuperClass / Interfaces ============ //

    @Override
    public boolean isAuthorized(final WebContext context, final List<U> profiles) {
        if (ListUtils.isNullOrEmpty(profiles)) {
            return false;
        }

        U profile = profiles.get(0);

        if (LabConfig.ALWAYS_CHECK_PERMISSIONS || (profile.getAttribute(AuthorizationManager.TOKEN_TYPE) != null
                && profile.getAttribute(AuthorizationManager.TOKEN_TYPE).equals(AuthorizationManager.TOKEN_TYPES.LONG.getName()))) {
            profile.setPermissions(ComponentManager.INSTANCE.getAuthManager().getDbPermissions(profile));
        }

        return isAnyAuthorized(context, profiles);
    }

    // ================ Public Methods ====================================== //

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
