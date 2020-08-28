package org.mltooling.lab;

import org.mltooling.core.utils.StringUtils;
import org.mltooling.core.utils.SystemUtils;
import org.mltooling.lab.authorization.AuthorizationManager;

import java.util.HashMap;
import java.util.Map;


public class LabConfig {

    // ================ Constants =========================================== //
    //
    private static final String ENV_NAME_LAB_PORT = "LAB_PORT";
    public static final String LAB_PORT = SystemUtils.getEnvVar(ENV_NAME_LAB_PORT, "");

    private static final String ENV_NAME_LAB_BASE_URL = "LAB_BASE_URL";
    public static final String LAB_BASE_URL = SystemUtils.getEnvVar(ENV_NAME_LAB_BASE_URL, "");

    // activate debug mode
    private static final String ENV_NAME_LAB_DEBUG = "LAB_DEBUG";
    public static final boolean IS_DEBUG = SystemUtils.getEnvVar(ENV_NAME_LAB_DEBUG, "false").equalsIgnoreCase("true");

    // The build version is automatically provided -> determines what images of core services will be used
    private static final String ENV_NAME_SERVICE_VERSION = "SERVICE_VERSION";
    public static final String SERVICE_VERSION = SystemUtils.getEnvVar(ENV_NAME_SERVICE_VERSION, "latest");

    // determines services runtime -> swarm, kubernetes, local
    public static final String ENV_NAME_SERVICES_RUNTIME = "SERVICES_RUNTIME";
    public static final String SERVICES_RUNTIME = SystemUtils.getEnvVar(ENV_NAME_SERVICES_RUNTIME, "local");

    // Namespace for all computation resources created from Lab (not the same as the Kubernetes namespace)
    public static final String ENV_NAME_LAB_NAMESPACE = "LAB_NAMESPACE";
    public static final String LAB_NAMESPACE = SystemUtils.getEnvVar(ENV_NAME_LAB_NAMESPACE, "lab");

    // always get permissions from mongo instead using the ones from the jwt token
    private static final String ENV_NAME_ALWAYS_CHECK_PERMISSIONS = "ALWAYS_CHECK_PERMISSIONS";
    public static final boolean ALWAYS_CHECK_PERMISSIONS = SystemUtils.getEnvVar(ENV_NAME_ALWAYS_CHECK_PERMISSIONS, "true").equalsIgnoreCase("true");

    // Allow self registrations via register dialog -> otherwise only admin can create Users
    private static final String ENV_NAME_ALLOW_SELF_REGISTRATIONS = "ALLOW_SELF_REGISTRATIONS";
    public static final boolean ALLOW_SELF_REGISTRATIONS = SystemUtils.getEnvVar(ENV_NAME_ALLOW_SELF_REGISTRATIONS, "true").equalsIgnoreCase("true");

    // Lab Action
    public static final String ENV_NAME_LAB_ACTION = "LAB_ACTION";
    public static final LabAction LAB_ACTION = LabAction.from(SystemUtils.getEnvVar(ENV_NAME_LAB_ACTION));

    // Workspace Backup
    public static final String ENV_NAME_WORKSPACE_BACKUP = "WORKSPACE_BACKUP";
    public static final String WORKSPACE_BACKUP = SystemUtils.getEnvVar(ENV_NAME_WORKSPACE_BACKUP, "false");

    // Workspace Image
    public static final String ENV_NAME_WORKSPACE_IMAGE = "WORKSPACE_IMAGE";
    public static final String WORKSPACE_IMAGE = SystemUtils.getEnvVar(ENV_NAME_WORKSPACE_IMAGE, "ml-workspace-lab:" + LabConfig.SERVICE_VERSION);

    // Minio Image
    public static final String ENV_NAME_MINIO_IMAGE = "MINIO_IMAGE";
    public static final String MINIO_IMAGE = SystemUtils.getEnvVar(ENV_NAME_MINIO_IMAGE, "minio/minio:RELEASE.2020-08-18T19-41-00Z");

    // Mongo Image
    public static final String ENV_NAME_MONGO_IMAGE = "MONGO_IMAGE";
    public static final String MONGO_IMAGE = SystemUtils.getEnvVar(ENV_NAME_MONGO_IMAGE, "mongo:4.0.19");

    // Portainer Image
    public static final String ENV_NAME_PORTAINER_IMAGE = "PORTAINER_IMAGE";
    public static final String PORTAINER_IMAGE = SystemUtils.getEnvVar(ENV_NAME_PORTAINER_IMAGE, "portainer/portainer:1.24.1");

    // Portainer Image
    public static final String ENV_NAME_NFS_SERVER_IMAGE = "NFS_SERVER_IMAGE";
    public static final String NFS_SERVER_IMAGE = SystemUtils.getEnvVar(ENV_NAME_NFS_SERVER_IMAGE, "gcr.io/google_containers/volume-nfs:0.8");

    // Model Service Image
    public static final String ENV_NAME_MODEL_SERVICE_IMAGE = "MODEL_SERVICE_IMAGE";
    public static final String MODEL_SERVICE_IMAGE = SystemUtils.getEnvVar(ENV_NAME_MODEL_SERVICE_IMAGE, "lab-model-service:" + LabConfig.SERVICE_VERSION);

    // Backend Service Image
    public static final String ENV_NAME_BACKEND_SERVICE_IMAGE = "BACKEND_SERVICE_IMAGE";
    public static final String BACKEND_SERVICE_IMAGE = SystemUtils.getEnvVar(ENV_NAME_BACKEND_SERVICE_IMAGE, "lab-service:" + LabConfig.SERVICE_VERSION);

    // ssl enabled
    public static final String ENV_NAME_SSL_ENABLED = "SERVICE_SSL_ENABLED";
    public static final boolean SERVICE_SSL_ENABLED = SystemUtils.getEnvVar(ENV_NAME_SSL_ENABLED, "false").equalsIgnoreCase("true");

    // jwt secret for authentication layer
    // if env name is changed, also change it in Nginx!
    public static final String ENV_NAME_JWT_SECRET = "JWT_SECRET";
    public static final String JWT_SECRET = SystemUtils.getEnvVar(ENV_NAME_JWT_SECRET, AuthorizationManager.DEFAULT_JWT_SECRET);

    // S3 Configuration - Allows configuration of an external S3 instance. If not provided, the Lab-internal Minio instance is used.
    private static final String ENV_NAME_S3_ENDPOINT = "S3_ENDPOINT";
    private static final String ENV_NAME_S3_ACCESS_KEY = "S3_ACCESS_KEY";
    private static final String ENV_NAME_S3_SECRET_KEY = "S3_SECRET_KEY";
    private static final String ENV_NAME_S3_SECURED = "S3_SECURED";

    public static final String ENV_S3_ENDPOINT = SystemUtils.getEnvVar(ENV_NAME_S3_ENDPOINT);
    public static final String ENV_S3_ACCESS_KEY = SystemUtils.getEnvVar(ENV_NAME_S3_ACCESS_KEY);
    public static final String ENV_S3_SECRET_KEY = SystemUtils.getEnvVar(ENV_NAME_S3_SECRET_KEY);
    public static final String ENV_S3_SECURED = SystemUtils.getEnvVar(ENV_NAME_S3_SECURED);

    // Proxy Settings
    public static final String ENV_NAME_HTTP_PROXY = "http_proxy";
    public static final String ENV_NAME_HTTPS_PROXY = "https_proxy";
    public static final String ENV_NAME_NO_PROXY = "no_proxy";

    public static final String ENV_HTTP_PROXY = SystemUtils.getEnvVar(ENV_NAME_HTTP_PROXY);
    public static final String ENV_HTTPS_PROXY = SystemUtils.getEnvVar(ENV_NAME_HTTPS_PROXY);
    public static final String ENV_NO_PROXY = SystemUtils.getEnvVar(ENV_NAME_NO_PROXY);

    // Services Hardware Restrictions
    public static final String ENV_NAME_SERVICES_MEMORY_LIMIT = "SERVICES_MEMORY_LIMIT";
    public static final String SERVICES_MEMORY_LIMIT = SystemUtils.getEnvVar(ENV_NAME_SERVICES_MEMORY_LIMIT, "100"); // in GB

    public static final String ENV_NAME_SERVICES_CPU_LIMIT = "SERVICES_CPU_LIMIT";
    public static final String SERVICES_CPU_LIMIT = SystemUtils.getEnvVar(ENV_NAME_SERVICES_CPU_LIMIT, "8"); // in CPU Count

    // Storage is currently for workspaces in Kubernetes-mode. Size of Workspace PVC
    public static final String ENV_NAME_SERVICES_STORAGE_LIMIT = "SERVICES_STORAGE_LIMIT";
    public static final String SERVICES_STORAGE_LIMIT = SystemUtils.getEnvVar(ENV_NAME_SERVICES_STORAGE_LIMIT, "100"); // in GB

    // Kubernetes Variables
    // Basic mount path where all data is stored
    public static final String ENV_NAME_HOST_ROOT_DATA_MOUNT_PATH = "LAB_DATA_ROOT";
    public static final String HOST_ROOT_DATA_MOUNT_PATH = SystemUtils.getEnvVar(ENV_NAME_HOST_ROOT_DATA_MOUNT_PATH);

    // the host path where workspaces are mounted can be specified in an extra setting.
    // If not specified, it defaults to the standard host mount path for other central services.
    public static final String ENV_NAME_HOST_ROOT_WORKSPACE_DATA_MOUNT_PATH = "LAB_DATA_WORKSPACE_ROOT";
    public static final String HOST_ROOT_WORKSPACE_DATA_MOUNT_PATH = SystemUtils.getEnvVar(ENV_NAME_HOST_ROOT_WORKSPACE_DATA_MOUNT_PATH, HOST_ROOT_DATA_MOUNT_PATH);

    // enable ssh jumphost if Lab should publish port 22 on startup and start an SSH server. The jumphost functionality can be used so that
    // users can ssh into their own workspace. SSHing into the workspace container itself is not possible.
    public static final String ENV_NAME_SSH_ENABLED = "LAB_SSH_ENABLED";
    public static final boolean SERVICE_SSH_ENABLED = SystemUtils.getEnvVar(ENV_NAME_SSH_ENABLED, "true").equalsIgnoreCase("true");

    // only for Kubernetes mode. Enable persistent volume claims instead of mounting host paths into the pods.
    public static final String ENV_NAME_IS_MANAGED_KUBERNETES = "LAB_MANAGED_KUBERNETES";
    public static final boolean IS_MANAGED_KUBERNETES_CLUSTER = SystemUtils.getEnvVar(ENV_NAME_IS_MANAGED_KUBERNETES, "false")
                                                                           .equalsIgnoreCase("true");
    public static final String ENV_NAME_K8S_STORAGE_CLASS = "LAB_STORAGE_CLASS";
    public static final String K8S_LAB_STORAGE_CLASS = SystemUtils.getEnvVar(ENV_NAME_K8S_STORAGE_CLASS, "default");
    public static final String ENV_NAME_K8S_PVC_MINIO_STORAGE_LIMIT = "LAB_PVC_MINIO_STORAGE_LIMIT";
    public static final int K8S_PVC_MINIO_STORAGE_LIMIT = Integer.parseInt(SystemUtils.getEnvVar(ENV_NAME_K8S_PVC_MINIO_STORAGE_LIMIT, "100"));
    public static final String ENV_NAME_K8S_PVC_MONGO_STORAGE_LIMIT = "LAB_PVC_MONGO_STORAGE_LIMIT";
    public static final int K8S_PVC_MONGO_STORAGE_LIMIT = Integer.parseInt(SystemUtils.getEnvVar(ENV_NAME_K8S_PVC_MONGO_STORAGE_LIMIT, "5"));

    // be able to set the Kubernetes namespace explicitly. If not set, it is read from the Kubernetes cluster (mounted as a secret by Kubernetes)
    public static final String ENV_NAME_K8S_NAMESPACE = "LAB_KUBERNETES_NAMESPACE";
    public static String K8S_NAMESPACE = SystemUtils.getEnvVar(ENV_NAME_K8S_NAMESPACE, "");

    public static final String ENV_NAME_MAX_CONTAINER_SIZE = "MAX_CONTAINER_SIZE";
    public static final int MAX_CONTAINER_SIZE = Integer.parseInt(SystemUtils.getEnvVar(ENV_NAME_MAX_CONTAINER_SIZE, "100"));

    // not configurable right now
    public static final Integer LOG_REQUEST_MAX_TIME = 2000; // 2 seconds

    public static final String TERMS_OF_SERVICE_FOLDER_PATH = "/resources/tos";
    public static final String TERMS_OF_SERVICE_FILE_PATH = TERMS_OF_SERVICE_FOLDER_PATH + "/terms-of-service.txt";
    public static String TERMS_OF_SERVICE_TEXT = "";

    // ================ Members ============================================= //

    // ================ Constructors & Main ================================= //

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //
    public static Map<String, String> getEnvVariables() {
        Map<String, String> envVariables = new HashMap<>();
        envVariables.put(LabConfig.ENV_NAME_LAB_BASE_URL, String.valueOf(LabConfig.LAB_BASE_URL));
        envVariables.put(LabConfig.ENV_NAME_LAB_DEBUG, String.valueOf(LabConfig.IS_DEBUG));
        envVariables.put(LabConfig.ENV_NAME_SERVICES_RUNTIME, LabConfig.SERVICES_RUNTIME);
        envVariables.put(LabConfig.ENV_NAME_SERVICE_VERSION, LabConfig.SERVICE_VERSION);
        envVariables.put(LabConfig.ENV_NAME_LAB_NAMESPACE, LabConfig.LAB_NAMESPACE);
        envVariables.put(LabConfig.ENV_NAME_ALWAYS_CHECK_PERMISSIONS, String.valueOf(LabConfig.ALWAYS_CHECK_PERMISSIONS));
        envVariables.put(LabConfig.ENV_NAME_ALLOW_SELF_REGISTRATIONS, String.valueOf(LabConfig.ALLOW_SELF_REGISTRATIONS));
        envVariables.put(LabConfig.ENV_NAME_LAB_ACTION, LAB_ACTION.getName());

        envVariables.put(LabConfig.ENV_NAME_S3_ENDPOINT, ENV_S3_ENDPOINT);
        envVariables.put(LabConfig.ENV_NAME_S3_ACCESS_KEY, ENV_S3_ACCESS_KEY);
        envVariables.put(LabConfig.ENV_NAME_S3_SECRET_KEY, ENV_S3_SECRET_KEY);
        envVariables.put(LabConfig.ENV_NAME_S3_SECURED, ENV_S3_SECURED);

        envVariables.put(LabConfig.ENV_NAME_SERVICES_MEMORY_LIMIT, SERVICES_MEMORY_LIMIT);
        envVariables.put(LabConfig.ENV_NAME_SERVICES_CPU_LIMIT, SERVICES_CPU_LIMIT);
        envVariables.put(LabConfig.ENV_NAME_SERVICES_STORAGE_LIMIT, SERVICES_STORAGE_LIMIT);
        envVariables.put(LabConfig.ENV_NAME_MAX_CONTAINER_SIZE, String.valueOf(MAX_CONTAINER_SIZE));

        envVariables.put(LabConfig.ENV_NAME_HTTP_PROXY, ENV_HTTP_PROXY);
        envVariables.put(LabConfig.ENV_NAME_HTTPS_PROXY, ENV_HTTPS_PROXY);
        envVariables.put(LabConfig.ENV_NAME_NO_PROXY, ENV_NO_PROXY);

        envVariables.put(LabConfig.ENV_NAME_SSL_ENABLED, String.valueOf(LabConfig.SERVICE_SSL_ENABLED));

        envVariables.put(LabConfig.ENV_NAME_JWT_SECRET, JWT_SECRET);

        envVariables.put(LabConfig.ENV_NAME_WORKSPACE_BACKUP, WORKSPACE_BACKUP);

        envVariables.put(LabConfig.ENV_NAME_WORKSPACE_IMAGE, WORKSPACE_IMAGE);
        envVariables.put(LabConfig.ENV_NAME_MINIO_IMAGE, MINIO_IMAGE);
        envVariables.put(LabConfig.ENV_NAME_MONGO_IMAGE, MONGO_IMAGE);
        envVariables.put(LabConfig.ENV_NAME_NFS_SERVER_IMAGE, NFS_SERVER_IMAGE);
        envVariables.put(LabConfig.ENV_NAME_PORTAINER_IMAGE, PORTAINER_IMAGE);
        envVariables.put(LabConfig.ENV_NAME_MODEL_SERVICE_IMAGE, MODEL_SERVICE_IMAGE);
        envVariables.put(LabConfig.ENV_NAME_BACKEND_SERVICE_IMAGE, BACKEND_SERVICE_IMAGE);

        if (HOST_ROOT_DATA_MOUNT_PATH != null) {
            envVariables.put(LabConfig.ENV_NAME_HOST_ROOT_DATA_MOUNT_PATH, HOST_ROOT_DATA_MOUNT_PATH);
        }

        if (HOST_ROOT_WORKSPACE_DATA_MOUNT_PATH != null) {
            envVariables.put(LabConfig.ENV_NAME_HOST_ROOT_WORKSPACE_DATA_MOUNT_PATH, HOST_ROOT_WORKSPACE_DATA_MOUNT_PATH);
        }

        envVariables.put(LabConfig.ENV_NAME_SSH_ENABLED, String.valueOf(LabConfig.SERVICE_SSH_ENABLED));

        envVariables.put(LabConfig.ENV_NAME_IS_MANAGED_KUBERNETES, String.valueOf(IS_MANAGED_KUBERNETES_CLUSTER));
        envVariables.put(LabConfig.ENV_NAME_K8S_PVC_MINIO_STORAGE_LIMIT, String.valueOf(K8S_PVC_MINIO_STORAGE_LIMIT));
        envVariables.put(LabConfig.ENV_NAME_K8S_PVC_MONGO_STORAGE_LIMIT, String.valueOf(K8S_PVC_MONGO_STORAGE_LIMIT));
        envVariables.put(LabConfig.ENV_NAME_K8S_NAMESPACE, String.valueOf(K8S_NAMESPACE));

        return envVariables;
    }
    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //


    public enum LabAction {
        UNINSTALL("uninstall"),
        UPDATE("update"),
        UPDATE_FULL("update-full"),
        INSTALL("install"),
        SERVE("serve"),
        UNKNOWN("");

        private String name;

        LabAction(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static LabAction from(String type) {
            if (StringUtils.isNullOrEmpty(type)) {
                return UNKNOWN;
            }

            for (LabAction mode : LabAction.values()) {
                if (type.equalsIgnoreCase(mode.getName())) {
                    return mode;
                }
            }
            return UNKNOWN;
        }

        @Override
        public String toString() {
            return getName();
        }

        public boolean isUnknown() {
            return this == UNKNOWN;
        }
    }
}
