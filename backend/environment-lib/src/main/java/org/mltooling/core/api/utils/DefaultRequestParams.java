package org.mltooling.core.api.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mltooling.core.utils.StringUtils;

public class DefaultRequestParams {

  // ================ Constants =========================================== //
  public static final String FIELDS = "fields";
  public static final String LIMIT = "limit";
  public static final String EXCLUDE = "exclude";
  public static final String SELECT = "select";
  public static final String DEBUG = "debug";

  // ================ Members ============================================= //
  private Map<String, String> queryParams = new HashMap<>();

  // ================ Constructors & Main ================================= //
  public DefaultRequestParams() {}

  public DefaultRequestParams(Map<String, String> queryParams) {
    setParams(queryParams);
  }
  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //
  public Integer getLimit() {
    String limitStr = getParam(DefaultRequestParams.LIMIT);
    if (StringUtils.isNullOrEmpty(limitStr)) {
      return null;
    }
    return Integer.valueOf(limitStr);
  }

  public DefaultRequestParams setLimit(Integer limit) {
    if (limit != null) {
      setParam(DefaultRequestParams.LIMIT, String.valueOf(limit));
    }

    return this;
  }

  public List<String> getFields() {
    return ApiUtils.getListFromParam(getParam(DefaultRequestParams.FIELDS));
  }

  public DefaultRequestParams setFields(String fields) {
    if (!StringUtils.isNullOrEmpty(fields)) {
      setParam(DefaultRequestParams.FIELDS, fields);
    }
    return this;
  }

  public boolean isDebug() {
    String debugStr = getParam(DefaultRequestParams.DEBUG);
    if (StringUtils.isNullOrEmpty(debugStr)) {
      return false;
    }
    return Boolean.valueOf(debugStr);
  }

  public void setDebug(String debug) {
    if (StringUtils.isNullOrEmpty(debug)) {
      return;
    }

    setParam(DefaultRequestParams.DEBUG, debug);
  }

  /** exclude: entity type, relation type, origin, */
  public List<String> getExcludedTypes() {
    return ApiUtils.getListFromParam(getParam(DefaultRequestParams.EXCLUDE));
  }

  public DefaultRequestParams setExcludedTypes(String excludedTypes) {
    if (!StringUtils.isNullOrEmpty(excludedTypes)) {
      setParam(DefaultRequestParams.EXCLUDE, excludedTypes);
    }
    return this;
  }

  /** select: entity type, relation type, origin */
  public List<String> getSelectedTypes() {
    return ApiUtils.getListFromParam(getParam(DefaultRequestParams.SELECT));
  }

  public DefaultRequestParams setSelectedTypes(String selectedTypes) {
    if (!StringUtils.isNullOrEmpty(selectedTypes)) {
      setParam(DefaultRequestParams.SELECT, selectedTypes);
    }

    return this;
  }

  public Map<String, String> getParams() {
    return queryParams;
  }

  public DefaultRequestParams setParams(Map<String, String> queryParams) {
    this.queryParams = queryParams;
    return this;
  }

  public DefaultRequestParams setParam(String param, String value) {
    queryParams.put(param, value);
    return this;
  }

  public String getParam(String param) {
    return queryParams.get(param);
  }

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
