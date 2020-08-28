package org.mltooling.core.service.basics.endpoints;

import org.mltooling.core.api.basics.SystemApi;
import org.mltooling.core.api.format.StatusMessageFormat;
import org.mltooling.core.service.auth.DefaultRoles;
import org.mltooling.core.service.basics.handlers.SystemApiHandler;
import org.mltooling.core.service.params.DefaultHeaderFields;
import org.mltooling.core.service.utils.AbstractApiEndpoint;
import org.mltooling.core.service.utils.UnifiedResponseFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;


@Api(value = SystemApi.ENDPOINT_PATH)
@Path(SystemApi.ENDPOINT_PATH)
public class SystemApiEndpoint extends AbstractApiEndpoint<SystemApiEndpoint> {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //
    private SystemApiHandler systemApiHandler;

    // ================ Constructors & Main ================================= //
    public SystemApiEndpoint(@Context UriInfo uriInfo, @Context HttpHeaders httpHeaders) {
        super(uriInfo, httpHeaders);

        // create api
        systemApiHandler = new SystemApiHandler();

        // register api
        this.registerHandler(systemApiHandler);
    }
    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //
    @GET
    @Path(SystemApi.METHOD_IS_HEALTHY)
    @ApiOperation(value = "Checks if service is healthy.", response = StatusMessageFormat.class)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(DefaultRoles.PUBLIC)
    public Response isHealthy(@BeanParam DefaultHeaderFields defaultHeaders) {
        return UnifiedResponseFactory.getResponse(systemApiHandler.isHealthy());
    }

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
