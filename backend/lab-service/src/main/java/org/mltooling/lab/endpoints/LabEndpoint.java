package org.mltooling.lab.endpoints;

import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.api.format.StatusMessageFormat;
import org.mltooling.core.api.format.ValueListFormat;
import org.mltooling.core.env.handler.FileHandlerUtils;
import org.mltooling.core.lab.LabApi;
import org.mltooling.core.service.params.DefaultHeaderFields;
import org.mltooling.core.service.utils.AbstractApiEndpoint;
import org.mltooling.core.service.utils.UnifiedResponseFactory;
import org.mltooling.core.lab.model.*;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.lab.api.LabApiHandler;
import org.mltooling.lab.authorization.AuthorizationManager;
import io.swagger.annotations.*;
import org.glassfish.jersey.media.multipart.ContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.pac4j.jax.rs.annotations.Pac4JProfile;
import org.pac4j.jax.rs.annotations.Pac4JSecurity;
import org.pac4j.mongo.profile.MongoProfile;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Api(value = LabApi.ENDPOINT_PATH, tags = "projects")
@Path(LabApi.ENDPOINT_PATH)
@Pac4JSecurity(clients = {
        AuthorizationManager.PAC4J_CLIENT_COOKIE,
        AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_IS_AUTHENTICATED)
public class LabEndpoint extends AbstractApiEndpoint<LabEndpoint> {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //
    private LabApiHandler labHandler;

    // ================ Constructors & Main ================================= //
    public LabEndpoint(@Context UriInfo uriInfo, @Context HttpHeaders httpHeaders) {
        super(uriInfo, httpHeaders);
        labHandler = new LabApiHandler();
        registerHandler(labHandler);
    }
    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //
    // Projects API

    @POST
    @Path(LabApi.METHOD_CREATE_PROJECT)
    @ApiOperation(value = "Create a new project.", response = LabProjectResponse.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_IS_AUTHENTICATED)
    public Response createProject(@ApiParam(value = "Project Configuration", required = true) LabProjectConfig config,
                                  @Pac4JProfile MongoProfile commonProfile,
                                  @BeanParam DefaultHeaderFields defaultHeaders) {
        if (commonProfile == null) {
            log.warn("User profile not injected.");
            return UnifiedResponseFactory.getErrorResponse(Response.Status.UNAUTHORIZED, "Could not resolve profile from credentials.");
        }

        labHandler.setAuthProfile(commonProfile);
        return UnifiedResponseFactory.getResponse(labHandler.createProject(config));
    }

    @GET
    @Path(LabApi.METHOD_PROJECT_AVAILABLE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Checks if a project name is available for project creation .", response = StatusMessageFormat.class)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_IS_AUTHENTICATED)
    public Response isProjectAvailable(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                                       @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.isProjectAvailable(project));
    }

    @DELETE
    @Path(LabApi.METHOD_DELETE_PROJECT)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete a project and all its associated networks, services & data.", response = StatusMessageFormat.class)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response deleteProject(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                                  @Pac4JProfile MongoProfile commonProfile,
                                  @BeanParam DefaultHeaderFields defaultHeaders) {
        labHandler.setAuthProfile(commonProfile);
        return UnifiedResponseFactory.getResponse(labHandler.deleteProject(project));
    }

    @GET
    @Path(LabApi.METHOD_GET_ALL_PROJECTS)
    @ApiOperation(value = "Get all available projects with details.", response = ListOfLabProjectsResponse.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_IS_AUTHENTICATED)
    public Response getProjects(@Pac4JProfile MongoProfile commonProfile,
                                @BeanParam DefaultHeaderFields defaultHeaders) {
        if (commonProfile == null) {
            log.warn("User profile not injected.");
            return UnifiedResponseFactory.getErrorResponse(Response.Status.UNAUTHORIZED, "Could not resolve profile from credentials.");
        }

        labHandler.setAuthProfile(commonProfile);
        return UnifiedResponseFactory.getResponse(labHandler.getProjects());
    }

    @GET
    @Path(LabApi.METHOD_GET_PROJECT_TOKEN)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get project token for the specified project.", response = StringResponse.class)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response createProjectToken(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                                       @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.createProjectToken(project));
    }

    @GET
    @Path(LabApi.METHOD_GET_PROJECT)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get details for the specified project.", response = LabProjectResponse.class)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response getProject(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                               @ApiParam(value = "Expand Information (files, services, experiments...)", required = false) @QueryParam(LabApi.PARAM_EXPAND) Boolean expand,
                               @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.getProject(project, expand));
    }

    // Deployment API

    @GET
    @Path(LabApi.METHOD_GET_PROJECT_SERVICES)
    @ApiOperation(value = "Get all services of a project with details and general statistics.", response = ListOfLabServicesResponse.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response getServices(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                                @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.getServices(project));
    }

    @GET
    @Path(LabApi.METHOD_GET_PROJECT_SERVICE)
    @ApiOperation(value = "Get a specific project service by name or type.", response = LabServiceResponse.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response getService(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                               @ApiParam(value = "Service Name or Type", required = true) @PathParam(LabApi.PARAM_SERVICE) String service,
                               @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.getService(project, service));
    }

    @DELETE
    @Path(LabApi.METHOD_DELETE_PROJECT_SERVICE)
    @ApiOperation(value = "Delete a specific project service by name or type.", response = StatusMessageFormat.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response deleteService(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                                  @ApiParam(value = "Service Name or Type", required = true) @PathParam(LabApi.PARAM_SERVICE) String service,
                                  @Pac4JProfile MongoProfile commonProfile,
                                  @BeanParam DefaultHeaderFields defaultHeaders) {
        labHandler.setAuthProfile(commonProfile);
        return UnifiedResponseFactory.getResponse(labHandler.deleteService(project, service));
    }

    @GET
    @Path(LabApi.METHOD_GET_PROJECT_SERVICE_LOGS)
    @ApiOperation(value = "Get the logs for a service.", response = StringResponse.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response getServiceLogs(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                                   @ApiParam(value = "Service Name or Type", required = true) @PathParam(LabApi.PARAM_SERVICE) String service,
                                   @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.getServiceLogs(project, service));
    }

    @POST
    @Path(LabApi.METHOD_DEPLOY_SERVICE)
    @ApiOperation(value = "Deploy a service for a specified project based on a provided image.", response = LabServiceResponse.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response deployService(@ApiParam(value = "Service Configuration", required = false) Map<String, String> config,
                                  @ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                                  @ApiParam(value = "Image Name", required = true) @QueryParam(LabApi.PARAM_DOCKER_IMAGE) String image,
                                  @ApiParam(value = "Service Name", required = false) @QueryParam(LabApi.PARAM_NAME) String name,
                                  @Pac4JProfile MongoProfile commonProfile,
                                  @BeanParam DefaultHeaderFields defaultHeaders) {
        if (commonProfile == null) {
            log.warn("User profile not injected.");
            return UnifiedResponseFactory.getErrorResponse(Response.Status.UNAUTHORIZED, "Could not resolve profile from credentials.");
        }

        labHandler.setAuthProfile(commonProfile);
        return UnifiedResponseFactory.getResponse(labHandler.deployService(project, image, name, config));
    }

    @POST
    @Path(LabApi.METHOD_DEPLOY_MODEL)
    @ApiOperation(value = "Deploy a model as a service for a specified project.", response = LabServiceResponse.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response deployModel(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                                @ApiParam(value = "Model Key", required = true) @QueryParam(LabApi.PARAM_FILE_KEY) String fileKey,
                                @ApiParam(value = "JSON config containing the environment variables to overwrite the default", required = false) Map<String, String> config,
                                @ApiParam(value = "Service Name", required = false) @QueryParam(LabApi.PARAM_NAME) String name,
                                @Pac4JProfile MongoProfile commonProfile,
                                @BeanParam DefaultHeaderFields defaultHeaders) {
        if (commonProfile == null) {
            log.warn("User profile not injected.");
            return UnifiedResponseFactory.getErrorResponse(Response.Status.UNAUTHORIZED, "Could not resolve profile from credentials.");
        }

        labHandler.setAuthProfile(commonProfile);
        return UnifiedResponseFactory.getResponse(labHandler.deployModel(project, fileKey, name, config));
    }

    // Jobs API
    @GET
    @Path(LabApi.METHOD_GET_ALL_JOBS)
    @ApiOperation(value = "Get all jobs of a project with details and general statistics.", response = ListOfLabJobsResponse.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response getJobs(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                            @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.getJobs(project));
    }

    @GET
    @Path(LabApi.METHOD_GET_SCHEDULED_JOBS)
    @ApiOperation(value = "Get all scheduled jobs of a project.", response = ListOfLabScheduledJobsResponse.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response getScheduledJobs(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                                     @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.getScheduledJobs(project));
    }

    @DELETE
    @Path(LabApi.METHOD_DELETE_SCHEDULED_JOBS)
    @ApiOperation(value = "Remove a scheduled job.", response = StatusMessageFormat.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response deleteScheduledJob(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                                       @ApiParam(value = "Job ID", required = true) @PathParam(LabApi.PARAM_JOB) String jobId,
                                       @Pac4JProfile MongoProfile commonProfile) {
        labHandler.setAuthProfile(commonProfile);
        return UnifiedResponseFactory.getResponse(labHandler.deleteScheduledJob(project, jobId));
    }

    @GET
    @Path(LabApi.METHOD_GET_JOB)
    @ApiOperation(value = "Get a specific project job by name or type.", response = LabJobResponse.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response getJob(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                           @ApiParam(value = "Job Name or Id", required = true) @PathParam(LabApi.PARAM_JOB) String job,
                           @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.getJob(project, job));
    }

    @DELETE
    @Path(LabApi.METHOD_DELETE_JOB)
    @ApiOperation(value = "Deletes a job from a project.", response = StatusMessageFormat.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response deleteJob(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                              @ApiParam(value = "Job Name or Id", required = true) @PathParam(LabApi.PARAM_JOB) String job,
                              @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.deleteJob(project, job));
    }

    @GET
    @Path(LabApi.METHOD_GET_JOB_LOGS)
    @ApiOperation(value = "Get the logs for a job.", response = StringResponse.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response getJobLogs(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                               @ApiParam(value = "Job Name or Id", required = true) @PathParam(LabApi.PARAM_JOB) String job,
                               @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.getJobLogs(project, job));
    }

    @POST
    @Path(LabApi.METHOD_DEPLOY_JOB)
    @ApiOperation(value = "Deploy a job for a specified project based on a provided image.", response = LabJobResponse.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response deployJob(@ApiParam(value = "Job Configuration", required = false) Map<String, String> config,
                              @ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                              @ApiParam(value = "Image Name", required = true) @QueryParam(LabApi.PARAM_DOCKER_IMAGE) String image,
                              @ApiParam(value = "Cron Schedule in UNIX format. If specified, the job is executed repeatedly according to the cron definition. A job cannot run "
                                      + "more often than once a minute.", required = false) @QueryParam(LabApi.PARAM_SCHEDULE) String schedule,
                              @ApiParam(value = "Job Name", required = false) @QueryParam(LabApi.PARAM_NAME) String name,
                              @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.deployJob(project, image, name, schedule, config));
    }

    // Experiments API

    @GET
    @Path(LabApi.METHOD_GET_EXPERIMENTS)
    @ApiOperation(value = "Get all experiments of a project with details.", response = ListOfLabExperimentsResponse.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response getExperiments(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                                   @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.getExperiments(project));
    }

    @DELETE
    @Path(LabApi.METHOD_DELETE_EXPERIMENT)
    @ApiOperation(value = "Deletes an experiment from a specified project.", response = StatusMessageFormat.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response deleteExperiment(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                                     @ApiParam(value = "Experiment ID", required = true) @QueryParam(LabApi.PARAM_EXPERIMENT) String experimentId,
                                     @Pac4JProfile MongoProfile commonProfile,
                                     @BeanParam DefaultHeaderFields defaultHeaders) {
        labHandler.setAuthProfile(commonProfile);
        return UnifiedResponseFactory.getResponse(labHandler.deleteExperiment(project, experimentId));
    }

    @POST
    @Path(LabApi.METHOD_SYNC_EXPERIMENT)
    @ApiOperation(value = "Sync an experiment to the experiments DB of a project.", response = StringResponse.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return id of experiment in db"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response syncExperiment(@ApiParam(value = "Experiment", required = true) LabExperiment experiment,
                                   @ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                                   @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.syncExperiment(project, experiment));
    }

    // Remote File API
    @GET
    @Path(LabApi.METHOD_GET_FILES)
    @ApiOperation(value = "Get all files of a project with details and general statistics filtered by data type and/or prefix.", response = ListOfLabFilesResponse.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response getFiles(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                             @ApiParam(value = "Data Type", required = false, allowableValues = LabFileDataType.ALLOWABLE_VALUES) @QueryParam(LabApi.PARAM_DATA_TYPE) String dataType,
                             @ApiParam(value = "File Key Prefix. If data type is provided, will prefix will be applied for datatype, otherwise on full remote storage.", required = false) @QueryParam(LabApi.PARAM_PREFIX) String prefix,
                             @ApiParam(value = "Aggregate Versions", required = false) @QueryParam(LabApi.PARAM_AGGREGATE_VERSIONS) Boolean aggregateVersions,
                             @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.getFiles(project, LabFileDataType.from(dataType), prefix, aggregateVersions));
    }

    @DELETE
    @Path(LabApi.METHOD_DELETE_FILE)
    @ApiOperation(value = "Deletes a file from a specified project.", response = StatusMessageFormat.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response deleteFile(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                               @ApiParam(value = "File Key", required = true) @QueryParam(LabApi.PARAM_FILE_KEY) String fileKey,
                               @ApiParam(value = "Keep the n-latest Versions", required = false) @QueryParam(LabApi.PARAM_KEEP_LATEST_VERSIONS) Integer keepLatestVersions,
                               @Pac4JProfile MongoProfile commonProfile,
                               @BeanParam DefaultHeaderFields defaultHeaders) {
        labHandler.setAuthProfile(commonProfile);
        return UnifiedResponseFactory.getResponse(labHandler.deleteFile(project, fileKey, keepLatestVersions));
    }

    @GET
    @Path(LabApi.METHOD_GET_FILE_INFO)
    @ApiOperation(value = "Get info about the specified file.", response = LabFileResponse.class)
    @Produces(MediaType.APPLICATION_JSON)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response getFileInfo(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                                @ApiParam(value = "File Key", required = true) @QueryParam(LabApi.PARAM_FILE_KEY) String fileKey,
                                @BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(labHandler.getFileInfo(project, fileKey));
    }

    @POST
    @ApiOperation(value = "Upload file to remote storage of selected project and returns key.", response = StringResponse.class)
    @Path(LabApi.METHOD_UPLOAD_FILE)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiImplicitParams({ @ApiImplicitParam(name = "file", paramType = "form", dataType = "file", required = true) })
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response uploadFile(
            @ApiParam(value = "File Input Stream", required = false, hidden = true) @FormDataParam(LabApi.PARAM_FILE) InputStream fileStream,
            @ApiParam(value = "File Metadata", required = false, hidden = true) @FormDataParam(LabApi.PARAM_FILE) FormDataContentDisposition fileDetail,
            @ApiParam(value = "File Name", required = false, hidden = true) @FormDataParam("name") String uppyFileName,
            @ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
            @ApiParam(value = "Data Type of File.", required = true, allowableValues = LabFileDataType.ALLOWABLE_VALUES) @QueryParam(LabApi.PARAM_DATA_TYPE) @Nullable String dataType,
            @ApiParam(value = "File Name. If not provided, the filename from file metadata will be used.", required = false) @QueryParam(LabApi.PARAM_FILE_NAME) String fileName,
            @ApiParam(value = "Versioning activated", required = false, defaultValue = "true") @QueryParam(LabApi.PARAM_VERSIONING) @Nullable Boolean versioning,
            @Pac4JProfile MongoProfile commonProfile,
            @BeanParam DefaultHeaderFields defaultHeaders) {

        if (StringUtils.isNullOrEmpty(fileName)) {
            if (fileDetail == null || StringUtils.isNullOrEmpty(fileDetail.getFileName())) {
                return UnifiedResponseFactory.getErrorResponse("fileName parameter is empty and no name provided in file metadata.");
            }
            fileName = fileDetail.getFileName();
        }

        if (!StringUtils.isNullOrEmpty(uppyFileName)) {
            // use file name from uppy as file name (e.g. when name changed)
            fileName = uppyFileName;
        }

        // TODO use content type from content disposition or uppy

        Map<String, String> metadata = new HashMap<>();

        Map<String, String> headers = getHeaders();
        for (String field : headers.keySet()) {
            if (field.startsWith(LabApi.FILE_METADATA_PREFIX)) {
                metadata.put(field, headers.get(field));
            }
        }

        labHandler.setAuthProfile(commonProfile);
        return UnifiedResponseFactory.getResponse(labHandler.uploadFile(project, fileStream, fileName, LabFileDataType.from(dataType), versioning, metadata));
    }

    @GET
    @ApiOperation(value = "Download file from remote storage of selected project.")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Return file stream"),
            @ApiResponse(code = 500, message = "Internal Server Error")
    })
    @Path(LabApi.METHOD_DOWNLOAD_FILE)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Pac4JSecurity(clients = {
            AuthorizationManager.PAC4J_CLIENT_COOKIE,
            AuthorizationManager.PAC4J_CLIENT_HEADER }, authorizers = AuthorizationManager.AUTHORIZER_PROJECT)
    public Response downloadFile(@ApiParam(value = "Project Name", required = true) @PathParam(LabApi.PARAM_PROJECT) String project,
                                 @ApiParam(value = "File Key", required = true) @QueryParam(LabApi.PARAM_FILE_KEY) String fileKey,
                                 @Pac4JProfile MongoProfile commonProfile,
                                 @BeanParam DefaultHeaderFields defaultHeaders) throws Exception {
        labHandler.setAuthProfile(commonProfile);
        SingleValueFormat<LabFile> fileDownload = labHandler.downloadFile(project, fileKey);

        if (fileDownload.hasError()) {
            return UnifiedResponseFactory.getResponse(fileDownload);
        }

        LabFile labFile = fileDownload.getData();

        if (labFile == null || labFile.getFileStream() == null) {
            return UnifiedResponseFactory.getErrorResponse("Error with remote file stream.");
        }

        ContentDisposition contentDisposition = ContentDisposition.type("attachment")
                                                                  .fileName(FileHandlerUtils.getFileNameFromKey(labFile.getKey(), true)) // always remove version here
                                                                  .size(labFile.getSize())
                                                                  .creationDate(labFile.getModifiedAt())
                                                                  .modificationDate(labFile.getModifiedAt()).build();

        return UnifiedResponseFactory.getFileDownloadResponse(labFile.getFileStream(), contentDisposition);
    }

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //


    // ================ Inner & Anonymous Classes =========================== //
    private static class StringResponse extends SingleValueFormat<String> {

        public String data;
    }


    private static class LabFileResponse extends SingleValueFormat<LabFile> {

        public LabFile data;
    }


    private static class ListOfLabFilesResponse extends ValueListFormat<LabFile> {

        public List<LabFile> data;
    }


    private static class LabProjectResponse extends SingleValueFormat<LabProject> {

        public LabProject data;
    }


    private static class ListOfLabProjectsResponse extends ValueListFormat<LabProject> {

        public List<LabProject> data;
    }


    private static class LabServiceResponse extends SingleValueFormat<LabService> {

        public LabService data;
    }


    private static class ListOfLabServicesResponse extends ValueListFormat<LabService> {

        public List<LabService> data;
    }


    private static class LabScheduledJobResponse extends SingleValueFormat<LabScheduledJob> {

        public LabScheduledJob data;
    }


    private static class ListOfLabScheduledJobsResponse extends ValueListFormat<LabScheduledJob> {

        public List<LabScheduledJob> data;
    }


    private static class LabJobResponse extends SingleValueFormat<LabJob> {

        public LabJob data;
    }


    private static class ListOfLabJobsResponse extends ValueListFormat<LabJob> {

        public List<LabJob> data;
    }


    private static class LabExperimentResponse extends SingleValueFormat<LabExperiment> {

        public LabExperiment data;
    }


    private static class ListOfLabExperimentsResponse extends ValueListFormat<LabExperiment> {

        public List<LabExperiment> data;
    }
}

