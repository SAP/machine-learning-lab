package org.mltooling.lab.api;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.apache.http.HttpStatus;
import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.api.format.StatusMessageFormat;
import org.mltooling.core.api.format.UnifiedFormat;
import org.mltooling.core.api.format.ValueListFormat;
import org.mltooling.core.api.handler.AbstractApiHandler;
import org.mltooling.core.lab.LabAdminApi;
import org.mltooling.core.lab.model.*;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.core.utils.structures.PropertyContainer;
import org.mltooling.lab.ComponentManager;
import org.mltooling.lab.LabConfig;
import org.mltooling.lab.authorization.AuthorizationManager;
import org.mltooling.lab.services.CoreService;
import org.pac4j.mongo.profile.MongoProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LabAdminApiHandler extends AbstractApiHandler<LabAdminApiHandler>
    implements LabAdminApi {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(LabAdminApiHandler.class);

  // ================ Members ============================================= //
  private ComponentManager componentManager;
  private MongoProfile authProfile;

  // ================ Constructors & Main ================================= //

  public LabAdminApiHandler() {
    componentManager = ComponentManager.INSTANCE;
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  /** Resets all workspaces. Use with caution. */
  @Override
  public StatusMessageFormat resetAllWorkspaces() {
    StatusMessageFormat response = new StatusMessageFormat();

    try {
      // log event
      componentManager
          .getEventLogManager()
          .logEvent(
              LabEvent.RESET_ALL_WORKSPACES,
              new PropertyContainer()
                  .addProperty("user", authProfile != null ? authProfile.getId() : "unk"));

      // reset all workspaces
      for (String user : componentManager.getAuthManager().getProfileIds()) {
        try {
          String imageName = null; //TODO: Get which image is used right now
          LabService workspaceService = componentManager.getServiceManger().resetWorkspace(user, imageName);
          if (workspaceService == null || !workspaceService.getIsHealthy()) {
            log.warn("Could not check workspace service availability for " + user);
          }
        } catch (Exception ex) {
          log.warn("Cannot reset workspace for " + user + ": " + ex.getMessage());
        }
      }
      response.setSuccessfulStatus();
      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  /**
   * Shutdown all workspaces that are not used based on the workspace backup file. Use with caution.
   */
  @Override
  public ValueListFormat<LabUser> shutdownUnusedWorkspaces(
      boolean dryRun, int daysThreshold, @Nullable List<String> includedIds) {
    ValueListFormat<LabUser> response = new ValueListFormat<>();

    try {
      List<LabUser> inactiveUsers =
          componentManager.getAuthManager().getInactiveUsers(daysThreshold);

      if (includedIds != null) {
        for (String userId : includedIds) {

          try {
            userId = AuthorizationManager.resolveUserName(userId);

            MongoProfile profile = ComponentManager.INSTANCE.getAuthManager().getUser(userId);
            if (profile == null) {
              continue;
            }
            inactiveUsers.add(AuthorizationManager.transformProfile(profile));
          } catch (Exception ex) {
            // do nothing
          }
        }
      }

      if (!dryRun) {
        // shutdown all unused workspaces
        for (LabUser user : inactiveUsers) {
          try {
            if (user.getLastActivity() != null) {
              long inactiveSince =
                  TimeUnit.MILLISECONDS.toDays(
                      new Date().getTime() - user.getLastActivity().getTime());
              log.info(
                  "Shutdown workspace for "
                      + user.getName()
                      + "; Inactive since (days): "
                      + inactiveSince);
            }

            componentManager.getServiceManger().shutdownWorkspace(user.getId());
          } catch (Exception ex) {
            log.warn("Cannot shutdown workspace for " + user.getName() + ": " + ex.getMessage());
          }
        }
      }

      // log event
      componentManager
          .getEventLogManager()
          .logEvent(
              LabEvent.SHUTDOWN_UNUSED_WORKSPACES,
              new PropertyContainer()
                  .addProperty("user", authProfile != null ? authProfile.getId() : "unk")
                  .addProperty("count", inactiveUsers.size()));

      response.setData(inactiveUsers);
      response.setSuccessfulStatus();
      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public ValueListFormat<String> shutdownDiskExceedingContainers(boolean dryRun) {
    ValueListFormat<String> response = new ValueListFormat<>();
    try {
      response.setData(componentManager.getServiceManger().shutdownDiskExceedingContainers(dryRun));
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
    }

    return prepareResponse(response);
  }

  /**
   * Removes the workspace service without volumes for a given user and starts it again.
   *
   * @param user - id which should be used to identify the container. Will be prefixed with
   *     {WORKSPACE_SERVICE_PREFIX}
   */
  public SingleValueFormat<LabService> resetWorkspace(String user){
    // TODO: Check which image was used by the user beforehand and reuse it by default
    return resetWorkspace(user, null);
  }

  public SingleValueFormat<LabService> resetWorkspace(String user, @Nullable String imageName) {

    SingleValueFormat<LabService> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(user)) {
        response.setErrorStatus(
            "The workspace id (user) parameter is empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      user = AuthorizationManager.resolveUserName(user);

      if (this.authProfile == null || user == null) {
        response.setErrorStatus(
            "Not authorized to reset workspace: " + user, HttpStatus.SC_UNAUTHORIZED);
        return prepareResponse(response);
      }

      if (!this.authProfile.getId().equalsIgnoreCase(user)
          && !componentManager.getAuthManager().isAdmin(this.authProfile)) {
        response.setErrorStatus(
            "User " + this.authProfile.getId() + " is not allowed to reset workspace: " + user,
            HttpStatus.SC_UNAUTHORIZED);
        return prepareResponse(response);
      }

      LabService workspaceService = componentManager.getServiceManger().resetWorkspace(user, imageName);
      if (workspaceService == null || !workspaceService.getIsHealthy()) {
        log.error("Could not check service availability.");
        response.setErrorStatus(
            "Could not check service availability.", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        return prepareResponse(response);
      }

      response.setSuccessfulStatus();
      response.setData(workspaceService);
      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  /**
   * Check if workspace container for user already exists and, if not, creates a new one.
   *
   * @param user - id which should be used to identify the container. Will be prefixed with
   *     {WORKSPACE_SERVICE_PREFIX}
   * @return
   */
  @Override
  public StatusMessageFormat checkWorkspace(String user) {
    StatusMessageFormat response = new StatusMessageFormat();
    try {
      if (StringUtils.isNullOrEmpty(user)) {
        response.setErrorStatus("The workspace id parameter is empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }
      log.debug("Checking workspace of " +  user);
      LabService workspaceService = componentManager.getServiceManger().checkWorkspace(user);
      if (workspaceService == null || !workspaceService.getIsHealthy()) {
        log.warn("Could not check service availability for user: " + user);
        response.setErrorStatus(
            "Could not check service availability.", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        return prepareResponse(response);
      }

      response.setSuccessfulStatus();
      //TODO differentiate between users that are running the default one and the other one.
      response.addMetadata("needsUpdate", false);
      response.addMetadata("image", workspaceService.getDockerImage());
//      response.addMetadata(
//          "needsUpdate",
//          !CoreService.WORKSPACE.getImage().equalsIgnoreCase(workspaceService.getDockerImage()));
      return prepareResponse(response);
    } catch (Exception e) {
      log.debug(e.getMessage(), e);
      // TODO do not return error -> currently this error comes if two check calls are called too
      // fast after each other
      response.setSuccessfulStatus();
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<LabInfo> getLabInfo() {
    SingleValueFormat<LabInfo> response = new SingleValueFormat<>();

    try {
      Map<String, String> coreServiceInfo = new HashMap<>();
      for (CoreService service : CoreService.values()) {
        if (!service.isUnknown()) {
          coreServiceInfo.put(service.getName(), service.getImage());
        }
      }

      LabInfo info = new LabInfo();
      info.setVersion(LabConfig.SERVICE_VERSION)
          .setNamespace(LabConfig.LAB_NAMESPACE)
          .setRuntime(LabConfig.SERVICES_RUNTIME)
          .setHealthy(ComponentManager.INSTANCE.getServiceManger().isLabAvailable())
          .setProjectsCount(ComponentManager.INSTANCE.getProjectManager().getProjects().size())
          .setTermsOfService(LabConfig.TERMS_OF_SERVICE_TEXT)
          .setCoreServiceInfo(coreServiceInfo);

      response.setData(info);
      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<LabProjectsStatistics> getStatistics() {
    SingleValueFormat<LabProjectsStatistics> response = new SingleValueFormat<>();

    try {
      response.setData(ComponentManager.INSTANCE.getStatsManager().getStats());
      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public ValueListFormat<LabEvent> getEvents(@Nullable String event) {
    ValueListFormat<LabEvent> response = new ValueListFormat<>();

    try {
      response.setData(componentManager.getEventLogManager().getEvents(event));
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
  @SuppressWarnings("UnusedReturnValue")
  public LabAdminApiHandler setAuthProfile(MongoProfile authProfile) {
    this.authProfile = authProfile;
    return this;
  }
  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
