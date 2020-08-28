package org.mltooling.lab.authorization;

import org.mltooling.core.lab.LabApi;
import org.mltooling.core.utils.ListUtils;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.lab.ComponentManager;
import org.mltooling.lab.LabConfig;
import org.mltooling.lab.components.ProjectManager;
import org.pac4j.core.authorization.authorizer.Authorizer;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ProjectAuthorizer<U extends CommonProfile> implements Authorizer<U> {

    // ================ Constants =========================================== //
    private static final Logger log = LoggerFactory.getLogger(ProjectAuthorizer.class);

    public static final String PROJECT_PERMISSION_PREFIX = "project-";

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
        if (LabConfig.ALWAYS_CHECK_PERMISSIONS || (profile.getAttribute(AuthorizationManager.TOKEN_TYPE) != null
                && profile.getAttribute(AuthorizationManager.TOKEN_TYPE).equals(AuthorizationManager.TOKEN_TYPES.LONG.getName()))) {
            permissions = ComponentManager.INSTANCE.getAuthManager().getDbPermissions(profile);
            profile.setPermissions(permissions);
        }

        if (permissions.isEmpty()) {
            return false;
        }

        // match path parameter /projects/{project}/
        Pattern pattern = Pattern.compile("/projects/([^/]+)");
        Matcher matcher = pattern.matcher(context.getPath());
        if (matcher.find()) {
            return hasProjectPermission(permissions, matcher.group(1), profile.getId());
        }

        // match query parameter ?project={project}
        String projectParam = context.getRequestParameter(LabApi.PARAM_PROJECT);
        if (!StringUtils.isNullOrEmpty(projectParam)) {
            return hasProjectPermission(permissions, projectParam, profile.getId());
        }

        return false;

    }
    // ================ Public Methods ====================================== //

    public static boolean hasProjectPermission(Set<String> permissions, String project, @Nullable String userId) {
        if (permissions.contains(AuthorizationManager.CORE_PERMISSIONS.ADMIN.getName())) {
            // Always allow admin
            return true;
        }

        try {
            if (!StringUtils.isNullOrEmpty(userId) && project.equalsIgnoreCase(AuthorizationManager.resolveUserProject(userId))) {
                // Allow user projects to be accessed from the same user
                return true;
            }
        } catch (Exception e) {
            log.error("This should not happen.", e);
        }

        return isProjectInPermissions(permissions, project);
    }

    public static boolean isProjectInPermissions(Set<String> permissions, String project) {
        String projectName = prefixProjectNameIfNecessary(project);
        return permissions.contains(projectName);
    }

    // ================ Private Methods ===================================== //

    public static String prefixProjectNameIfNecessary(String projectName) {
        projectName = ProjectManager.processNameToId(projectName);
        if (!projectName.startsWith(PROJECT_PERMISSION_PREFIX)) {
            return PROJECT_PERMISSION_PREFIX + projectName;
        }

        return projectName;
    }

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
