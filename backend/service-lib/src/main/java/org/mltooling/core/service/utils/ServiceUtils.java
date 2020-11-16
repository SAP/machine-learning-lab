package org.mltooling.core.service.utils;

import java.net.InetSocketAddress;
import java.net.Socket;

public final class ServiceUtils {
  // ================ Constants =========================================== //

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //
  public static Integer getPortFromArgs(String[] args) {
    Integer port = null;
    if (args != null && args.length > 0) {
      try {
        port = Integer.valueOf(args[0]);
      } catch (NumberFormatException e) {
        port = null;
      }
    }
    return port;
  }

  public static boolean serverListening(String host, int port) {
    Socket s = null;
    try {
      s = new Socket();
      s.setSoTimeout(1000);
      s.connect(new InetSocketAddress(host, port), 1000);
      return true;
    } catch (Exception e) {
      return false;
    } finally {
      if (s != null) {
        try {
          s.close();
        } catch (Exception e) {
        }
      }
    }
  }

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
