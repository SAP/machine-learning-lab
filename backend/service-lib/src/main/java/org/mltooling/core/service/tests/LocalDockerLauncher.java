package org.mltooling.core.service.tests;

import org.mltooling.core.api.basics.SystemApiClient;
import org.mltooling.core.utils.StringUtils;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class LocalDockerLauncher extends ExternalResource {

    // ================ Constants =========================================== //
    private static final Logger log = LoggerFactory.getLogger(LocalDockerLauncher.class);

    private static final String BUILD_SCRIPT_NAME = "build.py";
    private static final String RUN_SCRIPT_NAME = "run.py";
    private static final int DEFAULT_PORT = 8090;

    // ================ Members ============================================= //
    private Integer servicePort;
    private boolean serviceProxyActivated;
    private String serviceName;
    private String additionalArgs;

    // ================ Constructors & Main ================================= //
    public LocalDockerLauncher(String serviceName, Integer servicePort, boolean serviceProxyActivated, @Nullable String additionalRunArgs)
            throws Exception {
        this.serviceName = serviceName;
        this.servicePort = servicePort;
        this.serviceProxyActivated = serviceProxyActivated;
        this.additionalArgs = additionalRunArgs;
        init();
    }

    public LocalDockerLauncher(String serviceName) throws Exception {
        this.serviceName = serviceName;
        init();
    }

    private void init() throws Exception {
        String proxy = "";
        if (this.serviceProxyActivated) {
            proxy = " --proxy";
        }

        log.info("Building docker container in " + getExecutionPath());
        executeCommand("python " + BUILD_SCRIPT_NAME + " --docker --name " + this.serviceName + proxy);
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //
    @Override
    protected void before() throws Throwable {
        log.info("Starting docker container");
        // System.out.println(System.getProperty("user.dir"));

        if (getExecutionPath() != null) {
            String port = "";
            if (this.servicePort != null) {
                port = " --port " + String.valueOf(this.servicePort);
            }

            String proxy = "";
            if (this.serviceProxyActivated) {
                proxy = " --proxy";
            }

            if (StringUtils.isNullOrEmpty(additionalArgs)) {
                additionalArgs = "";
            } else {
                additionalArgs = " " + additionalArgs;
            }

            executeCommand("python " + RUN_SCRIPT_NAME + " --name " + this.serviceName + port + proxy + additionalArgs);

            do {
                Thread.sleep(5000);
            }
            while (!new SystemApiClient(getServiceUrl()).isHealthy().isSuccessful());
        }
    }

    @Override
    protected void after() {
        log.info("Stopping docker container");
        try {
            if (getExecutionPath() != null) {
                executeCommand("docker rm -f " + this.serviceName);
                Thread.sleep(500);
            }
        } catch (Exception e) {
            log.error("Failed to stop docker container");
        }
    }

    // ================ Public Methods ====================================== //
    public String getServiceUrl() {
        return "http://localhost:" + (this.servicePort != null ? String.valueOf(this.servicePort) : String.valueOf(DEFAULT_PORT));
    }

    // ================ Private Methods ===================================== //
    private void executeCommand(String command) throws IOException, InterruptedException {
        log.info("Executing command: " + command);
        String line;
        Process p = Runtime.getRuntime().exec(command);
        //p.waitFor();
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        while ((line = in.readLine()) != null) {
            log.info(line);
        }
        in.close();
    }

    private Path getExecutionPath() throws Exception {
        Path pwd = Paths.get("");
        if (!Files.exists(pwd)) {
            throw new Exception("Project path does not exist " + pwd.toAbsolutePath());
        }

        if (!Files.exists(pwd.resolve(BUILD_SCRIPT_NAME))) {
            throw new Exception(BUILD_SCRIPT_NAME + " does not exist in " + pwd.toAbsolutePath());
        }

        if (!Files.exists(pwd.resolve(RUN_SCRIPT_NAME))) {
            throw new Exception(RUN_SCRIPT_NAME + " does not exist in " + pwd.toAbsolutePath());
        }
        return pwd;
    }

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}