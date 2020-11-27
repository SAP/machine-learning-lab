package org.mltooling.core.api.utils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import org.mltooling.core.utils.ListUtils;
import org.mltooling.core.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ApiUtils {

  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(ApiUtils.class);
  public static final String LIST_PARAM_SPLIT_DELIMITER = ",";

  public static final String CONTENT_TYPE_JSON = "application/json";
  public static final String CONTENT_TYPE_PLAIN = "text/plain";
  public static final String CONTENT_TYPE_HEADER = "Content-Type";
  public static final String AUTHORIZATION_HEADER = "Authorization";

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //
  private ApiUtils() {}

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //
  public static String getParamOfList(Object... objects) {
    String param = "";
    if (ListUtils.isNullOrEmpty(objects)) {
      return null;
    }
    for (Object o : objects) {
      if (!StringUtils.isNullOrEmpty(o.toString())) {
        param += o.toString() + LIST_PARAM_SPLIT_DELIMITER;
      }
    }
    param = StringUtils.removeLastComma(param);
    return param;
  }

  public static String getParamOfList(Collection params) {
    String param = "";
    if (ListUtils.isNullOrEmpty(params)) {
      return null;
    }
    for (Object p : params) {
      if (p != null && !StringUtils.isNullOrEmpty(p.toString())) {
        param += p.toString() + LIST_PARAM_SPLIT_DELIMITER;
      }
    }
    param = StringUtils.removeLastComma(param);
    return param;
  }

  public static List<String> getListFromParam(String listParam) {
    List<String> resultList = new ArrayList<>();

    if (StringUtils.isNullOrEmpty(listParam)) {
      return resultList;
    }

    String[] paramSplit = listParam.split(LIST_PARAM_SPLIT_DELIMITER);
    for (String param : paramSplit) {
      param = param.trim();
      resultList.add(param);
    }

    return resultList;
  }

  public static String resolveBasicAuthToken(String user, String password) {
    try {
      return "Basic "
          + Base64.getEncoder().encodeToString((user + ":" + password).getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      log.error("Problem while encoding auth token", e);
      return null;
    }
  }

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
