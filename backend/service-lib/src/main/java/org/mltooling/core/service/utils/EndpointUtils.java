package org.mltooling.core.service.utils;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import org.mltooling.core.utils.StringUtils;

public final class EndpointUtils {

  // ================ Constants =========================================== //
  public static final String CONSUMES_CHARSET = "; charset=UTF-8";
  private static Charset UTF8 = Charset.forName("UTF-8");

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //
  public static Map<String, String> converHeadersToMap(HttpHeaders httpHeaders) {
    HashMap<String, String> result = new HashMap<>();
    for (String param : httpHeaders.getRequestHeaders().keySet()) {
      String value = httpHeaders.getRequestHeaders().getFirst(param);
      if (!StringUtils.isNullOrEmpty(value)) {
        result.put(param, value);
      }
    }
    return result;
  }

  public static Map<String, String> convertUriInfoToMap(UriInfo uriInfo) {
    HashMap<String, String> result = new HashMap<>();
    for (String param : uriInfo.getQueryParameters().keySet()) {
      String value = uriInfo.getQueryParameters().getFirst(param);
      if (!StringUtils.isNullOrEmpty(value)) {
        result.put(param, value);
      }
    }
    return result;
  }

  public static Map<String, Object> splitQuery(URL url) throws UnsupportedEncodingException {
    Map<String, Object> query_pairs = new LinkedHashMap<>();
    String query = url.getQuery();
    if (query == null) {
      return query_pairs;
    }
    String[] pairs = query.split("&");
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      query_pairs.put(
          URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
          URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    }
    return query_pairs;
  }

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
