package org.mltooling.core.service.basics.endpoints;

import org.mltooling.core.service.server.ServerConfig;
import org.mltooling.core.service.utils.UnifiedResponseFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URISyntaxException;


@Path("/")
public class DocsRootEndpoint {
    // TODO only uses the default api docs endpoint path -> can be reconfigured

    // ================ Constants =========================================== //

    // ================ Members ============================================= //

    // ================ Constructors & Main ================================= //

    // ================ Methods for/from SuperClass / Interfaces ============ //
    @GET
    public Response getRoot() throws URISyntaxException {
        // Returns site that redirects to the docs
        String redirectPage = "";
        redirectPage += "<html>\r\n";
        redirectPage += "<head><title>Redirect...</title>";
        redirectPage += "<meta http-equiv=\"refresh\" content=\"0; url=." + ServerConfig.DEFAULT_API_DOCS_ENDPOINT + "\" />\r\n";
        redirectPage += "</head>\r\n";
        redirectPage += "</body>\r\n";
        redirectPage += "<html>\r\n";
        return UnifiedResponseFactory.getResponse(redirectPage, "text/html");
    }

    // ================ Public Methods ====================================== //

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
