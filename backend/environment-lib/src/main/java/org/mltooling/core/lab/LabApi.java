package org.mltooling.core.lab;

import java.io.InputStream;
import java.util.Map;
import javax.annotation.Nullable;
import org.mltooling.core.api.BaseApi;
import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.api.format.StatusMessageFormat;
import org.mltooling.core.api.format.ValueListFormat;
import org.mltooling.core.lab.model.*;

public interface LabApi extends BaseApi {

  // ================ Constants =========================================== //
  String ENDPOINT_PATH = "/api";

  String PARAM_PROJECT = "project";
  String PARAM_SERVICE = "service";
  String PARAM_EXPERIMENT = "experiment";
  String PARAM_NAME = "name";
  String PARAM_JOB = "job";
  String PARAM_SCHEDULE = "schedule";
  String PARAM_VERSIONING = "versioning";
  String PARAM_DATA_TYPE = "dataType";
  String PARAM_DOCKER_IMAGE = "image";
  String PARAM_FILE = "file";
  String PARAM_FILE_NAME = "fileName";
  String PARAM_FILE_KEY = "fileKey";
  String PARAM_PREFIX = "prefix";
  String PARAM_EXPAND = "expand";
  String PARAM_AGGREGATE_VERSIONS = "aggregateVersions";
  String PARAM_KEEP_LATEST_VERSIONS = "keepLatestVersions";
  String PARAM_CONFIG = "config";

  String METHOD_CREATE_PROJECT = "/projects";
  String METHOD_GET_ALL_PROJECTS = "/projects";

  String METHOD_GET_PROJECT_TOKEN = "/projects/{" + PARAM_PROJECT + "}/token";

  String METHOD_GET_PROJECT = "/projects/{" + PARAM_PROJECT + "}";
  String METHOD_DELETE_PROJECT = "/projects/{" + PARAM_PROJECT + "}";
  String METHOD_PROJECT_AVAILABLE = "/projects/{" + PARAM_PROJECT + "}/available";

  String METHOD_GET_PROJECT_SERVICE =
      "/projects/{" + PARAM_PROJECT + "}/services/{" + PARAM_SERVICE + "}";
  String METHOD_DELETE_PROJECT_SERVICE =
      "/projects/{" + PARAM_PROJECT + "}/services/{" + PARAM_SERVICE + "}";
  String METHOD_GET_PROJECT_SERVICE_LOGS =
      "/projects/{" + PARAM_PROJECT + "}/services/{" + PARAM_SERVICE + "}/logs";
  String METHOD_GET_PROJECT_SERVICES = "/projects/{" + PARAM_PROJECT + "}/services";
  String METHOD_DEPLOY_SERVICE = "/projects/{" + PARAM_PROJECT + "}/services";

  String METHOD_GET_ALL_JOBS = "/projects/{" + PARAM_PROJECT + "}/jobs";
  String METHOD_DEPLOY_JOB = "/projects/{" + PARAM_PROJECT + "}/jobs";
  String METHOD_GET_JOB = "/projects/{" + PARAM_PROJECT + "}/jobs/{" + PARAM_JOB + "}";
  String METHOD_DELETE_JOB = "/projects/{" + PARAM_PROJECT + "}/jobs/{" + PARAM_JOB + "}";
  String METHOD_GET_JOB_LOGS = "/projects/{" + PARAM_PROJECT + "}/jobs/{" + PARAM_JOB + "}/logs";
  String METHOD_GET_SCHEDULED_JOBS = "/projects/{" + PARAM_PROJECT + "}/jobs/scheduled";
  String METHOD_DELETE_SCHEDULED_JOBS =
      "/projects/{" + PARAM_PROJECT + "}/jobs/scheduled/{" + PARAM_JOB + "}";

  String METHOD_SYNC_EXPERIMENT = "/projects/{" + PARAM_PROJECT + "}/experiments";
  String METHOD_GET_EXPERIMENTS = "/projects/{" + PARAM_PROJECT + "}/experiments";
  String METHOD_DELETE_EXPERIMENT = "/projects/{" + PARAM_PROJECT + "}/experiments";

  String METHOD_GET_FILE_INFO = "/projects/{" + PARAM_PROJECT + "}/files/info";
  String METHOD_UPLOAD_FILE = "/projects/{" + PARAM_PROJECT + "}/files/upload";
  String METHOD_DOWNLOAD_FILE = "/projects/{" + PARAM_PROJECT + "}/files/download";
  String METHOD_DELETE_FILE = "/projects/{" + PARAM_PROJECT + "}/files";
  String METHOD_GET_FILES = "/projects/{" + PARAM_PROJECT + "}/files";
  String METHOD_DEPLOY_MODEL = "/projects/{" + PARAM_PROJECT + "}/files/models/deploy";

  String FILE_METADATA_PREFIX =
      "x-amz-meta-"; // metadata provided in the headers needs to have the x-amz-meta prefix (S3
                     // conform)

  // ================ Methods ============================================= //
  // Projects API

  SingleValueFormat<LabProject> createProject(LabProjectConfig config);

  StatusMessageFormat isProjectAvailable(String project);

  StatusMessageFormat deleteProject(String project);

  ValueListFormat<LabProject> getProjects();

  SingleValueFormat<String> createProjectToken(String project);

  SingleValueFormat<LabProject> getProject(String project, @Nullable Boolean expand);

  // Deployment API

  ValueListFormat<LabService> getServices(String project);

  SingleValueFormat<LabService> getService(String project, String service);

  StatusMessageFormat deleteService(String project, String service);

  SingleValueFormat<String> getServiceLogs(String project, String service);

  SingleValueFormat<LabService> deployService(
      String project, String image, @Nullable String name, @Nullable Map<String, String> config);

  SingleValueFormat<LabService> deployModel(
      String project, String fileKey, @Nullable String name, @Nullable Map<String, String> config);

  // Jobs API

  ValueListFormat<LabJob> getJobs(String project);

  SingleValueFormat<LabJob> getJob(String project, String job);

  StatusMessageFormat deleteJob(String project, String job);

  SingleValueFormat<String> getJobLogs(String project, String job);

  SingleValueFormat<LabJob> deployJob(
      String project,
      String image,
      @Nullable String name,
      @Nullable String schedule,
      @Nullable Map<String, String> config);

  ValueListFormat<LabScheduledJob> getScheduledJobs(String project);

  StatusMessageFormat deleteScheduledJob(String project, String jobId);

  // Experiments API

  ValueListFormat<LabExperiment> getExperiments(String project);

  StatusMessageFormat deleteExperiment(String project, String experimentKey);

  SingleValueFormat<String> syncExperiment(String project, LabExperiment experiment);

  // Remote File API

  ValueListFormat<LabFile> getFiles(
      String project,
      @Nullable LabFileDataType dataType,
      @Nullable String prefix,
      @Nullable Boolean aggregateVersions);

  StatusMessageFormat deleteFile(
      String project, String fileKey, @Nullable Integer keepLatestVersions);

  SingleValueFormat<LabFile> getFileInfo(String project, String fileKey);

  SingleValueFormat<String> uploadFile(
      String project,
      InputStream fileStream,
      String fileName,
      LabFileDataType dataType,
      @Nullable Boolean versioning,
      @Nullable Map<String, String> metadata);

  SingleValueFormat<LabFile> downloadFile(String project, String fileKey);
}
