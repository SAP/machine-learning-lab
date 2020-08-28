package org.mltooling.lab;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.options.Options;
import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.api.format.ValueListFormat;
import org.mltooling.core.api.utils.ApiUtils;
import org.mltooling.core.env.Environment;
import org.mltooling.core.env.handler.FileHandlerUtils;
import org.mltooling.core.lab.LabApiClient;
import org.mltooling.core.lab.LabAuthApi;
import org.mltooling.core.lab.LabAuthApiClient;
import org.mltooling.core.service.tests.IntegrationTest;
import org.mltooling.core.service.tests.LocalDockerLauncher;
import org.mltooling.core.lab.LabApi;
import org.mltooling.core.lab.model.*;
import org.mltooling.core.utils.FileUtils;
import org.mltooling.core.utils.ListUtils;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.lab.authorization.AuthorizationManager;
import org.mltooling.lab.authorization.ProjectAuthorizer;
import org.mltooling.lab.components.ProjectManager;
import org.mltooling.lab.services.AbstractServiceManager;
import org.mltooling.lab.services.managers.DockerServiceManager;
import org.mltooling.lab.services.managers.KubernetesServiceManager;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.hamcrest.core.IsNull;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

@Category(IntegrationTest.class)
public class LabApiTest {

    // ================ Constants =========================================== //
    private static final Logger log = LoggerFactory.getLogger(LabApiTest.class);

    private static final long SLEEP = 5000;
    private final String DEFAULT_HOST = "127.0.0.1";
    private final int SERVICE_TEST_PORT = 30002; // use a port > 30000 so that it also works in Kubernetes mode (Kubernetes service ports must by default be >30000)

    private static final int DEFAULT_PROJECT_SERVICES_SIZE = 0; // no more initial project services

    private static long MAX_WAIT_TIME = TimeUnit.MINUTES.toMillis(5);
    private static long WAIT_INTERVALS = TimeUnit.SECONDS.toMillis(10);

    private static final String ADMIN_USER = "foo";
    private static final String ADMIN_PASSWORD = "bar";

    private static final String TEST_USER_ID = "test";
    private static final String TEST_USER_PASSWORD = "test";

    private static enum TEST_MODES {
        DOCKER("docker"), KUBERNETES("k8s");

        private String mode;
        TEST_MODES(String str) {
            this.mode = str;
        }
    };

    // Change to test Kubernetes instead of Docker mode
    private static final String TEST_MODE = TEST_MODES.DOCKER.mode;

    // ================ Members ============================================= //
    @Rule
    public final LocalDockerLauncher dockerLauncher;
    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    private LabApi labApi;
    private LabAuthApi authorizationApi;
    private String testProject1;
    private String testProject2;

    private String serviceUrl;
    private String adminApiToken;
    private String adminAppToken;

    private String userApiToken;
    private String userAppToken;
    // ================ Constructors & Main ================================= //
    public LabApiTest() throws Exception {
        resetUnirest();
        // currently service name needs to be "lab-service", otherwise the lab service (added in core service) cannot be found
        // change to env variable for images?
        
        if (TEST_MODE.equals(TEST_MODES.KUBERNETES.mode)) {
            this.dockerLauncher = new LocalDockerLauncher("lab-service", SERVICE_TEST_PORT, false, "--kubernetes --env SERVICES_CPU_LIMIT=1 --env SERVICES_MEMORY_LIMIT=2 --env LAB_KUBERNETES_NAMESPACE=lab"); //--swarm --kubernetes
        } else {
            this.dockerLauncher = new LocalDockerLauncher("lab-service", SERVICE_TEST_PORT, false, "");
        }

        Thread.sleep(SLEEP * 3);
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //
    @Before
    public void setUp() throws Exception {
        // Connect via API client to Service
        //final String SERVICE_URL = "http://" + DEFAULT_HOST + ":" + SERVICE_TEST_PORT;
        serviceUrl = "http://" + DEFAULT_HOST + ":" + SERVICE_TEST_PORT;
        adminAppToken = getAdminToken(serviceUrl);
        LabAuthApiClient authApi = new LabAuthApiClient(serviceUrl, adminAppToken);
        adminApiToken = authApi.createApiToken(ADMIN_USER).getData();

        log.info("Create Test User");
        authApi.createUser(TEST_USER_ID, TEST_USER_PASSWORD);
        log.info("Login Test User");
        userAppToken = authApi.loginUser(TEST_USER_ID, TEST_USER_PASSWORD).getData();

        log.info("Admin Token: " + adminApiToken);
        labApi = new LabApiClient(serviceUrl, adminApiToken);

        testProject1 = "test-" + System.currentTimeMillis();
        SingleValueFormat<LabProject> response = labApi.createProject(new LabProjectConfig(testProject1));

        resetUnirest();
        Assume.assumeThat(response.getMetadata("status"), not(500));

        // Add test user to project
        userAppToken = authApi.addUserToProject(TEST_USER_ID, testProject1).getData();
        userApiToken = authApi.createApiToken(TEST_USER_ID).getData();
        log.info("User Api Token: " + userApiToken);

        testProject2 = "test-" + System.currentTimeMillis();
        response = labApi.createProject(new LabProjectConfig(testProject2));
        Assume.assumeThat(response.getMetadata("status"), not(500));

        try {
            uploadTestFilesToMinio();
        } catch (Exception e) {
            log.error("Failed to initialize environment", e);
        }
    }

    @After
    public void tearDown() throws Exception {
        log.info("Deleting all projects");
        labApi.deleteProject(testProject1).isSuccessful();
        labApi.deleteProject(testProject2).isSuccessful();
        log.info("Delete user");
        new LabAuthApiClient(serviceUrl, adminAppToken).deleteUser(TEST_USER_ID);
        new LabAuthApiClient(serviceUrl, adminAppToken).deleteUser(ADMIN_USER);
        Thread.sleep(SLEEP * 2);
    }

    @BeforeClass
    public static void beforeClass() throws Exception {        
        if (TEST_MODE.equals(TEST_MODES.KUBERNETES.mode)) {
            try {
                KubernetesServiceManager.cleanUpLab();
                Thread.sleep(SLEEP * 2); // sleep as the cleanup happens asynchronously
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } else {
            DockerServiceManager.cleanUpLab(false);
        }
    }

    @AfterClass
    public static void afterClass() throws Exception {
        if (TEST_MODE.equals(TEST_MODES.KUBERNETES.mode)) {
            try {
                KubernetesServiceManager.cleanUpLab();
                Thread.sleep(SLEEP * 2); // sleep as the cleanup happens asynchronously
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } else {
            DockerServiceManager.cleanUpLab(false);
        }
    }

    // ================ Public Methods ====================================== //
    @Test
    public void testProjectHandling() {
        List<LabProject> projects = labApi.getProjects().getData();

        LabProject project1 = (projects.get(0).getId().equals(testProject1)) ? projects.get(0) : projects.get(1);
        LabProject project2 = (projects.get(0).getId().equals(testProject2)) ? projects.get(0) : projects.get(1);

        assertThat(projects.size(), is(equalTo(2)));
        assertThat(project1.getName(), is(equalTo(testProject1)));
        assertThat(project2.getName(), is(equalTo(testProject2)));

        // Create project with
        String TEMP_PROJECT = "Temp Project " + System.currentTimeMillis();
        SingleValueFormat<LabProject> project = labApi.createProject(new LabProjectConfig(TEMP_PROJECT));
        assertThat(project.getData().getId(), is(equalTo(ProjectManager.processNameToId(TEMP_PROJECT))));
        assertThat(project.getData().getName(), is(equalTo(TEMP_PROJECT)));
        assertThat(project.getData().getName(), is(not(equalTo(project.getData().getId()))));
        // there should be three projects now
        assertThat(labApi.getProjects().getData().size(), is(equalTo(3)));

        // Check project available
        assertThat(labApi.getProject(testProject2, false).isSuccessful(), is(equalTo(true)));

        // Delete project
        assertThat(labApi.deleteProject(ProjectManager.processNameToId(TEMP_PROJECT)).isSuccessful(), is(equalTo(true)));
        // there should be two projects again
        assertThat(labApi.getProjects().getData().size(), is(equalTo(2)));

        // Check project statistics
        project = labApi.getProject(testProject1, true);
        assertThat(project.getData().getStatistics().getServicesCount(), is(equalTo(DEFAULT_PROJECT_SERVICES_SIZE)));
    }

    @Test
    public void testServiceHandling() throws Exception {
        ValueListFormat<LabService> response = labApi.getServices(testProject1);
        List<LabService> services = response.getData();

        LabProjectsStatistics statistics = new LabProjectsStatistics((Map) response.getMetadata().getStats());
        assertThat(statistics.getServicesCount(), is(equalTo(DEFAULT_PROJECT_SERVICES_SIZE)));

        String TEST_SERVICE_IMAGE = "simple-demo-service:latest";

        final String TEST_CONFIG_ITEM = "TEST";
        Map<String, String> serviceConfig = new HashMap<>();
        serviceConfig.put(TEST_CONFIG_ITEM, TEST_CONFIG_ITEM);

        final String SERVICE_NAME = "test-service-withvery-VERY-very-very-very-very-very-VeRy-long-name";
        LabService service = labApi.deployService(testProject1, TEST_SERVICE_IMAGE, SERVICE_NAME, serviceConfig).getData();

        assertThat(service.getDockerImage(), is(equalTo(TEST_SERVICE_IMAGE)));
        long waitTime = 0;
        do {
            Thread.sleep(WAIT_INTERVALS);
            waitTime += WAIT_INTERVALS;
            if (waitTime >= MAX_WAIT_TIME) {
                log.error("Service " + service.getName() + " isn't available after " + TimeUnit.MILLISECONDS.toMinutes(waitTime) + " min wait time.");
                break;
            }
        }
        while (!labApi.getService(testProject1, service.getName()).getData().getIsHealthy());

        service = labApi.getService(testProject1, service.getName()).getData();
        assertThat(service.getIsHealthy(), is(true));

        assertThat(service.getName(), containsString(AbstractServiceManager.processServiceName(SERVICE_NAME)));
        assertThat(service.getConfiguration().get(TEST_CONFIG_ITEM), equalTo(TEST_CONFIG_ITEM));

        assertThat(labApi.getServices(testProject1).getData().size(), is(DEFAULT_PROJECT_SERVICES_SIZE + 1));

        // check if ports are correctly set
        Integer EXPOSED_TEST_PORT = 1234;
        assertThat(service.getConnectionPort(), is(equalTo(EXPOSED_TEST_PORT)));
        assertThat(service.getExposedPorts().contains(EXPOSED_TEST_PORT), is(equalTo(true)));
        assertThat(service.getExposedPorts().size(), is(equalTo(1)));

        // Test if service is reachable via port tunneling
        String url = this.serviceUrl + "/api/projects/" + testProject1 + "/services/" + service.getDockerName() + "/" + EXPOSED_TEST_PORT + "/";

        // this call requires the Bearer token
        log.info("Requesting: " + url);
        Integer status = Unirest.get(url)
                                .header(ApiUtils.AUTHORIZATION_HEADER, "Bearer " + adminAppToken)
                                .getHttpRequest()
                                .asString()
                                .getStatus();
        assertThat(status, is(equalTo(200)));

        // Check if also accessible via user api token
        log.info("Requesting: " + url);
        status = Unirest.get(url)
                                .header(ApiUtils.AUTHORIZATION_HEADER, "Bearer " + userApiToken)
                                .getHttpRequest()
                                .asString()
                                .getStatus();
        assertThat(status, is(equalTo(200)));

        // Check if logs exist
        assertThat(!StringUtils.isNullOrEmpty(labApi.getServiceLogs(testProject1, service.getDockerName()).getData()), is(equalTo(true)));

        // remove service
        assertThat(labApi.deleteService(testProject1, service.getDockerName()).isSuccessful(), is(equalTo(true)));
        // should be deleted
        assertThat(labApi.getServices(testProject1).getData().size(), is(DEFAULT_PROJECT_SERVICES_SIZE));

        // Test if access to core service is prevented
        url = this.serviceUrl + "/api/projects/" + testProject1 + "/services/lab-minio/" + EXPOSED_TEST_PORT + "/";
        log.info("Requesting: " + url);
        // this call requires the Bearer token
        status = Unirest.get(url)
                        .header(ApiUtils.AUTHORIZATION_HEADER, "Bearer " + adminAppToken)
                        .getHttpRequest()
                        .asString()
                        .getStatus();
        assertThat(status, not(equalTo(200)));

    }

    @Test
    public void testJobHandling() throws Exception {
        ValueListFormat<LabJob> response = labApi.getJobs(testProject1);

        assertThat(response.getData().size(), is(equalTo(0))); // No jobs should be started

        String TEST_JOB_IMAGE = "simple-demo-job:latest";

        final String TEST_CONFIG_ITEM = "TEST";
        Map<String, String> jobConfig = new HashMap<>();
        jobConfig.put(TEST_CONFIG_ITEM, TEST_CONFIG_ITEM);

        final String JOB_NAME = "test-job";
        LabJob job = labApi.deployJob(testProject1, TEST_JOB_IMAGE, JOB_NAME, null, jobConfig).getData();

        assertThat(job.getDockerImage(), is(equalTo(TEST_JOB_IMAGE)));

        job = labApi.getJob(testProject1, job.getDockerName()).getData();

        assertThat(job.getName(), containsString(JOB_NAME));
        assertThat(job.getConfiguration().get(TEST_CONFIG_ITEM), equalTo(TEST_CONFIG_ITEM));

        assertThat(labApi.getJobs(testProject1).getData().size(), is(1));

        // Check if job finishes successfully
        long waitTime = 0;
        do {
            Thread.sleep(WAIT_INTERVALS);
            waitTime += WAIT_INTERVALS;
            if (waitTime >= MAX_WAIT_TIME) {
                log.error("Job " + job.getName() + " isn't available after " + TimeUnit.MILLISECONDS.toMinutes(waitTime) + " min wait time.");
                break;
            }
        }
        while (!labApi.getJob(testProject1, job.getDockerName()).getData().getStatus().equalsIgnoreCase(LabJob.State.SUCCEEDED.getName()));

        assertThat(labApi.getJob(testProject1, job.getDockerName()).getData().getStatus().equalsIgnoreCase(LabJob.State.SUCCEEDED.getName()), is(true));

        // Check if logs exist
        assertThat(!StringUtils.isNullOrEmpty(labApi.getJobLogs(testProject1, job.getDockerName()).getData()), is(equalTo(true)));


        // Test job scheduling
        assertThat(labApi.getScheduledJobs(testProject1).getData().size(), is(equalTo(0)));

        // Schedule job for every minute
        assertThat(labApi.deployJob(testProject1, TEST_JOB_IMAGE, JOB_NAME, "*/1 * * * *", jobConfig).isSuccessful(), is(equalTo(true)));

        // There should be one scheduled job
        assertThat(labApi.getScheduledJobs(testProject1).getData().size(), is(equalTo(1)));

        // wait for about one minute and check if job was started
        Thread.sleep(TimeUnit.SECONDS.toMillis(120));

        assertThat(labApi.getJobs(testProject1).getData().size() >= 2, is(equalTo(true))); // there should be 2 jobs now

        // delete scheduled job
        LabScheduledJob scheduledJob = labApi.getScheduledJobs(testProject1).getData().get(0);
        assertThat(labApi.deleteScheduledJob(testProject1, scheduledJob.getId()).isSuccessful(), is(equalTo(true)));
        assertThat(labApi.getScheduledJobs(testProject1).getData().size(), is(equalTo(0)));
    }

    @Test
    public void testFileHandling() throws Exception {

        ValueListFormat<LabFile> response = labApi.getFiles(testProject1, LabFileDataType.DATASET, null, null);
        LabProjectsStatistics statistics = new LabProjectsStatistics((Map) response.getMetadata().getStats());
        assertThat(statistics.getFilesCount(), is(equalTo(1)));
        assertThat(response.getData().size(), is(equalTo(1)));

        response = labApi.getFiles(testProject1, LabFileDataType.MODEL, null, null);
        statistics = new LabProjectsStatistics((Map) response.getMetadata().getStats());
        assertThat(statistics.getFilesCount(), is(equalTo(1)));
        assertThat(response.getData().size(), is(equalTo(1)));

        Path rootFolder = tempFolder.newFolder("temp-file-handler-test").toPath();

        Environment env = new Environment(testProject2, serviceUrl, adminApiToken).setRootFolder(rootFolder);

        // remove all files
        for (LabFile labFile : env.getFileHandler().listRemoteFiles("", false)) {
            env.getFileHandler().deleteRemoteFile(labFile.getKey(), null);
        }

        assertThat(env.getFileHandler().listRemoteFiles("", false).size(), is(equalTo(0)));

        String testFileName = "file-handler-test.txt";
        String testFileContent = "test";
        Path testFilePath = env.getRootFolder().resolve(testFileName).toAbsolutePath();
        // create temp file
        FileUtils.writeStrToFile(testFilePath.toString(), testFileContent);

        String updateFileName = "file-handler-update.txt";
        String updateFileContent = "update";

        Path updateFilePath = env.getRootFolder().resolve(updateFileName).toAbsolutePath();
        // create temp updated file
        FileUtils.writeStrToFile(updateFilePath.toString(), updateFileContent);

        // Upload dataset for first time with default settings (versioning is activated)
        assertThat(env.uploadFile(testFilePath, LabFileDataType.DATASET), is(IsNull.notNullValue()));
        // Get dataset
        Path downloadedFile = env.getFileHandler().getFile(testFilePath.getFileName().toString(), LabFileDataType.DATASET).toPath().toAbsolutePath();
        // expected file should be already version 1 since always versioning is default configuration
        Path expectedFile = env.getProjectFolder().resolve(LabFileDataType.DATASET.getDefaultFolder()).resolve(testFileName+".v1").toAbsolutePath();
        assertThat(downloadedFile.toString(), is(equalTo(expectedFile.toString())));
        //check if file content is equal to the uploaded file
        assertThat(FileUtils.readFile(downloadedFile.toAbsolutePath().toString()), is(equalTo(testFileContent)));

        // Upload dataset again with default settings (versioning is activated)
        // First, check if the file instructions are correct
        assertThat(env.uploadFile(testFilePath, LabFileDataType.DATASET), is(IsNull.notNullValue()));
        downloadedFile = env.getFileHandler().getFile(testFilePath.getFileName().toString(), LabFileDataType.DATASET).toPath().toAbsolutePath();
        // expect to be version 2
        expectedFile = env.getProjectFolder().resolve(LabFileDataType.DATASET.getDefaultFolder()).resolve(testFileName + ".v2").toAbsolutePath();
        assertThat(downloadedFile.toString(), is(equalTo(expectedFile.toString())));

        // Upload dataset again with default settings (versioning is activated)
        // This time we upload with version in name (v1) -> it should only overwrite this version
        File renamedFile = FileUtils.copyFile(updateFilePath.toString(), updateFilePath.getParent().resolve(testFileName + ".v1").toString(), true);
        assertThat(env.uploadFile(renamedFile.toPath(), LabFileDataType.DATASET), is(IsNull.notNullValue()));
        // Get dataset
        downloadedFile = env.getFileHandler().getFile(testFilePath.getFileName().toString() + ".v1", LabFileDataType.DATASET).toPath().toAbsolutePath();
        expectedFile = env.getProjectFolder().resolve(LabFileDataType.DATASET.getDefaultFolder()).resolve(testFileName + ".v1").toAbsolutePath();
        assertThat(downloadedFile.toString(), is(equalTo(expectedFile.toString())));
        //check if file content is equal to the uploaded file
        assertThat(FileUtils.readFile(downloadedFile.toAbsolutePath().toString()), is(equalTo(updateFileContent)));

        // there should be only one dataset listed right now -> since version aggregation is on
        assertThat(env.getFileHandler().listRemoteFiles(LabFileDataType.DATASET).size(), is(equalTo(1)));
        // when it is not aggregated, there should be two datasets
        assertThat(env.getFileHandler().listRemoteFiles(LabFileDataType.DATASET.getDefaultFolder(), false).size(), is(equalTo(2)));
        // this dataset should be version 2
        assertThat(env.getFileHandler().listRemoteFiles(LabFileDataType.DATASET).get(0).getVersion(), is(equalTo(2)));

        // Upload  other dataset  with default settings (versioning is activated)
        assertThat(env.uploadFile(updateFilePath, LabFileDataType.DATASET), is(IsNull.notNullValue()));
        downloadedFile = env.getFile(updateFilePath.getFileName().toString(), LabFileDataType.DATASET).toPath().toAbsolutePath();
        // expected file should be already version 1 since always versioning is default configuration
        expectedFile = env.getProjectFolder().resolve(LabFileDataType.DATASET.getDefaultFolder()).resolve(updateFileName+".v1").toAbsolutePath();
        assertThat(downloadedFile.toString(), is(equalTo(expectedFile.toString())));
        //check if file content is equal to the uploaded file
        assertThat(FileUtils.readFile(downloadedFile.toAbsolutePath().toString()), is(equalTo(updateFileContent)));

        // there should be two dataset listed right now -> since version aggregation is on
        assertThat(env.getFileHandler().listRemoteFiles(LabFileDataType.DATASET).size(), is(equalTo(2)));
        // when it is not aggregated, there should be three datasets
        assertThat(env.getFileHandler().listRemoteFiles(LabFileDataType.DATASET.getDefaultFolder(), false).size(), is(equalTo(3)));

        // Upload dataset again with versioning deactivated
        assertThat(env.getFileHandler().uploadFile(updateFilePath, LabFileDataType.DATASET), is(IsNull.notNullValue()));
        // Get dataset
        downloadedFile = env.getFile(testFilePath.getFileName().toString(), LabFileDataType.DATASET).toPath().toAbsolutePath();
        // expected File is the version 2 of the previously uploaded file..
        expectedFile = env.getProjectFolder().resolve(LabFileDataType.DATASET.getDefaultFolder()).resolve(testFileName + ".v2").toAbsolutePath();
        assertThat(downloadedFile.toString(), is(equalTo(expectedFile.toString())));
        //check if file content is equal to the uploaded file
        assertThat(FileUtils.readFile(downloadedFile.toAbsolutePath().toString()), is(equalTo(testFileContent)));

        // upload it again and add metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("operator", "test");
        metadata.put("project", testProject2);
        String uploadedFileKey = env.getFileHandler().uploadFile(testFilePath, testFilePath.getFileName().toString(), LabFileDataType.DATASET, true, metadata);
        assertThat(uploadedFileKey, is(IsNull.notNullValue()));
        // check if metadata was added an can be retrieved
        LabFile remoteFile = labApi.getFileInfo(testProject2, uploadedFileKey).getData();
        assertThat(remoteFile.getMetadata(), is(IsNull.notNullValue()));
        for (String key : metadata.keySet()) {
            assertThat(remoteFile.getMetadata().containsKey(key), is(equalTo(true)));
            assertThat(remoteFile.getMetadata().get(key).equalsIgnoreCase(metadata.get(key)), is(equalTo(true)));
        }

        // Check if default content types are set
        assertThat(remoteFile.getMetadata().get(LabFile.META_MODIFIED_BY).equalsIgnoreCase(ADMIN_USER), is(equalTo(true)));
        assertThat(remoteFile.getMetadata().get(LabFile.META_CONTENT_TYPE).equalsIgnoreCase("text/plain"), is(equalTo(true)));


        // check if keep latest version is working -> delete dataset and keep latest version
        String datasetKey = FileHandlerUtils.resolveKey(testFilePath.getFileName().toString(), LabFileDataType.DATASET);
        assertThat(env.getFileHandler().listRemoteFiles(LabFileDataType.DATASET.getDefaultFolder(), false).size(), is(equalTo(5)));
        env.getFileHandler().deleteRemoteFile(datasetKey, 1);
        // there should only be 2 datasets in total
        assertThat(env.getFileHandler().listRemoteFiles(LabFileDataType.DATASET.getDefaultFolder(), false).size(), is(equalTo(3)));
        // delete again -> should still keep last file
        env.getFileHandler().deleteRemoteFile(datasetKey, 1);
        assertThat(env.getFileHandler().listRemoteFiles(LabFileDataType.DATASET.getDefaultFolder(), false).size(), is(equalTo(3)));
        // fully delete updated file -> only 1 dataset left
        String updateFileKey = FileHandlerUtils.resolveKey(updateFilePath.getFileName().toString(), LabFileDataType.DATASET);
        env.getFileHandler().deleteRemoteFile(updateFileKey, null);
        assertThat(env.getFileHandler().listRemoteFiles(LabFileDataType.DATASET.getDefaultFolder(), false).size(), is(equalTo(1)));

        // Upload data as model and test various stuff
        assertThat(env.uploadFile(testFilePath, LabFileDataType.MODEL), is(IsNull.notNullValue()));
        downloadedFile = env.getFile(testFilePath.getFileName().toString(), LabFileDataType.MODEL).toPath().toAbsolutePath();
        // expected file should be already version 1 since always versioning is default configuration
        expectedFile = env.getProjectFolder().resolve(LabFileDataType.MODEL.getDefaultFolder()).resolve(testFileName+".v1").toAbsolutePath();
        assertThat(downloadedFile.toString(), is(equalTo(expectedFile.toString())));
        //check if file content is equal to the uploaded file
        assertThat(FileUtils.readFile(downloadedFile.toAbsolutePath().toString()), is(equalTo(testFileContent)));
        // there should be only one model listed right now
        assertThat(env.getFileHandler().listRemoteFiles(LabFileDataType.MODEL).size(), is(equalTo(1)));
        // this model should be version 1
        assertThat(env.getFileHandler().listRemoteFiles(LabFileDataType.MODEL).get(0).getVersion(), is(equalTo(1)));

        // remove all files
        for (LabFile labFile : env.getFileHandler().listRemoteFiles("", false)) {
            env.getFileHandler().deleteRemoteFile(labFile.getKey(), null);
        }
        // no files should be available
        assertThat(env.getFileHandler().listRemoteFiles("", false).size(), is(equalTo(0)));

        // TEST FOLDER HANDLING

        // create a folder containing a file
        TemporaryFolder testFolder = new TemporaryFolder();
        testFolder.create();
        String testFolderName = "test-folder";
        Path testFolderPath = testFolder.newFolder(testFolderName).toPath();
        testFilePath = testFolderPath.resolve(testFileName).toAbsolutePath();
        Path testFile2Path = testFolderPath.resolve(updateFileName).toAbsolutePath();
        // create two files within the temp folder
        FileUtils.writeStrToFile(testFilePath.toString(), testFileContent);
        FileUtils.writeStrToFile(testFile2Path.toString(), testFileContent);

        // upload the folder that contains the file
        assertThat(env.uploadFolder(testFolderPath, LabFileDataType.MODEL), is(IsNull.notNullValue()));

        Path downloadedFolder = env.getFolder(FileHandlerUtils.resolveKey(testFolderName + ".zip", LabFileDataType.MODEL)).toPath().toAbsolutePath();
        Path expectedFolder = env.getProjectFolder().resolve(LabFileDataType.MODEL.getDefaultFolder()).resolve(testFolderName).toAbsolutePath();
        // check if the zip-file was unpacked
        assertThat(downloadedFolder.toString(), is(equalTo(expectedFolder.toString())));
        // check if the unpacked folder contains both files
        assertThat(Files.exists(downloadedFolder.resolve(testFileName).toAbsolutePath()), is(equalTo(true)));
        assertThat(Files.exists(downloadedFolder.resolve(updateFileName).toAbsolutePath()), is(equalTo(true)));
        assertThat(FileUtils.readFile(downloadedFolder.resolve(testFileName).toAbsolutePath()), is(equalTo(testFileContent)));

        testFolder.delete();
    }

    @Test
    public void testUserAuthFlow() throws Exception {
        LabAuthApiClient authApi = new LabAuthApiClient(serviceUrl).setAuthToken(adminApiToken);

        final String CHANGED_PASSWORD = "changed";

        final String TEST_USER_ID_2 = "test2";
        final String TEST_USER_PASSWORD_2 = "test2";

        final String TEST_PROJECT = "auth-test";

        log.info("Login Test User");
        String appToken = authApi.loginUser(TEST_USER_ID, TEST_USER_PASSWORD).getData();

        assertThat("User token should not be null.", !StringUtils.isNullOrEmpty(appToken), is(equalTo(true)));
        // Change Auth Token to user token
        authApi.setAuthToken(appToken);

        log.info("Test access to other User Profile");
        assertThat(authApi.createUser(TEST_USER_ID_2, TEST_USER_PASSWORD_2).isSuccessful(), is(equalTo(true)));
        assertThat("Should not see other user.", authApi.getUser(TEST_USER_ID_2).isSuccessful(), is(equalTo(false)));

        log.info("Get API Token");
        String apiToken = authApi.createApiToken(TEST_USER_ID).getData();
        assertThat("API token should not be null.", apiToken, is(IsNull.notNullValue()));

        // Test stuff with admin token, api token, and user token
        for (String token : Arrays.asList(appToken, adminApiToken, apiToken)) {
            authApi.setAuthToken(token);
            LabApiClient labApi = new LabApiClient(serviceUrl).setAuthToken(token);

            log.info("Get User Profile");
            assertThat("Should return user profile.", authApi.getUser(TEST_USER_ID).getData().getName(), is(equalTo(TEST_USER_ID)));

            log.info("Get API Token");
            assertThat("API token should not be null.", authApi.createApiToken(TEST_USER_ID).isSuccessful(), is(equalTo(true)));

            log.info("Change Password");
            assertThat(authApi.updateUserPassword(TEST_USER_ID, CHANGED_PASSWORD).isSuccessful(), is(equalTo(true)));
            String newToken = authApi.loginUser(TEST_USER_ID, CHANGED_PASSWORD).getData();
            assertThat(!StringUtils.isNullOrEmpty(newToken), is(equalTo(true)));
            if (token.equalsIgnoreCase(appToken)) {
                // set new token if app token
                authApi.setAuthToken(newToken);
                labApi.setAuthToken(newToken);
            }

            assertThat("User token should not be null.", !StringUtils.isNullOrEmpty(newToken), is(equalTo(true)));
            assertThat("User name should be same as logged in user.", authApi.getUser(TEST_USER_ID).getData().getName(), is(equalTo(TEST_USER_ID)));
            assertThat("Login should not work with all password.", authApi.loginUser(TEST_USER_ID, TEST_USER_PASSWORD).isSuccessful(), is(equalTo(false)));
        }

        // Only Admin
        // Change Auth Token to admin token
        authApi.setAuthToken(adminApiToken);

        log.info("Create Project");
        assertThat(labApi.createProject(new LabProjectConfig(TEST_PROJECT)).isSuccessful(), is(equalTo(true)));

        log.info("Add User to Project");
        String newUserToken = authApi.addUserToProject(TEST_USER_ID, TEST_PROJECT).getData();
        assertThat(!StringUtils.isNullOrEmpty(newUserToken), is(equalTo(true)));
        assertThat("User profile should contain project permission.", authApi.getUser(TEST_USER_ID).getData().getPermissions().contains(ProjectAuthorizer.PROJECT_PERMISSION_PREFIX + TEST_PROJECT), is(equalTo(true)));

        log.info("Get Projects");
        LabApiClient labApi = new LabApiClient(serviceUrl).setAuthToken(newUserToken);
        List<LabProject> projects = labApi.getProjects().getData();
        assertThat("Should return at least one project.", ListUtils.isNullOrEmpty(projects), is(equalTo(false)));

        Set<String> uniqueProjects = new HashSet<>();
        for (LabProject project : projects) {
            uniqueProjects.add(project.getName());
        }
        assertThat("User should see added profile.", uniqueProjects.contains(TEST_PROJECT), is(equalTo(true)));

        log.info("Remove User from Project");
        assertThat("User should be removed.", authApi.removeUserFromProject(TEST_USER_ID, TEST_PROJECT).isSuccessful(), is(equalTo(true)));
        assertThat("User profile should not contain project permission.", authApi.getUser(TEST_USER_ID).getData().getPermissions().contains(ProjectAuthorizer.PROJECT_PERMISSION_PREFIX + TEST_PROJECT), is(equalTo(false)));

        log.info("Delete Project.");
        labApi.setAuthToken(adminApiToken);
        assertThat(labApi.deleteProject(TEST_PROJECT).isSuccessful(), is(equalTo(true)));

        log.info("Update User Permission");
        log.info("Add User to Project");
        assertThat(authApi.addUserToProject(TEST_USER_ID, testProject1).isSuccessful(), is(equalTo(true)));
        assertThat("User profile should contain project permission.", authApi.getUser(TEST_USER_ID).getData().getPermissions().contains(ProjectAuthorizer.PROJECT_PERMISSION_PREFIX + testProject1), is(equalTo(true)));

        final String PERMISSION_TO_ADD = "admin";
        assertThat(authApi.updatePermissions(TEST_USER_ID, Collections.singletonList(PERMISSION_TO_ADD), null).isSuccessful(), is(equalTo(true)));
        Collection<String> permissions = authApi.getUser(TEST_USER_ID).getData().getPermissions();
        assertThat("User profile should contain added permission.", permissions.contains(PERMISSION_TO_ADD), is(equalTo(true)));
        assertThat("User profile should contain only one permission.", permissions.size(), is(equalTo(1)));

        log.info("Delete User");
        assertThat(authApi.deleteUser(TEST_USER_ID).isSuccessful(), is(equalTo(true)));
        assertThat(authApi.deleteUser(TEST_USER_ID_2).isSuccessful(), is(equalTo(true)));
        assertThat("Login should not work with deleted user.", authApi.loginUser(TEST_USER_ID, CHANGED_PASSWORD).isSuccessful(), is(equalTo(false)));

        // TODO check user access to user project

    }

    // ================ Private Methods ===================================== //
    private void uploadTestFilesToMinio() throws IOException {

        Path rootFolder = tempFolder.newFolder("temp-env").toPath();
        Environment env = new Environment(testProject1, serviceUrl, adminApiToken).setRootFolder(rootFolder);

        String testFileName1 = "test.txt";
        String testFileName2 = "test2.txt";
        String testFileContent = "test";
        Path testFilePath1 = env.getRootFolder().resolve(testFileName1).toAbsolutePath();
        // create temp file
        FileUtils.writeStrToFile(testFilePath1.toString(), testFileContent);

        Path testFilePath2 = env.getRootFolder().resolve(testFileName2).toAbsolutePath();
        FileUtils.writeStrToFile(testFilePath2.toString(), testFileContent);

        env.uploadFile(testFilePath1, LabFileDataType.DATASET);
        env.uploadFile(testFilePath1, LabFileDataType.MODEL);

        env = new Environment(testProject2, serviceUrl, adminApiToken).setRootFolder(rootFolder);

        env.uploadFile(testFilePath2, LabFileDataType.MODEL);
    }

    private String getAdminToken(String serviceUrl) {
        // all API calls in this test will be made via this user / the user's token
        authorizationApi = new LabAuthApiClient(serviceUrl);
        authorizationApi.createAdminUser(ADMIN_USER, ADMIN_PASSWORD, AuthorizationManager.DEFAULT_JWT_SECRET);
        return authorizationApi.loginUser(ADMIN_USER, ADMIN_PASSWORD).getData();
    }

    /**
     * This is somehow necessary, as Unirest will otherwise not accept '127.0.0.1' or 'localhost' whereas it is the opposite of
     * the {@link LabApiTest#DEFAULT_HOST} value. Hence, if {@link LabApiTest#DEFAULT_HOST} is set to '127.0.0.1', Unirest will return
     * a forbidden response when making the endpoint call with the authToken (whereas it works when you manually set the host of the
     * endpoint call to 'localhost') and vice-versa. I assume that Unirest, since it is a Singleton, makes some weird host-mapping or caching
     * upon first request.
     *
     * @throws IOException
     */
    private void resetUnirest() throws IOException {

        Unirest.shutdown();
        Options.refresh();

        HttpClient httpClient = HttpClients.custom()
                                           .disableCookieManagement()
                                           .build();
        Unirest.setHttpClient(httpClient);
    }
    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
