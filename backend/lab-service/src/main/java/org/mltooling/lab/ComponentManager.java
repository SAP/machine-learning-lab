package org.mltooling.lab;

import org.mltooling.core.lab.model.LabService;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.lab.authorization.AuthorizationManager;
import org.mltooling.lab.components.*;
import org.mltooling.lab.services.AbstractServiceManager;
import org.mltooling.lab.services.CoreService;
import org.mltooling.lab.services.managers.DockerServiceManager;
import org.mltooling.lab.services.managers.KubernetesServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ComponentManager {
  INSTANCE;

  // ================ Constants =========================================== //
  protected static final Logger log = LoggerFactory.getLogger(ComponentManager.class);

  // ================ Members ============================================= //
  private FileStorageManager fileStorageManager;
  private ExperimentsManager experimentsManager;
  private MongoDbManager mongoDbManager;
  private JobManager jobManager;
  private AbstractServiceManager serviceManager;
  private ProjectManager projectManager;
  private AuthorizationManager authManager;
  private EventLogManager eventLogManager;
  private StatsCacheManager statsCacheManager;

  // ================ Constructors & Main ================================= //

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  /**
   * Get a singleton instance of the respective ServiceManager, determined by {@link
   * LabConfig#SERVICES_RUNTIME}. The <a
   * href="https://github.com/spotify/docker-client/blob/master/docs/user_manual.md#connection-pooling">
   * official documentation </a> states that a DockerClient can have multiple concurrent requests,
   * hence it should be threadsafe. Creating a Docker client on each request is expensive.
   *
   * @return the singleton instance of the service manager
   */
  public AbstractServiceManager getServiceManger() {
    if (serviceManager == null) {
      if (isLocalRuntime()) {
        serviceManager = new DockerServiceManager();
      } else if (isKubernetesRuntime()) {
        // log.error(mode + " not implemented!");
        // serviceManager = new DockerServiceManager();
        serviceManager = new KubernetesServiceManager();
      } else {
        // DEFAULT
        serviceManager = new DockerServiceManager();
      }
    }

    return serviceManager;
  }

  public FileStorageManager getFileManager() {
    if (fileStorageManager == null) {
      try {
        String s3Endpoint;
        String s3AccessKey;
        String s3SecretKey;
        boolean s3Secured;

        if (LabConfig.ENV_S3_ENDPOINT != null
            && LabConfig.ENV_S3_ACCESS_KEY != null
            && LabConfig.ENV_S3_SECRET_KEY != null) {
          s3Endpoint = LabConfig.ENV_S3_ENDPOINT;
          s3AccessKey = LabConfig.ENV_S3_ACCESS_KEY;
          s3SecretKey = LabConfig.ENV_S3_SECRET_KEY;
          s3Secured =
              (LabConfig.ENV_S3_SECURED == null)
                  ? false
                  : Boolean.valueOf(LabConfig.ENV_S3_SECURED);
        } else {
          LabService minioService = getServiceManger().getService(CoreService.MINIO.getName());
          s3Endpoint =
              "http://" + minioService.getDockerName() + ":" + minioService.getConnectionPort();
          s3AccessKey = AbstractServiceManager.DEFAULT_SERVICE_USER;
          s3SecretKey = AbstractServiceManager.DEFAULT_SERVICE_PASSWORD;
          s3Secured = false;
        }

        this.fileStorageManager =
            new FileStorageManager(s3Endpoint, s3AccessKey, s3SecretKey, s3Secured);
      } catch (Exception e) {
        log.error("Failed to initialize file handler.", e);
      }
    }

    return fileStorageManager;
  }

  public ExperimentsManager getExperimentsManager() {
    if (experimentsManager == null) {
      try {
        experimentsManager = new ExperimentsManager(getMongoManager());
      } catch (Exception e) {
        log.error("Failed to initialize experiments handler.", e);
      }
    }

    return experimentsManager;
  }

  public MongoDbManager getMongoManager() {
    if (mongoDbManager == null) {
      try {
        LabService mongoService = getServiceManger().getService(CoreService.MONGO.getName());
        mongoDbManager =
            new MongoDbManager(
                mongoService.getDockerName(),
                mongoService.getConnectionPort(),
                false,
                AbstractServiceManager.DEFAULT_SERVICE_USER,
                AbstractServiceManager.DEFAULT_SERVICE_PASSWORD);
      } catch (Exception e) {
        log.error("Failed to initialize mongo handler.", e);
      }
    }

    return mongoDbManager;
  }

  public ComponentManager setMongoDbManager(MongoDbManager mongoDbManager) {
    this.mongoDbManager = mongoDbManager;
    return this;
  }

  public JobManager getJobManager() {
    if (jobManager == null) {
      try {
        jobManager = new JobManager(getMongoManager());
      } catch (Exception e) {
        log.error("Failed to initialize jobs handler.", e);
      }
    }

    return jobManager;
  }

  public AuthorizationManager getAuthManager() {
    if (authManager == null) {
      try {
        authManager = new AuthorizationManager(getMongoManager());
      } catch (Exception e) {
        log.error("Failed to initialize authorization manager.", e);
      }
    }

    return authManager;
  }

  public ProjectManager getProjectManager() {
    if (projectManager == null) {
      try {
        projectManager = new ProjectManager(getMongoManager(), getServiceManger());
      } catch (Exception e) {
        log.error("Failed to initialize project manager.", e);
      }
    }

    return projectManager;
  }

  public EventLogManager getEventLogManager() {
    if (eventLogManager == null) {
      try {
        eventLogManager = new EventLogManager(getMongoManager());
      } catch (Exception e) {
        log.error("Failed to initialize project manager.", e);
      }
    }

    return eventLogManager;
  }

  public StatsCacheManager getStatsManager() {
    if (statsCacheManager == null) {
      try {
        statsCacheManager = new StatsCacheManager(this);
      } catch (Exception e) {
        log.error("Failed to initialize statistics manager.", e);
      }
    }

    return statsCacheManager;
  }

  public boolean isLocalRuntime() {
    final String mode = LabConfig.SERVICES_RUNTIME;
    return StringUtils.isNullOrEmpty(mode)
        || mode.equalsIgnoreCase("local")
        || mode.equalsIgnoreCase("docker");
  }

  public boolean isKubernetesRuntime() {
    final String mode = LabConfig.SERVICES_RUNTIME;
    return mode.equalsIgnoreCase("k8s") || mode.equalsIgnoreCase("kubernetes");
  }

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
