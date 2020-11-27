package org.mltooling.core.env;

import com.mashape.unirest.http.Unirest;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import javax.annotation.Nullable;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Env {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(Env.class);

  private static Environment instance = null;

  // ================ Members ============================================= //
  static {
    try {
      HttpClient httpClient =
          HttpClients.custom()
              .setSSLContext(
                  new SSLContextBuilder()
                      .loadTrustMaterial(null, (x509Certificates, s) -> true)
                      .build())
              .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
              .build();
      Unirest.setHttpClient(httpClient);
    } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
      log.warn("Failed to ignore SSL Verify for Unirest.");
    }
  }

  // ================ Constructors & Main ================================= //
  protected Env() {
    // Exists only to defeat instantiation.
  }

  public static Environment init() {
    instance = new Environment();
    instance.printInfo();
    return instance;
  }

  public static Environment init(Path rootFolder) {
    instance = new Environment(rootFolder);
    instance.printInfo();
    return instance;
  }

  public static Environment init(String project, String endpoint, @Nullable String apiToken) {
    instance = new Environment(project, endpoint, apiToken);
    instance.printInfo();
    return instance;
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  public static Environment getInstance() {
    if (instance == null) {
      init();
    }
    return instance;
  }

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
