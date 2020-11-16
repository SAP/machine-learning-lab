package org.mltooling.lab.services;

import com.spotify.docker.client.ProgressHandler;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ProgressMessage;
import java.util.*;
import org.mltooling.core.utils.ListUtils;
import org.mltooling.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DockerUtils {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(DockerUtils.class);

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //
  public enum ContainerState {
    HEALTHY("healthy"),
    // only when healthcheck is implemented
    UNHEALTHY("unhealthy"),
    // only when healthcheck is implemented
    STARTING("starting"),
    // only when healthcheck is implemented
    RUNNING("running"),
    STOPPED("stopped"),
    KILLED("killed"),
    PAUSED("paused"),
    CREATED("created"),
    UNKNOWN("unknown");

    private String name;

    ContainerState(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public static ContainerState from(String type) {
      if (StringUtils.isNullOrEmpty(type)) {
        return UNKNOWN;
      }

      for (ContainerState mode : ContainerState.values()) {
        if (type.equalsIgnoreCase(mode.getName())) {
          return mode;
        }
      }
      return UNKNOWN;
    }

    @Override
    public String toString() {
      return getName();
    }

    public boolean isUnknown() {
      return this == UNKNOWN;
    }
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //
  public static List<String> convertEnvMapToList(Map<String, String> envVariables) {
    List<String> convertedVars = new ArrayList<>();
    for (String env : envVariables.keySet()) {
      convertedVars.add(env + "=" + envVariables.get(env));
    }
    return convertedVars;
  }

  public static Map<String, String> convertEnvListToMap(List<String> envVariables) {
    if (ListUtils.isNullOrEmpty(envVariables)) {
      return new HashMap<>();
    }

    Map<String, String> envVarMap = new HashMap<>();
    for (String envVar : envVariables) {
      String[] envVarSplit =
          envVar.split(
              "=",
              2); // just split by first equal sign (limit: 2-1=1); all following equal signs belong
                  // to the env's value
      if (envVarSplit.length == 2) {
        envVarMap.put(envVarSplit[0].trim(), envVarSplit[1].trim());
      }
    }
    return envVarMap;
  }

  public static String getEnvVariableValue(List<String> envVariables, String envVariableName) {
    if (envVariables == null || StringUtils.isNullOrEmpty(envVariableName)) {
      return "";
    }

    for (String envVariable : envVariables) {
      if (envVariable.startsWith(envVariableName)) {
        return envVariable.replace(envVariableName + "=", "");
      }
    }
    return "";
  }

  public static String extractNameFromImage(String image) {
    String serviceName = image;

    if (serviceName.contains("/")) {
      serviceName = serviceName.substring(serviceName.lastIndexOf("/") + 1, serviceName.length());
    }

    if (serviceName.contains(":")) {
      serviceName = serviceName.substring(0, serviceName.lastIndexOf(":"));
    }
    return serviceName;
  }

  public static String extractVersionFromImage(String image) {
    if (!image.contains(":")) {
      log.warn("Image name has no version.");
      return "";
    }
    String[] imageNameSplit = image.split(":");
    return imageNameSplit[imageNameSplit.length - 1];
  }

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //

  public static class DebugProgressHandler implements ProgressHandler {

    @Override
    public void progress(ProgressMessage progressMessage) throws DockerException {
      // only log via debug
      log.debug(progressMessage.toString());
    }
  }

  @SuppressWarnings("SpellCheckingInspection")
  public static class Network {

    static final int MAX_NUMBER = 255; // 8 bits
    static final int FIRST_THREE_OCTETS = 3;

    private String cidr;
    private String[] octets;
    private int[] octetsAsIntegers;
    private String prefixLength;
    private String gateway;

    public Network() {}

    /**
     * The gateway is set to x.y.z.1 whereas x.y.z are the first three octets of the cidr.
     *
     * @param cidr in the form of x.y.z.0/X where X is the prefixLength of the network
     */
    public Network(String cidr) {
      this.cidr = cidr;
      String[] ipAddressAndPrefixLength = cidr.split("/");
      octets = ipAddressAndPrefixLength[0].split("\\.");
      octetsAsIntegers = Arrays.stream(octets).mapToInt(Integer::parseInt).toArray();
      prefixLength = ipAddressAndPrefixLength[1];
      gateway = String.format("%s.%s.%s.1", octets[0], octets[1], octets[2]);
    }

    /**
     * Compare two network addresses octet-wise and return the highest network address. Examples: -
     * 172.33.0.0 is greater than 171.33.0.0 - 172.34.0.0 is greater than 172.33.255.255 -
     * 172.34.10.0 is greater than 172.34.9.0
     *
     * @param cidr1
     * @param cidr2
     * @return the higher network address or cidr2 in case both ip addresses are identical
     */
    public static Network getHigherCidr(Network cidr1, Network cidr2) {
      for (int i = 0; i < FIRST_THREE_OCTETS; i++) {
        if (cidr1.getOctetsAsIntegers()[i] > cidr2.getOctetsAsIntegers()[i]) {
          return cidr1;
        } else if (cidr1.getOctetsAsIntegers()[i] < cidr2.getOctetsAsIntegers()[i]) {
          return cidr2;
        }
      }

      return cidr2;
    }

    /**
     * Increases the ip address by 1, starting at the third octet. Examples: - network ipaddress =
     * 172.33.0.12 => 172.33.1.0 - network ipaddress = 172.33.255.39 => 172.34.0.0 - network
     * ipaddress = 255.255.255.0 => 255.255.255.0
     *
     * @param network the ip address to be increased
     * @return the increased network or the same network if it's ip address was "255.255.255.0"
     */
    public static Network nextSubnet(Network network) {
      if (network.getOctetsAsIntegers()[0] == MAX_NUMBER
          && network.getOctetsAsIntegers()[1] == MAX_NUMBER
          && network.getOctetsAsIntegers()[2] == MAX_NUMBER) {
        return network;
      }

      int[] newOctets = {
        network.getOctetsAsIntegers()[0],
        network.getOctetsAsIntegers()[1],
        network.getOctetsAsIntegers()[2],
        network.getOctetsAsIntegers()[3]
      };
      // just consider first three octets as last one is always 0 for our subnets
      for (int i = FIRST_THREE_OCTETS - 1; i >= 0; i--) {
        int octet = network.getOctetsAsIntegers()[i] + 1;
        if (octet > MAX_NUMBER) {
          newOctets[i] = 0;
        } else {
          newOctets[i] = octet;
          break;
        }
      }

      return new Network(
          String.format(
              "%s.%s.%s.%s/%s",
              newOctets[0], newOctets[1], newOctets[2], newOctets[3], network.getPrefixLength()));
    }

    @Override
    public String toString() {
      return this.cidr;
    }

    public String[] getOctets() {
      return octets;
    }

    public Network setOctets(String[] octets) {
      this.octets = octets;
      return this;
    }

    public int[] getOctetsAsIntegers() {
      return octetsAsIntegers;
    }

    public Network setOctetsAsIntegers(int[] octetsAsIntegers) {
      this.octetsAsIntegers = octetsAsIntegers;
      return this;
    }

    public String getPrefixLength() {
      return prefixLength;
    }

    public Network setPrefixLength(String prefixLength) {
      this.prefixLength = prefixLength;
      return this;
    }

    public String getGateway() {
      return gateway;
    }

    public Network setGateway(String gateway) {
      this.gateway = gateway;
      return this;
    }
  }
}
