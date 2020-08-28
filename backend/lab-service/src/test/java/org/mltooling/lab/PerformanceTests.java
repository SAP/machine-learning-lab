package org.mltooling.lab;

import com.mashape.unirest.http.Unirest;
import org.mltooling.core.lab.*;
import org.mltooling.core.lab.LabApi;
import org.mltooling.core.lab.LabApiClient;
import org.mltooling.core.lab.model.LabProjectConfig;
import org.mltooling.core.utils.PerfLogger;
import org.mltooling.lab.authorization.AuthorizationManager;
import com.spotify.docker.client.shaded.org.apache.http.conn.ssl.NoopHostnameVerifier;
import com.spotify.docker.client.shaded.org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class PerformanceTests {

    // ================ Constants =========================================== //
    private static final Logger log = LoggerFactory.getLogger(PerformanceTests.class);

    private static final String DEFAULT_PROTOCOL = "http";
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int SERVICE_TEST_PORT = 33855;

    private static final String ADMIN_USER = "foobar2";
    private static final String ADMIN_PASSWORD = "bar";

    // ================ Members ============================================= //

    private String serviceUrl;
    private String adminApiToken;

    // ================ Constructors & Main ================================= //

    public static void main(String[] args) throws Exception {
        HttpClient httpClient = HttpClients.custom()
                                           .setSSLContext(new SSLContextBuilder().loadTrustMaterial(null, (x509Certificates, s) -> true).build())
                                           .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                                           .build();
        Unirest.setHttpClient(httpClient);

        PerformanceTests performanceTests = new PerformanceTests();
        performanceTests.init("https", "localhost", 30001);

        //runTests(performanceTests);
        //performanceTests.deleteUsers(50);
        //performanceTests.deleteProjects(100);

        PerfLogger jobLogger = new PerfLogger();
        jobLogger.start();
        List<Thread> jobThreads = performanceTests.createJob(150);
        new Thread(() -> {
            for (Thread t : jobThreads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    log.error("Could not join thread");
                }
            }

            log.info("Creating jobs took " + jobLogger.end() + " milliseconds");
        }).start();
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //

    private static void runTests(PerformanceTests performanceTests) {
        final int numberProjects = 300;
        final int numberUsersWithWorkspaces = 100;

        PerfLogger perfLogger = new PerfLogger();
        perfLogger.start();

        PerfLogger createProjectsPerfLogger = new PerfLogger();
        createProjectsPerfLogger.start();
        List<Thread> projectThreads = performanceTests.createProjects(numberProjects);
        //performanceTests.createUsers(50);
        //performanceTests.checkWorkspaces(50);
        PerfLogger userAndWorkspaceCheckPerfLogger = new PerfLogger();
        userAndWorkspaceCheckPerfLogger.start();

        List<Thread> userAndWorkspaceThreads = performanceTests.createUserLoginAndCheckWorkspace(numberUsersWithWorkspaces);

        new Thread(() -> {
            for (Thread t : projectThreads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    log.error("Could not join thread");
                }
            }

            log.info("Creating Projects took " + createProjectsPerfLogger.end() + " milliseconds");
        }).start();

        new Thread(() -> {
            for (Thread t : userAndWorkspaceThreads) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    log.error("Could not join thread");
                }
            }

            log.info("Creating users and checking workspaces took " + userAndWorkspaceCheckPerfLogger.end() + " milliseconds");
        }).start();

        log.info("Running all tests took " + perfLogger.end() + " milliseconds");
    }

    private void init() {
        init(DEFAULT_PROTOCOL, DEFAULT_HOST, SERVICE_TEST_PORT);
    }

    private void init(String protocol, String host, int port) {
        serviceUrl = protocol + "://" + host + ":" + port;

        //new LocalDockerLauncher("lab-service", SERVICE_TEST_PORT, false, "");
        LabAuthApiClient authorizationApi = new LabAuthApiClient(serviceUrl);
        authorizationApi.createAdminUser(ADMIN_USER, ADMIN_PASSWORD, AuthorizationManager.DEFAULT_JWT_SECRET);
        String adminAppToken = authorizationApi.loginUser(ADMIN_USER, ADMIN_PASSWORD).getData();

        adminApiToken = new LabAuthApiClient(serviceUrl, adminAppToken).createApiToken(ADMIN_USER).getData();

        log.info("User Token: " + adminApiToken);
    }

    private void createUsers(int number) {
        for (int i = 0; i < number; i++) {
            final int j = i;
            new Thread(() -> {
                LabAuthApiClient authorizationApi = new LabAuthApiClient(serviceUrl);
                authorizationApi.createUser("user-" + j, "password-" + j);
            }).start();
        }
    }

    private void checkWorkspaces(int number) {
        for (int i = 0; i < number; i++) {
            final int j = i;
            new Thread(() -> {
                LabAdminApiClient labAdminApiClient = new LabAdminApiClient(serviceUrl, adminApiToken);
                labAdminApiClient.checkWorkspace("user-" + j);
            }).start();
        }
    }

    private List<Thread> createJob(int number) {
        List<Thread> threads = new ArrayList<>();
        final String projectName = "stress";
        final LabApi labApi = new LabApiClient(serviceUrl);
        try {
            labApi.createProject(new LabProjectConfig(projectName));
        } catch (Exception ignored) {
        }

        for (int i = 0; i < number; i++) {
            Thread t = new Thread(() -> new LabApiClient(serviceUrl)
                    .deployJob(projectName, "simple-stresstest-job:0.1.8",
                               "Stresstest", "", null));
            t.start();
            threads.add(t);
        }

        return threads;
    }

    private void deleteProjects(int number) {
        for (int i = 0; i < number; i++) {
            LabApi labApi = new LabApiClient(serviceUrl);
            labApi.deleteProject("subnet-test-" + i);
        }
    }

    private void deleteUsers(int number) {
        for (int i = 0; i < number; i++) {
            LabAuthApiClient authorizationApi = new LabAuthApiClient(serviceUrl);
            authorizationApi.deleteUser("user-" + i);
        }
    }

    private List<Thread> createUserLoginAndCheckWorkspace(int number) {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            final int j = i;
            Thread t = new Thread(() -> {
                new LabAuthApiClient(serviceUrl).createUser("user-" + j, "password-" + j);
                new LabAdminApiClient(serviceUrl, adminApiToken).checkWorkspace("user-" + j);
            });
            t.start();
            threads.add(t);
        }

        return threads;
    }

    private List<Thread> createProjects(int number) {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            final int j = i;
            Thread t = new Thread(() -> {
                LabApiClient labApi = new LabApiClient(serviceUrl, adminApiToken);
                labApi.createProject(new LabProjectConfig("subnet-test-" + j));
            });
            t.start();
            threads.add(t);
        }
        return threads;
    }
    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
