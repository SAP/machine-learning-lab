package org.mltooling.lab.api;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;
import org.apache.http.HttpStatus;
import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.api.format.StatusMessageFormat;
import org.mltooling.core.api.format.UnifiedFormat;
import org.mltooling.core.api.format.ValueListFormat;
import org.mltooling.core.api.handler.AbstractApiHandler;
import org.mltooling.core.env.handler.FileHandlerUtils;
import org.mltooling.core.lab.LabApi;
import org.mltooling.core.lab.model.*;
import org.mltooling.core.utils.FileUtils;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.core.utils.structures.PropertyContainer;
import org.mltooling.lab.ComponentManager;
import org.mltooling.lab.LabConfig;
import org.mltooling.lab.authorization.AuthorizationManager;
import org.mltooling.lab.components.ExperimentsManager;
import org.mltooling.lab.components.ProjectManager;
import org.mltooling.lab.services.AbstractServiceManager;
import org.mltooling.lab.services.CoreService;
import org.mltooling.lab.services.DockerDeploymentConfig;
import org.pac4j.mongo.profile.MongoProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LabApiHandler extends AbstractApiHandler<LabApiHandler> implements LabApi {

  // ================ Constants =========================================== //

  private static final Logger log = LoggerFactory.getLogger(LabApiHandler.class);

  // ================ Members ============================================= //
  private MongoProfile authProfile;

  private AbstractServiceManager serviceManager;
  private ComponentManager componentManager;
  private ProjectManager projectManager;
  private AuthorizationManager authManager;

  // ================ Constructors & Main ================================= //

  public LabApiHandler() {
    componentManager = ComponentManager.INSTANCE;
    serviceManager = componentManager.getServiceManger();
    authManager = componentManager.getAuthManager();
    projectManager = componentManager.getProjectManager();
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  @Override
  public SingleValueFormat<LabProject> createProject(LabProjectConfig projectConfig) {
    SingleValueFormat<LabProject> response = new SingleValueFormat<>();

    try {
      if (projectConfig == null) {
        response.setErrorStatus(
            "The project configuration in the body should not be empty.",
            HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      if (authProfile != null && AuthorizationManager.isProjectAdmin(authProfile.getId())) {
        response.setErrorStatus(
            "A project admin user is not allowed to create projects.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      projectConfig.setCreator(authProfile.getId());

      projectManager.createProject(projectConfig);
      LabProject project = projectManager.getProject(projectConfig.getName());
      if (project == null) {
        response.setErrorStatus(
            "Failed to create the project " + projectConfig.getName(),
            HttpStatus.SC_INTERNAL_SERVER_ERROR);
        return prepareResponse(response);
      }

      authManager.addProjectPermission(authProfile.getId(), project.getId());

      response.setData(project);
      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public StatusMessageFormat isProjectAvailable(String project) {
    StatusMessageFormat response = new StatusMessageFormat();

    try {
      if (projectManager.isProjectAvailable(project)) {
        response.setSuccessfulStatus();
      } else {
        response.setErrorStatus(
            "Project name is not available for project creation.",
            HttpStatus.SC_INTERNAL_SERVER_ERROR);
      }

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public StatusMessageFormat deleteProject(String project) {
    StatusMessageFormat response = new StatusMessageFormat();

    try {
      if (authProfile != null && AuthorizationManager.isProjectAdmin(authProfile.getId())) {
        response.setErrorStatus(
            "A project admin user is not allowed to delete projects.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      projectManager.deleteProject(project);

      // log delete project event
      componentManager
          .getEventLogManager()
          .logEvent(
              LabEvent.DELETE_PROJECT,
              new PropertyContainer()
                  .addProperty(LabApi.PARAM_PROJECT, project)
                  .addProperty("user", authProfile != null ? authProfile.getId() : "unk"));

      response.setSuccessfulStatus();
      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public ValueListFormat<LabProject> getProjects() {
    ValueListFormat<LabProject> response = new ValueListFormat<>();

    try {
      List<LabProject> filteredProjects =
          authManager.filterProjects(
              authProfile.getPermissions(), componentManager.getProjectManager().getProjects());
      response.setData(filteredProjects);

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<String> createProjectToken(String project) {
    SingleValueFormat<String> response = new SingleValueFormat<>();

    try {
      project = projectManager.resolveProjectName(project);
      String token = authManager.createProjectToken(project);

      if (StringUtils.isNullOrEmpty(token)) {
        response.setErrorStatus("Failed to get project API token.", HttpStatus.SC_BAD_REQUEST);
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
  public SingleValueFormat<LabProject> getProject(String project, @Nullable Boolean expand) {
    SingleValueFormat<LabProject> response = new SingleValueFormat<>();

    try {
      project = projectManager.resolveProjectName(project);

      LabProject labProject = projectManager.getProject(project);
      if (labProject == null) {
        response.setErrorStatus("Project " + project + " not found.", HttpStatus.SC_NOT_FOUND);
        return prepareResponse(response);
      }

      labProject.setStatistics(componentManager.getStatsManager().getStats(project));

      if (expand != null && expand) {
        // TODO do not expand other info for now.
        // labProject.setServices(serviceManager.getServices(project));
        // labProject.setModels(componentManager.getFileManager().listRemoteFiles(project,
        // LabFileDataType.MODEL));
        // labProject.setDatasets(componentManager.getFileManager().listRemoteFiles(project,
        // LabFileDataType.DATASET));
        // labProject.setExperiments(componentManager.getExperimentsManager().getExperiments(project));
        labProject.setMembers(projectManager.getProjectMembers(project));
      }

      response.setData(labProject);
      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public ValueListFormat<LabService> getServices(String project) {
    ValueListFormat<LabService> response = new ValueListFormat<>();

    try {
      project = projectManager.resolveProjectName(project);

      List<LabService> services = serviceManager.getServices(project);
      response.setData(services);

      LabProjectsStatistics projectStatistics = new LabProjectsStatistics();
      projectStatistics.setServicesCount(services.size());
      response.getMetadata().setStats(projectStatistics.getProperties());

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<LabService> getService(String project, String service) {
    SingleValueFormat<LabService> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(service)) {
        response.setErrorStatus("The service parameter is empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = projectManager.resolveProjectName(project);
      try {
        response.setData(serviceManager.getService(service, project));
      } catch (Exception e) {
        response.setErrorStatus("No service found for " + service, HttpStatus.SC_NOT_FOUND);
        return prepareResponse(response);
      }

      if (response.getData() == null) {
        response.setErrorStatus("No service found for " + service, HttpStatus.SC_NOT_FOUND);
        return prepareResponse(response);
      }

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public StatusMessageFormat deleteService(String project, String service) {
    StatusMessageFormat response = new StatusMessageFormat();

    try {
      if (StringUtils.isNullOrEmpty(service)) {
        response.setErrorStatus("The service parameter is empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = projectManager.resolveProjectName(project);
      serviceManager.deleteService(service, true, project);

      // log delete event
      componentManager
          .getEventLogManager()
          .logEvent(
              LabEvent.DELETE_SERVICE,
              new PropertyContainer()
                  .addProperty(LabApi.PARAM_PROJECT, project)
                  .addProperty("user", authProfile != null ? authProfile.getId() : "unk"));

      response.setSuccessfulStatus();
      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<String> getServiceLogs(String project, String service) {
    SingleValueFormat<String> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(service)) {
        response.setErrorStatus("The service parameter is empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = projectManager.resolveProjectName(project);
      String logs =
          serviceManager.getServiceLogs(serviceManager.getService(service, project).getDockerId());
      response.setData(logs);

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public ValueListFormat<LabFile> getFiles(
      String project,
      @Nullable LabFileDataType dataType,
      @Nullable String prefix,
      @Nullable Boolean aggregateVersions) {
    ValueListFormat<LabFile> response = new ValueListFormat<>();

    try {
      project = projectManager.resolveProjectName(project, true);

      LabFileCollection fileCollection =
          componentManager
              .getFileManager()
              .listRemoteFiles(project, dataType, prefix, aggregateVersions);
      response.setData(fileCollection.getLabFiles());

      LabProjectsStatistics projectStatistics = new LabProjectsStatistics();
      projectStatistics.setFilesCount(fileCollection.getFileCount());
      projectStatistics.setFilesTotalSize((double) fileCollection.getTotalSize());
      if (fileCollection.getLastModified() != null) {
        projectStatistics.setLastModified(fileCollection.getLastModified());
      }
      response.getMetadata().setStats(projectStatistics.getProperties());

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public ValueListFormat<LabExperiment> getExperiments(String project) {
    ValueListFormat<LabExperiment> response = new ValueListFormat<>();

    try {
      project = projectManager.resolveProjectName(project);

      List<LabExperiment> experiments =
          componentManager.getExperimentsManager().getExperiments(project);

      response.setData(experiments);
      response.getMetadata().setStats(ExperimentsManager.getExperimentsStats(experiments));

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public StatusMessageFormat deleteExperiment(String project, String experimentKey) {
    StatusMessageFormat response = new StatusMessageFormat();

    try {
      if (StringUtils.isNullOrEmpty(experimentKey)) {
        response.setErrorStatus(
            "The experiment parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = projectManager.resolveProjectName(project);

      // delete experiment folder if it exists
      componentManager
          .getFileManager()
          .deleteFolder(
              project, FileHandlerUtils.resolveKey(experimentKey, LabFileDataType.EXPERIMENT));

      // delete experiment
      componentManager.getExperimentsManager().deleteExperiment(project, experimentKey);

      // log delete experiment event
      componentManager
          .getEventLogManager()
          .logEvent(
              LabEvent.DELETE_EXPERIMENT,
              new PropertyContainer()
                  .addProperty(LabApi.PARAM_PROJECT, project)
                  .addProperty("user", authProfile != null ? authProfile.getId() : "unk")
                  // keep latests versions .addProperty("")
                  .addProperty(LabApi.PARAM_EXPERIMENT, experimentKey));

      response.setSuccessfulStatus();
      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<String> syncExperiment(String project, LabExperiment experiment) {
    SingleValueFormat<String> response = new SingleValueFormat<>();

    try {
      project = projectManager.resolveProjectName(project);

      LabExperiment updatedExperiment =
          componentManager.getExperimentsManager().updateExperiment(project, experiment);
      if (updatedExperiment == null) {
        response.setErrorStatus("Failed to sync experiment.", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        return prepareResponse(response);
      }

      response.setData(updatedExperiment.getKey());
      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<LabService> deployModel(
      String project,
      String modelKey,
      @Nullable String name,
      @Nullable Map<String, String> config) {

    SingleValueFormat<LabService> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(modelKey)) {
        response.setErrorStatus("The modelKey should not be empty", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      if (!StringUtils.isNullOrEmpty(name) && !serviceManager.isValidServiceName(name)) {
        response.setErrorStatus(name + " is not a valid service name.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = projectManager.resolveProjectName(project);

      if (config == null) {
        config = new HashMap<>();
      }

      LabFile labFile = componentManager.getFileManager().getFileInfo(modelKey, project);
      if (labFile == null) {
        response.setErrorStatus(
            "Failed to deploy model. Model file not found.", HttpStatus.SC_NOT_FOUND);
        return prepareResponse(response);
      }

      final String ENV_MODEL_KEY = "MODEL_KEY";
      config.put(ENV_MODEL_KEY, labFile.getKey());

      // TODO check if correct unified model

      if (StringUtils.isNullOrEmpty(name)) {
        String modelName = FileHandlerUtils.getFileNameFromKey(labFile.getKey(), true);
        String modelVersionSuffix = "-v" + labFile.getVersion();
        name =
            StringUtils.simplifyKey(
                    StringUtils.shorten(modelName, 62 - modelVersionSuffix.length()))
                + modelVersionSuffix;
      }

      // should not be longer than 63 because of kubernetes label restrictions
      return deployService(project, CoreService.UNIFIED_MODEL_SERVICE.getImage(), name, config);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(
          "Failed to deploy model as service", HttpStatus.SC_INTERNAL_SERVER_ERROR, e);
      return prepareResponse(response);
    }
  }

  @Override
  public ValueListFormat<LabJob> getJobs(String project) {
    ValueListFormat<LabJob> response = new ValueListFormat<>();

    try {
      project = projectManager.resolveProjectName(project);

      List<LabJob> jobs = serviceManager.getJobs(project);
      response.setData(jobs);
      Integer succeededJobs = 0;
      Integer failedJobs = 0;
      Integer runningJobs = 0;

      for (LabJob job : jobs) {
        if (job.getStatus().equals(LabJob.State.RUNNING.getName())) {
          runningJobs++;
        } else if (job.getStatus().equals(LabJob.State.FAILED.getName())) {
          failedJobs++;
        } else if (job.getStatus().equals(LabJob.State.SUCCEEDED.getName())) {
          succeededJobs++;
        }
      }

      HashMap<String, Object> stats = new HashMap<>();
      stats.put(LabJob.State.SUCCEEDED.getName(), succeededJobs);
      stats.put(LabJob.State.FAILED.getName(), failedJobs);
      stats.put(LabJob.State.RUNNING.getName(), runningJobs);
      response.getMetadata().setStats(stats);

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public ValueListFormat<LabScheduledJob> getScheduledJobs(String project) {
    ValueListFormat<LabScheduledJob> response = new ValueListFormat<>();

    try {
      project = projectManager.resolveProjectName(project);

      response.setData(componentManager.getJobManager().getScheduledJobs(project));

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public StatusMessageFormat deleteScheduledJob(String project, String jobId) {
    StatusMessageFormat response = new StatusMessageFormat();

    try {
      if (StringUtils.isNullOrEmpty(jobId)) {
        response.setErrorStatus("TThe job id is not provided.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = projectManager.resolveProjectName(project);

      if (!componentManager.getJobManager().deleteScheduledJob(project, jobId)) {
        response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
      } else {
        response.setSuccessfulStatus();

        // log delete event
        componentManager
            .getEventLogManager()
            .logEvent(
                LabEvent.DELETE_SCHEDULED_JOB,
                new PropertyContainer()
                    .addProperty(LabApi.PARAM_PROJECT, project)
                    .addProperty("user", authProfile != null ? authProfile.getId() : "unk"));
      }

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<LabJob> getJob(String project, String job) {
    SingleValueFormat<LabJob> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(job)) {
        response.setErrorStatus("The name parameter is empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = projectManager.resolveProjectName(project);
      response.setData(serviceManager.getJob(job, project));

      if (response.getData() == null) {
        response.setErrorStatus("No job found for " + job, HttpStatus.SC_NOT_FOUND);
        return prepareResponse(response);
      }

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public StatusMessageFormat deleteJob(String project, String job) {
    StatusMessageFormat response = new StatusMessageFormat();

    try {
      if (StringUtils.isNullOrEmpty(job)) {
        response.setErrorStatus("The job parameter is empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = projectManager.resolveProjectName(project);
      serviceManager.deleteJob(job, project);

      // log delete event
      componentManager
          .getEventLogManager()
          .logEvent(
              LabEvent.DELETE_JOB,
              new PropertyContainer()
                  .addProperty(LabApi.PARAM_PROJECT, project)
                  .addProperty("user", authProfile != null ? authProfile.getId() : "unk"));

      response.setSuccessfulStatus();
      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<String> getJobLogs(String project, String job) {
    SingleValueFormat<String> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(job)) {
        response.setErrorStatus("The name parameter is empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = projectManager.resolveProjectName(project);
      String logs = serviceManager.getJobLogs(serviceManager.getJob(job, project).getDockerId());
      response.setData(logs);

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<LabJob> deployJob(
      String project,
      String image,
      @Nullable String name,
      @Nullable String schedule,
      @Nullable Map<String, String> config) {
    SingleValueFormat<LabJob> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(image)) {
        response.setErrorStatus("The image should not be empty", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      if (!StringUtils.isNullOrEmpty(name) && !serviceManager.isValidServiceName(name)) {
        response.setErrorStatus(
            name
                + " is not a valid job name. The name has to conform the this regex: "
                + serviceManager.SERVICE_NAME_VALIDATION_REGEX,
            HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = projectManager.resolveProjectName(project);

      if (!StringUtils.isNullOrEmpty(schedule)) {
        componentManager.getJobManager().addScheduledJob(project, image, schedule, name, config);
        response.setSuccessfulStatus();
        // TODO don't return lab job?
      } else {
        // run directly
        LabJob job =
            serviceManager.deployJob(
                serviceManager.createProjectJob(project, image, name).addEnvVariables(config));

        if (job != null) {
          response.setSuccessfulStatus();
          response.setData(job);
        } else {
          response.setErrorStatus(
              "Failed to deploy the job in project " + project,
              HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
      }

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR, e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<LabService> deployService(
      String project, String image, @Nullable String name, @Nullable Map<String, String> config) {
    SingleValueFormat<LabService> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(image)) {
        response.setErrorStatus("The image should not be empty", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      if (!StringUtils.isNullOrEmpty(name) && !serviceManager.isValidServiceName(name)) {
        response.setErrorStatus(
            name
                + " is not a valid service name. The name has to conform the this regex: "
                + serviceManager.SERVICE_NAME_VALIDATION_REGEX,
            HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = projectManager.resolveProjectName(project);

      DockerDeploymentConfig deploymentConfig =
          serviceManager.createProjectService(project, image, name).addEnvVariables(config);

      try {
        // check if service with same name already exists
        if (serviceManager.getService(deploymentConfig.getName()) != null) {
          response.setErrorStatus(
              "A service with the same name already exists.", HttpStatus.SC_BAD_REQUEST);
          return prepareResponse(response);
        }
      } catch (Exception ex) {
        // do nothing
      }

      LabService service = serviceManager.deployService(deploymentConfig);

      if (service != null) {
        response.setSuccessfulStatus();
        response.setData(service);
      } else {
        response.setErrorStatus(
            "Failed to deploy the service in project " + project,
            HttpStatus.SC_INTERNAL_SERVER_ERROR);
      }

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e.getMessage(), HttpStatus.SC_INTERNAL_SERVER_ERROR, e);
      return prepareResponse(response);
    }
  }

  @Override
  public StatusMessageFormat deleteFile(
      String project, String fileKey, @Nullable Integer keepLatestVersions) {
    StatusMessageFormat response = new StatusMessageFormat();

    try {
      if (StringUtils.isNullOrEmpty(fileKey)) {
        response.setErrorStatus(
            "The fileKey parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = projectManager.resolveProjectName(project, true);

      componentManager.getFileManager().deleteFile(fileKey, project, keepLatestVersions);

      // log delete event
      componentManager
          .getEventLogManager()
          .logEvent(
              LabEvent.DELETE_FILE,
              new PropertyContainer()
                  .addProperty(LabApi.PARAM_PROJECT, project)
                  .addProperty("user", authProfile != null ? authProfile.getId() : "unk")
                  .addProperty(LabApi.PARAM_KEEP_LATEST_VERSIONS, keepLatestVersions)
                  // keep latests versions .addProperty("")
                  .addProperty(LabApi.PARAM_FILE_KEY, fileKey));

      response.setSuccessfulStatus();

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<LabFile> getFileInfo(String project, String fileKey) {
    SingleValueFormat<LabFile> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(fileKey)) {
        response.setErrorStatus(
            "The fileKey parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = projectManager.resolveProjectName(project, true);

      try {
        LabFile labFile = componentManager.getFileManager().getFileInfo(fileKey, project);
        response.setData(labFile);
        return prepareResponse(response);
      } catch (NoSuchElementException ex) {
        response.setErrorStatus(ex.getMessage(), HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<String> uploadFile(
      String project,
      InputStream fileStream,
      String fileName,
      LabFileDataType dataType,
      @Nullable Boolean versioning,
      @Nullable Map<String, String> metadata) {
    SingleValueFormat<String> response = new SingleValueFormat<>();

    try {
      project = projectManager.resolveProjectName(project, true);

      if (metadata == null) {
        metadata = new HashMap<>();
      }

      if (authProfile != null) {
        // add creator as metadata
        // only write in if the filed does not exist yet?
        metadata.put(LabApi.FILE_METADATA_PREFIX + LabFile.META_MODIFIED_BY, authProfile.getId());
      }

      metadata.put(LabApi.FILE_METADATA_PREFIX + LabFile.META_PROJECT, project);

      if (!metadata.containsKey(LabApi.FILE_METADATA_PREFIX + LabFile.META_CONTENT_TYPE)) {
        // guess content type by file extension if content type is not provided in metadata
        metadata.put(
            LabApi.FILE_METADATA_PREFIX + LabFile.META_CONTENT_TYPE,
            FileUtils.getContentType(FileHandlerUtils.removeVersionFromKey(fileName)));
      }

      String uploadedKey =
          componentManager
              .getFileManager()
              .uploadFile(fileName, fileStream, dataType, project, versioning, metadata);

      if (!StringUtils.isNullOrEmpty(uploadedKey)) {
        response.setData(uploadedKey);
      } else {
        response.setErrorStatus("Failed to upload file", HttpStatus.SC_INTERNAL_SERVER_ERROR);
      }

      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  @Override
  public SingleValueFormat<LabFile> downloadFile(String project, String fileKey) {
    SingleValueFormat<LabFile> response = new SingleValueFormat<>();

    try {
      if (StringUtils.isNullOrEmpty(fileKey)) {
        response.setErrorStatus(
            "The fileKey parameter should not be empty.", HttpStatus.SC_BAD_REQUEST);
        return prepareResponse(response);
      }

      project = projectManager.resolveProjectName(project, true);

      LabFile labFile = componentManager.getFileManager().getFile(fileKey, project);

      if (labFile == null) {
        response.setErrorStatus(
            "Cannot find file "
                + fileKey
                + (project != null ? " in bucket: " + project : "")
                + " in "
                + project
                + " project.",
            HttpStatus.SC_NOT_FOUND);
        return prepareResponse(response);
      }

      // log download event
      componentManager
          .getEventLogManager()
          .logEvent(
              LabEvent.DOWNLOADED_FILE,
              new PropertyContainer()
                  .addProperty(LabApi.PARAM_PROJECT, project)
                  .addProperty("user", authProfile != null ? authProfile.getId() : "unk")
                  .addProperty(LabApi.PARAM_FILE_KEY, labFile.getKey()));

      response.setData(labFile);
      return prepareResponse(response);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      response.setErrorStatus(e);
      return prepareResponse(response);
    }
  }

  // ================ Public Methods ====================================== //
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

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  @SuppressWarnings("UnusedReturnValue")
  public LabApiHandler setAuthProfile(MongoProfile authProfile) {
    this.authProfile = authProfile;
    return this;
  }

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
