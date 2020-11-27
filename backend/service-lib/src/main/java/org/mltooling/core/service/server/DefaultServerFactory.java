package org.mltooling.core.service.server;

import java.net.URI;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpHandlerRegistration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.mltooling.core.service.basics.endpoints.SystemApiEndpoint;
import org.mltooling.core.service.utils.GsonBodyReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultServerFactory {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(DefaultServerFactory.class);

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  public static void createAndStartServer(ServerConfig serverConfig) {
    // starting and configuring service
    final ResourceConfig rc =
        new ResourceConfig()
            .register(SystemApiEndpoint.class)
            .packages(serverConfig.getEndpointPackage());

    if (serverConfig.isLoggingActivated()) {
      // Configure Grizzly to print error messages
      java.util.logging.Logger l =
          java.util.logging.Logger.getLogger("org.glassfish.grizzly.http.server.HttpHandler");
      l.setLevel(Level.FINE);
      l.setUseParentHandlers(false);
      ConsoleHandler ch = new ConsoleHandler();
      ch.setLevel(Level.ALL);
      l.addHandler(ch);
    }

    if (serverConfig.getAuthFilter() != null) {
      rc.register(serverConfig.getAuthFilter());
    }

    rc.register(DefaultHeaders.class);
    rc.register(GsonBodyReader.class);
    rc.register(MultiPartFeature.class);

    if (serverConfig.getServerCallback() != null) {
      serverConfig.getServerCallback().onInit(rc);
    }

    HttpServer server =
        GrizzlyHttpServerFactory.createHttpServer(URI.create(serverConfig.getAddress()), rc);

    // configure api docs swagger gui
    HttpHandler swaggerHandler =
        new CLStaticHttpHandler(ClassLoader.getSystemClassLoader(), "/swagger/");
    server
        .getServerConfiguration()
        .addHttpHandler(
            swaggerHandler,
            HttpHandlerRegistration.builder()
                .contextPath(serverConfig.getApiDocsEndpoint())
                .urlPattern("/")
                .build()); // serverConfig.getApiDocsEndpoint());

    if (serverConfig.getServerCallback() != null) {
      serverConfig.getServerCallback().onStart(server);
    }

    log.info("Started service! Address: " + serverConfig.getAddress());

    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                new Runnable() {

                  @Override
                  public void run() {
                    if (serverConfig.getServerCallback() != null) {
                      serverConfig.getServerCallback().onShutdown();
                    }

                    log.info("Stopping service...");
                    server.shutdownNow();
                  }
                },
                "shutdownHook"));

    try {
      log.info("Press CTRL^C to exit.");
      Thread.currentThread().join();
    } catch (Exception e) {
      log.info("There was an error while starting Grizzly service.", e);
    }
  }
  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //

  public interface ServerCallback {

    void onInit(ResourceConfig rc);

    void onStart(HttpServer server);

    void onShutdown();
  }
}
