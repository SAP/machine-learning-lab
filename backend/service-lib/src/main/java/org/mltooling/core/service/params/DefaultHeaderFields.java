package org.mltooling.core.service.params;

import io.swagger.annotations.ApiParam;
import javax.ws.rs.HeaderParam;

public class DefaultHeaderFields {
  // ================ Constants =========================================== //

  // ================ Members ============================================= //
  @ApiParam(value = "Authorization Token", required = false)
  @HeaderParam(value = "authorization")
  public String authorization;

  // ================ Constructors & Main ================================= //

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
