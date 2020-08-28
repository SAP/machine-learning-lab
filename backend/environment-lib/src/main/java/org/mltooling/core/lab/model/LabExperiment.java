package org.mltooling.core.lab.model;

import org.mltooling.core.utils.StringUtils;
import io.swagger.annotations.ApiModelProperty;

import java.util.*;


public class LabExperiment {

    // ================ Constants =========================================== //
    public enum State {
        INITIALIZED("initialized"),
        QUEUED("queued"),
        RUNNING("running"),
        COMPLETED("completed"),
        DEAD("dead"),
        INTERRUPTED("interrupted"),
        FAILED("failed"),
        UNKNOWN("unknown");

        private String name;

        State(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return getName();
        }

        public static State from(String stateStr) {
            if (StringUtils.isNullOrEmpty(stateStr)) {
                return UNKNOWN;
            }

            for (State state : State.values()) {
                if (stateStr.equalsIgnoreCase(state.getName())) {
                    return state;
                }
            }
            return UNKNOWN;
        }
    }


    // ================ Members ============================================= //
    public static class HostInfo {

        private String hostname;
        private String os;
        private String cpu;
        private String cpuCores;
        private String memorySize;
        private String pythonVersion;
        private String pythonCompiler;
        private String pythonImpl; // Cython, IronPython, Jython, PyPy
        private String workspaceVersion;
        private List<String> gpus; //cpu & gpu

        public String getHostname() {
            return hostname;
        }

        public HostInfo setHostname(String hostname) {
            this.hostname = hostname;
            return this;
        }

        public String getOs() {
            return os;
        }

        public HostInfo setOs(String os) {
            this.os = os;
            return this;
        }

        public String getPythonVersion() {
            return pythonVersion;
        }

        public HostInfo setPythonVersion(String pythonVersion) {
            this.pythonVersion = pythonVersion;
            return this;
        }

        public List<String> getGpus() {
            return gpus;
        }

        public HostInfo setGpus(List<String> gpus) {
            this.gpus = gpus;
            return this;
        }

        public HostInfo addGpu(String device) {
            if (this.gpus == null) {
                this.gpus = new ArrayList<>();
            }

            this.gpus.add(device);
            return this;
        }

        public String getCpu() {
            return cpu;
        }

        public HostInfo setCpu(String cpu) {
            this.cpu = cpu;
            return this;
        }

        public String getCpuCores() {
            return cpuCores;
        }

        public HostInfo setCpuCores(String cpuCores) {
            this.cpuCores = cpuCores;
            return this;
        }

        public String getMemorySize() {
            return memorySize;
        }

        public HostInfo setMemorySize(String memorySize) {
            this.memorySize = memorySize;
            return this;
        }

        public String getWorkspaceVersion() {
            return workspaceVersion;
        }

        public HostInfo setWorkspaceVersion(String workspaceVersion) {
            this.workspaceVersion = workspaceVersion;
            return this;
        }

        public String getPythonCompiler() {
            return pythonCompiler;
        }

        public HostInfo setPythonCompiler(String pythonCompiler) {
            this.pythonCompiler = pythonCompiler;
            return this;
        }

        public String getPythonImpl() {
            return pythonImpl;
        }

        public HostInfo setPythonImpl(String pythonImpl) {
            this.pythonImpl = pythonImpl;
            return this;
        }
    }


    public static class GitInfo {

        private String commit;
        private String remoteUrl;
        private String branch;
        private String userName;
        private String userEmail;

        public String getCommit() {
            return commit;
        }

        public GitInfo setCommit(String commit) {
            this.commit = commit;
            return this;
        }

        public String getRemoteUrl() {
            return remoteUrl;
        }

        public GitInfo setRemoteUrl(String remoteUrl) {
            this.remoteUrl = remoteUrl;
            return this;
        }

        public String getUserName() {
            return userName;
        }

        public GitInfo setUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public String getUserEmail() {
            return userEmail;
        }

        public GitInfo setUserEmail(String userEmail) {
            this.userEmail = userEmail;
            return this;
        }

        public String getBranch() {
            return branch;
        }

        public GitInfo setBranch(String branch) {
            this.branch = branch;
            return this;
        }
    }


    public static class ExperimentResources {

        private List<String> input; // = requested = key
        private List<String> artifacts; // can contain any string specification for artifacts, do not resolve as key
        private List<String> output; // = uploaded = key

        private String experimentDir; // the remote directory of the experiment
        private String experimentBackup;
        private String sourceCode; // full source code repository (if it is a git repo)
        private String sourceScript; // only the calling script (single file)
        private String tensorboardLogs; // tensorboard logging directory
        private String stdout; // single stdout file

        public List<String> getInput() {
            return input;
        }

        public ExperimentResources setInput(List<String> libraries) {
            this.input = libraries;
            return this;
        }

        public ExperimentResources addInput(String library) {
            if (this.input == null) {
                this.input = new ArrayList<>();
            }

            this.input.add(library);
            return this;
        }

        public List<String> getOutput() {
            return output;
        }

        public ExperimentResources setOutput(List<String> others) {
            this.output = others;
            return this;
        }

        public ExperimentResources addOutput(String others) {
            if (this.output == null) {
                this.output = new ArrayList<>();
            }

            this.output.add(others);
            return this;
        }

        public String getExperimentDir() {
            return experimentDir;
        }

        public ExperimentResources setExperimentDir(String experimentDir) {
            this.experimentDir = experimentDir;
            return this;
        }

        public String getSourceCode() {
            return sourceCode;
        }

        public ExperimentResources setSourceCode(String sourceCode) {
            this.sourceCode = sourceCode;
            return this;
        }

        public String getTensorboardLogs() {
            return tensorboardLogs;
        }

        public ExperimentResources setTensorboardLogs(String tensorboardLogs) {
            this.tensorboardLogs = tensorboardLogs;
            return this;
        }

        public String getStdout() {
            return stdout;
        }

        public ExperimentResources setStdout(String stdout) {
            this.stdout = stdout;
            return this;
        }

        public List<String> getArtifacts() {
            return artifacts;
        }

        public ExperimentResources setArtifacts(List<String> artifacts) {
            this.artifacts = artifacts;
            return this;
        }

        public String getExperimentBackup() {
            return experimentBackup;
        }

        public void setExperimentBackup(String experimentBackup) {
            this.experimentBackup = experimentBackup;
        }

        public String getSourceScript() {
            return sourceScript;
        }

        public void setSourceScript(String sourceScript) {
            this.sourceScript = sourceScript;
        }
    }


    private String key;
    private String groupKey;
    private String name;
    private String operator;
    private String scriptName;
    private String scriptType;
    private String project;
    @ApiModelProperty(dataType = "java.lang.Long")
    private Date startedAt;
    @ApiModelProperty(dataType = "java.lang.Long")
    private Date finishedAt;
    @ApiModelProperty(dataType = "java.lang.Long")
    private Date updatedAt;
    private Long duration;
    private String status;
    private String command;
    private Set<String> tags;
    private String note;
    private String clientVersion;

    private GitInfo git;
    private HostInfo host;
    private List<String> dependencies = new ArrayList<>();

    private ExperimentResources resources;
    private Map<String, Object> parameters = new HashMap<>();
    private Map<String, Object> metrics = new HashMap<>();
    private Map<String, Object> others = new HashMap<>();
    private String result;

    // ================ Constructors & Main ================================= //

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //
    public String getKey() {
        return key;
    }

    public LabExperiment setKey(String key) {
        this.key = key;
        return this;
    }

    public String getName() {
        return name;
    }

    public LabExperiment setName(String name) {
        this.name = name;
        return this;
    }

    public String getScriptName() {
        return scriptName;
    }

    public LabExperiment setScriptName(String scriptName) {
        this.scriptName = scriptName;
        return this;
    }

    public String getScriptType() {
        return scriptType;
    }

    public LabExperiment setScriptType(String scriptType) {
        this.scriptType = scriptType;
        return this;
    }

    public String getOperator() {
        return operator;
    }

    public LabExperiment setOperator(String operator) {
        this.operator = operator;
        return this;
    }

    public String getClientVersion() {
        return clientVersion;
    }

    public LabExperiment setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
        return this;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public LabExperiment setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
        return this;
    }

    public Date getFinishedAt() {
        return finishedAt;
    }

    public LabExperiment setFinishedAt(Date finishedAt) {
        this.finishedAt = finishedAt;
        return this;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getDuration() {
        return duration;
    }

    public LabExperiment setDuration(Long duration) {
        this.duration = duration;
        return this;
    }

    public String getNote() {
        return note;
    }

    public LabExperiment setNote(String note) {
        this.note = note;
        return this;
    }

    @ApiModelProperty(dataType = "java.lang.String")
    public State getStatus() {
        return State.from(status);
    }

    public LabExperiment setStatus(State status) {
        this.status = status.getName();
        return this;
    }

    public String getCommand() {
        return command;
    }

    public LabExperiment setCommand(String command) {
        this.command = command;
        return this;
    }

    public GitInfo getGit() {
        return git;
    }

    public LabExperiment setGit(GitInfo git) {
        this.git = git;
        return this;
    }

    public HostInfo getHost() {
        return host;
    }

    public LabExperiment setHost(HostInfo host) {
        this.host = host;
        return this;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public LabExperiment setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
        return this;
    }

    public LabExperiment addDependency(String dependency) {
        if (this.dependencies == null) {
            this.dependencies = new ArrayList<>();
        }

        this.dependencies.add(dependency);
        return this;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public LabExperiment setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
        return this;
    }

    public LabExperiment addConfig(String configKey, Object configValue) {
        if (this.parameters == null) {
            this.parameters = new HashMap<>();
        }

        this.parameters.put(configKey, configValue);
        return this;
    }

    public Map<String, Object> getMetrics() {
        return metrics;
    }

    public LabExperiment addMetric(String metricKey, Object metricValue) {
        if (this.metrics == null) {
            this.metrics = new HashMap<>();
        }

        this.metrics.put(metricKey, metricValue);
        return this;
    }

    public LabExperiment setMetrics(Map<String, Object> metrics) {
        this.metrics = metrics;
        return this;
    }

    public String getResult() {
        return result;
    }

    public LabExperiment setResult(String result) {
        this.result = result;
        return this;
    }

    public String getProject() {
        return project;
    }

    public LabExperiment setProject(String project) {
        this.project = project;
        return this;
    }

    public ExperimentResources getResources() {
        return resources;
    }

    public LabExperiment setResources(ExperimentResources resources) {
        this.resources = resources;
        return this;
    }

    public Map<String, Object> getOthers() {
        return others;
    }

    public LabExperiment setOthers(Map<String, Object> others) {
        this.others = others;
        return this;
    }

    public LabExperiment addOther(String key, Object value) {
        if (this.others == null) {
            this.others = new HashMap<>();
        }

        this.others.put(key, value);
        return this;
    }

    public String getGroupKey() {
        return groupKey;
    }

    public LabExperiment setGroupKey(String groupKey) {
        this.groupKey = groupKey;
        return this;
    }

    public Set<String> getTags() {
        return tags;
    }

    public LabExperiment setTags(Set<String> tags) {
        this.tags = tags;
        return this;
    }

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
