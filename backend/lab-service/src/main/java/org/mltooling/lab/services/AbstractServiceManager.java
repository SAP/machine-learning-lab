package org.mltooling.lab.services;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerCertificateException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.DockerRequestException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.messages.ImageInfo;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.mltooling.core.lab.model.LabJob;
import org.mltooling.core.lab.model.LabService;
import org.mltooling.core.utils.CryptoUtils;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.lab.ComponentManager;
import org.mltooling.lab.LabConfig;
import org.mltooling.lab.authorization.AuthorizationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractServiceManager {

  // ================ Constants =========================================== //
  protected final Logger log = LoggerFactory.getLogger(getClass());

  // Lab Feature Labels
  protected static final String LABEL_NAMESPACE = "lab.namespace";
  protected static final String LABEL_DOCKER_NAME = "lab.docker.name";
  protected static final String LABEL_FEATURE_TYPE = "lab.feature.type";
  protected static final String LABEL_FEATURE_NAME = "lab.feature.name";
  protected static final String LABEL_PROJECT = "lab.project";

  // Lab Configuration Variables
  protected static final String ENV_NAME_LAB_ENDPOINT = "LAB_ENDPOINT";
  protected static final String ENV_NAME_LAB_API_TOKEN = "LAB_API_TOKEN";
  protected static final String ENV_NAME_LAB_PROJECT = "LAB_PROJECT";
  protected static final String ENV_NAME_LAB_SERVICE_PATH = "LAB_SERVICE_PATH";

  protected static final String ENV_NAME_OMP_NUM_THREADS = "OMP_NUM_THREADS";

  // Lab Default Configurations
  protected static final String DEFAULT_PERSISTENCE_PATH = "/data";
  protected static final Integer DEFAULT_CONNECTION_PORT = 8080;

  // use default service user or password if user/password is required for a certain service
  public static final String DEFAULT_SERVICE_USER = "user-dev";
  public static final String DEFAULT_SERVICE_PASSWORD = LabConfig.JWT_SECRET; // "Password4DEV";

  protected static final String CORE_SERVICES_NETWORK = LabConfig.LAB_NAMESPACE + "-core";
  protected static final String DOCKER_PROJECT_NETWORK_PREFIX = "project";

  // https://stackoverflow.com/questions/42642561/docker-restrictions-regarding-naming-container
  // https://stackoverflow.com/questions/27791913/what-characters-are-allowed-in-kubernetes-port-and-container-names
  // . should not be part of docker name
  public static final String SERVICE_NAME_VALIDATION_REGEX =
      "[a-zA-Z0-9][a-zA-Z0-9\\-]+[a-zA-Z0-9]";

  private static final Integer DOCKER_NAME_MAX_LENGTH = 63; // max length of kubernetes is 63
  private static Integer SERVICE_NAME_MAX_LENGTH = 63;

  protected final long MAX_WAIT_TIME =
      TimeUnit.MINUTES.toMillis(
          2); // 2 minutes might be too little, especially if the images have to be pulled first.
  // So, for the very first deployment where the images do not exist on the host yet,
  // the time might be too small.

  protected static final int MAX_CONTAINER_SIZE_DISABLED = -1;

  // ================ Members ============================================= //
  protected DockerClient client;

  // ================ Constructors & Main ================================= //
  public AbstractServiceManager() {
    try {
      client = DefaultDockerClient.fromEnv().build();
    } catch (DockerCertificateException e) {
      log.error("Could not init docker client", e.getMessage());
    }
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  /** Delete landscape resources for a project */
  public abstract boolean deleteProjectResources(String project) throws Exception;

  /** Deploy a service on the landscape */
  public abstract LabService deployService(DockerDeploymentConfig deploymentConfig)
      throws Exception;

  /** Deploy a job on the landscape */
  public abstract LabJob deployJob(DockerDeploymentConfig deploymentConfig) throws Exception;

  /** Get all jobs for a give project */
  public abstract List<LabJob> getJobs(String project) throws Exception;

  /**
   * Find a job by docker name or docker id and filter by project
   *
   * @param jobId docker name or docker id
   */
  public abstract LabJob getJob(String jobId, @Nullable String project) throws Exception;

  /**
   * Delete a job by docker name or docker id and filter by project
   *
   * @param jobId docker name or docker id
   */
  public abstract boolean deleteJob(String jobId, String project) throws Exception;

  /** Get all services or filter by project */
  public abstract List<LabService> getServices(@Nullable String project) throws Exception;

  /**
   * Find a service by docker name, id or feature name
   *
   * @param serviceId docker name, docker id or feature name
   */
  public abstract LabService getService(String serviceId, @Nullable String project)
      throws Exception;

  /** Delete a specific service */
  public abstract boolean deleteService(
      String serviceId, boolean removeVolumes, @Nullable String project) throws Exception;

  /** Get the logs of a specified service by docker name, docker id or feature name */
  public abstract String getServiceLogs(String serviceId) throws Exception;

  /** Get the logs of a specified job by docker name or docker id */
  public abstract String getJobLogs(String jobId) throws Exception;

  /** Get Lab Service */
  protected abstract LabService getLabService() throws Exception;

  /** Uninstall Lab */
  public abstract void uninstallLab() throws Exception;

  /** Remove all containers that exceed the disk limit defined by $MAX_CONTAINER_SIZE */
  public abstract List<String> shutdownDiskExceedingContainers(boolean dryRun) throws Exception;

  /** Update Lab */
  public void updateLab(boolean backendOnly) throws Exception {
    log.info(
        "Updating Lab (namespace: "
            + LabConfig.LAB_NAMESPACE
            + ") to version "
            + LabConfig.SERVICE_VERSION);

    LabService backendService = null;
    try {
      backendService = getService(CoreService.LAB_BACKEND.getName(), null);
    } catch (Exception ex) {
      log.warn("Failed to find service: " + CoreService.LAB_BACKEND.getName());
    }

    if (backendService != null) {
      // check important configuration
      String namespace = backendService.getConfiguration().get(LabConfig.ENV_NAME_LAB_NAMESPACE);
      if (!StringUtils.isNullOrEmpty(namespace) && !namespace.equals(LabConfig.LAB_NAMESPACE)) {
        throw new Exception(
            LabConfig.ENV_NAME_LAB_NAMESPACE
                + " should be the same value as the one of the current Lab instance: "
                + namespace);
      }

      String runtime = backendService.getConfiguration().get(LabConfig.ENV_NAME_SERVICES_RUNTIME);
      if (!StringUtils.isNullOrEmpty(runtime) && !runtime.equals(LabConfig.SERVICES_RUNTIME)) {
        throw new Exception(
            LabConfig.ENV_NAME_SERVICES_RUNTIME
                + " should be the same value as the one of the current Lab instance: "
                + runtime);
      }

      if (backendOnly) {
        // only check these value if the light-update is selected = allow changing parameters in
        // update-full
        String jwtToken = backendService.getConfiguration().get(LabConfig.ENV_NAME_JWT_SECRET);
        if (!StringUtils.isNullOrEmpty(jwtToken) && !jwtToken.equals(LabConfig.JWT_SECRET)) {
          throw new Exception(
              LabConfig.ENV_NAME_JWT_SECRET
                  + " should be the same value as the one of the current Lab instance: "
                  + jwtToken);
        }

        String sslEnabled = backendService.getConfiguration().get(LabConfig.ENV_NAME_SSL_ENABLED);
        if (!StringUtils.isNullOrEmpty(sslEnabled)
            && !Boolean.valueOf(sslEnabled) == LabConfig.SERVICE_SSL_ENABLED) {
          throw new Exception(
              LabConfig.ENV_NAME_SSL_ENABLED
                  + " should be the same value as the one of the current Lab instance: "
                  + sslEnabled);
        }

        String hostRootSslMount =
            backendService.getConfiguration().get(LabConfig.ENV_NAME_HOST_ROOT_SSL_MOUNT_PATH);
        if ((StringUtils.isNullOrEmpty(hostRootSslMount)
                && !StringUtils.isNullOrEmpty(LabConfig.HOST_ROOT_SSL_MOUNT_PATH))
            || (!StringUtils.isNullOrEmpty(hostRootSslMount)
                && !hostRootSslMount.equals(LabConfig.HOST_ROOT_SSL_MOUNT_PATH))) {
          throw new Exception(
              LabConfig.ENV_NAME_HOST_ROOT_SSL_MOUNT_PATH
                  + " should be the same value as the one of the current Lab instance: "
                  + hostRootSslMount);
        }

        String hostRootMount =
            backendService.getConfiguration().get(LabConfig.ENV_NAME_HOST_ROOT_DATA_MOUNT_PATH);
        if ((StringUtils.isNullOrEmpty(hostRootMount)
                && !StringUtils.isNullOrEmpty(LabConfig.HOST_ROOT_DATA_MOUNT_PATH))
            || (!StringUtils.isNullOrEmpty(hostRootMount)
                && !hostRootMount.equals(LabConfig.HOST_ROOT_DATA_MOUNT_PATH))) {
          throw new Exception(
              LabConfig.ENV_NAME_HOST_ROOT_DATA_MOUNT_PATH
                  + " should be the same value as the one of the current Lab instance: "
                  + hostRootMount);
        }

        String hostWorkspaceMount =
            backendService
                .getConfiguration()
                .get(LabConfig.ENV_NAME_HOST_ROOT_WORKSPACE_DATA_MOUNT_PATH);
        if ((StringUtils.isNullOrEmpty(hostWorkspaceMount)
                && !StringUtils.isNullOrEmpty(LabConfig.HOST_ROOT_WORKSPACE_DATA_MOUNT_PATH))
            || (!StringUtils.isNullOrEmpty(hostWorkspaceMount)
                && !hostWorkspaceMount.equals(LabConfig.HOST_ROOT_WORKSPACE_DATA_MOUNT_PATH))) {
          throw new Exception(
              LabConfig.ENV_NAME_HOST_ROOT_WORKSPACE_DATA_MOUNT_PATH
                  + " should be the same value as the one of the current Lab instance: "
                  + hostWorkspaceMount);
        }
      }

      deleteService(backendService.getDockerId(), false, null);
    }

    if (!backendOnly) {
      // delete other core services but no volumes
      deleteService(CoreService.MONGO.getName(), false, null);
      deleteService(CoreService.MINIO.getName(), false, null);
    }

    Thread.sleep(5000);

    // install lab again
    installLab();

    if (!isLabAvailable()) {
      throw new Exception("Failed to install Lab.");
    }

    log.info("Lab was updated successfully");
  }

  /**
   * Find a service by docker name, id or feature name
   *
   * @param serviceId docker name, docker id or feature name
   */
  public LabService getService(String serviceId) throws Exception {
    return getService(serviceId, null);
  }

  /** Create landscape resources for a project */
  public void createProjectResources(String project) throws Exception {
    // Nothing to do here, no default project services exist
  }

  /** Check if workspace container for user already exists and, if not, creates a new one. */
  public LabService checkWorkspace(String user) throws Exception {

    user = AuthorizationManager.resolveUserName(user);

    if (ComponentManager.INSTANCE.getAuthManager().getUser(user) == null) {
      throw new Exception("User " + user + " does not exist.");
    }

    String workspaceName = getWorkspaceName(user);
    LabService workspaceService = null;

    // wrap around try-catch because underlying method 'getService' throws an exception when the
    // defined service is not found
    try {
      workspaceService = getService(workspaceName);
    } catch (Exception ignored) {
    }

    if (workspaceService != null) {
      return workspaceService;
    }

    return deployService(createWorkspaceService(user));
  }

  public boolean shutdownWorkspace(String user) throws Exception {
    if (ComponentManager.INSTANCE.getAuthManager().getUser(user) == null) {
      throw new Exception("User " + user + " does not exist.");
    }

    String workspaceName = getWorkspaceName(user);
    LabService workspaceService = getService(workspaceName);

    if (workspaceService == null) {
      throw new Exception("Could not find workspace for user: " + user);
    }

    return deleteService(workspaceService.getDockerName(), false, null);
  }

  public boolean deleteWorkspace(String user) throws Exception {
    if (ComponentManager.INSTANCE.getAuthManager().getUser(user) == null) {
      throw new Exception("User " + user + " does not exist.");
    }

    String workspaceName = getWorkspaceName(user);
    LabService workspaceService = getService(workspaceName);

    if (workspaceService == null) {
      return false;
    }

    // TODO delete network in docker service manager?
    return deleteService(workspaceService.getDockerName(), true, null);
  }

  public LabService resetWorkspace(String user) throws Exception {
    // remove workspace without volumes
    shutdownWorkspace(user);

    Thread.sleep(2000);

    // start workspace again
    return checkWorkspace(user);
  }

  /** Install Lab - this script is always called in local docker mode only */
  public void installLab() throws Exception {
    log.info("Started Lab Installation.");

    if (!isServiceHealthy(CoreService.MINIO.getName())) {
      log.info(
          CoreService.MINIO.getName()
              + " service does not exist. Create service based on "
              + CoreService.MINIO.getImage());
      // TODO set username and password
      DockerDeploymentConfig minioConfig =
          createCoreService(CoreService.MINIO.getImage(), CoreService.MINIO.getName())
              .setVolumePath(DEFAULT_PERSISTENCE_PATH)
              .addEnvVariable("MINIO_ACCESS_KEY", DEFAULT_SERVICE_USER)
              .addEnvVariable("MINIO_SECRET_KEY", DEFAULT_SERVICE_PASSWORD)
              .setCmd("server " + DEFAULT_PERSISTENCE_PATH)
              .setReplicationAllowed(false)
              .setNodeSelector(DockerDeploymentConfig.NODE_SELECTOR_MASTER);

      deployService(minioConfig);
    }

    if (!isServiceHealthy(CoreService.MONGO.getName())) {
      log.info(
          CoreService.MONGO.getName()
              + " service does not exist. Create service based on "
              + CoreService.MONGO.getImage());

      DockerDeploymentConfig mongoConfig =
          createCoreService(CoreService.MONGO.getImage(), CoreService.MONGO.getName())
              .setVolumePath("/data/db") // TODO what to do with configdb
              // TODO if user and password is set, authentication is activated
              .addEnvVariable("MONGO_INITDB_ROOT_USERNAME", DEFAULT_SERVICE_USER)
              .addEnvVariable("MONGO_INITDB_ROOT_PASSWORD", DEFAULT_SERVICE_PASSWORD)
              .setReplicationAllowed(false)
              .setNodeSelector(DockerDeploymentConfig.NODE_SELECTOR_MASTER);

      deployService(mongoConfig);
    }

    if (!isServiceHealthy(CoreService.LAB_BACKEND.getName())) {
      log.info(
          CoreService.LAB_BACKEND.getName()
              + " service does not exist. Create service based on "
              + CoreService.LAB_BACKEND.getImage());
      deployService(createLabService());
    }
  }

  /** Checks if service is available */
  public boolean isServiceAvailable(String serviceId) {
    return isServiceAvailable(serviceId, null);
  }

  /** Checks if lab landscape is available. */
  public boolean isLabAvailable() {

    if (!isServiceAvailable(CoreService.LAB_BACKEND.getName(), MAX_WAIT_TIME)) {
      log.error("Lab Backend service is not available");
      return false;
    }

    if (!isServiceAvailable(CoreService.MINIO.getName(), MAX_WAIT_TIME)) {
      log.error("Minio service is not available");
      return false;
    }

    if (!isServiceAvailable(CoreService.MONGO.getName(), MAX_WAIT_TIME)) {
      log.error("Mongo service is not available");
      return false;
    }

    return true;
  }

  /** Create a workspace service for a given user. */
  public DockerDeploymentConfig createWorkspaceService(String user) throws Exception {
    String dockerName = getWorkspaceName(user);

    String jupyterBaseUrl = "workspace/id/" + user + "/";
    if (!StringUtils.isNullOrEmpty(LabConfig.LAB_BASE_URL)) {
      jupyterBaseUrl = LabConfig.LAB_BASE_URL + "/" + jupyterBaseUrl;
    }

    HashMap<String, String> envVars = new HashMap<>();
    envVars.put(ENV_NAME_LAB_ENDPOINT, this.getLabBackendEndpoint());
    envVars.put(
        ENV_NAME_LAB_API_TOKEN, ComponentManager.INSTANCE.getAuthManager().createApiToken(user));
    envVars.put("LAB_BACKUP", LabConfig.WORKSPACE_BACKUP); // activate lab backup functionality
    envVars.put("WORKSPACE_BASE_URL", jupyterBaseUrl);
    envVars.put(
        "WORKSPACE_STORAGE_LIMIT",
        LabConfig.SERVICES_STORAGE_LIMIT); // 100 GB limit on /workspace folder
    // kill kernel if idle to long (more than 48 hours)
    envVars.put("SHUTDOWN_INACTIVE_KERNELS", "172800");
    envVars.put("MAX_NUM_THREADS", LabConfig.SERVICES_CPU_LIMIT);
    // Set workspace port to 8091 -> the default port might be changed in the future
    envVars.put("WORKSPACE_PORT", "8091");
    // env variables for SSH Tunneling
    // is the same like Workspace Name, but will be used by Workspace Container to differentiate
    // between setup with or without runtime manager
    envVars.put("SSH_JUMPHOST_TARGET", dockerName);

    // provide proxy settings
    if (!StringUtils.isNullOrEmpty(LabConfig.ENV_HTTP_PROXY)) {
      envVars.put(LabConfig.ENV_NAME_HTTP_PROXY, LabConfig.ENV_HTTP_PROXY);
    }

    if (!StringUtils.isNullOrEmpty(LabConfig.ENV_NAME_HTTPS_PROXY)) {
      envVars.put(LabConfig.ENV_NAME_HTTPS_PROXY, LabConfig.ENV_HTTPS_PROXY);
    }

    if (!StringUtils.isNullOrEmpty(LabConfig.ENV_NAME_NO_PROXY)) {
      envVars.put(LabConfig.ENV_NAME_NO_PROXY, LabConfig.ENV_NO_PROXY);
    }

    String dockerImage = CoreService.WORKSPACE.getImage();

    final String volumePath = "/workspace";
    DockerDeploymentConfig workspaceConfig =
        new DockerDeploymentConfig()
            .setName(dockerName)
            .setImage(dockerImage)
            .setFeatureType(FeatureType.WORKSPACE)
            .setFeatureName(CoreService.WORKSPACE.getName() + "-" + user) // workspace-<user>
            .setNamespace(LabConfig.LAB_NAMESPACE)
            .setVolumePath(volumePath)
            .addNetwork(dockerName) // use workspace name as network name as well
            .addPortsToPublish(getPortToPublish(dockerImage))
            .setReplicationAllowed(false) // should not be replicated, but can be distributed
            .setEnvVariables(envVars); // set user API token in workspace// ;

    HashMap<String, String> labels = new HashMap<>();
    labels.put(LABEL_NAMESPACE, workspaceConfig.getNamespace());
    labels.put(LABEL_DOCKER_NAME, dockerName);
    labels.put(LABEL_FEATURE_TYPE, workspaceConfig.getFeatureType().getName());
    labels.put(LABEL_FEATURE_NAME, workspaceConfig.getFeatureName());
    workspaceConfig.setLabels(labels);

    return workspaceConfig;
  }

  /** Create a lab backend service. */
  public DockerDeploymentConfig createLabService() {
    String publishedPort = String.valueOf(CoreService.LAB_BACKEND.getConnectionPort());

    if (!StringUtils.isNullOrEmpty(LabConfig.LAB_PORT)) {
      publishedPort = LabConfig.LAB_PORT + ":" + publishedPort;
    }
    DockerDeploymentConfig deploymentConfig =
        createCoreService(CoreService.LAB_BACKEND.getImage(), CoreService.LAB_BACKEND.getName())
            .addEnvVariables(LabConfig.getEnvVariables())
            .addEnvVariable(LabConfig.ENV_NAME_LAB_ACTION, LabConfig.LabAction.SERVE.getName())
            .addMount(
                DockerDeploymentConfig.BIND_MOUNT_TYPE, "/var/run/docker.sock:/var/run/docker.sock")
            .addPortsToPublish(publishedPort) // publish port
            .setReplicationAllowed(true);

    return deploymentConfig;
  }

  /** Create a core service. */
  public DockerDeploymentConfig createCoreService(String image, @Nullable String name) {

    if (StringUtils.isNullOrEmpty(name)) {
      name = DockerUtils.extractNameFromImage(image);
    }

    name = processServiceName(name);

    String dockerName = processDockerName(LabConfig.LAB_NAMESPACE + "-" + name.toLowerCase());

    HashMap<String, String> envVars = new HashMap<>();
    envVars.put(ENV_NAME_LAB_ENDPOINT, this.getLabBackendEndpoint());
    // TODO does not work for mongo since mongo is needed for user db
    // envVars.put(ENV_NAME_LAB_API_TOKEN,
    // ComponentManager.INSTANCE.getAuthManager().createAdminToken());

    DockerDeploymentConfig config =
        new DockerDeploymentConfig()
            .setName(dockerName)
            .setImage(image)
            .setNamespace(LabConfig.LAB_NAMESPACE)
            .addNetwork(CORE_SERVICES_NETWORK)
            .setFeatureName(name)
            .setFeatureType(FeatureType.CORE_SERVICE)
            .addPortsToPublish(getPortToPublish(image))
            .setEnvVariables(envVars);

    HashMap<String, String> labels = new HashMap<>();
    labels.put(LABEL_NAMESPACE, config.getNamespace());
    labels.put(LABEL_DOCKER_NAME, dockerName);
    labels.put(LABEL_FEATURE_TYPE, config.getFeatureType().getName());
    labels.put(LABEL_FEATURE_NAME, config.getFeatureName());
    config.setLabels(labels);

    return config;
  }

  /** Create a service bound to a project. */
  public DockerDeploymentConfig createProjectService(
      String project, String image, @Nullable String name) {
    // Project should be already checked to be valid

    if (StringUtils.isNullOrEmpty(name)) {
      name = DockerUtils.extractNameFromImage(image);
    }

    name = processServiceName(name);

    String dockerName =
        processDockerName(LabConfig.LAB_NAMESPACE + "-" + project + "-" + name.toLowerCase());

    Map<String, String> envVars = new HashMap<>();
    envVars.put(ENV_NAME_LAB_ENDPOINT, this.getLabBackendEndpoint());
    try {
      envVars.put(
          ENV_NAME_LAB_API_TOKEN,
          ComponentManager.INSTANCE.getAuthManager().createProjectToken(project));
    } catch (Exception e) {
      log.error("Failed to create project token.", e);
    }

    envVars.put(ENV_NAME_LAB_PROJECT, project);
    envVars.put(
        ENV_NAME_LAB_SERVICE_PATH,
        LabConfig.LAB_BASE_URL + "/lab/projects/" + project + "/services/" + dockerName + "/");
    // Set OMP NUM Threads to prevent overusage of CPUs of certain frameworks
    envVars.put(ENV_NAME_OMP_NUM_THREADS, LabConfig.SERVICES_CPU_LIMIT);

    // Create docker deployment config
    DockerDeploymentConfig deploymentConfig =
        new DockerDeploymentConfig()
            .setName(dockerName)
            .setImage(image)
            .setNamespace(LabConfig.LAB_NAMESPACE)
            .setFeatureType(FeatureType.PROJECT_SERVICE)
            .setFeatureName(name)
            .addPortsToPublish(getPortToPublish(image))
            .addNetwork(getProjectNetworkName(project))
            .setEnvVariables(envVars);

    HashMap<String, String> labels = new HashMap<>();
    labels.put(LABEL_NAMESPACE, deploymentConfig.getNamespace());
    labels.put(LABEL_DOCKER_NAME, dockerName);
    labels.put(LABEL_FEATURE_TYPE, deploymentConfig.getFeatureType().getName());
    labels.put(LABEL_FEATURE_NAME, deploymentConfig.getFeatureName());
    labels.put(LABEL_PROJECT, project);
    deploymentConfig.setLabels(labels);

    return deploymentConfig;
  }

  /** Create a job bound to a project. */
  public DockerDeploymentConfig createProjectJob(
      String project, String image, @Nullable String name) {
    // Project should be already checked to be valid

    if (StringUtils.isNullOrEmpty(name)) {
      name = DockerUtils.extractNameFromImage(image);
    }

    name = processServiceName(name);

    // -14 from full length to allow addition of current timestamp
    String dockerName =
        processDockerName(
            LabConfig.LAB_NAMESPACE + "-" + project + "-" + name.toLowerCase(),
            DOCKER_NAME_MAX_LENGTH - 14);

    // Add timestamp to name to make the name unique; otherwise, only a single job per image &
    // project could exist.
    dockerName = dockerName + "-" + new Date().getTime();

    Map<String, String> envVars = new HashMap<>();
    envVars.put(ENV_NAME_LAB_ENDPOINT, this.getLabBackendEndpoint());
    try {
      envVars.put(
          ENV_NAME_LAB_API_TOKEN,
          ComponentManager.INSTANCE.getAuthManager().createProjectToken(project));
    } catch (Exception e) {
      log.error("Failed to create project token.", e);
    }
    envVars.put(ENV_NAME_LAB_PROJECT, project);
    // Set OMP NUM Threads to prevent overusage of CPUs of certain frameworks
    envVars.put(ENV_NAME_OMP_NUM_THREADS, LabConfig.SERVICES_CPU_LIMIT);

    DockerDeploymentConfig deploymentConfig =
        new DockerDeploymentConfig()
            .setName(dockerName)
            .setImage(image)
            .setNamespace(LabConfig.LAB_NAMESPACE)
            .setFeatureType(FeatureType.PROJECT_JOB)
            .setFeatureName(name)
            .addNetwork(getProjectNetworkName(project))
            .setEnvVariables(envVars);

    HashMap<String, String> labels = new HashMap<>();
    labels.put(LABEL_NAMESPACE, deploymentConfig.getNamespace());
    labels.put(LABEL_DOCKER_NAME, dockerName);
    labels.put(LABEL_FEATURE_TYPE, deploymentConfig.getFeatureType().getName());
    labels.put(LABEL_FEATURE_NAME, deploymentConfig.getFeatureName());
    labels.put(LABEL_PROJECT, project);
    deploymentConfig.setLabels(labels);

    return deploymentConfig;
  }

  public String getLabBackendEndpoint() {
    // http<s>://<LAB_SERVICE_NAME>:8091
    String protocol = "http";
    if (LabConfig.SERVICE_SSL_ENABLED) {
      protocol = "https";
    }

    return protocol
        + "://"
        + LabConfig.LAB_NAMESPACE
        + "-"
        + CoreService.LAB_BACKEND.getName()
        + ":"
        + CoreService.LAB_BACKEND.getConnectionPort();
  }

  public boolean isValidServiceName(String serviceName) {
    return !StringUtils.isNullOrEmpty(serviceName)
        && serviceName.matches(SERVICE_NAME_VALIDATION_REGEX);
  }

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //
  private Set<String> getPortToPublish(String dockerImage) {
    Set<Integer> exposedPorts = getExposedPortsFromImage(dockerImage);
    Integer connectionPort = DEFAULT_CONNECTION_PORT;
    CoreService coreService = CoreService.from(dockerImage);
    if (!coreService.isUnknown()) {
      // use connection port from core service
      // use connection port from core service
      connectionPort = coreService.getConnectionPort();
    } else if (exposedPorts.size() > 0 && !exposedPorts.contains(connectionPort)) {
      // select the first port of the exposed ports
      connectionPort = exposedPorts.iterator().next();
    }
    exposedPorts.add(connectionPort);

    Set<String> publishedPorts = new HashSet<>(exposedPorts.size());
    for (Integer port : exposedPorts) {
      publishedPorts.add(port.toString());
    }

    return publishedPorts;
  }

  protected Set<Integer> getExposedPortsFromImage(String dockerImage) {

    Set<Integer> ports = new HashSet<>();
    try {
      ImageInfo imageDetails = inspectImage(dockerImage);
      if (imageDetails != null
          && imageDetails.config() != null
          && imageDetails.config().exposedPorts() != null) {
        for (String port : imageDetails.config().exposedPorts()) {
          ports.add(Integer.valueOf(port.split("/")[0]));
        }
        return ports;
      } else {
        log.info(
            "Failed to inspect image: "
                + dockerImage
                + ". Probably does not have any exposed ports.");
        return ports;
      }
    } catch (DockerException | InterruptedException e) {
      log.info("Failed to inspect image: " + dockerImage, e);
      return ports;
    }
  }

  protected String getWorkspaceName(String user) {
    return processDockerName(
        LabConfig.LAB_NAMESPACE + "-" + CoreService.WORKSPACE.getName() + "-" + user);
  }

  protected String getProjectNetworkName(String projectName) {
    return LabConfig.LAB_NAMESPACE + "-" + DOCKER_PROJECT_NETWORK_PREFIX + "-" + projectName;
  }

  protected boolean isServiceHealthy(String serviceName) {
    // could be service name or id
    try {
      return getService(serviceName).getIsHealthy();
    } catch (Exception e) {
      return false;
    }
  }

  protected boolean isServiceAvailable(String serviceName, @Nullable Long maxWaitTime) {
    final long WAIT_INTERVALS = TimeUnit.SECONDS.toMillis(5);
    long waitTime = 0;

    try {
      while (!getService(serviceName).getIsHealthy()) {
        if (maxWaitTime == null) {
          return false;
        }
        Thread.sleep(WAIT_INTERVALS);
        waitTime += WAIT_INTERVALS;
        if (waitTime >= maxWaitTime) {
          log.info(
              "Service "
                  + serviceName
                  + " isn't available after "
                  + TimeUnit.MILLISECONDS.toMinutes(waitTime)
                  + " min wait time.");
          return false;
        }
      }
    } catch (Exception e) {
      log.info("Service " + serviceName + " is not available.", e);
      return false;
    }

    return true;
  }

  /**
   * Checks whether image exists locally and, if not, pulls it. It is Docker-specific code
   *
   * @param dockerImage the image to pull if needed
   * @return imageInfo object from Docker client 'inspectImage' method
   */
  protected ImageInfo inspectImage(String dockerImage)
      throws DockerException, InterruptedException {
    String imageVersion = DockerUtils.extractVersionFromImage(dockerImage);
    ImageInfo imageInfo = null;

    // make this check to prevent always pulling. First check whether image is already pulled, as
    // pulling is an expensive (network) operation
    if (!StringUtils.isNullOrEmpty(imageVersion)
        && (imageVersion.equalsIgnoreCase(
            "latest"))) { //  || imageVersion.toUpperCase().endsWith("SNAPSHOT")
      try {
        log.info("Image uses " + imageVersion + " version. Always pull.");
        log.info("Pulling " + dockerImage);
        client.pull(dockerImage, new DockerUtils.DebugProgressHandler());
        log.info("Finished pulling " + dockerImage);
        imageInfo = client.inspectImage(dockerImage);
      } catch (ImageNotFoundException | DockerRequestException ex) {
        log.info("Image not found, try to use local version");
        imageInfo = client.inspectImage(dockerImage);
      }
    } else {
      try {
        imageInfo = client.inspectImage(dockerImage);
      } catch (ImageNotFoundException ex) {
        log.info("Image not found locally, pulling images from remote.");
        log.info("Pulling " + dockerImage);
        client.pull(dockerImage, new DockerUtils.DebugProgressHandler());
        log.info("Finished pulling " + dockerImage);
        imageInfo = client.inspectImage(dockerImage);
      } catch (DockerException ex) {
        log.info("Error in requesting the docker api");
      }
    }

    return imageInfo;
  }

  public static String processDockerName(String dockerName) {
    return processDockerName(dockerName, DOCKER_NAME_MAX_LENGTH);
  }

  protected static String processDockerName(String dockerName, Integer maxLength) {
    dockerName = StringUtils.simplifyKey(dockerName);
    if (dockerName.length() > maxLength) {
      int HASH_LENGTH = 5;
      // use first 5 chars of name hash if name is shortened
      String hash = StringUtils.shorten(CryptoUtils.md5(dockerName), HASH_LENGTH);
      dockerName = StringUtils.shorten(dockerName, maxLength - (HASH_LENGTH + 1));
      dockerName = dockerName + "-" + hash;
    }

    if (dockerName.endsWith("-")) {
      dockerName = StringUtils.removeLastChar(dockerName);
    }

    return dockerName;
  }

  public static String processServiceName(String serviceName) {
    // feature name is not allowed to be longer than 63 chars (kubernetes restriction)
    // https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/
    serviceName = StringUtils.simplifyKey(serviceName);
    if (serviceName.length() >= SERVICE_NAME_MAX_LENGTH) {
      serviceName = StringUtils.shorten(serviceName, SERVICE_NAME_MAX_LENGTH);
    }

    if (serviceName.startsWith("-")) {
      serviceName = serviceName.replaceFirst("-", "");
    }

    if (serviceName.endsWith("-")) {
      serviceName = StringUtils.removeLastChar(serviceName);
    }

    return serviceName;
  }

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
