package org.mltooling.lab.services.managers;

import org.mltooling.core.lab.model.LabJob;
import org.mltooling.core.lab.model.LabService;
import org.mltooling.core.utils.FileUtils;
import org.mltooling.core.utils.ListUtils;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.core.utils.SystemUtils;
import org.mltooling.lab.LabConfig;
import org.mltooling.lab.services.AbstractServiceManager;
import org.mltooling.lab.services.CoreService;
import org.mltooling.lab.services.DockerDeploymentConfig;
import org.mltooling.lab.services.FeatureType;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.exceptions.DockerRequestException;
import com.spotify.docker.client.exceptions.ImageNotFoundException;
import com.spotify.docker.client.shaded.org.apache.http.MethodNotSupportedException;

import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.Exec;
import io.kubernetes.client.apis.*;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.models.*;
import io.kubernetes.client.util.Config;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.mltooling.lab.LabConfig.*;


public class KubernetesServiceManager extends AbstractServiceManager {

    // ================ Constants =========================================== //
    @Deprecated
    private static final String ORIGIN_NAME = "originName"; // deprecated, replaced with lab.docker.nane

    private static final String KUBE_CONFIG_PATH = "/root/.kube/config";

    private static final String NFS_PATH_PREFIX = "/exports";

    public static String NFS_MOUNT_TYPE = "nfs";
    public static String SECRET_MOUNT_TYPE = "secret";
    public static String PERSISTENT_VOLUME_CLAIM_MOUNT_TYPE = "pvc";

    // TODO: check whether storage class exists in k8s cluster in init method
    public static String STORAGE_CLASS_NAME = LabConfig.K8S_LAB_STORAGE_CLASS;
    // In managed kubernetes mode, the Lab will get access to the cluster via the incluster config provided by a service account
    // The ServiceAccount has to be created in advance
    public static String SERVICE_ACCOUNT_NAME = "SERVICE_ACCOUNT_NAME";
    public static String K8S_NAMESPACE = "";

    private static String PRIVILEGED_MODE_ATTR = "privileged";

    private static final String NO_LOGS_MSG = "No logs available.";

    private final String SERVICE_ADMIN_BASE_URL = "/service-admin/#!";
    private final String SERVICE_ADMIN_DEPLOYMENT_URL = SERVICE_ADMIN_BASE_URL + "/deployment/" + LAB_NAMESPACE + "/%s?namespace=" + LAB_NAMESPACE; // replace %s with the name of the deployment / service
    private final String SERVICE_ADMIN_JOB_URL = SERVICE_ADMIN_BASE_URL + "/job/" + LAB_NAMESPACE + "/%s?namespace=" + LAB_NAMESPACE;

    private final String RESOURCE_ALREADY_EXISTS = "Conflict";

    private final String SECRET_NAME_SSL = "ssl";
    private final String SECRET_NAME_TERMS_OF_SERVICE = "terms-of-service";

    // ================ Members ============================================= //

    private static Yaml yaml;

    private CoreV1Api api;
    //private AppsV1beta2Api betaApi;
    private AppsV1Api appsApi;
    private NetworkingV1Api networkingV1Api;
    private RbacAuthorizationV1Api authorizationApi;
    private BatchV1Api batchApi;
    private Exec exec;

    // ================ Constructors & Main ================================= //
    public KubernetesServiceManager() {
        try {
            initKubernetesClient();
        } catch (IOException e) {
            log.error("Could not initialize Kubernetes client. " + e.getMessage());
            System.exit(-1);
        }
    }
    // ================ Methods for/from SuperClass / Interfaces ============ //

    @Override
    public void installLab() {
        if (StringUtils.isNullOrEmpty(LabConfig.HOST_ROOT_DATA_MOUNT_PATH) &&
                !LabConfig.IS_MANAGED_KUBERNETES_CLUSTER) {
            // In managed Kubenretes mode, we don't mount host paths
            log.error(LabConfig.ENV_NAME_HOST_ROOT_DATA_MOUNT_PATH + " should not be null or empty for Kubernetes installation.");
            return;
        }

        try {
            // pull central image if it does not exist before
            // only run during installation for central core services which have to run on the Master node,
            // as the installation container also runs on the Master node where the image will then be pulled.
            super.inspectImage(CoreService.MINIO.getImage());
            super.inspectImage(CoreService.MONGO.getImage());
            if (!LabConfig.IS_MANAGED_KUBERNETES_CLUSTER) {
                super.inspectImage(CoreService.NFS.getImage());
            }
        } catch (DockerException | InterruptedException e) {
            log.error("Could not pull image of core service", e.getMessage());
        }

        // create namespace
        try {
            createNamespace();
        } catch (Exception e) {
            log.error("Could not create namespace: " + e.getMessage());
        }

        try {
            // apply network policy so the communication between Pods is controlled.
            createResourcesFromYaml("network-policy");
        } catch (Exception e) {
            if (!e.getMessage().equalsIgnoreCase(RESOURCE_ALREADY_EXISTS)) {
                log.error("Could not create network-policy", e);
                return;
            }

            log.warn("Network-policy already existed. Continue installation.", e.getMessage());
        }

        if (!LabConfig.IS_MANAGED_KUBERNETES_CLUSTER) {
            // In managed Kubernetes mode, we mount persistent volumes directly to the workspaces. Hence, there is no need to use NFS at all
            try {
                String hostMountPath = new File(getWorkspaceMountPath()).toPath().resolve(CoreService.NFS.getName()).toString();
                DockerDeploymentConfig nfsDeploymentConfig = createCoreService(CoreService.NFS.getImage(), CoreService.NFS.getName())
                        .addAttribute(PRIVILEGED_MODE_ATTR, true)
                        .addPortsToPublish("2049").addPortsToPublish("20048").addPortsToPublish("111")
                        .addMount(DockerDeploymentConfig.BIND_MOUNT_TYPE, hostMountPath + ":" + NFS_PATH_PREFIX)
                        .setNodeSelector(DockerDeploymentConfig.NODE_SELECTOR_MASTER);
                deployService(nfsDeploymentConfig);
            } catch (Exception e) {
                if (!e.getMessage().equalsIgnoreCase(RESOURCE_ALREADY_EXISTS)) {
                    log.error("Could not deploy nfs-service", e);
                    return;
                }

                log.warn("NFS service already existed. Continue installation.", e.getMessage());
            }

            // TODO: fix issues with service-admin
            // Service admin did not work out of the box when testing.
            try {
                createResourcesFromYaml("service-admin");
                //deployCentralService("dashboard", null, null);
            } catch (Exception e) {
                if (!e.getMessage().equalsIgnoreCase(RESOURCE_ALREADY_EXISTS)) {
                    log.error("Could not deploy service-admin", e);
                    return;
                }

                log.warn("Service-admin already existed. Continue installation.", e.getMessage());
            }
        }

        try {
            super.installLab();
        } catch (Exception e) {
            log.error("Could not install lab", e);
        }
        // redeploy all services belonging to existing projects.
        // This is needed in case Kubernetes resources are deleted (e.g. when updating the Lab). In this case,
        // the db, which is persisted even when Kubernetes resources are deleted, is checked for projects.

        //TODO: will raise a NullPointer during install (probably mongoDb not initialized here
        //            List<LabProject> projects = ComponentManager.INSTANCE.getProjectManager().getProjects();
        //            for (LabProject project : projects) {
        //                createProjectResources(project.getName());
        //            }
    }

    @Override
    public LabService deployService(DockerDeploymentConfig deploymentConfig) throws Exception {
        // Check whether the image can be found and if not it throws an Exception. 
        // NOTE: In the current implementation, this pulls the image to the node where Lab is running, which is not directly necessary since the pod might be started on some other node.
        try {
        super.inspectImage(deploymentConfig.getImage());
        } catch(ImageNotFoundException e) {
            throw e;
        // if image cannot be found it is a problem but DockerRequestException might occur when the Docker daemon mounted into the Lab pod does not have the right permissions.
        // in this case, the image exists but cannot be pulled from within the Lab pod, but likely from the cluster itself.
        } catch (DockerRequestException ignore) {}

        // Create the service before the deployment so that Kubernetes injects the service's access information into the pods.
        V1Service service = buildServiceConfig(deploymentConfig);
        service = api.createNamespacedService(K8S_NAMESPACE, service, "false", null, null);

        //V1beta2Deployment deploymentBeta = buildDeploymentConfigBeta(deploymentConfig);
        V1Deployment deployment = buildDeploymentConfig(deploymentConfig);
        appsApi.createNamespacedDeployment(K8S_NAMESPACE, deployment, "false", null, null);
        //deployment = betaApi.createNamespacedDeployment(LabConfig.LAB_NAMESPACE, deploymentBeta, "false", null, null);

        return transformService(service, deployment);
    }

    @Override
    public boolean deleteService(String serviceId, boolean removeVolumes, @Nullable String project) throws Exception {
        log.info("Delete service: " + serviceId + " (remove volumes: " + removeVolumes + ")");

        // delete the different resources like this instead of just the deployment, because sometimes pods are not deleted correctly when the
        // deployment is deleted. Likely a bug in Kubernetes.
        LabService service = null;
        try {
            service = getService(serviceId, project);

        } catch (Exception e) {
            log.warn(String.format("Did not find service '%s'", serviceId), e.getMessage());
        }

        try {
            if (service != null && service.getLabels().containsKey(ORIGIN_NAME)) {
                // TODO compatibility code for old origin label
                String selector = getOriginNameSelector(serviceId);
                if (!StringUtils.isNullOrEmpty(project)) {
                    selector = selector + "," + getProjectSelector(project);
                }
                deleteAllDeploymentResources(selector);
            } else {
                String selector = getDockerNameSelector(serviceId);
                if (!StringUtils.isNullOrEmpty(project)) {
                    selector = selector + "," + getProjectSelector(project);
                }

                deleteAllDeploymentResources(selector);
            }

            if (service != null) {
                api.deleteNamespacedService(service.getDockerName(), K8S_NAMESPACE, "false",
                                            new V1DeleteOptions().gracePeriodSeconds(0L), null, null, null, "Foreground");
            }
        } catch (Exception e) {
            // deleteDeployment throws an Exception as something is wrong with the k8s client lib (wrong json format).
            // However, the deployment is still deleted.
            log.warn(String.format("Issue in deployment resource deletion of service %s", serviceId), e.getMessage());
            throw e;
        }

        Thread.sleep(5000);

        return true;
    }

    /**
     * This method is supposed to delete the deployment, the belonging replicaset, and belonging pods. Usually it would be enough to just delete the deployment via 
     * .deleteNamespacedDeployment, but experience showed that sometimes replicasets and/or pods are not deleted correctly even if the deployment is deleted. Hence, we explicitly trigger the deletion of those resources.
     * For that, we have to use the respective deleteCollection* methods since names of replicasets and pods are suffixed with unknown tokens and we have to use the labelselector to find them.
     * @param labelSelector which is used to identify the deployment, replicaset, and pod to be deleted
     * @throws Exception
     */
    protected void deleteAllDeploymentResources(String labelSelector) throws Exception {
        // TODO: better than sleeps would be checking whether the resources are deleted and returning then
        
        appsApi.deleteCollectionNamespacedDeployment(K8S_NAMESPACE, "true", null, null,
                                                     labelSelector, null, null, null, null);
        Thread.sleep(5000);
        appsApi.deleteCollectionNamespacedReplicaSet(K8S_NAMESPACE, "true", null, null,
                                                     labelSelector, null, null, null, null);
        Thread.sleep(5000);
        api.deleteCollectionNamespacedPod(K8S_NAMESPACE, "true", null, null,
                                          labelSelector, null, null, null, null);
    }

    @Override
    public boolean deleteProjectResources(String project) throws Exception {
        for (LabService service : getServices(project)) {
            api.deleteNamespacedService(service.getDockerName(), K8S_NAMESPACE, "false",
                                        new V1DeleteOptions().gracePeriodSeconds(5L), null, null, null, null);
        }

        // when a deployment is deleted, also its corresponding pods and its containers are deleted.
        appsApi.deleteCollectionNamespacedDeployment(K8S_NAMESPACE, null, null, null, getProjectSelector(project), null, null, null, null);
        batchApi.deleteCollectionNamespacedJob(K8S_NAMESPACE, null, null, null, getProjectSelector(project), null, null, null, null);
        return true;
    }

    @Override
    public LabJob deployJob(DockerDeploymentConfig deploymentConfig) throws Exception {
        V1PodTemplateSpec podTemplateSpec = buildPodTemplateSpec(deploymentConfig);
        podTemplateSpec.getSpec().restartPolicy("Never");
        V1Job job = new V1Job()
                .metadata(getResourceMetadata(deploymentConfig))
                .spec(new V1JobSpec()
                              .backoffLimit(0)
                              .template(podTemplateSpec)
                );
        job = batchApi.createNamespacedJob(K8S_NAMESPACE, job, "false", null, null);
        return transformJob(job, null);
    }

    @Override
    public List<LabJob> getJobs(String project) throws Exception {
        List<LabJob> jobs = new ArrayList<>();

        String projectLabel = getProjectSelector(project);
        V1JobList v1JobList = batchApi.listNamespacedJob(K8S_NAMESPACE, null, null, null,
                                                            projectLabel, null, null, null, null);
        if (ListUtils.isNullOrEmpty(v1JobList.getItems())) {
            return jobs;
        }

        V1PodList v1PodList = api.listNamespacedPod(K8S_NAMESPACE, null, null, null,
                                                        projectLabel, null, null, null, null);

        HashMap<String, V1Pod> dockerNameToPod = new HashMap<>();
        for (V1Pod v1Pod : v1PodList.getItems()) {
            dockerNameToPod.put(v1Pod.getMetadata().getLabels().get(LABEL_DOCKER_NAME), v1Pod);
        }

        for (V1Job v1Job : v1JobList.getItems()) {
            V1Pod podBelongingToJob = dockerNameToPod.getOrDefault(v1Job.getMetadata().getLabels().get(LABEL_DOCKER_NAME), null);
            LabJob labJob = transformJob(v1Job, podBelongingToJob);
            jobs.add(labJob);
        }

        return jobs;
    }

    @Override
    public LabJob getJob(String jobId, @Nullable String project) throws Exception {
        V1JobList v1JobList = batchApi.listNamespacedJob(K8S_NAMESPACE, null, null, getNameSelector(jobId),
                                                         getProjectSelector(project), null, null, null, null);

        if (ListUtils.isNullOrEmpty(v1JobList.getItems())) {
            throw new Exception("Failed to find job " + jobId);
        }

        V1Job v1Job = v1JobList.getItems().get(0);
        V1Pod v1Pod = getPod(LABEL_DOCKER_NAME, v1Job.getMetadata().getLabels().get(LABEL_DOCKER_NAME));

        return transformJob(v1Job, v1Pod); // TODO: also query the deployment
    }

    @Override
    public boolean deleteJob(String jobId, String project) throws Exception {
        getJob(jobId, project);
    
        String labelSelector = getDockerNameSelector(jobId);
        if (!StringUtils.isNullOrEmpty(project)) {
            labelSelector = labelSelector + "," + getProjectSelector(project);
        }

        try {
            batchApi.deleteCollectionNamespacedJob(K8S_NAMESPACE, null, null, null, labelSelector, null, null, null, null);
        } catch(ApiException e) {
            log.error("Failed to delete job with selector " + labelSelector, e);
            return false;
        }
        
        return true;
    }

    @Override
    public List<LabService> getServices(@Nullable String project) throws Exception {
        V1ServiceList v1ServiceList = api.listNamespacedService(K8S_NAMESPACE, null, null,
                                                                null, getProjectSelector(project), null,
                                                                null, null, null);
        if (ListUtils.isNullOrEmpty(v1ServiceList.getItems())) {
            return new ArrayList<>();
        }
        // Get all deployments directly here (in one batch), as the one-by-one lookup for each service by name takes too long
        V1DeploymentList deploymentList = appsApi.listNamespacedDeployment(K8S_NAMESPACE, null, null,
                                                                                null, getProjectSelector(project),
                                                                                null, null, null, null);
        Map<String, V1Deployment> nameToDeployment = deploymentList.getItems().stream().collect(
                Collectors.toMap(
                        (V1Deployment deployment) -> deployment.getMetadata().getName(),
                        (V1Deployment deployment) -> deployment
                ));

        List<LabService> services = new ArrayList<>(v1ServiceList.getItems().size());
        for (V1Service v1Service : v1ServiceList.getItems()) {
            services.add(transformService(v1Service, nameToDeployment.get(v1Service.getMetadata().getName())));
        }

        return services;
    }

    @Override
    public LabService getService(String serviceId) throws Exception {
        return getService(serviceId, null);
    }

    @Override
    public LabService getService(String serviceId, @Nullable String project) throws Exception {
        String labelSelector = getLabNamespaceSelector() + "," + getProjectSelector(project);
        V1ServiceList v1ServiceList = api.listNamespacedService(K8S_NAMESPACE, null, null,
                                                                getNameSelector(serviceId), labelSelector,
                                                                1, null, null, null);

        if (ListUtils.isNullOrEmpty(v1ServiceList.getItems())) {
            labelSelector = getLabNamespaceSelector() + "," +  getFeatureNameSelector(serviceId) ;
            if (!StringUtils.isNullOrEmpty(project)) {
                labelSelector = labelSelector + "," + getProjectSelector(project);
            }
            v1ServiceList = api.listNamespacedService(K8S_NAMESPACE, null, null,
                                                      null, labelSelector, 1,
                                                      null, null, null);
            if (ListUtils.isNullOrEmpty(v1ServiceList.getItems())) {
                throw new Exception("Failed to find service " + serviceId);
            }
        }

        return transformService(v1ServiceList.getItems().get(0), null); // TODO: also query the deployment
    }

    @Override
    public String getServiceLogs(String serviceId) throws Exception {
        String logs = null;
        try {
            // TODO compatibility code to support old label
            logs = api.readNamespacedPodLog(getPod(ORIGIN_NAME, serviceId).getMetadata().getName(), K8S_NAMESPACE, null, null,
                                            null, "true", null, null, null, null);
        } catch (Exception ex) {
            // do nothing
        }

        if (StringUtils.isNullOrEmpty(logs)) {
            logs = api.readNamespacedPodLog(getPod(LABEL_DOCKER_NAME, serviceId).getMetadata().getName(), K8S_NAMESPACE, null, null,
                                            null, "true", null, null, null, null);
        }

        if (StringUtils.isNullOrEmpty(logs)) {
            logs = NO_LOGS_MSG;
        }
        return logs;
    }

    @Override
    public String getJobLogs(String jobId) throws Exception {
        String logs = getServiceLogs(jobId); //TODO: should work as we access the Pod in both cases?
        if (StringUtils.isNullOrEmpty(logs)) {
            logs = NO_LOGS_MSG;
        }
        return logs;
    }

    @Override
    protected LabService getLabService() throws Exception {
        return getService(CoreService.LAB_BACKEND.getName());
    }

    @Override
    public void uninstallLab() throws Exception {
        cleanUpLab();
    }

    @Override
    public void updateLab(boolean backendOnly) throws Exception {
        // delete current network policies so that the new ones can be installed.
        // TODO: In this time period - between uninstalling and installing - the network between the pods is not secured for a few (milli)-seconds. A possibility would be to name the policies.
        networkingV1Api.deleteCollectionNamespacedNetworkPolicy(K8S_NAMESPACE, null, null,
                                                                null, null, null,
                                                                null, null, null);

        if (!backendOnly) {
            // delete other core services but no volumes
            deleteService(CoreService.NFS.getName(), false, null);
        }

        super.updateLab(backendOnly);
    }

    @Override
    public DockerDeploymentConfig createWorkspaceService(String user) throws Exception {
        DockerDeploymentConfig workspaceDeployment = super.createWorkspaceService(user);

        if (!LabConfig.IS_MANAGED_KUBERNETES_CLUSTER) {
            // connect NFS container
            final String nfsPath = NFS_PATH_PREFIX + "/" + workspaceDeployment.getName();

            // create the directory on the nfs-mounted path that is used by the here created workspace
            V1Pod nfsPod = getPod(LABEL_FEATURE_NAME, CoreService.NFS.getName());
            final String[] command = { "mkdir", nfsPath };
            exec.exec(nfsPod, command, false);

            return workspaceDeployment
                    .addMount(NFS_MOUNT_TYPE, getNfsServerIp() + "@" + nfsPath + ":" + workspaceDeployment.getVolumePath());
        }
        else {
            return workspaceDeployment
                    .addMount(PERSISTENT_VOLUME_CLAIM_MOUNT_TYPE, SERVICES_STORAGE_LIMIT + "G:" + workspaceDeployment.getVolumePath());
        }
    }

    @Override
    public DockerDeploymentConfig createLabService() {
        DockerDeploymentConfig deploymentConfig = super.createLabService();

        // TODO not needed here, is added in LabConfig.getEnvVariables deploymentConfig.addEnvVariable(ENV_NAME_HOST_ROOT_DATA_MOUNT_PATH, HOST_ROOT_DATA_MOUNT_PATH);
        // TODO: does it throw an error when kube/config is not available?

        // when Lab runs in managed Kubernetes mode, we load the config from the cluster
        // TODO: think about loading it from the cluster also in the non-managed setup
        if (!LabConfig.IS_MANAGED_KUBERNETES_CLUSTER) {
            createKubeConfigAsSecret();
            deploymentConfig.addMount(SECRET_MOUNT_TYPE, "kubeconfig:/root/.kube:ro"); // mount kube config for kubernetes.
        } else {
            // TODO: document that a serviceaccount with name lab has to exist
            deploymentConfig.addAttribute(SERVICE_ACCOUNT_NAME, "lab");
        }

        if (LabConfig.SERVICE_SSL_ENABLED) {
            // TODO only add volume if container exists? Needs better solution!
            // add ssl volume to backend service if ssl is enabled
            String sslResourcesPath = SystemUtils.getEnvVar("_SSL_RESOURCES_PATH", "/resources/ssl");
            // TODO: Kubernetes: better way?

            V1SecretList secretList = null;
            try {
                secretList = api.listNamespacedSecret(K8S_NAMESPACE, null, null, "metadata.name=" + SECRET_NAME_SSL, null, 1, null, null, null);
            } catch (ApiException e) {
                log.warn("Could not list ssl secret", e);
            }

            boolean sslSecretExists = true;
            if (secretList == null || secretList.getItems().size() == 0) {
                sslSecretExists = createSSLCertAsSecret();
            }

            if (sslSecretExists) {
                log.info("Add ssl secret mount");
                deploymentConfig.addMount(SECRET_MOUNT_TYPE, SECRET_NAME_SSL + ":" + sslResourcesPath + ":ro");
            }
        }

        V1SecretList secretList = null;
        try {
            secretList = api.listNamespacedSecret(K8S_NAMESPACE, null, null, "metadata.name=" + SECRET_NAME_TERMS_OF_SERVICE, null, 1, null, null, null);
        } catch (ApiException e) {
            log.warn("Could not list terms of service secret", e);
        }

        if ((secretList != null && secretList.getItems().size() == 1) || 
            new File(LabConfig.TERMS_OF_SERVICE_FOLDER_PATH + "/terms-of-service.txt").exists()) {
            boolean termsOfServiceSecretExists = true;
            if (secretList == null || secretList.getItems().size() == 0) {
                termsOfServiceSecretExists = createTermsOfServiceAsSecret();
            }

            if (termsOfServiceSecretExists) {
                log.info("Add terms of service secret mount");
                deploymentConfig.addMount(SECRET_MOUNT_TYPE, SECRET_NAME_TERMS_OF_SERVICE + ":" + LabConfig.TERMS_OF_SERVICE_FOLDER_PATH + ":ro");
            }
        }

        return deploymentConfig;
    }

    @Override
    public List<String> shutdownDiskExceedingContainers(boolean dryRun) throws Exception {
        throw new MethodNotSupportedException("This method cannot be executed in Kubernetes-mode, as there the Kubernetes-native 'ephemeral-storage' property is set for all created pods based on the $MAX_CONTAINER_SIZE variable Lab was started with.");
    }
    // ================ Public Methods ====================================== //

    public static void cleanUpLab() throws IOException, ApiException {
        ApiClient client = Config.defaultClient();

        if (LabConfig.IS_DEBUG) {
            client.setDebugging(true);
        }

        Configuration.setDefaultApiClient(client);
        CoreV1Api api = new CoreV1Api();
        initKubernetesNamespaceVariable();
        api.deleteNamespace(K8S_NAMESPACE, "false", new V1DeleteOptions(), "",
                            0, false, null);
    }

    // ================ Private Methods ===================================== //

    private boolean createSSLCertAsSecret() {
        try {
            api.createNamespacedSecret(K8S_NAMESPACE, new V1Secret()
                                               .type("generic")
                                               .metadata(new V1ObjectMeta().name(SECRET_NAME_SSL))
                                               .putDataItem("cert.key", org.apache.commons.io.FileUtils.readFileToByteArray(new File("/resources/ssl/cert.key")))
                                               .putDataItem("cert.crt", org.apache.commons.io.FileUtils.readFileToByteArray(new File("/resources/ssl/cert.crt"))),
                                       "false", null, null
            );
        } catch (ApiException | IOException e) {
            log.error("Could not create SSL secret", e.getMessage());
            return false;
        }

        return true;
    }

    private boolean createTermsOfServiceAsSecret() {
        try {
            api.createNamespacedSecret(K8S_NAMESPACE, new V1Secret()
                                               .type("generic")
                                               .metadata(new V1ObjectMeta().name(SECRET_NAME_TERMS_OF_SERVICE))
                                               .putDataItem("terms-of-service.txt", org.apache.commons.io.FileUtils.readFileToByteArray(new File(LabConfig.TERMS_OF_SERVICE_FOLDER_PATH + "/terms-of-service.txt"))),
                                       "false", null, null
            );
        } catch (ApiException | IOException e) {
            log.error("Could not create Terms of Service secret", e.getMessage());
            return false;
        }

        return true;
    }

    private void createKubeConfigAsSecret() {
        try {
            api.createNamespacedSecret(K8S_NAMESPACE, new V1Secret()
                                               .type("generic")
                                               .metadata(new V1ObjectMeta().name("kubeconfig"))
                                               .putDataItem("config", org.apache.commons.io.FileUtils.readFileToByteArray(new File(KUBE_CONFIG_PATH))),
                                       "false", null, null
            );
        } catch (ApiException | IOException e) {
            if (!e.getMessage().equalsIgnoreCase(RESOURCE_ALREADY_EXISTS)) {
                log.error("Could not create kube-config Secret", e.getMessage());
                // end installation process, as without the kube-config, the Lab pod cannot use the k8s api
                System.exit(-1);
            }

            log.error("Could not create secret again", e);
        }
    }

    /**
     * @return the ip address of the NFS service / container or empty String, if service is not found
     */
    private String getNfsServerIp() {
        // Kubernetes injects the ip addresses of all services to all pods as environment variables
        // in case the service already existed when the pod was created.
        final String nfsServerIpEnv = (LabConfig.LAB_NAMESPACE + "_" + CoreService.NFS.getName() + "_service_host")
                .replaceAll("-", "_")
                .toUpperCase();

        return SystemUtils.getEnvVar(nfsServerIpEnv, "");
    }

    private void initKubernetesClient() throws IOException {
        ApiClient client;

        // In managed Kubernetes mode, we cannot mount the config into the pod but have to load it from the cluster via
        // a ServiceAccount.
        // TODO: think about doing this also in the non-managed mode
        if (LabConfig.IS_MANAGED_KUBERNETES_CLUSTER) {
            client = Config.fromCluster();
        } else {
            client = Config.defaultClient();
        }

        if (LabConfig.IS_DEBUG) {
            client.setDebugging(true);
        }

        Configuration.setDefaultApiClient(client);
        api = new CoreV1Api();
        //betaApi = new AppsV1beta2Api();
        appsApi = new AppsV1Api();
        networkingV1Api = new NetworkingV1Api();
        authorizationApi = new RbacAuthorizationV1Api();
        batchApi = new BatchV1Api();
        exec = new Exec();

        initKubernetesNamespaceVariable();
    }

    private void createNamespace() throws ApiException {
        api.createNamespace(new V1Namespace()
                                    .metadata(new V1ObjectMeta()
                                                      .name(K8S_NAMESPACE)
                                                      .putLabelsItem("name", K8S_NAMESPACE)
                                    ), "false", null, null
        );
    }

    private V1Service buildServiceConfig(DockerDeploymentConfig dockerDeploymentConfig) {
        V1Service service = new V1Service();
        service.metadata(getResourceMetadata(dockerDeploymentConfig));
        V1ServiceSpec serviceSpec = new V1ServiceSpec();
        addPortInformation(serviceSpec, dockerDeploymentConfig);
        serviceSpec.selector(getLabelSelector(dockerDeploymentConfig).getMatchLabels());
        service.spec(serviceSpec);
        return service;
    }

    private void addPortInformation(V1ServiceSpec serviceSpec, DockerDeploymentConfig dockerDeploymentConfig) {
        List<V1ServicePort> servicePorts = new ArrayList<>();
        String serviceType = "ClusterIP";
        Set<Integer> addedPorts = new HashSet<>();

        List<String> portsToPublish = new ArrayList<>(dockerDeploymentConfig.getPortsToPublish());

        // k8s throws an error when, for example, two times the port "8091" is defined for a service
        // Thus, sort the ports so that the more specific ones (direct mapping via ":") appear first in the list,
        // so that only one ServicePort is added for a specific target port
        portsToPublish.sort((o1, o2) -> {
            if (o1.contains(":") && o2.contains(":")) {
                return 0;
            } else if (o1.contains(":")) {
                return -1;
            } else {
                return 1;
            }
        });

        for (String port : portsToPublish) {
            V1ServicePort servicePort = new V1ServicePort()
                    .name(port.replace(":", "-"))
                    .protocol("TCP");
            if (port.contains(":")) {
                Integer publishedPort = Integer.parseInt(port.split(":")[0]);
                Integer targetPort = Integer.parseInt(port.split(":")[1]);

                if (addedPorts.contains(targetPort)) {
                    continue;
                }
                servicePort.port(targetPort).nodePort(publishedPort);
                addedPorts.add(targetPort);
                serviceType = "NodePort"; // service can always be only of one Type. So if it must be NodePort for one port, it cannot be set back to ClusterIP
                // For managed kubernetes clusters, we set the service type to ClusterIP. Either add a LoadBalancer service or change the type to LoadBalancer manually.
                if (LabConfig.IS_MANAGED_KUBERNETES_CLUSTER) {
                    servicePort.setNodePort(null); // ClusterIP services must not have the NodePort property
                    serviceType = "ClusterIP";
                }
            } else {
                final Integer targetPort = Integer.parseInt(port);
                if (addedPorts.contains(targetPort)) {
                    continue;
                }
                addedPorts.add(targetPort);
                servicePort.port(targetPort);
            }

            servicePorts.add(servicePort);
        }

        if (servicePorts.isEmpty()) {
            servicePorts.add(new V1ServicePort().name("default").protocol("TCP").port(DEFAULT_CONNECTION_PORT));
        }

        serviceSpec.ports(servicePorts).type(serviceType);
    }

    private V1Deployment buildDeploymentConfig(DockerDeploymentConfig dockerDeploymentConfig) {
        V1Deployment deployment = new V1Deployment();
        deployment.metadata(getResourceMetadata(dockerDeploymentConfig));

        // Deployment Spec
        V1DeploymentSpec deploymentSpec = new V1DeploymentSpec();
        deploymentSpec.replicas(1);
        deploymentSpec.selector(getLabelSelector(dockerDeploymentConfig));

        // Pod Template Spec
        V1PodTemplateSpec podTemplateSpec = buildPodTemplateSpec(dockerDeploymentConfig);
        addMountInformation(podTemplateSpec.getSpec(),
                            podTemplateSpec.getSpec().getContainers().get(0),
                            dockerDeploymentConfig,
                            DockerDeploymentConfig.BIND_MOUNT_TYPE,
                            NFS_MOUNT_TYPE,
                            SECRET_MOUNT_TYPE,
                            PERSISTENT_VOLUME_CLAIM_MOUNT_TYPE);

        deploymentSpec.template(podTemplateSpec);
        deployment.spec(deploymentSpec);
        return deployment;
    }

    @Deprecated
    private V1beta2Deployment buildDeploymentConfigBeta(DockerDeploymentConfig dockerDeploymentConfig) {
        V1beta2Deployment deployment = new V1beta2Deployment();
        deployment.metadata(getResourceMetadata(dockerDeploymentConfig));

        // Deployment Spec
        V1beta2DeploymentSpec deploymentSpec = new V1beta2DeploymentSpec();
        deploymentSpec.replicas(1);
        deploymentSpec.selector(getLabelSelector(dockerDeploymentConfig));

        // Pod Template Spec
        V1PodTemplateSpec podTemplateSpec = buildPodTemplateSpec(dockerDeploymentConfig);
        addMountInformation(podTemplateSpec.getSpec(),
                            podTemplateSpec.getSpec().getContainers().get(0),
                            dockerDeploymentConfig,
                            DockerDeploymentConfig.BIND_MOUNT_TYPE,
                            NFS_MOUNT_TYPE,
                            SECRET_MOUNT_TYPE,
                            PERSISTENT_VOLUME_CLAIM_MOUNT_TYPE);

        deploymentSpec.template(podTemplateSpec);
        deployment.spec(deploymentSpec);
        return deployment;
    }

    private void addMountInformation(V1PodSpec podSpec,
                                     V1Container v1Container,
                                     DockerDeploymentConfig dockerDeploymentConfig,
                                     String... mountTypes) {
        int mountCount = 1;
        for (String mountType : mountTypes) {
            Collection<String> mounts = dockerDeploymentConfig.getMounts().get(mountType);
            if (!ListUtils.isNullOrEmpty(mounts)) {
                for (String mount : mounts) {
                    final String mountName = "mount" + mountCount;

                    // format is (<ip-address>@)?(hostPath|quantity):containerPath(:ro)?
                    String[] mountParts = mount.split(":");
                    String mountContainerPath = mountParts[1];
                    boolean isReadOnly = false;
                    if (mountParts.length == 3) {
                        isReadOnly = mountParts[2].equalsIgnoreCase("ro");
                    }

                    v1Container.addVolumeMountsItem(new V1VolumeMount()
                                                            .name(mountName)
                                                            .mountPath(mountContainerPath)
                                                            .readOnly(isReadOnly)
                    );

                    if (mountType.equals(DockerDeploymentConfig.BIND_MOUNT_TYPE)) {
                        String mountHostPath = mountParts[0];
                        podSpec.addVolumesItem(new V1Volume()
                                                       .name(mountName)
                                                       .hostPath(new V1HostPathVolumeSource().path(mountHostPath))
                        );
                    } else if (mountType.equals(NFS_MOUNT_TYPE)) {
                        mountParts = mountParts[0].split("@"); // format is <ip_address>@<host_path>:<container_path>
                        String mountHostIp = mountParts[0];
                        String nfsPath = mountParts[1]; // corresponds to the <host_path> part
                        podSpec.addVolumesItem(new V1Volume().name(mountName)
                                                             .nfs(new V1NFSVolumeSource()
                                                                          .server(mountHostIp)
                                                                          .path(nfsPath)
                                                                          .readOnly(false)
                                                             )
                        );
                    } else if (mountType.equals(SECRET_MOUNT_TYPE)) {
                        final String secretName = mountParts[0];

                        podSpec.addVolumesItem(new V1Volume()
                                                       .name(mountName)
                                                       .secret(new V1SecretVolumeSource()
                                                                       .secretName(secretName)
                                                       )
                        );
                    } else if (mountType.equals(PERSISTENT_VOLUME_CLAIM_MOUNT_TYPE)) {
                        String quantity = mountParts[0]; //for pvc mount type, the format has to be <quantity>:<container_path>

                        V1PersistentVolumeClaim pvc = new V1PersistentVolumeClaim()
                                .metadata(getResourceMetadata(dockerDeploymentConfig))
                                .spec(new V1PersistentVolumeClaimSpec()
                                              .storageClassName(STORAGE_CLASS_NAME)
                                              .resources(new V1ResourceRequirements().requests(
                                                      Collections.singletonMap("storage", new Quantity(quantity)))
                                              )
                                              .addAccessModesItem("ReadWriteOnce")
                                );
                        
                        boolean isPersistentVolumeClaimExisting = false;
                        try {
                            api.createNamespacedPersistentVolumeClaim(K8S_NAMESPACE, pvc, "false", null, null);
                            isPersistentVolumeClaimExisting = true;
                        } catch (ApiException e) {
                            log.error("Could not create persistent volume claim with name " + mountName + " | " + e.getMessage());

                            // If code is 409, the PVC already exists and, therefore, can be mounted to the pod.
                            if (e.getCode() == 409) {
                                isPersistentVolumeClaimExisting = true;
                            }
                        }

                        if (isPersistentVolumeClaimExisting) {
                            podSpec.addVolumesItem(new V1Volume()
                                .name(mountName)
                                .persistentVolumeClaim(new V1PersistentVolumeClaimVolumeSource()
                                                            .claimName(dockerDeploymentConfig.getName())
                                )
                            );
                        }

                    }
                    mountCount++;
                }
            }
        }
    }

    private V1PodTemplateSpec buildPodTemplateSpec(DockerDeploymentConfig dockerDeploymentConfig) {
        // Pod Template Spec
        V1PodTemplateSpec podTemplateSpec = new V1PodTemplateSpec();
        podTemplateSpec.metadata(getResourceMetadata(dockerDeploymentConfig));

        // Pod Spec
        V1PodSpec podSpec = new V1PodSpec();

        if (!StringUtils.isNullOrEmpty(dockerDeploymentConfig.getNodeSelector())) {
            // On a managed Kubernetes cluster, there is no need to pin a pod to "master". In fact, we don't even have a master node.
            if (!(LabConfig.IS_MANAGED_KUBERNETES_CLUSTER &&
                    dockerDeploymentConfig.getNodeSelector().equalsIgnoreCase(DockerDeploymentConfig.NODE_SELECTOR_MASTER))) {
                podSpec.nodeSelector(Collections.singletonMap("role", dockerDeploymentConfig.getNodeSelector()));
            }
        }

        // deactivate enableServiceLinks

        // Set resource restrictions
        V1ResourceRequirements resourceRequirements = new V1ResourceRequirements();

        try {
            Integer cpuLimit = Integer.valueOf(LabConfig.SERVICES_CPU_LIMIT);
            resourceRequirements
                    .putLimitsItem("cpu", new Quantity(String.valueOf(cpuLimit * 1000) + "m")) // e.g. 8000m = 8000 micro-cores = 8 cores (https://cloud.google.com/blog/products/gcp/kubernetes-best-practices-resource-requests-and-limits)
                    .putRequestsItem("cpu", new Quantity("0m")); // set requests explicitly, otherwise request will be same as limit
        } catch (NumberFormatException | NullPointerException ex) {
            // do nothing
        }

        try {
            Integer memoryLimit = Integer.valueOf(LabConfig.SERVICES_MEMORY_LIMIT);
            resourceRequirements
                    .putLimitsItem("memory", new Quantity(String.valueOf(memoryLimit) + "Gi"))
                    .putRequestsItem("memory", new Quantity("0Gi"));
        } catch (NumberFormatException | NullPointerException ex) {
            // do nothing
        }

        // Set a limit for the ephemeral-storage (the disk that is used for container paths that is not mounted via volume).
        // If the storage increases too much (container size is blowing up), the whole node can go down.
        // Ignore Lab core services.
        try {
            Integer ephemeralStorageLimit = Integer.valueOf(LabConfig.MAX_CONTAINER_SIZE);
            if (ephemeralStorageLimit != null && ephemeralStorageLimit != MAX_CONTAINER_SIZE_DISABLED &&
                    !dockerDeploymentConfig.getLabels().containsValue(FeatureType.CORE_SERVICE.getName())) {
                resourceRequirements
                        .putLimitsItem("ephemeral-storage", new Quantity(String.valueOf(ephemeralStorageLimit) + "Gi"))
                        .putRequestsItem("ephemeral-storage", new Quantity("0Gi")); // set requests explicitly, otherwise request will be same as limit
            }
        } catch (NumberFormatException | NullPointerException ex) {
            // do nothing
        }

        // Container
        V1Container container = new V1Container();
        container
                .name(dockerDeploymentConfig.getName())
                .image(dockerDeploymentConfig.getImage())
                .imagePullPolicy("IfNotPresent") // set explicitly, as in k8s for image tag :latest the default policy is "Always"
                .resources(resourceRequirements);

        if (!ListUtils.isNullOrEmpty(dockerDeploymentConfig.getCmd())) {
            container.args(dockerDeploymentConfig.getCmd());
        }

        if (dockerDeploymentConfig.getAttributes().containsKey(PRIVILEGED_MODE_ATTR)
                && (Boolean) dockerDeploymentConfig.getAttributes().get(PRIVILEGED_MODE_ATTR)) {
            // privileged mode has been set
            container.securityContext(new V1SecurityContext().privileged(true));
        }

        if (dockerDeploymentConfig.getAttributes().containsKey(SERVICE_ACCOUNT_NAME)) {
            podSpec = podSpec.serviceAccountName((String) dockerDeploymentConfig.getAttributes().get(SERVICE_ACCOUNT_NAME));
        }

        List<V1EnvVar> envVars = new ArrayList<>();
        for (String envVar : dockerDeploymentConfig.getEnvVariables().keySet()) {
            envVars.add(new V1EnvVar().name(envVar).value(dockerDeploymentConfig.getEnvVariables().get(envVar)));
        }
        container.env(envVars);

        // the name is used by Kubernetes to match the container-volume and the pod-volume section
        final String volumeName = "data";
        // if mount is present, don't use the Volumepath as the Mount information is more specific
        if (!StringUtils.isNullOrEmpty(dockerDeploymentConfig.getVolumePath())
                && ListUtils.isNullOrEmpty(dockerDeploymentConfig.getMounts().keySet())) {

            if (!LabConfig.IS_MANAGED_KUBERNETES_CLUSTER) {
                // TODO: Problem if pod is running on another node - Data Path might not exist
                String hostPath = new File(HOST_ROOT_DATA_MOUNT_PATH).toPath().resolve(dockerDeploymentConfig.getName()).toString();
                container.addVolumeMountsItem(new V1VolumeMount()
                                                      .name(volumeName)
                                                      .mountPath(dockerDeploymentConfig.getVolumePath()));

                podSpec.addVolumesItem(new V1Volume()
                                               .name(volumeName)
                                               .hostPath(new V1HostPathVolumeSource().path(hostPath))
                );
            } else {
                dockerDeploymentConfig.addMount(PERSISTENT_VOLUME_CLAIM_MOUNT_TYPE,
                                                getPersistentVolumeClaimMountInfo(
                                                        getPersistentVolumeClaimQuantity(dockerDeploymentConfig.getFeatureName()),
                                                        dockerDeploymentConfig.getVolumePath()
                                                )
                );
            }
        }

        return podTemplateSpec.spec(podSpec.addContainersItem(container));
    }

    private V1ObjectMeta getResourceMetadata(DockerDeploymentConfig dockerDeploymentConfig) {
        return new V1ObjectMeta()
                .namespace(K8S_NAMESPACE) // deprecated: dockerDeploymentConfig.getNamespace()
                .name(dockerDeploymentConfig.getName())
                .labels(dockerDeploymentConfig.getLabels());
    }

    private V1LabelSelector getLabelSelector(DockerDeploymentConfig dockerDeploymentConfig) {
        return new V1LabelSelector()
                .putMatchLabelsItem(LABEL_DOCKER_NAME, dockerDeploymentConfig.getName());
    }

    private void createResourcesFromYaml(String yamlFileName) throws IOException, ApiException {
        final String yamlPath = String.format("/resources/kubernetes/%s.yaml", yamlFileName);
        String yamlFile = FileUtils.readFile(yamlPath);

        final String qualifiedLabName = LabConfig.LAB_NAMESPACE + "-" + CoreService.LAB_BACKEND.getName();
        yamlFile = yamlFile.replaceAll("<SERVICE_NAMESPACE>", K8S_NAMESPACE).replaceAll("<"+ LabConfig.ENV_NAME_LAB_NAMESPACE+ ">", LabConfig.LAB_NAMESPACE).replaceAll("<LAB_NAME>", qualifiedLabName);
        String[] resources = splitYamlIntoItsResources(yamlFile);
        Yaml yaml = getYamlParser();
        for (String resource : resources) {
            Map<String, Object> loadedResource = yaml.loadAs(resource, Map.class);
            if (loadedResource == null) {
                continue;
            }
            final String kind = (String) loadedResource.get("kind");
            switch (kind) {
                case "NetworkPolicy":
                    V1NetworkPolicy networkPolicy = yaml.loadAs(resource, V1NetworkPolicy.class);
                    if (!ListUtils.isNullOrEmpty(networkPolicy.getSpec().getIngress())) {
                        for (V1NetworkPolicyIngressRule ingressRule : networkPolicy.getSpec().getIngress()) {
                            if (!ListUtils.isNullOrEmpty(ingressRule.getPorts())) {
                                for (V1NetworkPolicyPort port : ingressRule.getPorts()) {
                                    port.port(new IntOrString(Integer.parseInt(port.getPort().getStrValue())));
                                }
                            }
                        }
                    }
                    networkingV1Api.createNamespacedNetworkPolicy(K8S_NAMESPACE, networkPolicy, "false", null, null);
                    break;
                case "Secret":
                    api.createNamespacedSecret(K8S_NAMESPACE, yaml.loadAs(resource, V1Secret.class), "false", null, null);
                    break;
                case "ServiceAccount":
                    api.createNamespacedServiceAccount(K8S_NAMESPACE, yaml.loadAs(resource, V1ServiceAccount.class), "false", null, null);
                    break;
                case "RoleBinding":
                    authorizationApi.createNamespacedRoleBinding(K8S_NAMESPACE, yaml.loadAs(resource, V1RoleBinding.class), "false", null, null);
                    break;
                case "Role":
                    authorizationApi.createNamespacedRole(K8S_NAMESPACE, yaml.loadAs(resource, V1Role.class), "false", null, null);
                    break;
                case "ClusterRole":
                    try {
                        authorizationApi.createClusterRole(yaml.loadAs(resource, V1ClusterRole.class), "false", null, null);
                    } catch (ApiException e) {
                        log.error("Could not create ClusterRole", e.getMessage());
                    }
                    break;
                case "ClusterRoleBinding":
                    try {
                        authorizationApi.createClusterRoleBinding(yaml.loadAs(resource, V1ClusterRoleBinding.class), "false", null, null);
                    } catch (ApiException e) {
                        log.error("Could not create ClusterRoleBinding", e.getMessage());
                    }
                    break;
                case "Deployment":
                    appsApi.createNamespacedDeployment(K8S_NAMESPACE, yaml.loadAs(resource, V1Deployment.class), "false", null, null);
                    break;
                case "Service":
                    V1Service service = yaml.loadAs(resource, V1Service.class);
                    // explicitly map the port value to an Integer as yaml.loadAs will return the port (e.g. 9090) as a String ("9090") which causes an error from the Kubernetes client
                    for (V1ServicePort servicePort : service.getSpec().getPorts()) {
                        servicePort.targetPort(new IntOrString(Integer.parseInt(servicePort.getTargetPort().getStrValue())));
                    }
                    api.createNamespacedService(K8S_NAMESPACE, service, "false", null, null);
                    break;
            }
        }
    }

    private static String[] splitYamlIntoItsResources(String yamlFile) {
        return yamlFile.split("\n---\n");
    }

    private static Yaml getYamlParser() {
        if (yaml == null) {
            // exclude fields with 'null' value
            Representer representer = new Representer() {

                @Override
                protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
                    // if value of property is null, ignore it.
                    if (propertyValue == null) {
                        return null;
                    } else {
                        return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                    }
                }
            };

            yaml = new Yaml(representer);
        }
        return yaml;
    }

    private LabService transformService(V1Service service, @Nullable V1Deployment deployment) {//@Nullable V1beta2Deployment deployment) {

        String dockerImage = "";
        if (deployment == null) {
            try {
                deployment = getDeployment(service.getMetadata().getName(), service.getMetadata().getLabels().get(LABEL_PROJECT));
            } catch (Exception ignore) {
                log.info("Deployment is null for service " + service.getMetadata().getName());
            }
        }

        Map<String, String> configuration = new HashMap<>();
        boolean healthy = true; // default assume that service is healthy
        if (deployment != null) {
            configuration = new HashMap<>();
            V1Container container = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
            dockerImage = container.getImage();
            for (V1EnvVar envVar : container.getEnv()) {
                configuration.put(envVar.getName(), envVar.getValue());
            }

            // set status to 'false' when deployment status does not exist at all or when the 'readyReplicas' does not match the (desired) 'replicas'
            healthy =
                    (deployment.getStatus() != null
                            && deployment.getStatus().getReadyReplicas() != null
                            && deployment.getStatus().getReplicas() != null)
                            && deployment.getStatus().getReadyReplicas().equals(deployment.getStatus().getReplicas());
        }

        Date creationDate = new Date();
        if (service.getMetadata().getCreationTimestamp() != null) {
            creationDate = service.getMetadata().getCreationTimestamp().toDate();
        }

        final String featureName = service.getMetadata().getLabels().get(LABEL_FEATURE_NAME);
        Integer connectionPort = DEFAULT_CONNECTION_PORT;
        CoreService coreService = CoreService.from(featureName);
        Set<Integer> exposedPorts = new HashSet<>();
        if (!ListUtils.isNullOrEmpty(service.getSpec().getPorts())) {
            for (V1ServicePort port : service.getSpec().getPorts()) {
                exposedPorts.add(port.getPort());
            }
        }

        if (!coreService.isUnknown()) {
            // use connection port from core service. A core service only has one port.
            connectionPort = coreService.getConnectionPort();
        } else if (exposedPorts.size() > 0 && !exposedPorts.contains(connectionPort)) {
            // select the first port of the exposed ports and set it as the connection port
            connectionPort = exposedPorts.iterator().next();
        }
        // also add the connection port to the list of exposed ports
        exposedPorts.add(connectionPort);

        return new LabService()
                .setName(featureName)
                .setDockerName(service.getMetadata().getName())
                .setDockerId(service.getMetadata().getName()) //(service.getMetadata().getUid())
                .setDockerImage(dockerImage)
                .setAdminLink(String.format(SERVICE_ADMIN_DEPLOYMENT_URL, service.getMetadata().getName()))
                .setConnectionPort(connectionPort)
                .setExposedPorts(exposedPorts)
                .setLabels(service.getMetadata().getLabels())
                .setModifiedAt(creationDate)
                .setFeatureType(service.getMetadata().getLabels().get(LABEL_FEATURE_TYPE))
                .setConfiguration(configuration)
                .setIsHealthy(healthy);
    }

    private LabJob transformJob(V1Job job, @Nullable V1Pod jobPod) {
        V1Container container = job.getSpec().getTemplate().getSpec().getContainers().get(0);
        Map<String, String> configuration = new HashMap<>();
        String dockerImage = container.getImage();
        for (V1EnvVar envVar : container.getEnv()) {
            configuration.put(envVar.getName(), envVar.getValue());
        }

        Date startedAt = null;
        if (job.getStatus().getStartTime() != null) {
            startedAt = job.getStatus().getStartTime().toDate();
        }
        Date finishedAt = null;
        if (job.getStatus().getCompletionTime() != null) {
            finishedAt = job.getStatus().getCompletionTime().toDate();
        }

        //TODO default state is failed? also contains // getFailed
        String status = LabJob.State.RUNNING.getName();
        if (job.getStatus() != null) {
            if (job.getStatus().getSucceeded() != null && job.getStatus().getSucceeded() == 1) {
                status = LabJob.State.SUCCEEDED.getName();
            } else if (job.getStatus().getActive() != null && job.getStatus().getActive() == 1) {
                status = LabJob.State.RUNNING.getName();
            } else {
                status = LabJob.State.FAILED.getName();
            }
        }

        Integer exitCode = null;
        if (jobPod != null) {
            try {
                // Note: if the pod belonging to a job has multiple containers, it will take the Exit code of the first container in the pod
                exitCode = jobPod.getStatus().getContainerStatuses().get(0).getState().getTerminated().getExitCode();
            } catch(IndexOutOfBoundsException | NullPointerException e) {
                log.warn("Not able to extract exit code from container for job " + job.getMetadata().getName(), e);         
            } catch (Exception e) {
                log.warn("Not able to find pod belonging to job " + job.getMetadata().getName(), e);            
            }
        }

        return new LabJob()
                .setName(job.getMetadata().getLabels().get(LABEL_FEATURE_NAME))
                .setDockerName(job.getMetadata().getName())
                .setDockerId(job.getMetadata().getName()) //(job.getMetadata().getUid())
                .setDockerImage(dockerImage)
                .setAdminLink(String.format(SERVICE_ADMIN_JOB_URL, job.getMetadata().getName()))
                .setConfiguration(configuration)
                .setStartedAt(startedAt)
                .setFinishedAt(finishedAt)
                .setStatus(status)
                .setFeatureType(job.getMetadata().getLabels().get(LABEL_FEATURE_TYPE))
                .setExitCode(exitCode);
        // TODO: set exit code - exit code needs to come from pod
    }

    @Override
    public boolean isLabAvailable() {
        if (!super.isLabAvailable()) {
            return false;
        }

        if (!LabConfig.IS_MANAGED_KUBERNETES_CLUSTER &&
                !isServiceAvailable(CoreService.NFS.getName(), MAX_WAIT_TIME)) {
            log.error("NFS service is not available");
            return false;
        }

        return true;
    }

    private V1Pod getPod(String labelName, String labelValue) throws Exception {
        final String label = String.format("%s=%s", labelName, labelValue);
        // set to null as it prevents getting logs of failed pods / jobs
        final String fieldSelectorRunning = null; //String.format("%s=%s", "status.phase", "Running"); // get only pods that are running
        V1PodList podList = api.listNamespacedPod(K8S_NAMESPACE, "false", null, fieldSelectorRunning,
                                                  label, null, null, null, null);
        if (ListUtils.isNullOrEmpty(podList.getItems())) {
            throw new Exception("Failed to find pod with label " + label);
        }

        if (podList.getItems().size() > 1) {
            log.warn(String.format("getPod should find one pod, not many. For label: -l %s=%s", labelName, labelValue));
            //TODO: if more than one is found, select the one that has status 'Running'. If more running pods exist, select one randomly
        }

        return podList.getItems().get(0);
    }

    private V1Deployment getDeployment(String serviceId, @Nullable String project) throws Exception {
        V1DeploymentList deployments = appsApi.listNamespacedDeployment(K8S_NAMESPACE, "false", null, getNameSelector(serviceId),
                                                                             getProjectSelector(project), 1, null, null, null);

        if (ListUtils.isNullOrEmpty(deployments.getItems())) {
            String labelSelector = getFeatureNameSelector(serviceId);
            if (!StringUtils.isNullOrEmpty(project)) {
                labelSelector = labelSelector + "," + getProjectSelector(project);
            }
            deployments = appsApi.listNamespacedDeployment(K8S_NAMESPACE, "false", null,
                                                           null, labelSelector, 1,
                                                           null, null, null);
            if (ListUtils.isNullOrEmpty(deployments.getItems())) {
                throw new Exception("Failed to find deployment for " + serviceId);
            }
        }

        return deployments.getItems().get(0);
    }

    private static void initKubernetesNamespaceVariable() throws IOException {
        if (!StringUtils.isNullOrEmpty(LabConfig.K8S_NAMESPACE)) {
            K8S_NAMESPACE = LabConfig.K8S_NAMESPACE;
        } else {
            final String namespaceSecretFile = "/var/run/secrets/kubernetes.io/serviceaccount/namespace";
            try {
                K8S_NAMESPACE = new String(Files.readAllBytes(Paths.get(namespaceSecretFile)));
            } catch(IOException e) {
                throw new IOException("Could not read namespace secret file " + namespaceSecretFile);
            }
        }
    }

    private String getProjectSelector(String project) {
        if (StringUtils.isNullOrEmpty(project)) {
            return null;
        }
        return String.format("%s=%s", LABEL_PROJECT, project);
    }

    private String getFeatureNameSelector(String featureName) {
        if (StringUtils.isNullOrEmpty(featureName)) {
            return null;
        }
        return String.format("%s=%s", LABEL_FEATURE_NAME, featureName);
    }

    private String getDockerNameSelector(String name) {
        if (StringUtils.isNullOrEmpty(name)) {
            return null;
        }

        // prefix the name of the service with the correct namespace
        if (!name.startsWith(LAB_NAMESPACE)) {
            name = LAB_NAMESPACE + "-" + name;
        }

        return String.format("%s=%s", LABEL_DOCKER_NAME, name);
    }

    private String getLabNamespaceSelector() {
        return String.format("%s=%s", LABEL_NAMESPACE, LabConfig.LAB_NAMESPACE);
    }

    @Deprecated
    private String getOriginNameSelector(String name) {
        // TODO old compatibility method - use getDockerNameSelector instead
        if (StringUtils.isNullOrEmpty(name)) {
            return null;
        }

        // prefix the name of the service with the correct namespace
        if (!name.startsWith(LAB_NAMESPACE)) {
            name = LAB_NAMESPACE + "-" + name;
        }

        return String.format("%s=%s", ORIGIN_NAME, name);
    }

    private String getNameSelector(String name) {
        if (StringUtils.isNullOrEmpty(name)) {
            return null;
        }
        return String.format("%s=%s", "metadata.name", name);
    }

    private String getWorkspaceMountPath() {
        String hostMountPath = HOST_ROOT_WORKSPACE_DATA_MOUNT_PATH;
        if (StringUtils.isNullOrEmpty(hostMountPath)) {
            hostMountPath = HOST_ROOT_DATA_MOUNT_PATH;
        }
        return hostMountPath;
    }

    private int getPersistentVolumeClaimQuantity(String serviceName) {
        if (CoreService.MINIO.getName().equals(serviceName)) {
            return LabConfig.K8S_PVC_MINIO_STORAGE_LIMIT;
        } else if (CoreService.MONGO.getName().equals(serviceName)) {
            return LabConfig.K8S_PVC_MONGO_STORAGE_LIMIT;
        }

        return 0;
    }

    private String getPersistentVolumeClaimMountInfo(int amountInGb, String mountPath) {
        return amountInGb + "G:" + mountPath;
    }
    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
