package org.mltooling.core.env;

import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.env.handler.FileHandler;
import org.mltooling.core.lab.LabApiClient;
import org.mltooling.core.lab.LabAuthApiClient;
import org.mltooling.core.lab.model.LabUser;
import org.mltooling.core.lab.model.LabFileDataType;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.core.utils.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Environment {

    // ================ Constants =========================================== //
    private static final Logger log = LoggerFactory.getLogger(Environment.class);

    public static final String ENV_NAME_ENV_ROOT_PATH = "DATA_ENVIRONMENT";

    private static final String LOCAL_ENV_FOLDER_NAME = "environment";
    private static final String EXPERIMENTS_FOLDER_NAME = "experiments";

    public static final String ENV_NAME_LAB_ENDPOINT = "LAB_ENDPOINT";
    public static final String ENV_NAME_LAB_API_TOKEN = "LAB_API_TOKEN";
    public static final String ENV_NAME_LAB_PROJECT = "LAB_PROJECT";

    public static final String LOCAL_OPERATOR = "local";
    public static final String LOCAL_PROJECT = "local";

    // ================ Members ============================================= //
    private Path rootFolder;
    private Path projectFolder;
    private Path experimentsFolder;

    private String project;
    private String operator;

    private LabApiClient labApiHandler;
    private FileHandler fileHandler;

    private boolean connected = false;

    // ================ Constructors & Main ================================= //
    public Environment() {
        init();
    }

    public Environment(Path rootFolder) {
        this.rootFolder = rootFolder;
        init();
    }

    public Environment(String project, LabApiClient labApiClient) {
        init(project, labApiClient);
    }

    public Environment(String project, String labEndpoint, @Nullable String labApiToken) {
        init(project, labEndpoint, labApiToken);
    }

    private void init() {

        if (!StringUtils.isNullOrEmpty(SystemUtils.getEnvVar(ENV_NAME_LAB_ENDPOINT)) && !StringUtils.isNullOrEmpty(SystemUtils.getEnvVar(ENV_NAME_LAB_PROJECT))) {
            log.info("Lab endpoint and project were configured via environment variables. Initializing environment with lab API.");
            init(SystemUtils.getEnvVar(ENV_NAME_LAB_PROJECT),
                 SystemUtils.getEnvVar(ENV_NAME_LAB_ENDPOINT),
                 SystemUtils.getEnvVar(ENV_NAME_LAB_API_TOKEN));
        } else {
            init(null, null);
        }
    }

    private void init(String project, String labEndpoint, @Nullable String labApiToken) {
        this.init(project, new LabApiClient(labEndpoint, labApiToken));
    }

    private void init(@Nullable String project, @Nullable LabApiClient labApiClient) {
        this.project = project;

        this.labApiHandler = labApiClient;

        if (labApiClient != null && labApiClient.isAvailable()) {
            if (StringUtils.isNullOrEmpty(this.project)) {
                log.warn("Project ist not provided, but initializing with Lab Instance.");
                // Project will be set later to local project
            }

            try {
                SingleValueFormat<LabUser> labUser = new LabAuthApiClient(labApiClient.getServiceUrl(), labApiClient.getAuthToken()).getMe();
                if (labUser == null
                        || !labUser.isSuccessful()
                        || labUser.getData() == null
                        || StringUtils.isNullOrEmpty(labUser.getData().getId())) {
                    log.warn("Failed to get user information from Lab Instance. Initializing local environment");
                    connected = false;
                    this.operator = LOCAL_OPERATOR;
                } else {
                    connected = true;
                    this.operator = labUser.getData().getId();
                }
            } catch (Exception e) {
                log.warn("Failed to get user information from Lab Instance. Initializing local environment", e);
                connected = false;
                this.operator = LOCAL_OPERATOR;
            }
        }

        if (StringUtils.isNullOrEmpty(this.project)) {
            this.project = LOCAL_PROJECT; // default project is local
        }

        this.fileHandler = new FileHandler(this);
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //

    public void printInfo() {
        if (isConnected()) {
            log.info("Lab Endpoint: " + getLabApiHandler().getServiceUrl());
        } else {
            log.info("Lab Endpoint: Not connected!");
        }

        log.info("Configured Project: " + getProjectName());
        log.info("Configured Operator: " + getOperator());

        log.info("Root folder: " + getRootFolder().toAbsolutePath());
        log.info("Project folder: " + getProjectFolder().toAbsolutePath());
        log.info("Experiments folder: " + getExperimentFolder().toAbsolutePath());

        if (isConnected()) {
            log.info("Available datasets: " + String.valueOf(getFileHandler().listRemoteFiles(LabFileDataType.DATASET)));
            log.info("Available models: " + String.valueOf(getFileHandler().listRemoteFiles(LabFileDataType.MODEL)));
        }
    }

    public boolean isConnected() {
        return this.getLabApiHandler() != null && connected;
    }

    public String getProjectName() {
        return this.project;
    }

    public String getOperator() {
        return this.operator;
    }

    public Path getProjectFolder() {
        if (projectFolder == null) {
            projectFolder = getRootFolder().resolve(this.project);
            projectFolder.toFile().mkdirs();
        }

        return projectFolder;
    }

    public Path getRootFolder() {
        if (rootFolder == null) {
            if (!StringUtils.isNullOrEmpty(SystemUtils.getEnvVar(ENV_NAME_ENV_ROOT_PATH))) {
                rootFolder = Paths.get(SystemUtils.getEnvVar(ENV_NAME_ENV_ROOT_PATH));
            } else {
                rootFolder = Paths.get(LOCAL_ENV_FOLDER_NAME);
            }
            rootFolder.toFile().mkdirs();

        }
        return rootFolder;
    }

    public Path getExperimentFolder() {
        if (experimentsFolder == null) {
            experimentsFolder = getProjectFolder().resolve(EXPERIMENTS_FOLDER_NAME);
            experimentsFolder.toFile().mkdirs();
        }

        return experimentsFolder;
    }

    // Handlers

    public LabApiClient getLabApiHandler() {
        return labApiHandler;
    }

    public FileHandler getFileHandler() {
        return fileHandler;
    }

    // Default file operations, for more operations use file handler

    public File getFile(String key) {
        return fileHandler.getFile(key);
    }

    public File getFile(String fileName, LabFileDataType dataType) {
        return fileHandler.getFile(fileName, dataType);
    }

    public File getFolder(String key) {
        return fileHandler.getFolder(key);
    }

    public String uploadFile(Path filePath, LabFileDataType dataType) {
        return fileHandler.uploadFile(filePath, dataType);
    }

    public String uploadFolder(Path folderPath, LabFileDataType dataType) {
        return fileHandler.uploadFolder(folderPath, dataType);
    }

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    public Environment setRootFolder(Path rootFolder) {
        this.rootFolder = rootFolder;
        return this;
    }

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
