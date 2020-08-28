package org.mltooling.lab;

import com.mashape.unirest.http.Unirest;
import org.mltooling.core.service.server.DefaultServerFactory;
import org.mltooling.core.service.server.ServerConfig;
import org.mltooling.core.utils.FileUtils;
import org.mltooling.lab.endpoints.LabEndpoint;
import org.mltooling.lab.services.AbstractServiceManager;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpHandlerRegistration;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.logging.LogManager;


public class Launcher {

    // ================ Constants =========================================== //
    protected static final Logger log = LoggerFactory.getLogger(Launcher.class);

    private static final String API_DOCS_ENDPOINT_PATH = LabConfig.LAB_BASE_URL + "/api-docs";
    private static final String LAB_DOCS_ENDPOINT_PATH = LabConfig.LAB_BASE_URL + "/docs";
    private static final String WEBAPP_ENDPOINT_PATH = LabConfig.LAB_BASE_URL + "/app";

    // ================ Members ============================================= //
    static {
        // Tunnel logs from other logging frameworks to SLF4J
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
    }

    // ================ Constructors & Main ================================= //
    public static void main(String[] args) throws Exception {
        AbstractServiceManager serviceManager = ComponentManager.INSTANCE.getServiceManger();
        if (LabConfig.LAB_ACTION == LabConfig.LabAction.SERVE) {
            startService();
        } else if (LabConfig.LAB_ACTION == LabConfig.LabAction.UNINSTALL) {
            serviceManager.uninstallLab();
            System.exit(0);
        } else if (LabConfig.LAB_ACTION == LabConfig.LabAction.UPDATE) {
            // only update the backend
            serviceManager.updateLab(true);
            if (!serviceManager.isLabAvailable()) {
                log.error("Failed to update Lab");
                System.exit(1);
            }
            System.exit(0);
        } else if (LabConfig.LAB_ACTION == LabConfig.LabAction.UPDATE_FULL) {
            serviceManager.updateLab(false);
            if (!serviceManager.isLabAvailable()) {
                log.error("Failed to update Lab");
                System.exit(1);
            }
            System.exit(0);
        } else if (LabConfig.LAB_ACTION == LabConfig.LabAction.INSTALL || LabConfig.LAB_ACTION.isUnknown()) {
            // TODO: always use the DockerService Manager here? Than setup would be easier also for k8s
            serviceManager.installLab();
            if (!serviceManager.isLabAvailable()) {
                log.error("Failed to install Lab");
                System.exit(1);
            }
            System.exit(0);
        }
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //
    private static void startService() {
        startService(null);
    }

    private static void startService(@Nullable Integer port) {
        Unirest.setTimeouts(10000, 180000);

        AbstractServiceManager serviceManager = ComponentManager.INSTANCE.getServiceManger();
        if (!serviceManager.isLabAvailable()) {
            log.error("Failed to initialize Lab Landscape. Lab might not be installed correctly.");
            return;
        } else {
            log.info("Landscape is available.");
        }

        // if file was mounted into the lab backend, use it for terms-of-service
        File termsOfService = new File(LabConfig.TERMS_OF_SERVICE_FILE_PATH);
        if (termsOfService.exists()) {
            log.info("Reading terms of service file from: " + termsOfService.getPath());
            try {
                LabConfig.TERMS_OF_SERVICE_TEXT = FileUtils.fileToString(termsOfService);
            } catch (IOException e) {
                log.info("Failed to read terms of service file.");
            }
        }

        // add health check with isLabAvailable

        // if not in swarm -> swarm init?
        ServerConfig serverConfig = new ServerConfig(LabEndpoint.class.getPackage().toString())
                .setBaseUrl(LabConfig.LAB_BASE_URL)
                .setServicePort(port)
                .setLoggingActivated(true)
                .setApiDocsEndpoint(API_DOCS_ENDPOINT_PATH)
                .setServerCallback(new DefaultServerFactory.ServerCallback() {

                    @Override
                    public void onStart(HttpServer server) {

                        HttpHandler documentationHandler = new CLStaticHttpHandler(ClassLoader.getSystemClassLoader(), "/docs/");
                        //server.getServerConfiguration().addHttpHandler(documentationHandler, LAB_DOCS_ENDPOINT_PATH); // TODO add trailing slash with new libs?
                        server.getServerConfiguration().addHttpHandler(documentationHandler, HttpHandlerRegistration.builder().contextPath(LAB_DOCS_ENDPOINT_PATH).urlPattern("/").build());

                        HttpHandler webappHandler = new CLStaticHttpHandler(ClassLoader.getSystemClassLoader(), "/app/");
                        server.getServerConfiguration().addHttpHandler(webappHandler, HttpHandlerRegistration.builder().contextPath(WEBAPP_ENDPOINT_PATH).urlPattern("/").build());
                        //server.getServerConfiguration().addHttpHandler(webappHandler, WEBAPP_ENDPOINT_PATH); // TODO add trailing slash with new libs?

                        ComponentManager.INSTANCE.getJobManager().startJobScheduleMonitor();
                    }

                    @Override
                    public void onInit(ResourceConfig resourceConfig) {
                        ComponentManager.INSTANCE.getAuthManager().configureSecuritySettings(resourceConfig);
                    }

                    @Override
                    public void onShutdown() {
                    }
                });

        DefaultServerFactory.createAndStartServer(serverConfig);
    }
    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ================================ ===== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}