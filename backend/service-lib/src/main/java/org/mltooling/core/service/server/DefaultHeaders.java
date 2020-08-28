package org.mltooling.core.service.server;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;


@Provider
public class DefaultHeaders implements ContainerResponseFilter {

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    @Override
    public void filter(final ContainerRequestContext requestContext,
                       final ContainerResponseContext cres) {
        cres.getHeaders().add(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        cres.getHeaders().add(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS, DELETE");
        cres.getHeaders().add("Access-Control-Allow-Headers", "X-Custom-Header, Content-Type, X-th-csrf, Authorization");
    }
}