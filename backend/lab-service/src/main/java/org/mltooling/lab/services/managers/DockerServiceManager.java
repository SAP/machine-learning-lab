package org.mltooling.lab.services.managers;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.LogStream;
import com.spotify.docker.client.exceptions.ContainerNotFoundException;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.DockerRequestException;
import com.spotify.docker.client.messages.*;
import com.spotify.docker.client.shaded.com.google.common.collect.ImmutableMap;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.mltooling.core.lab.model.LabJob;
import org.mltooling.core.lab.model.LabService;
import org.mltooling.core.utils.ListUtils;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.core.utils.SystemUtils;
import org.mltooling.lab.LabConfig;
import org.mltooling.lab.components.ProjectManager;
import org.mltooling.lab.services.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerServiceManager extends AbstractServiceManager {

  // ================ Constants =========================================== //
  protected static final String FILTER_LABEL = "label";
  protected static final String FILTER_NAME = "name";

  protected static String SERVICE_ADMIN_BASE_URL = LabConfig.LAB_BASE_URL + "/service-admin";

  protected static String SSL_VOLUME_NAME = "lab-ssl";
  protected static String TOS_VOLUME_NAME = "lab-tos";

  // we create networks in the range of 172.33-255.0.0/24
  // Docker by default uses the range 172.17-32.0.0, so we should be save using that range
  private static final Integer INITIAL_CIDR_FIRST_OCTET = 172;
  private static final Integer INITIAL_CIDR_SECOND_OCTET = 33;
  private static final DockerUtils.Network INITIAL_CIDR =
      new DockerUtils.Network(
          String.format("%d.%d.0.0/24", INITIAL_CIDR_FIRST_OCTET, INITIAL_CIDR_SECOND_OCTET));

  // ================ Members ============================================= //

  Map<String, String> networkNameToId = new HashMap<>();

  // ================ Constructors & Main ================================= //

  public DockerServiceManager() {
    super();
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //
  @Override
  public List<LabService> getServices(@Nullable String project) throws Exception {
    List<LabService> services = new ArrayList<>();

    try {
      List<Container> dockerContainers;

      if (StringUtils.isNullOrEmpty(project)) {
        dockerContainers =
            client.listContainers(
                DockerClient.ListContainersParam.withLabel(
                    LABEL_NAMESPACE, LabConfig.LAB_NAMESPACE));
      } else {
        dockerContainers =
            client.listContainers(
                DockerClient.ListContainersParam.withLabel(
                    LABEL_NAMESPACE, LabConfig.LAB_NAMESPACE),
                DockerClient.ListContainersParam.withLabel(
                    LABEL_FEATURE_TYPE, FeatureType.PROJECT_SERVICE.getName()),
                DockerClient.ListContainersParam.withLabel(LABEL_PROJECT, project));
      }
      for (Container container : dockerContainers) {
        LabService labService = getService(container.id());
        services.add(labService);
      }

    } catch (DockerException e) {
      log.error("Could not get services via Docker API.", e);
    }

    return services;
  }

  /** get service by feature name, docker name, or docker id - filter by project */
  @Override
  public LabService getService(String serviceId, @Nullable String project) throws Exception {
    // check if type == central or project service
    return transformService(getContainer(serviceId, project));
  }

  @Override
  public boolean deleteService(String serviceId, boolean removeVolumes, @Nullable String project)
      throws Exception {
    LabService service = null;

    try {
      service = getService(serviceId, project);
    } catch (Exception ex) {
      log.warn("Failed to find service for deletion.", ex);
      return false;
    }

    if (service == null) {
      return false;
    }

    log.info(
        "Delete service: " + service.getDockerName() + " (remove volumes: " + removeVolumes + ")");

    // delete service and all volumes
    if (removeVolumes) {
      client.removeContainer(
          service.getDockerId(),
          DockerClient.RemoveContainerParam.removeVolumes(true),
          DockerClient.RemoveContainerParam.forceKill());
    } else {
      // do not force kill to prevent database corruption
      // client.removeContainer(service.getDockerId(),
      // DockerClient.RemoveContainerParam.forceKill());
      client.stopContainer(service.getDockerId(), 10);
      client.removeContainer(service.getDockerId());
    }

    return true;
  }

  @Override
  public LabJob getJob(String jobId, @Nullable String project) throws Exception {
    return transformJob(getContainer(jobId, project));
  }

  @Override
  public boolean deleteJob(String jobId, String project) throws Exception {
    LabJob job = null;

    try {
      job = getJob(jobId, project);
    } catch (Exception ex) {
      log.warn("Failed to find job for deletion.", ex);
      return false;
    }

    if (job == null) {
      return false;
    }

    log.info("Delete job: " + job.getDockerName());

    // force kill the job immediately
    client.removeContainer(
        job.getDockerId(),
        DockerClient.RemoveContainerParam.removeVolumes(true),
        DockerClient.RemoveContainerParam.forceKill());

    return true;
  }

  @Override
  public String getServiceLogs(String serviceId) throws Exception {
    return getContainerLogs(serviceId);
  }

  @Override
  public String getJobLogs(String jobId) throws Exception {
    return getContainerLogs(jobId);
  }

  @Override
  public void createProjectResources(String project) throws Exception {
    try {
      String projectNetworkName = getProjectNetworkName(project);

      Map<String, String> networkLabels = new HashMap<>();
      networkLabels.put(LABEL_PROJECT, project);
      createNetwork(projectNetworkName, networkLabels);

      connectToNetwork(getLabService().getDockerId(), projectNetworkName);

    } catch (DockerRequestException e) {
      throw new Exception("Project could not be created", e);
    }

    super.createProjectResources(project);
  }

  @Override
  public boolean deleteProjectResources(String project) throws Exception {
    for (Container containers :
        client.listContainers(
            DockerClient.ListContainersParam.allContainers(),
            DockerClient.ListContainersParam.withLabel(LABEL_NAMESPACE, LabConfig.LAB_NAMESPACE),
            DockerClient.ListContainersParam.withLabel(LABEL_PROJECT, project))) {
      // client.killContainer(containers.id());
      client.removeContainer(
          containers.id(),
          DockerClient.RemoveContainerParam.removeVolumes(true),
          DockerClient.RemoveContainerParam.forceKill());
    }
    Thread.sleep(5000);

    VolumeList volumeList =
        client.listVolumes(
            DockerClient.ListVolumesParam.filter(FILTER_LABEL, LABEL_PROJECT + "=" + project),
            DockerClient.ListVolumesParam.filter(
                FILTER_LABEL, LABEL_NAMESPACE + "=" + LabConfig.LAB_NAMESPACE));

    if (volumeList != null) {
      List<Volume> volumes = volumeList.volumes();
      if (!ListUtils.isNullOrEmpty(volumes)) {
        for (Volume volume : volumes) {
          client.removeVolume(volume);
        }
      }
    }

    Thread.sleep(5000);

    for (Network network :
        client.listNetworks(
            DockerClient.ListNetworksParam.filter(FILTER_LABEL, LABEL_PROJECT + "=" + project),
            DockerClient.ListNetworksParam.filter(
                FILTER_LABEL, LABEL_NAMESPACE + "=" + LabConfig.LAB_NAMESPACE))) {
      ImmutableMap<String, Network.Container> connectedContainers =
          client.inspectNetwork(network.id()).containers();
      if (!ListUtils.isNullOrEmpty(connectedContainers)) {
        for (String containerId : connectedContainers.keySet()) {
          // disconnect all containers from network
          client.disconnectFromNetwork(containerId, network.id());
        }
      }

      // remove project network
      client.removeNetwork(network.id());
    }

    return true;
  }

  @Override
  public List<LabJob> getJobs(String project) throws Exception {
    List<LabJob> projectJobs = new ArrayList<>();

    try {
      List<Container> dockerContainers =
          client.listContainers(
              DockerClient.ListContainersParam.allContainers(),
              DockerClient.ListContainersParam.withLabel(LABEL_NAMESPACE, LabConfig.LAB_NAMESPACE),
              DockerClient.ListContainersParam.withLabel(
                  LABEL_FEATURE_TYPE, FeatureType.PROJECT_JOB.getName()),
              DockerClient.ListContainersParam.withLabel(LABEL_PROJECT, project));

      for (Container container : dockerContainers) {
        projectJobs.add(transformJob(client.inspectContainer(container.id())));
      }

    } catch (DockerException e) {
      log.error("Could not get jobs of project " + project + " via Docker API.", e);
    }

    return projectJobs;
  }

  @Override
  public void installLab() throws Exception {
    try {
      createNetwork(CORE_SERVICES_NETWORK, null);
    } catch (Exception e) {
      log.error("Could not create the lab network", e);
    }

    super.installLab();

    // check if portainer and research-hub are started, if not -> start those
    if (!isServiceHealthy(CoreService.PORTAINER.getName())) {
      log.info(
          CoreService.PORTAINER.getName()
              + " service does not exist. Create service based on "
              + CoreService.PORTAINER.getImage());
      DockerDeploymentConfig portainerConfig =
          createCoreService(CoreService.PORTAINER.getImage(), CoreService.PORTAINER.getName())
              .setVolumePath(DEFAULT_PERSISTENCE_PATH)
              .setCmd("-H unix:///var/run/docker.sock --no-auth")
              .setReplicationAllowed(false)
              .setNodeSelector(DockerDeploymentConfig.NODE_SELECTOR_MASTER)
              .addMount(
                  DockerDeploymentConfig.BIND_MOUNT_TYPE,
                  "/var/run/docker.sock:/var/run/docker.sock");
      deployService(portainerConfig);
    }
  }

  @Override
  protected LabService getLabService() throws Exception {
    String containerId =
        System.getenv("HOSTNAME"); // HOSTNAME is default in docker to get the container id
    return getService(containerId);
  }

  @Override
  public void uninstallLab() throws Exception {
    cleanUpLab(false);
  }

  @Override
  public void updateLab(boolean backendOnly) throws Exception {
    // delete portainer as well?

    super.updateLab(backendOnly);

    // connect lab backend to all existing networks
    LabService backendService = getService(CoreService.LAB_BACKEND.getName(), null);
    for (Network network :
        client.listNetworks(
            DockerClient.ListNetworksParam.filter(
                FILTER_LABEL, LABEL_NAMESPACE + "=" + LabConfig.LAB_NAMESPACE))) {
      if (network.name().equalsIgnoreCase(CORE_SERVICES_NETWORK)) {
        // do not add lab-core network
        continue;
      }
      try {
        this.connectToNetwork(backendService.getDockerId(), network.id());
      } catch (Exception ex) {
        log.warn("Failed to add lab backend to network " + network.name(), ex);
      }
    }
  }

  @Override
  public LabService deployService(DockerDeploymentConfig serviceConfig) throws Exception {
    return getService(deployContainer(serviceConfig));
  }

  @Override
  public LabJob deployJob(DockerDeploymentConfig serviceConfig) throws Exception {
    return transformJob(client.inspectContainer(deployContainer(serviceConfig)));
  }

  @Override
  public DockerDeploymentConfig createLabService() {
    DockerDeploymentConfig deploymentConfig = super.createLabService();
    try {
      if (LabConfig.SERVICE_SSL_ENABLED
          && client
                  .listVolumes(DockerClient.ListVolumesParam.filter(FILTER_NAME, SSL_VOLUME_NAME))
                  .volumes()
                  .size()
              == 1) {
        // TODO only add volume if container exists? Needs better solution!
        // add ssl volume to backend service if ssl is enabled and if the ssl-volume was manually
        // populated with certificates.
        // if the volume does not exist, do not mount it as the :ro part would prevent the ML Lab
        // container to auto-generate certificates.
        String sslResourcesPath = SystemUtils.getEnvVar("_SSL_RESOURCES_PATH", "/resources/ssl");
        deploymentConfig.addMount(
            DockerDeploymentConfig.BIND_MOUNT_TYPE,
            SSL_VOLUME_NAME + ":" + sslResourcesPath + ":ro");
        // TODO: add SSL support for Swarm
      }
    } catch (DockerException | InterruptedException e) {
      log.error(String.format("Checking the ssl volume %s failed", SSL_VOLUME_NAME));
    }

    // Check if terms of services volume exists
    try {
      if (client
              .listVolumes(DockerClient.ListVolumesParam.filter(FILTER_NAME, TOS_VOLUME_NAME))
              .volumes()
              .size()
          == 1) {
        // Only mount TOS if volume exists
        deploymentConfig.addMount(
            DockerDeploymentConfig.BIND_MOUNT_TYPE,
            TOS_VOLUME_NAME + ":" + LabConfig.TERMS_OF_SERVICE_FOLDER_PATH + ":ro");
      }
    } catch (DockerException | InterruptedException e) {
      log.error(String.format("Checking the tos volume %s failed", TOS_VOLUME_NAME));
    }

    return deploymentConfig;
  }

  @Override
  public DockerDeploymentConfig createWorkspaceService(String user) throws Exception {
    DockerDeploymentConfig workspaceDeployment = super.createWorkspaceService(user);

    if (!StringUtils.isNullOrEmpty(LabConfig.HOST_ROOT_WORKSPACE_DATA_MOUNT_PATH)) {
      // if workspace data mount is configured, use it
      Path hostPath =
          new File(LabConfig.HOST_ROOT_WORKSPACE_DATA_MOUNT_PATH)
              .toPath()
              .resolve(workspaceDeployment.getName());

      // set mount path
      workspaceDeployment.addMount(
          DockerDeploymentConfig.BIND_MOUNT_TYPE,
          hostPath.toString() + ":" + workspaceDeployment.getVolumePath());
      workspaceDeployment.setVolumePath(null); // do not use volume path
    }

    return workspaceDeployment;
  }

  @Override
  public List<String> shutdownDiskExceedingContainers(boolean dryRun) throws Exception {

    List<String> containerNamesToRemove = new ArrayList<>();
    List<Container> containers =
        client.listContainers(
            DockerClient.ListContainersParam.withLabel(LABEL_NAMESPACE, LabConfig.LAB_NAMESPACE),
            DockerClient.ListContainersParam.withLabel(
                LABEL_FEATURE_TYPE, FeatureType.WORKSPACE.getName()),
            DockerClient.ListContainersParam.withContainerSizes(true));
    client.listContainers();
    for (Container container : containers) {
      // ignore all core services
      // TODO: following check is only needed when the functionality should be extended to all
      // containers, not only workspaces.
      // But then, re-creation logic has to be finished
      if (container
          .labels()
          .getOrDefault(LABEL_FEATURE_TYPE, FeatureType.CORE_SERVICE.getName())
          .equalsIgnoreCase(FeatureType.CORE_SERVICE.getName())) {
        continue;
      }

      if (container.sizeRw() != null) {
        final int containerSizeInGb = (int) (container.sizeRw() / 1000 / 1000 / 1000);
        if (LabConfig.MAX_CONTAINER_SIZE != MAX_CONTAINER_SIZE_DISABLED
            && containerSizeInGb > LabConfig.MAX_CONTAINER_SIZE) {

          // The container name returned by the client is preceded with a "/", e.g. /lab-backend
          String containerName = container.names().get(0).replace("/", "");
          containerNamesToRemove.add(containerName);

          if (!dryRun) {
            // Remove the container. Workspace containers are automatically re-created when user
            // tries to use it.

            // ContainerInfo containerInfo = client.inspectContainer(container.id());

            client.removeContainer(container.id(), DockerClient.RemoveContainerParam.forceKill());

            // TODO: if re-created in this way, somehow the hostConfig settings (e.g. cpu limi etc.)
            // are not considered. Some other way to perserve the settings? Probably call the
            // respective Lab endpoints instead.
            // ContainerCreation containerCreation = client.createContainer(containerInfo.config(),
            // containerName);
            // String newContainerId = containerCreation.id();
            // client.startContainer(newContainerId);

            // // Container is automatically connected to the "bridge" network upon creation
            // client.disconnectFromNetwork(newContainerId, "bridge");

            // // Connect the re-created container to the old networks
            // ImmutableMap<String, AttachedNetwork> networks =
            // containerInfo.networkSettings().networks();
            // for (String networkName : networks.keySet()) {
            //     AttachedNetwork network = networks.get(networkName);
            //     client.connectToNetwork(newContainerId, network.networkId());
            // }
          }
        }
      }
    }

    log.info("Remove containers due to disk exceeding: " + containerNamesToRemove.toString());

    return containerNamesToRemove;
  }

  // ================ Public Methods ====================================== //

  public static void cleanUpLab(boolean keepVolumes) throws Exception {
    Logger log = LoggerFactory.getLogger(DockerServiceManager.class);

    log.info("Cleanup of local lab landscape with namespace " + LabConfig.LAB_NAMESPACE);
    DockerClient client = DefaultDockerClient.fromEnv().build();
    // try {
    //     for (Service service :
    // client.listServices(Service.Criteria.builder().addLabel(LABEL_NAMESPACE,
    // LabConfig.LAB_NAMESPACE).build())) {
    //         try {
    //             log.info("Remove container " + service.id());
    //             client.removeService(service.id());
    //             for (Container container :
    // client.listContainers(DockerClient.ListContainersParam.allContainers(),
    // DockerClient.ListContainersParam.withLabel("com.docker.swarm.service.id", service.id()))) {
    //                 try {
    //                     log.info("Remove container " + container.id() + " - " +
    // container.image());
    //                     if (keepVolumes) {
    //                         client.removeContainer(container.id(),
    // DockerClient.RemoveContainerParam.removeVolumes(false),
    // DockerClient.RemoveContainerParam.forceKill());
    //                     } else {
    //                         client.removeContainer(container.id(),
    // DockerClient.RemoveContainerParam.removeVolumes(true),
    // DockerClient.RemoveContainerParam.forceKill());
    //                     }
    //                 } catch (Exception e) {
    //                     log.warn("Failed to remove container.", e);
    //                 }
    //             }
    //         } catch (Exception e) {
    //             log.warn("Failed to remove service.", e);
    //         }
    //     }
    //     log.info("Removed all services");
    // } catch (Exception e) {
    //     log.warn("Failed to remove services. Docker Swarm might not be initialized.", e);
    // }

    Thread.sleep(10000);
    for (Container container :
        client.listContainers(
            DockerClient.ListContainersParam.allContainers(),
            DockerClient.ListContainersParam.withLabel(LABEL_NAMESPACE, LabConfig.LAB_NAMESPACE))) {
      try {
        log.info("Remove container " + container.id() + " - " + container.image());
        if (keepVolumes) {
          client.removeContainer(
              container.id(),
              DockerClient.RemoveContainerParam.removeVolumes(false),
              DockerClient.RemoveContainerParam.forceKill());
        } else {
          client.removeContainer(
              container.id(),
              DockerClient.RemoveContainerParam.removeVolumes(true),
              DockerClient.RemoveContainerParam.forceKill());
        }
      } catch (Exception e) {
        log.warn("Failed to remove container.", e);
      }
    }

    log.info("Removed all containers");

    Thread.sleep(10000);
    for (Network network :
        client.listNetworks(
            DockerClient.ListNetworksParam.withLabel(LABEL_NAMESPACE, LabConfig.LAB_NAMESPACE))) {
      try {
        log.info("Remove network " + network.name());
        client.removeNetwork(network.id());
      } catch (Exception e) {
        log.warn("Failed to remove network.", e);
      }
    }
    log.info("Removed all networks");

    if (!keepVolumes) {
      VolumeList volumeList =
          client.listVolumes(
              DockerClient.ListVolumesParam.filter(
                  FILTER_LABEL, LABEL_NAMESPACE + "=" + LabConfig.LAB_NAMESPACE));
      if (volumeList.volumes() != null) {
        //noinspection ConstantConditions
        for (Volume volume : volumeList.volumes()) {
          try {
            log.info("Remove volume " + volume.name());
            client.removeVolume(volume);
          } catch (Exception e) {
            log.warn("Failed to remove volume.", e);
          }
        }
      }
      log.info("Removed all volumes");
    }

    Thread.sleep(10000);
  }

  // ================ Private Methods ===================================== //

  /**
   * check container's health status by Leveraging Docker's native Health information (either health
   * or normal state).
   *
   * @param container for which the health should be checked
   */
  private Boolean isHealthy(ContainerInfo container) {
    DockerUtils.ContainerState containerState = getContainerStatus(container);
    return containerState == DockerUtils.ContainerState.RUNNING
        || containerState == DockerUtils.ContainerState.HEALTHY;
  }

  private DockerUtils.ContainerState getContainerStatus(ContainerInfo container) {

    if (container.state() == null) {
      return DockerUtils.ContainerState.UNKNOWN;
    }

    if (container.state().health() != null) {
      // Use healthcheck flag only if implemented
      // the status for containers is one of (starting, healthy, unhealthy, none). Exited containers
      // also have the status 'unhealthy'.
      String status = container.state().health().status();
      return DockerUtils.ContainerState.from(status);
    }

    String status = container.state().status();
    return DockerUtils.ContainerState.from(status);
  }

  protected void connectToNetwork(String dockerId, String networkName)
      throws DockerException, InterruptedException {
    client.connectToNetwork(dockerId, networkName);
  }

  protected String deployContainer(DockerDeploymentConfig serviceConfig) throws Exception {
    ContainerConfig.Builder containerConfig =
        ContainerConfig.builder()
            .image(serviceConfig.getImage())
            .env(DockerUtils.convertEnvMapToList(serviceConfig.getEnvVariables()))
            .labels(serviceConfig.getLabels());

    HostConfig.Builder hostConfig = HostConfig.builder();

    // restart always for all services, but no restart for jobs
    if (serviceConfig.getFeatureType() != FeatureType.PROJECT_JOB) {
      hostConfig.restartPolicy(HostConfig.RestartPolicy.always());
    }

    // publish ports
    if (!ListUtils.isNullOrEmpty(serviceConfig.getPortsToPublish())) {
      final Map<String, List<PortBinding>> portBindings = new HashMap<>();
      Set<String> exposedPorts = new HashSet<>();
      for (String port : serviceConfig.getPortsToPublish()) {
        // First, publish all ports that are explicitly marked
        if (port.contains(":")) {
          // if publish to port is explicitly given, -> expose this port
          String publishedPort = port.split(":")[0];
          String targetPort = port.split(":")[1];

          List<PortBinding> publishedPorts = new ArrayList<>();
          publishedPorts.add(PortBinding.of("0.0.0.0", publishedPort));
          portBindings.put(targetPort, publishedPorts);
          exposedPorts.add(targetPort);
        }
      }

      if (LabConfig.IS_DEBUG) {
        // in debug mode, publish all ports with random ports
        for (String port : serviceConfig.getPortsToPublish()) {
          if (!port.contains(":") && !exposedPorts.contains(port)) {
            // only if it is not already published
            List<PortBinding> randomPort = new ArrayList<>();
            randomPort.add(PortBinding.randomPort("0.0.0.0"));
            portBindings.put(port, randomPort);
            exposedPorts.add(port);
          }
        }
      }

      hostConfig.portBindings(portBindings);
      containerConfig.exposedPorts(exposedPorts);
    }

    /*
    if (LabConfig.IS_DEBUG) {
        // in debug mode, publish all ports that are marked as EXPOSE
        hostConfig.publishAllPorts(true);
    }*/

    // add volume (creates a named volume, not a host-bind volume)
    if (!StringUtils.isNullOrEmpty(serviceConfig.getVolumePath())) {

      if (!StringUtils.isNullOrEmpty(LabConfig.HOST_ROOT_DATA_MOUNT_PATH)) {
        // created volume on mount path
        Path hostPath =
            new File(LabConfig.HOST_ROOT_DATA_MOUNT_PATH).toPath().resolve(serviceConfig.getName());

        hostConfig.appendBinds(
            HostConfig.Bind.from(hostPath.toString())
                .to(serviceConfig.getVolumePath())
                .readOnly(false)
                .build());
      } else {
        // created named volume
        Volume containerVolume =
            Volume.builder()
                .name(serviceConfig.getName())
                .driver("local")
                .labels(serviceConfig.getLabels())
                .build();

        hostConfig.appendBinds(
            HostConfig.Bind.from(client.createVolume(containerVolume))
                .to(serviceConfig.getVolumePath())
                .readOnly(false)
                .build());
      }
    }

    // add binds
    if (!ListUtils.isNullOrEmpty(
        serviceConfig.getMounts().get(DockerDeploymentConfig.BIND_MOUNT_TYPE))) {
      hostConfig.appendBinds(serviceConfig.getMounts().get(DockerDeploymentConfig.BIND_MOUNT_TYPE));
    }

    // add resource limits
    try {
      // cpus to nano cpus
      // https://stackoverflow.com/questions/52398136/how-to-verify-cpu-limit-assigned-to-a-docker-container
      // https://github.com/moby/moby/issues/24713
      Integer cpus = Integer.valueOf(LabConfig.SERVICES_CPU_LIMIT);
      // only use a maximum of available cpus
      cpus = Math.min(cpus, Runtime.getRuntime().availableProcessors());
      Double cpuLimit = cpus * 1e9;
      hostConfig.nanoCpus(cpuLimit.longValue());
    } catch (NumberFormatException | NullPointerException ex) {
      // do nothing
    }

    try {
      Double memoryLimit = Double.valueOf(LabConfig.SERVICES_MEMORY_LIMIT) * 1e+9; // to bytes
      hostConfig.memory(memoryLimit.longValue());
    } catch (NumberFormatException | NullPointerException ex) {
      // do nothing
    }

    containerConfig.hostConfig(hostConfig.build());

    // add commands
    if (!ListUtils.isNullOrEmpty(serviceConfig.getCmd())) {
      containerConfig.cmd(serviceConfig.getCmd());
    }

    ContainerConfig finalizedConfig = containerConfig.build();

    // check image
    super.inspectImage(finalizedConfig.image());

    ContainerCreation creation = client.createContainer(finalizedConfig, serviceConfig.getName());
    client.startContainer(creation.id());

    if (!ListUtils.isNullOrEmpty(serviceConfig.getNetworks())) {
      try {
        // Remove from bridge network
        client.disconnectFromNetwork(creation.id(), "bridge", true);
      } catch (DockerException e) {
        // this should be fine in most cases
        log.warn(
            "Failed to disconnect container " + creation.id() + " from bridge network",
            e.getMessage());
      }
    }

    // add network
    for (String network : serviceConfig.getNetworks()) {
      try {
        // Check if network exist
        client.inspectNetwork(network);
      } catch (DockerException e) {
        // Create network if it does not exist
        // add project label if contains has one
        Map<String, String> networkLabels = new HashMap<>();
        if (serviceConfig.getLabels() != null
            && !StringUtils.isNullOrEmpty(serviceConfig.getLabels().get(LABEL_PROJECT))) {
          networkLabels.put(LABEL_PROJECT, serviceConfig.getLabels().get(LABEL_PROJECT));
        }

        createNetwork(network, networkLabels);
      }

      try {
        // connect lab service to network: all services have access to lab
        client.connectToNetwork(getLabService().getDockerId(), network);
      } catch (Exception e) {
        // Catch all exceptions here: this should be fine in most cases
        // For minio / mongo -> Lab Backend does not exist yet and cannot be found
        // getLabService just throws exception if service could not be found
        log.debug("Could not connect Lab backend to network: " + network, e);
      }

      try {
        client.connectToNetwork(creation.id(), network);
      } catch (DockerException | InterruptedException e) {
        log.warn(
            "Could not connect docker container " + creation.id() + " to network " + network, e);
      }
    }

    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
    return creation.id();
  }

  /**
   * Create Docker networks by determining whether a new subnet has to be used. Otherwise, the
   * default Docker subnet would be used and, as a result, the amount of networks that can be
   * created is strongly limited. See:
   * https://stackoverflow.com/questions/41609998/how-to-increase-maximum-docker-network-on-one-server
   * ; https://loomchild.net/2016/09/04/docker-can-create-only-31-networks-on-a-single-machine/
   *
   * @param name The name of the network to be created
   * @param networkLabels
   */
  protected void createNetwork(String name, @Nullable Map<String, String> networkLabels)
      throws Exception {
    List<Network> networks =
        client
            .listNetworks(); // client.listNetworks(DockerClient.ListNetworksParam.byNetworkName(name))
    DockerUtils.Network highestCidr = INITIAL_CIDR;
    for (Network network : networks) {
      if (network.name().equalsIgnoreCase(name)) {
        log.info("Network " + name + " already exist.");
        return;
      }

      // determine subnet for the network to be created
      if (network.ipam() != null
          && network.ipam().config() != null
          && !network.ipam().config().isEmpty()
          && !StringUtils.isNullOrEmpty(network.ipam().config().get(0).subnet())) {
        DockerUtils.Network cidr =
            new DockerUtils.Network(network.ipam().config().get(0).subnet()); // e.g.: 172.33.0.1/24
        if (cidr.getOctetsAsIntegers()[0] == INITIAL_CIDR_FIRST_OCTET
            && cidr.getOctetsAsIntegers()[1] >= INITIAL_CIDR_SECOND_OCTET) {
          highestCidr = DockerUtils.Network.getHigherCidr(cidr, highestCidr);
        }
      }
    }

    DockerUtils.Network nextCidr = DockerUtils.Network.nextSubnet(highestCidr);
    if (nextCidr.getOctetsAsIntegers()[0] > INITIAL_CIDR_FIRST_OCTET) {
      log.error("Reached network capacity");
      throw new Exception("No more networks available");
    }

    if (networkLabels == null) {
      networkLabels = new HashMap<>();
    }
    networkLabels.put(LABEL_NAMESPACE, LabConfig.LAB_NAMESPACE);

    try {
      NetworkCreation networkCreation =
          client.createNetwork(
              NetworkConfig.builder()
                  .labels(networkLabels)
                  .name(name)
                  .ipam(
                      Ipam.create(
                          "default",
                          Collections.singletonList(
                              IpamConfig.create(nextCidr.toString(), null, nextCidr.getGateway()))))
                  .build());
      networkNameToId.put(name, networkCreation.id());
    } catch (DockerRequestException e) {
      log.error("Could not create network", e.getMessage());
      throw e;
    }
  }

  protected ContainerInfo getContainer(String containerId, @Nullable String project)
      throws Exception {
    ContainerInfo containerInfo = null;
    try {
      // docker name or docker id
      containerInfo = client.inspectContainer(containerId);

      Map<String, String> containerLabels = containerInfo.config().labels();
      String namespace = containerLabels.get(LABEL_NAMESPACE);
      if (StringUtils.isNullOrEmpty(namespace)
          || !namespace.equalsIgnoreCase(LabConfig.LAB_NAMESPACE)) {
        containerInfo = null; // set to null because the one that was found is not in namespace
        throw new ContainerNotFoundException(
            "Container with name "
                + containerId
                + " found but not in the correct namespace "
                + LabConfig.LAB_NAMESPACE);
      }

    } catch (ContainerNotFoundException ex) {
      List<Container> dockerContainers = new ArrayList<>();

      if (!StringUtils.isNullOrEmpty(project)) {
        // also filter based on project
        dockerContainers =
            client.listContainers(
                DockerClient.ListContainersParam.withLabel(
                    LABEL_NAMESPACE, LabConfig.LAB_NAMESPACE),
                DockerClient.ListContainersParam.withLabel(LABEL_FEATURE_NAME, containerId),
                DockerClient.ListContainersParam.withLabel(
                    LABEL_PROJECT, ProjectManager.processNameToId(project)));
      } else {
        dockerContainers =
            client.listContainers(
                DockerClient.ListContainersParam.withLabel(
                    LABEL_NAMESPACE, LabConfig.LAB_NAMESPACE),
                DockerClient.ListContainersParam.withLabel(LABEL_FEATURE_NAME, containerId));
      }

      if (dockerContainers.size() == 1) {
        containerInfo = client.inspectContainer(dockerContainers.get(0).id());
      } else if (dockerContainers.size() == 0) {
        log.info("No container found for " + containerId);
      } else {
        log.warn("More than one container found for " + containerId);
      }
    }

    if (containerInfo == null) {
      throw new Exception("Failed to find container " + containerId);
    }

    if (!StringUtils.isNullOrEmpty(project)) {
      String serviceProject = containerInfo.config().labels().get(LABEL_PROJECT);
      if (StringUtils.isNullOrEmpty(serviceProject)) {
        throw new Exception(
            "Container was found for " + containerId + " but does not has a project attached.");
      }
      if (!serviceProject.equalsIgnoreCase(project)) {
        throw new Exception(
            "Container was found for "
                + containerId
                + " but is part of different project: "
                + serviceProject);
      }
    }
    return containerInfo;
  }

  protected String getContainerLogs(String containerId) throws Exception {
    final String logs;
    try (LogStream stream =
        client.logs(
            containerId, DockerClient.LogsParam.stdout(), DockerClient.LogsParam.stderr())) {
      logs = stream.readFully();
    }
    return logs;
  }

  private LabJob transformJob(ContainerInfo container) {

    String containerImage = container.config().image();
    String containerName = container.name().replace("/", "");

    Map<String, String> containerLabels = container.config().labels();
    String featureName = containerLabels.getOrDefault(LABEL_FEATURE_NAME, containerName);
    String featureType = containerLabels.get(LABEL_FEATURE_TYPE);

    Map<String, String> envVariables = DockerUtils.convertEnvListToMap(container.config().env());
    Date startedAt = container.state().startedAt();
    if (startedAt == null) {
      startedAt = container.created();
    }

    Date finishedAt = null;
    Integer exitCode = null;

    LabJob.State state = LabJob.State.RUNNING;
    if (!container.state().running()) {
      if (container.state().exitCode() == 0) {
        state = LabJob.State.SUCCEEDED;
      } else {
        state = LabJob.State.FAILED;
      }

      finishedAt = container.state().finishedAt();
      exitCode = container.state().exitCode().intValue();
    }

    return new LabJob()
        .setName(featureName)
        .setDockerName(containerName)
        .setDockerId(container.id())
        .setDockerImage(containerImage)
        .setAdminLink(SERVICE_ADMIN_BASE_URL + "/#/containers/" + container.id())
        .setConfiguration(envVariables)
        .setStartedAt(startedAt)
        .setFinishedAt(finishedAt)
        .setExitCode(exitCode)
        .setStatus(state)
        .setFeatureType(featureType);
  }

  private LabService transformService(ContainerInfo container) {
    String containerImage = container.config().image();
    String containerName = container.name().replace("/", "");

    Map<String, String> containerLabels = container.config().labels();
    String featureName = containerLabels.getOrDefault(LABEL_FEATURE_NAME, containerName);
    String featureType = containerLabels.get(LABEL_FEATURE_TYPE);

    Set<Integer> exposedPorts = new HashSet<>();

    if (container.config().exposedPorts() != null) {
      for (String port : container.config().exposedPorts()) {
        // get all exposed ports directly from inspected container
        exposedPorts.add(Integer.valueOf(port.split("/")[0]));
      }
    }

    Integer connectionPort = DEFAULT_CONNECTION_PORT;
    CoreService coreService = CoreService.from(containerImage);
    if (!coreService.isUnknown()) {
      // use connection port from core service
      connectionPort = coreService.getConnectionPort();
    } else if (exposedPorts.size() > 0 && !exposedPorts.contains(connectionPort)) {
      // select the first port of the exposed ports
      connectionPort = exposedPorts.iterator().next();
    }
    exposedPorts.add(connectionPort);

    Map<String, String> envVariables = DockerUtils.convertEnvListToMap(container.config().env());

    boolean healthy = isHealthy(container);
    /* TODO Do not check port since this will not work during install
       if (healthy && connectionPort != null) {
        // if healthy, also check if main port is accessible
        healthy = ServiceUtils.serverListening(containerName, connectionPort);
    }
     */

    LabService mlService =
        new LabService()
            .setName(featureName)
            .setDockerName(containerName)
            .setDockerId(container.id())
            .setDockerImage(containerImage)
            .setConnectionPort(connectionPort)
            .setExposedPorts(exposedPorts)
            .setConfiguration(envVariables)
            .setLabels(containerLabels)
            .setStartedAt(container.created())
            .setStatus(getContainerStatus(container).getName())
            .setAdminLink(SERVICE_ADMIN_BASE_URL + "/#/containers/" + container.id())
            .setModifiedAt(container.created())
            .setFeatureType(featureType)
            .setIsHealthy(healthy);

    return mlService;
  }

  @Deprecated
  private Integer getPublishedPort(
      Integer internalPort, Map<String, List<PortBinding>> portMappings) {
    if (internalPort == null || ListUtils.isNullOrEmpty(portMappings)) {
      log.warn("internal port or port mapping is null");
      // TODO return 0??
      return 0;
    }

    for (String innerPortStr : portMappings.keySet()) {
      if (StringUtils.isNullOrEmpty(innerPortStr)) {
        log.warn("Port mapping is null or empty");
        continue;
      }
      Integer innerPort;
      try {
        if (innerPortStr.contains("/")) {
          innerPort = Integer.valueOf(innerPortStr.split("/")[0]);
        } else {
          innerPort = Integer.valueOf(innerPortStr);
        }
      } catch (NumberFormatException e) {
        log.warn("Cannot process port " + innerPortStr, e);
        continue;
      }

      if (!internalPort.equals(innerPort)) {
        continue;
      }

      List<PortBinding> portBindings = portMappings.get(innerPortStr);
      if (portBindings.size() == 0) {
        log.warn("No port bindings found for " + internalPort);
        continue;
      }

      if (portBindings.size() > 1) {
        log.warn(portBindings.size() + " port bindings found for " + internalPort);
      }

      PortBinding portBinding = portBindings.get(0);
      if (StringUtils.isNullOrEmpty(portBinding.hostPort())) {
        log.warn("Host port is empty");
      }

      return Integer.valueOf(portBinding.hostPort());
    }

    // TODO return 0??
    return 0;
  }

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
