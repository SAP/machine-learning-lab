package org.mltooling.core.service.tests;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.rules.ExternalResource;
import org.mltooling.core.api.basics.SystemApiClient;
import org.mltooling.core.utils.StringUtils;
import org.mltooling.core.utils.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalDockerLauncher extends ExternalResource {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(LocalDockerLauncher.class);
  private static final String LAB_TEST_INSTALLATION_CONTAINER = "lab-test-installation";

  private static final String ENV_KUBE_CONFIG_PATH =
      SystemUtils.getEnvVar("KUBE_CONFIG_PATH", null);

  // ================ Members ============================================= //
  private String serviceHost;
  private Integer servicePort;

  private Map<String, String> envVars;
  private String dockerImage;
  private boolean isKubernetes;

  // ================ Constructors & Main ================================= //

  public LocalDockerLauncher(String serviceHost, Integer servicePort, Map<String, String> envVars,
      String dockerImage, boolean isKubernetes) {
    this.serviceHost = serviceHost;
    this.servicePort = servicePort;
    this.envVars = (envVars == null) ? new HashMap<>() : envVars;
    this.dockerImage = dockerImage;
    this.isKubernetes = isKubernetes;
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //
  @Override
  protected void before() throws Throwable {
    log.info("Starting docker container");
    executeCommand(getDockerInstallCommand(false));
    do {
      Thread.sleep(5000);
    } while (!new SystemApiClient(getServiceUrl()).isHealthy().isSuccessful());
  }

  @Override
  protected void after() {
    log.info("Stopping docker container");
    try {
      executeCommand(getDockerInstallCommand(true));
      Thread.sleep(10000);
    } catch (Exception e) {
      log.error("Failed to stop docker container");
    }
  }

  private String getDockerInstallCommand(boolean isUninstall) {
    StringBuilder envArgsBuilder = new StringBuilder("");
    for (Entry<String, String> envEntry : envVars.entrySet()) {
      envArgsBuilder.append(String.format(" --env %s=%s", envEntry.getKey(), envEntry.getValue()));
    }


    String kubernetesArgs = "";
    if (isKubernetes) {
      kubernetesArgs = " --env SERVICES_RUNTIME=k8s";
      if (!StringUtils.isNullOrEmpty(ENV_KUBE_CONFIG_PATH)) {
        kubernetesArgs = kubernetesArgs + " -v " + ENV_KUBE_CONFIG_PATH + ":/root/.kube/";
      } else {
        kubernetesArgs = kubernetesArgs + " -v $HOME/.kube/config:/root/.kube/config";
      }
    }

    // TODO: use the Java Client here for more flexibility
    String labAction = (isUninstall) ? "uninstall" : "install";
    String dockerCommand = getDockerPath();
    return dockerCommand + " run --rm --name " + LAB_TEST_INSTALLATION_CONTAINER
        + " -v /var/run/docker.sock:/var/run/docker.sock" + kubernetesArgs
        + " --env LAB_DEBUG=true --env LAB_NAMESPACE=lab-test " + " --env LAB_ACTION=" + labAction
        + " --env LAB_PORT=" + servicePort + envArgsBuilder.toString() + " " + dockerImage;
  }

  // ================ Public Methods ====================================== //
  public String getServiceUrl() {
    return "http://" + this.serviceHost + ":" + this.servicePort;
  }

  // ================ Private Methods ===================================== //

  private static String getDockerPath() {
    final String dockerPath = "/usr/bin/docker"; // path on Ubuntu
    File f = new File(dockerPath);
    if (f.exists() && !f.isDirectory()) {
      return dockerPath;
    }

    // path on Mac
    return "/usr/local/bin/docker";
  }

  private void executeCommand(String command) throws IOException, InterruptedException {
    log.info("Executing command: {}", command);
    String line;
    Process p = Runtime.getRuntime().exec(new String[] {"/bin/bash", "-c", command});
    // p.waitFor();
    // BufferedReader inErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    // while ((line = inErr.readLine()) != null) {
    // log.info(line);
    // }
    // inErr.close();
    BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
    while ((line = in.readLine()) != null) {
      log.info(line);
    }
    in.close();
  }

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
