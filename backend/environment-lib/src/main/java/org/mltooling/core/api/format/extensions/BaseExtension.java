package org.mltooling.core.api.format.extensions;

import java.util.HashMap;
import java.util.Map;

public class BaseExtension {
  // ================ Constants =========================================== //

  // ================ Members ============================================= //
  private Map valueMap;

  // ================ Constructors & Main ================================= //

  public BaseExtension(Map valueMap) {
    this.valueMap = valueMap;
  }

  public BaseExtension() {
    valueMap = new HashMap<>();
  }
  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //
  public <T> T getValue(String key, Class<T> type) {
    Object value = getValueMap().get(key);
    if (value == null) {
      return null;
    }

    if (value instanceof Double) {
      if (Integer.class == type) {
        return (T) Integer.valueOf(((Double) value).intValue());
      } else if (Float.class == type) {
        return (T) Float.valueOf(((Double) value).floatValue());
      } else if (Long.class == type) {
        return (T) Long.valueOf(((Double) value).longValue());
      } else if (Double.class == type) {
        return (T) ((Double) value);
      }
    }

    if (String.class == type) {
      return (T) value.toString();
    }

    return (T) value;
  }

  public void addValue(String key, Object value) {
    getValueMap().put(key, value);
  }

  public Object getValue(String key) {
    return getValueMap().get(key);
  }

  public Map<String, Object> getValueMap() {
    if (valueMap == null) {
      valueMap = new HashMap<>();
    }
    return valueMap;
  }

  public void setValueMap(Map valueMap) {
    this.valueMap = valueMap;
  }
  // ================ Builder Pattern ===================================== //

  public BaseExtension withValue(String key, Object value) {
    addValue(key, value);
    return this;
  }
  // ================ Inner & Anonymous Classes =========================== //
}
