package org.mltooling.lab.endpoints;

import org.mltooling.core.service.utils.UnifiedResponseFactory;
import io.swagger.annotations.Api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URISyntaxException;


@Path("/")
@Api(hidden = true)
public class LabRootEndpoint {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //

    // ================ Constructors & Main ================================= //

    // ================ Methods for/from SuperClass / Interfaces ============ //
    @GET
    @Path("/")
    public Response getRoot() throws URISyntaxException {
        // Returns site that redirects to webapp (/app)
        String redirectPage = "";
        redirectPage += "<html>\r\n";
        redirectPage += "<head><title>ML Lab</title>";
        redirectPage += "<meta http-equiv=\"refresh\" content=\"0; url=./app\" />\r\n";
        redirectPage += "</head>\r\n";
        //redirectPage += "<p><a href=\"/app\">Redirect</a></p>\r\n";
        redirectPage += "</body>\r\n";
        redirectPage += "<html>\r\n";
        return UnifiedResponseFactory.getResponse(redirectPage, "text/html");
    }

    // TODO .. build more custom routing?: https://stackoverflow.com/questions/37196465/how-to-serve-static-content-and-resource-at-same-base-url-with-grizzly

    // ================ Public Methods ====================================== //

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
