package org.mltooling.lab;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.options.Options;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.*;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.mltooling.core.api.format.SingleValueFormat;
import org.mltooling.core.api.utils.ApiUtils;
import org.mltooling.core.lab.*;
import org.mltooling.core.lab.model.LabProject;
import org.mltooling.core.lab.model.LabProjectConfig;
import org.mltooling.core.lab.model.LabService;
import org.mltooling.core.service.tests.IntegrationTest;
import org.mltooling.core.service.tests.LocalDockerLauncher;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.core.utils.SystemUtils;
import org.mltooling.lab.authorization.AuthorizationManager;
import org.mltooling.lab.services.CoreService;
import org.mltooling.lab.services.managers.DockerServiceManager;
import org.mltooling.lab.services.managers.KubernetesServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Category(IntegrationTest.class)
public class LabAdminApiTest {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(LabAdminApiTest.class);

  private static final long SLEEP = 5000;

  private static long MAX_WAIT_TIME = TimeUnit.MINUTES.toMillis(5);
  private static long WAIT_INTERVALS = TimeUnit.SECONDS.toMillis(10);

  private static final String ADMIN_USER = "foo";
  private static final String ADMIN_PASSWORD = "bar";

  private static final String TEST_USER_ID = "test";
  private static final String TEST_USER_PASSWORD = "test";

  private static final String NEW_WORKSPACE_IMAGE = "new-workspace:latest";
  private static final String DEFAULT_WORKSPACE_IMAGE = CoreService.WORKSPACE.getImage();


  // ================ Members ============================================= //
  @Rule public final LocalDockerLauncher dockerLauncher;
  @Rule public final TemporaryFolder tempFolder = new TemporaryFolder();

  // @ClassRule
  // set this namespace so that the .cleanup method clean lab-test resources and not
  // actual resources
  // Comment the other environment variables in if you start the test from within the IDE; otherwise
  // set it on the host machine / container.
  // public static final EnvironmentVariables environmentVariables =
  //     new EnvironmentVariables()
  //         .set(LabConfig.ENV_NAME_LAB_NAMESPACE, "lab-test")
  //         .set(LabConfig.ENV_NAME_K8S_NAMESPACE, "ml-test");
  private final String DEFAULT_HOST = SystemUtils.getEnvVar("SERVICE_HOST", "localhost");
  // by default use a port > 30000 so that it also works in Kubernetes mode (Kubernetes service ports must
  // by default be >30000)
  // private static final int LAB_PORT =
  //   StringUtils.isNullOrEmpty(SystemUtils.getEnvVar(LabConfig.ENV_NAME_LAB_PORT)) ? 30002 : Integer.parseInt(SystemUtils.getEnvVar(LabConfig.ENV_NAME_LAB_PORT));
  private final int LAB_PORT = StringUtils.isNullOrEmpty(LabConfig.LAB_PORT) ? 30002 : Integer.parseInt(LabConfig.LAB_PORT);
  private final int LAB_SERVICE_PORT =
    Integer.parseInt(SystemUtils.getEnvVar("LAB_SERVICE_PORT", "" + this.LAB_PORT));
  private final int DEFAULT_PROJECT_SERVICES_SIZE = 0; // no more initial project services

  private final Boolean IS_KIND_CLUSTER =
    Boolean.parseBoolean(SystemUtils.getEnvVar("IS_KIND_CLUSTER", "False"));

  private LabApi labApi;
  private LabAuthApi authorizationApi;
  private LabAdminApi labAdminApiAdminUser;
  private LabAdminApi labAdminApiNonAdminUser;

  private String testProject1;
  private String testProject2;

  private String serviceUrl;
  private String adminApiToken;
  private String adminAppToken;

  private String userApiToken;
  private String userAppToken;

  // ================ Constructors & Main ================================= //
  public LabAdminApiTest() throws Exception {
    resetUnirest();
    // currently service name needs to be "lab-service", otherwise the lab service (added in
    // core service) cannot be found
    final String dockerImage = LabConfig.BACKEND_SERVICE_IMAGE;

    if (ComponentManager.INSTANCE.isKubernetesRuntime()) {
      Map<String, String> envVars = new HashMap<>();
      envVars.put(LabConfig.ENV_NAME_SERVICES_RUNTIME, LabConfig.SERVICES_RUNTIME);
      envVars.put(LabConfig.ENV_NAME_SERVICES_CPU_LIMIT, "1");
      envVars.put(LabConfig.ENV_NAME_SERVICES_MEMORY_LIMIT, "2");
      envVars.put(LabConfig.ENV_NAME_K8S_NAMESPACE, LabConfig.K8S_NAMESPACE);
      envVars.put(
        LabConfig.ENV_NAME_HOST_ROOT_DATA_MOUNT_PATH, LabConfig.HOST_ROOT_DATA_MOUNT_PATH);
      this.dockerLauncher =
        new LocalDockerLauncher(DEFAULT_HOST, LAB_SERVICE_PORT, LAB_PORT, envVars, dockerImage, true);
    } else {
      Map<String, String> envVars = new HashMap<>();
      // Have to explicitly pass the workspace image as the backend image could be built with a different
      // SERVICE_VERSION, which makes the workspace tests fail
      envVars.put(LabConfig.ENV_NAME_WORKSPACE_IMAGE, LabConfig.WORKSPACE_IMAGE);
      this.dockerLauncher =
        new LocalDockerLauncher(DEFAULT_HOST, LAB_SERVICE_PORT, LAB_PORT, envVars, dockerImage, false);
    }

    Thread.sleep(SLEEP * 3);
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //
  @Before
  public void setUp() throws Exception {
    // Connect via API client to Service
    // final String SERVICE_URL = "http://" + DEFAULT_HOST + ":" + LAB_SERVICE_PORT;
    serviceUrl = "http://" + DEFAULT_HOST + ":" + LAB_SERVICE_PORT;
    adminAppToken = getAdminToken(serviceUrl);
    LabAuthApiClient authApi = new LabAuthApiClient(serviceUrl, adminAppToken);
    adminApiToken = authApi.createApiToken(ADMIN_USER).getData();

    log.info("Create Test User");
    authApi.createUser(TEST_USER_ID, TEST_USER_PASSWORD);
    log.info("Login Test User");
    userAppToken = authApi.loginUser(TEST_USER_ID, TEST_USER_PASSWORD).getData();

    log.info("Admin Token: " + adminApiToken);
    labApi = new LabApiClient(serviceUrl, adminApiToken);
    labAdminApiAdminUser =  new LabAdminApiClient(serviceUrl, adminApiToken);

    testProject1 = "test-" + System.currentTimeMillis();
    SingleValueFormat<LabProject> response =
      labApi.createProject(new LabProjectConfig(testProject1));

    resetUnirest();
    Assume.assumeThat(response.getMetadata("status"), not(500));

    // Add test user to project
    userAppToken = authApi.addUserToProject(TEST_USER_ID, testProject1).getData();
    userApiToken = authApi.createApiToken(TEST_USER_ID).getData();
    labAdminApiNonAdminUser =  new LabAdminApiClient(serviceUrl, userApiToken);

    log.info("User Api Token: " + userApiToken);

    testProject2 = "test-" + System.currentTimeMillis();
    response = labApi.createProject(new LabProjectConfig(testProject2));
    Assume.assumeThat(response.getMetadata("status"), not(500));

    try{
      this.dockerLauncher.tagDockerImage(LabConfig.WORKSPACE_IMAGE, NEW_WORKSPACE_IMAGE);
    } catch (Throwable throwable) {
      throwable.printStackTrace();
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
    if (ComponentManager.INSTANCE.isKubernetesRuntime()) {
      try {
        KubernetesServiceManager.cleanUpLab();
        Thread.sleep(SLEEP * 4); // sleep as the cleanup happens asynchronously
      } catch (Exception e) {
        log.error(e.getMessage());
      }
    } else {
      DockerServiceManager.cleanUpLab(false);
    }
  }

  @AfterClass
  public static void afterClass() throws Exception {
    if (ComponentManager.INSTANCE.isKubernetesRuntime()) {
      try {
        KubernetesServiceManager.cleanUpLab();
        Thread.sleep(SLEEP * 2); // sleep as the cleanup happens asynchronously
      } catch (Exception e) {
        log.error(e.getMessage());
      }
    } else {
      DockerServiceManager.cleanUpLab(false);
    }

    log.info("Finished tests and cleaned up");
  }

  @Test
  public void testWorkspaceHandling() throws Exception {
    log.info("Checking creation of default workspaces");
    // Admin should be able to check it's  own workspace
    SingleValueFormat<LabService>  response =  labAdminApiAdminUser.checkWorkspace(ADMIN_USER);
    assertThat(response.getStatus(), is(equalTo(200)));
    assertThat(response.getData().getDockerImage(), is(equalTo(DEFAULT_WORKSPACE_IMAGE)));
    // A  user should be able to check  it's own workspace
    SingleValueFormat<LabService>  responseTestUser =  labAdminApiNonAdminUser.checkWorkspace(TEST_USER_ID);
    assertThat(responseTestUser.getStatus(), is(equalTo(200)));
    assertThat(responseTestUser.getData().getDockerImage(), is(equalTo(DEFAULT_WORKSPACE_IMAGE)));
    // Admin should be able to check another user's workspace
    response =  labAdminApiAdminUser.checkWorkspace(TEST_USER_ID);
    assertThat(response.getStatus(), is(equalTo(200)));
    assertThat(response.getData().getDockerImage(), is(equalTo(DEFAULT_WORKSPACE_IMAGE)));
    // A  user shouldn't be able to check another user workspace
    responseTestUser =  labAdminApiNonAdminUser.checkWorkspace(ADMIN_USER);
    assertThat(responseTestUser.getStatus(), is(equalTo(401)));

    log.info("Checking creation of custom workspaces");
    // A  user should be able to create a custom workspace for himself
    responseTestUser =  labAdminApiNonAdminUser.resetWorkspace(TEST_USER_ID, NEW_WORKSPACE_IMAGE);
    assertThat(responseTestUser.getStatus(), is(equalTo(200)));
    assertThat(responseTestUser.getData().getDockerImage(), is(equalTo(NEW_WORKSPACE_IMAGE)));
    // Checking the workspace shouldn't change the image
    responseTestUser =  labAdminApiNonAdminUser.checkWorkspace(TEST_USER_ID);
    assertThat(responseTestUser.getStatus(), is(equalTo(200)));
    assertThat(responseTestUser.getData().getDockerImage(), is(equalTo(NEW_WORKSPACE_IMAGE)));
    // Resetting the workspace without specifying an image should spawn back the previously used image
    responseTestUser =  labAdminApiNonAdminUser.resetWorkspace(TEST_USER_ID, null);
    assertThat(responseTestUser.getStatus(), is(equalTo(200)));
    assertThat(responseTestUser.getData().getDockerImage(), is(equalTo(NEW_WORKSPACE_IMAGE)));
    responseTestUser =  labAdminApiNonAdminUser.checkWorkspace(TEST_USER_ID);
    assertThat(responseTestUser.getStatus(), is(equalTo(200)));
    assertThat(responseTestUser.getData().getDockerImage(), is(equalTo(NEW_WORKSPACE_IMAGE)));

    // A user shouldn't be able to  change the workspace of somebody else
    responseTestUser =  labAdminApiNonAdminUser.resetWorkspace(ADMIN_USER, null);
    assertThat(responseTestUser.getStatus(), is(equalTo(401)));
    responseTestUser =  labAdminApiNonAdminUser.resetWorkspace(ADMIN_USER, NEW_WORKSPACE_IMAGE);
    assertThat(responseTestUser.getStatus(), is(equalTo(401)));


    // Admin should be able to change the workspace of somebody else
    response =  labAdminApiAdminUser.resetWorkspace(ADMIN_USER, NEW_WORKSPACE_IMAGE);
    assertThat(response.getStatus(), is(equalTo(200)));
    assertThat(response.getData().getDockerImage(), is(equalTo(NEW_WORKSPACE_IMAGE)));
    response =  labAdminApiAdminUser.resetWorkspace(ADMIN_USER, DEFAULT_WORKSPACE_IMAGE);
    assertThat(response.getStatus(), is(equalTo(200)));
    assertThat(response.getData().getDockerImage(), is(equalTo(DEFAULT_WORKSPACE_IMAGE)));

    // Test if workspace is reachable via URL
    String urlAdminWorkspace =
      this.serviceUrl
        + "/workspace/id/"
        + ADMIN_USER
        + "/tree";

    // this call requires the Bearer token
    log.info("Requesting with admin token: " + urlAdminWorkspace);
    Integer status =
      Unirest.get(urlAdminWorkspace)
        .header(ApiUtils.AUTHORIZATION_HEADER, "Bearer " + adminAppToken)
        .getHttpRequest()
        .asString()
        .getStatus();
    assertThat(status, is(equalTo(200)));
    log.info("Requesting with user token: " + urlAdminWorkspace);
    status =
      Unirest.get(urlAdminWorkspace)
        .header(ApiUtils.AUTHORIZATION_HEADER, "Bearer " + userAppToken)
        .getHttpRequest()
        .asString()
        .getStatus();
    assertThat(status, is(equalTo(401)));


    // Test if workspace is reachable via URL
    String urlUserWorkspace =
      this.serviceUrl
        + "/workspace/id/"
        + TEST_USER_ID
        + "/tree";

    // this call requires the Bearer token
    log.info("Requesting with user Token: " + urlUserWorkspace);
    status =
      Unirest.get(urlUserWorkspace)
        .header(ApiUtils.AUTHORIZATION_HEADER, "Bearer " + userAppToken)
        .getHttpRequest()
        .asString()
        .getStatus();
    assertThat(status, is(equalTo(200)));
    // this call requires the Bearer token
    log.info("Requesting with admin Token: " + urlUserWorkspace);
    status =
      Unirest.get(urlUserWorkspace)
        .header(ApiUtils.AUTHORIZATION_HEADER, "Bearer " + adminAppToken)
        .getHttpRequest()
        .asString()
        .getStatus();
    assertThat(status, is(equalTo(200)));

  }


  // ================ Private Methods ===================================== //
  private String getAdminToken(String serviceUrl) {
    // all API calls in this test will be made via this user / the user's token
    authorizationApi = new LabAuthApiClient(serviceUrl);
    authorizationApi.createAdminUser(
      ADMIN_USER, ADMIN_PASSWORD, AuthorizationManager.DEFAULT_JWT_SECRET);
    return authorizationApi.loginUser(ADMIN_USER, ADMIN_PASSWORD).getData();
  }

  /**
   * This is somehow necessary, as Unirest will otherwise not accept '127.0.0.1' or 'localhost'
   * whereas it is the opposite of the {@link LabAdminApiTest#DEFAULT_HOST} value. Hence, if {@link
   * LabAdminApiTest#DEFAULT_HOST} is set to '127.0.0.1', Unirest will return a forbidden response when
   * making the endpoint call with the authToken (whereas it works when you manually set the host of
   * the endpoint call to 'localhost') and vice-versa. I assume that Unirest, since it is a
   * Singleton, makes some weird host-mapping or caching upon first request.
   *
   * @throws IOException
   */
  private void resetUnirest() throws IOException {

    Unirest.shutdown();
    Options.refresh();

    HttpClient httpClient = HttpClients.custom().disableCookieManagement().build();
    Unirest.setHttpClient(httpClient);
  }
}
