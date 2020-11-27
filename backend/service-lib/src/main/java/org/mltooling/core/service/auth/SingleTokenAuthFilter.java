package org.mltooling.core.service.auth;

import java.io.IOException;
import java.util.Arrays;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.mltooling.core.utils.StringUtils;

public class SingleTokenAuthFilter implements ContainerRequestFilter {

  // ================ Constants =========================================== //
  private static final String PREFIX_BASIC_AUTH = "Basic";
  // ================ Members ============================================= //
  private String cleanedToken;

  @Context private ResourceInfo resourceInfo;
  // ================ Constructors & Main ================================= //

  public SingleTokenAuthFilter(String token) {
    this.cleanedToken = cleanToken(token);
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  @Override
  public void filter(ContainerRequestContext containerRequestContext) throws IOException {
    if (containerRequestContext.getMethod().equalsIgnoreCase(HttpMethod.OPTIONS)
        || StringUtils.isNullOrEmpty(cleanedToken)
        || (resourceInfo.getResourceMethod().isAnnotationPresent(RolesAllowed.class)
            && Arrays.asList(
                    resourceInfo.getResourceMethod().getAnnotation(RolesAllowed.class).value())
                .contains(DefaultRoles.PUBLIC))) {
      return;
    }

    String authorizationHeaderValue =
        cleanToken(containerRequestContext.getHeaders().getFirst("Authorization"));
    if (StringUtils.isNullOrEmpty(authorizationHeaderValue)
        || !authorizationHeaderValue.contains(cleanedToken)) {
      abort(containerRequestContext);
    }
  }

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //
  private void abort(ContainerRequestContext containerRequestContext) {
    containerRequestContext.abortWith(
        Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic").build());
  }

  private String cleanToken(String token) {
    if (StringUtils.isNullOrEmpty(token)) {
      return token;
    }

    if (token.startsWith(PREFIX_BASIC_AUTH)) {
      return token.replace(PREFIX_BASIC_AUTH, "").trim();
    }
    return token;
  }
  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
