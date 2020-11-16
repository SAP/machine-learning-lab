package org.mltooling.core.lab;

import java.util.List;
import javax.annotation.Nullable;
import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.api.format.StatusMessageFormat;
import org.mltooling.core.api.format.ValueListFormat;
import org.mltooling.core.lab.model.LabEvent;
import org.mltooling.core.lab.model.LabInfo;
import org.mltooling.core.lab.model.LabProjectsStatistics;
import org.mltooling.core.lab.model.LabUser;

public interface LabAdminApi {

  // ================ Constants =========================================== //
  String ENDPOINT_PATH = LabApi.ENDPOINT_PATH + "/admin";

  // REST method params
  String PARAM_TYPE = "type";
  String PARAM_WORKSPACE_ID = "id";
  String PARAM_EVENT = "event";
  String PARAM_IS_ANONYMOUS = "anonymous";
  String PARAM_DRY_RUN = "dryrun";
  String PARAM_DAYS_THRESHOLD = "threshold";

  String METHOD_CHECK_WORKSPACE = "/workspace/check";
  String METHOD_RESET_WORKSPACE = "/workspace/reset";
  String METHOD_RESET_ALL_WORKSPACES = "/workspace/reset-all";
  String METHOD_SHUTDOWN_UNUSED_WORKSPACES = "/workspace/shutdown-unused";

  String METHOD_SHUTDOWN_DISK_EXCEEDING_CONTAINERS = "/workspace/shutdown-disk-exceeding";

  String METHOD_GET_INFO = "/info";
  String METHOD_GET_STATISTICS = "/statistics";

  String METHOD_GET_EVENTS = "/events";

  // ================ Methods ============================================= //
  // TODO why config with Object??

  StatusMessageFormat checkWorkspace(String workspaceId);

  SingleValueFormat resetWorkspace(String workspaceId);

  StatusMessageFormat resetAllWorkspaces();

  ValueListFormat<LabUser> shutdownUnusedWorkspaces(
      boolean dryRun, int daysThreshold, @Nullable List<String> includedIds);

  ValueListFormat<String> shutdownDiskExceedingContainers(boolean dryRun);

  SingleValueFormat<LabInfo> getLabInfo();

  SingleValueFormat<LabProjectsStatistics> getStatistics();

  ValueListFormat<LabEvent> getEvents(@Nullable String event);
}
