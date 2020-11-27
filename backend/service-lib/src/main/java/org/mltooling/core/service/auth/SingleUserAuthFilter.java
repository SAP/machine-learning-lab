package org.mltooling.core.service.auth;

import java.util.Base64;

public class SingleUserAuthFilter extends SingleTokenAuthFilter {

  // ================ Constants =========================================== //

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //

  public SingleUserAuthFilter(String user, String password) {
    super(Base64.getEncoder().encodeToString((user + ":" + password).getBytes()));
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
