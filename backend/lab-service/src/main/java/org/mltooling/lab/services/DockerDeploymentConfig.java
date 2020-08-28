package org.mltooling.lab.services;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.mltooling.core.utils.ListUtils;
import org.mltooling.core.utils.StringUtils;

import javax.annotation.Nullable;
import java.util.*;


public class DockerDeploymentConfig {

    // ================ Constants =========================================== //
    public static String BIND_MOUNT_TYPE = "bind";

    public static String NODE_SELECTOR_MASTER = "master";
    // ================ Members ============================================= //
    private String name;
    // docker image + version
    private String image;
    // contains one cmd command that is called when the container is started
    private List<String> cmd;

    // used in docker local and swarm, ignored in kubernetes
    private List<String> networks = new ArrayList<>();

    // internal path in container that should be persisted, default is /data
    private String volumePath;

    // Flag to describe that service should not be replicated. Currently not really applied. Replication count should be set after deployment
    private boolean replicationAllowed = true;

    // Select on which node (e.g. master) a specific service should be started
    private String nodeSelector;

    // either contains single port, or port:targetPort...
    private Set<String> portsToPublish = new HashSet<>();

    // mount instruction with with type; as default only supports bind mount
    private Multimap<String, String> mounts = LinkedListMultimap.create();

    private Map<String, String> labels = new HashMap<>();
    private Map<String, String> envVariables = new HashMap<>();

    private String featureName; // name is usually with prefix/suffixes
    private FeatureType featureType;

    // all resources are in one common namespace -> default = lab
    private String namespace;

    // use this map for additional service manager specific attributes
    private Map<String, Object> attributes = new HashMap<>();

    // ================ Constructors & Main ================================= //
    public DockerDeploymentConfig(String name, String image) {
        this.name = name;
        this.image = image;
    }

    public DockerDeploymentConfig() {}

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //

    public String getName() {
        return name;
    }

    public DockerDeploymentConfig setName(String name) {
        this.name = name;
        return this;
    }

    public String getImage() {
        return image;
    }

    public DockerDeploymentConfig setImage(String image) {
        this.image = image;
        return this;
    }

    public Set<String> getPortsToPublish() {
        return portsToPublish;
    }

    public DockerDeploymentConfig setPortsToPublish(Set<String> portsToPublish) {
        this.portsToPublish = portsToPublish;
        return this;
    }

    public DockerDeploymentConfig addPortsToPublish(String portsToPublish) {
        this.portsToPublish.add(portsToPublish);
        return this;
    }

    public DockerDeploymentConfig addPortsToPublish(Set<String> portsToPublish) {
        if (this.portsToPublish == null) {
            this.portsToPublish = new HashSet<>();
        }

        if (portsToPublish != null) {
            for (String port : portsToPublish) {
                addPortsToPublish(port);
            }
        }

        return this;
    }

    public String getVolumePath() {
        return volumePath;
    }

    public DockerDeploymentConfig setVolumePath(String volumePath) {
        this.volumePath = volumePath;
        return this;
    }

    public Multimap<String, String> getMounts() {
        return mounts;
    }

    public DockerDeploymentConfig setMounts(Multimap<String, String> mounts) {
        this.mounts = mounts;
        return this;
    }

    public DockerDeploymentConfig addMount(String type, String mount) {
        this.mounts.put(type, mount);
        return this;
    }

    public List<String> getNetworks() {
        return networks;
    }

    public DockerDeploymentConfig addNetwork(String networkName) {
        this.networks.add(networkName);
        return this;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public DockerDeploymentConfig setLabels(Map<String, String> labels) {
        this.labels = labels;
        return this;
    }

    public DockerDeploymentConfig addLabel(String key, String value) {
        this.labels.put(key, value);
        return this;
    }

    public Map<String, String> getEnvVariables() {
        return envVariables;
    }

    public DockerDeploymentConfig setEnvVariables(@Nullable Map<String, String> envVariables) {
        this.envVariables = new HashMap<>();

        this.addEnvVariables(envVariables);

        return this;
    }

    public DockerDeploymentConfig addEnvVariables(@Nullable Map<String, String> envVariables) {
        if (this.envVariables == null) {
            this.envVariables = new HashMap<>();
        }

        if (envVariables != null) {
            for (String key : envVariables.keySet()) {
                addEnvVariable(key, envVariables.get(key));
            }
        }

        return this;
    }

    public DockerDeploymentConfig addEnvVariable(String key, String value) {
        if (value != null) {
            // only add if value is not null
            // TODO do not uppercase key?
            this.envVariables.put(key, value);
        }
        return this;
    }

    public boolean isReplicationAllowed() {
        return replicationAllowed;
    }

    public DockerDeploymentConfig setReplicationAllowed(boolean replicationAllowed) {
        this.replicationAllowed = replicationAllowed;
        return this;
    }

    public FeatureType getFeatureType() {
        return featureType;
    }

    public DockerDeploymentConfig setFeatureType(FeatureType featureType) {
        this.featureType = featureType;
        return this;
    }

    public DockerDeploymentConfig setCmd(String cmd) {
        if (StringUtils.isNullOrEmpty(cmd)) {
            return this;
        }

        // split by whitespaces
        this.cmd = Arrays.asList(cmd.split("\\s+"));
        return this;
    }

    public DockerDeploymentConfig setCmd(List<String> cmd) {
        if (ListUtils.isNullOrEmpty(cmd)) {
            return this;
        }

        this.cmd = cmd;
        return this;
    }

    public List<String> getCmd() {
        return this.cmd;
    }

    public String getFeatureName() {
        return featureName;
    }

    public DockerDeploymentConfig setFeatureName(String featureName) {
        this.featureName = featureName;
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public DockerDeploymentConfig setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public DockerDeploymentConfig setAttributes(@Nullable Map<String, Object> attributes) {
        this.attributes = attributes;
        return this;
    }

    public DockerDeploymentConfig addAttribute(String key, Object value) {
        if (value != null) {
            this.attributes.put(key, value);
        }
        return this;
    }

    public String getNodeSelector() {
        return nodeSelector;
    }

    public DockerDeploymentConfig setNodeSelector(String nodeSelector) {
        this.nodeSelector = nodeSelector;
        return this;
    }

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
