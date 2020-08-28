package org.mltooling.core.lab;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mashape.unirest.http.HttpClientHelper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.http.utils.ClientFactory;
import com.mashape.unirest.http.utils.ResponseUtils;
import com.mashape.unirest.request.HttpRequest;
import org.mltooling.core.api.client.AbstractApiClient;
import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.api.format.StatusMessageFormat;
import org.mltooling.core.api.format.ValueListFormat;
import org.mltooling.core.api.utils.ApiUtils;
import org.mltooling.core.lab.model.*;
import org.mltooling.core.utils.ReflectionUtils;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.core.utils.structures.CloseCallbackInputStream;
import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;
import me.tongfei.progressbar.ProgressBarStyle;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;


public class LabApiClient extends AbstractApiClient<LabApiClient> implements LabApi {

    // ================ Constants =========================================== //
    private static final Logger log = LoggerFactory.getLogger(LabApiClient.class);

    // ================ Members ============================================= //

    // ================ Constructors & Main ================================= //
    public LabApiClient(String serviceUrl) {
        super(serviceUrl, null);
    }

    public LabApiClient(String serviceUrl, String authToken) {
        super(serviceUrl, authToken);
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //
    @Override
    public String getEndpointUrl() {
        try {
            return new URI(getServiceUrl()).resolve(ENDPOINT_PATH).toString();
        } catch (URISyntaxException e) {
            log.error("Failed to resolve endpoint URL", e);
            return "";
        }
    }

    // Projects API

    @Override
    public SingleValueFormat<LabProject> createProject(LabProjectConfig config) {
        return executeRequest(Unirest.post(getEndpointUrl() + METHOD_CREATE_PROJECT)
                                     .header(ApiUtils.CONTENT_TYPE_HEADER, ApiUtils.CONTENT_TYPE_JSON)
                                     .body(new Gson().toJson(config)).getHttpRequest(),
                              new TypeToken<SingleValueFormat<LabProject>>() {}.getType());
    }

    @Override
    public StatusMessageFormat isProjectAvailable(String project) {
        return executeRequest(Unirest.get(getEndpointUrl() + METHOD_PROJECT_AVAILABLE)
                                     .getHttpRequest().routeParam(PARAM_PROJECT, project),
                              new TypeToken<StatusMessageFormat>() {}.getType());
    }

    @Override
    public StatusMessageFormat deleteProject(String project) {
        return executeRequest(Unirest.delete(getEndpointUrl() + METHOD_DELETE_PROJECT).getHttpRequest()
                                     .routeParam(PARAM_PROJECT, project),
                              new TypeToken<StatusMessageFormat>() {}.getType());
    }

    @Override
    public ValueListFormat<LabProject> getProjects() {
        return executeRequest(Unirest.get(getEndpointUrl() + METHOD_GET_ALL_PROJECTS).getHttpRequest(),
                              new TypeToken<ValueListFormat<LabProject>>() {}.getType());
    }

    @Override
    public SingleValueFormat<String> createProjectToken(String project) {
        return executeRequest(Unirest.get(getEndpointUrl() + METHOD_GET_PROJECT_TOKEN)
                                     .routeParam(PARAM_PROJECT, project)
                                     .getHttpRequest(),
                              new TypeToken<SingleValueFormat<String>>() {}.getType());
    }

    @Override
    public SingleValueFormat<LabProject> getProject(String project, @Nullable Boolean expand) {

        HttpRequest request = Unirest.get(getEndpointUrl() + METHOD_GET_PROJECT)
                                     .getHttpRequest().routeParam(PARAM_PROJECT, project);

        if (expand != null) {
            request = request.queryString(PARAM_EXPAND, expand);
        }

        return executeRequest(request, new TypeToken<SingleValueFormat<LabProject>>() {}.getType());
    }

    // Deployment API

    @Override
    public ValueListFormat<LabService> getServices(String project) {
        return executeRequest(Unirest.get(getEndpointUrl() + METHOD_GET_PROJECT_SERVICES).getHttpRequest()
                                     .routeParam(PARAM_PROJECT, project),
                              new TypeToken<ValueListFormat<LabService>>() {}.getType());
    }

    @Override
    public SingleValueFormat<LabService> getService(String project, String service) {
        return executeRequest(Unirest.get(getEndpointUrl() + METHOD_GET_PROJECT_SERVICE)
                                     .getHttpRequest().routeParam(PARAM_PROJECT, project)
                                     .routeParam(PARAM_SERVICE, service),
                              new TypeToken<SingleValueFormat<LabService>>() {}.getType());
    }

    @Override
    public StatusMessageFormat deleteService(String project, String service) {
        return executeRequest(Unirest.delete(getEndpointUrl() + METHOD_DELETE_PROJECT_SERVICE)
                                     .getHttpRequest().routeParam(PARAM_PROJECT, project)
                                     .routeParam(PARAM_SERVICE, service),
                              new TypeToken<StatusMessageFormat>() {}.getType());
    }

    @Override
    public SingleValueFormat<String> getServiceLogs(String project, String service) {
        return executeRequest(Unirest.get(getEndpointUrl() + METHOD_GET_PROJECT_SERVICE_LOGS)
                                     .getHttpRequest().routeParam(PARAM_PROJECT, project)
                                     .routeParam(PARAM_SERVICE, service),
                              new TypeToken<SingleValueFormat<String>>() {}.getType());
    }

    @Override
    public SingleValueFormat<LabService> deployService(String project, String image, @Nullable String name, @Nullable Map<String, String> config) {

        HttpRequest request = Unirest.post(getEndpointUrl() + METHOD_DEPLOY_SERVICE)
                                     .body(config != null ? new Gson().toJson(config) : "").getHttpRequest()
                                     .header(ApiUtils.CONTENT_TYPE_HEADER, ApiUtils.CONTENT_TYPE_JSON)
                                     .routeParam(PARAM_PROJECT, project)
                                     .queryString(PARAM_DOCKER_IMAGE, image);

        if (name != null) {
            request = request.queryString(PARAM_NAME, name);
        }

        return executeRequest(request, new TypeToken<SingleValueFormat<LabService>>() {}.getType());
    }

    @Override
    public SingleValueFormat<LabService> deployModel(String project, String fileKey, @Nullable String name, @Nullable Map<String, String> config) {
        HttpRequest request = Unirest.post(getEndpointUrl() + METHOD_DEPLOY_MODEL)
                                     .body(config != null ? new Gson().toJson(config) : "").getHttpRequest()
                                     .header(ApiUtils.CONTENT_TYPE_HEADER, ApiUtils.CONTENT_TYPE_JSON)
                                     .routeParam(PARAM_PROJECT, project)
                                     .queryString(PARAM_FILE_KEY, fileKey);

        if (name != null) {
            request = request.queryString(PARAM_NAME, name);
        }

        return executeRequest(request, new TypeToken<SingleValueFormat<LabService>>() {}.getType());
    }

    // Jobs API

    @Override
    public ValueListFormat<LabJob> getJobs(String project) {
        return executeRequest(Unirest.get(getEndpointUrl() + METHOD_GET_ALL_JOBS).getHttpRequest()
                                     .routeParam(PARAM_PROJECT, project),
                              new TypeToken<ValueListFormat<LabJob>>() {}.getType());
    }

    @Override
    public ValueListFormat<LabScheduledJob> getScheduledJobs(String project) {
        return executeRequest(Unirest.get(getEndpointUrl() + METHOD_GET_SCHEDULED_JOBS).getHttpRequest()
                                     .routeParam(PARAM_PROJECT, project),
                              new TypeToken<ValueListFormat<LabScheduledJob>>() {}.getType());
    }

    @Override
    public StatusMessageFormat deleteScheduledJob(String project, String jobId) {
        return executeRequest(Unirest.delete(getEndpointUrl() + METHOD_DELETE_SCHEDULED_JOBS).getHttpRequest()
                                     .routeParam(PARAM_PROJECT, project)
                                     .routeParam(PARAM_JOB, jobId),
                              new TypeToken<StatusMessageFormat>() {}.getType());
    }

    @Override
    public SingleValueFormat<LabJob> getJob(String project, String job) {
        return executeRequest(Unirest.get(getEndpointUrl() + METHOD_GET_JOB)
                                     .getHttpRequest().routeParam(PARAM_PROJECT, project)
                                     .routeParam(PARAM_JOB, job),
                              new TypeToken<SingleValueFormat<LabJob>>() {}.getType());
    }

    @Override
    public StatusMessageFormat deleteJob(String project, String job) {
        return executeRequest(Unirest.delete(getEndpointUrl() + METHOD_DELETE_JOB)
                                     .getHttpRequest().routeParam(PARAM_PROJECT, project)
                                     .routeParam(PARAM_JOB, job),
                              new TypeToken<StatusMessageFormat>() {}.getType());
    }

    @Override
    public SingleValueFormat<String> getJobLogs(String project, String job) {
        return executeRequest(Unirest.get(getEndpointUrl() + METHOD_GET_JOB_LOGS)
                                     .getHttpRequest().routeParam(PARAM_PROJECT, project)
                                     .routeParam(PARAM_JOB, job),
                              new TypeToken<SingleValueFormat<String>>() {}.getType());
    }

    @Override
    public SingleValueFormat<LabJob> deployJob(String project, String image, @Nullable String name, @Nullable String schedule, @Nullable Map<String, String> config) {
        HttpRequest request = Unirest.post(getEndpointUrl() + METHOD_DEPLOY_JOB)
                                     .body(config != null ? new Gson().toJson(config) : "").getHttpRequest()
                                     .header(ApiUtils.CONTENT_TYPE_HEADER, ApiUtils.CONTENT_TYPE_JSON)
                                     .routeParam(PARAM_PROJECT, project)
                                     .queryString(PARAM_DOCKER_IMAGE, image);

        if (name != null) {
            request = request.queryString(PARAM_NAME, name);
        }

        if (schedule != null) {
            request = request.queryString(PARAM_SCHEDULE, schedule);
        }

        return executeRequest(request, new TypeToken<SingleValueFormat<LabJob>>() {}.getType());
    }

    // Experiments API

    @Override
    public ValueListFormat<LabExperiment> getExperiments(String project) {
        return executeRequest(Unirest.get(getEndpointUrl() + METHOD_GET_EXPERIMENTS).getHttpRequest()
                                     .routeParam(PARAM_PROJECT, project),
                              new TypeToken<ValueListFormat<LabExperiment>>() {}.getType());
    }

    @Override
    public StatusMessageFormat deleteExperiment(String project, String experimentId) {
        return executeRequest(Unirest.delete(getEndpointUrl() + METHOD_DELETE_EXPERIMENT).getHttpRequest()
                                     .routeParam(PARAM_PROJECT, project)
                                     .queryString(PARAM_EXPERIMENT, experimentId),
                              new TypeToken<StatusMessageFormat>() {}.getType());
    }

    @Override
    public SingleValueFormat<String> syncExperiment(String project, LabExperiment experiment) {
        return executeRequest(Unirest.post(getEndpointUrl() + METHOD_SYNC_EXPERIMENT)
                                     .body(new Gson().toJson(experiment)).getHttpRequest()
                                     .header(ApiUtils.CONTENT_TYPE_HEADER, ApiUtils.CONTENT_TYPE_JSON)
                                     .routeParam(PARAM_PROJECT, project),
                              new TypeToken<SingleValueFormat<String>>() {}.getType());
    }

    // Remote File API

    @Override
    public ValueListFormat<LabFile> getFiles(String project, @Nullable LabFileDataType dataType, @Nullable String prefix, @Nullable Boolean aggregateVersions) {

        HttpRequest request = Unirest.get(getEndpointUrl() + METHOD_GET_FILES).getHttpRequest()
                                     .routeParam(PARAM_PROJECT, project);

        if (prefix != null) {
            request = request.queryString(PARAM_PREFIX, prefix);
        }

        if (dataType != null && !dataType.isUnknown()) {
            request = request.queryString(PARAM_DATA_TYPE, dataType.getName());
        }

        if (aggregateVersions != null) {
            request = request.queryString(PARAM_AGGREGATE_VERSIONS, aggregateVersions);
        }

        return executeRequest(request, new TypeToken<ValueListFormat<LabFile>>() {}.getType());
    }

    @Override
    public StatusMessageFormat deleteFile(String project, String fileKey, @Nullable Integer keepLatestVersion) {
        HttpRequest request = Unirest.delete(getEndpointUrl() + METHOD_DELETE_FILE).getHttpRequest()
                                     .routeParam(PARAM_PROJECT, project)
                                     .queryString(PARAM_FILE_KEY, fileKey);

        if (keepLatestVersion != null) {
            request = request.queryString(PARAM_KEEP_LATEST_VERSIONS, keepLatestVersion);
        }

        return executeRequest(request, new TypeToken<StatusMessageFormat>() {}.getType());
    }

    @Override
    public SingleValueFormat<LabFile> getFileInfo(String project, String fileKey) {
        return executeRequest(Unirest.get(getEndpointUrl() + METHOD_GET_FILE_INFO).getHttpRequest()
                                     .routeParam(PARAM_PROJECT, project)
                                     .queryString(PARAM_FILE_KEY, fileKey), new TypeToken<SingleValueFormat<LabFile>>() {}.getType());
    }

    @Override
    public SingleValueFormat<String> uploadFile(String project, InputStream fileStream, String fileName, LabFileDataType dataType, @Nullable Boolean versioning, @Nullable Map<String, String> metadata) {
        Map<String, String> processedMetadata = new HashMap<>();

        // Add metadata as headers, to be valid, metadata needs to have the x-amz-meta prefix (S3 conform)
        if (metadata != null) {
            for (String field : metadata.keySet()) {
                String value = metadata.get(field);
                if (!field.startsWith(FILE_METADATA_PREFIX)) {
                    field = FILE_METADATA_PREFIX + field;
                }
                processedMetadata.put(field, value);
            }
        }

        InputStream progressInputStream = ProgressBar.wrap(fileStream, new ProgressBarBuilder()
                .showSpeed()
                .setUnit("MB", 1048576L)
                .setTaskName("Uploading " + fileName + ": ")
                .setUpdateIntervalMillis(5000)
                .setStyle(ProgressBarStyle.UNICODE_BLOCK)
                .setPrintStream(System.out));

        HttpRequest request = Unirest.post(getEndpointUrl() + METHOD_UPLOAD_FILE)
                                     .field(PARAM_FILE, progressInputStream, ContentType.APPLICATION_OCTET_STREAM, fileName).getHttpRequest()
                                     .routeParam(PARAM_PROJECT, project)
                                     .queryString(PARAM_DATA_TYPE, dataType.getName())
                                     .queryString(PARAM_FILE_NAME, fileName);
        if (versioning != null) {
            request = request.queryString(PARAM_VERSIONING, versioning);
        }

        if (metadata != null) {
            request = request.headers(processedMetadata);
        }

        SingleValueFormat<String> response = executeRequest(request, new TypeToken<SingleValueFormat<String>>() {}.getType());
        try {
            progressInputStream.close();
        } catch (Exception e) {
            // do nothing
        }

        return response;
    }

    @Override
    public SingleValueFormat<LabFile> downloadFile(String project, String fileKey) {
        if (!StringUtils.isNullOrEmpty(authToken)) {
            // set auth token if not already provided -> needs to be done here because this is not using the execute method
            this.setAuthToken(authToken);
        }

        try {
            InputStream fileStream = requestFile(Unirest.get(getEndpointUrl() + METHOD_DOWNLOAD_FILE).getHttpRequest()
                                                        .routeParam(PARAM_PROJECT, project)
                                                        .queryString(PARAM_FILE_KEY, fileKey)
                                                        .headers(getHeaders()));
            SingleValueFormat<LabFile> fileInfo = getFileInfo(project, fileKey);
            fileInfo.getData().setFileStream(fileStream);
            return fileInfo;
        } catch (Exception e) {
            log.warn("Exception while requesting data: " + e.getMessage()); // Don't log stacktrace hered#
            SingleValueFormat<LabFile> unifiedFormat = new SingleValueFormat<>();
            unifiedFormat.setErrorStatus(e);
            return unifiedFormat;
        }
    }

    // ================ Public Methods ====================================== //

    // ================ Private Methods ===================================== //
    public static InputStream requestFile(HttpRequest request) throws Exception {
        HttpRequestBase requestObj = ReflectionUtils.invokeMethod(HttpClientHelper.class, (Object) null, "prepareRequest", (HttpRequest) request, false);
        HttpClient client = ClientFactory.getHttpClient();
        org.apache.http.HttpResponse response;

        try {
            response = client.execute(requestObj);
            if (response.getStatusLine().getStatusCode() < HttpStatus.SC_OK || response.getStatusLine().getStatusCode() >= HttpStatus.SC_MULTIPLE_CHOICES) {
                log.info("Download was unsuccessful with status: " + response.getStatusLine().getReasonPhrase() + "(" + response.getStatusLine().getStatusCode() + ")");
                return null;
            } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
                log.info("Requested file on server and client match by version and size, no need to download.");
                return null;
            } else {
                HttpEntity responseEntity = response.getEntity();

                InputStream responseInputStream = responseEntity.getContent();
                if (ResponseUtils.isGzipped(responseEntity.getContentEncoding())) {
                    responseInputStream = new GZIPInputStream(responseEntity.getContent());
                }

                return new CloseCallbackInputStream(responseInputStream, new CloseCallbackInputStream.CloseCallback() {

                    @Override
                    public void close() {
                        requestObj.releaseConnection();
                    }
                });
            }
        } catch (Exception e) {
            requestObj.releaseConnection();
            throw new UnirestException(e);
        }
    }

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
