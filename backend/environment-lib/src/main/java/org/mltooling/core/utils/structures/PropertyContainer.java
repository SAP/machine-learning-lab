package org.mltooling.core.utils.structures;

import io.swagger.annotations.ApiModelProperty;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PropertyContainer<T extends PropertyContainer<T>> {
  // ================ Constants =========================================== //

  // ================ Members ============================================= //
  private Map<String, Object> properties = new HashMap<String, Object>();

  // ================ Constructors & Main ================================= //
  public PropertyContainer() {}

  public PropertyContainer(Map<String, Object> properties) {
    this.properties = properties;
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Public Methods ====================================== //
  public Long getLong(String key) {
    Object value = this.getProperty(key);
    if (value == null) {
      return null;
    }

    if (value instanceof Long) {
      return (Long) value;
    }

    if (value instanceof Double) {
      return (long) ((Double) value).doubleValue();
    }

    if (value instanceof Integer) {
      return ((Integer) value).longValue();
    }

    return Long.valueOf(value.toString());
  }

  protected Integer getInteger(String key) {
    Object value = this.getProperty(key);
    if (value == null) {
      return null;
    }

    if (value instanceof Double) {
      return ((Double) value).intValue();
    }

    if (value instanceof Integer) {
      return (Integer) value;
    }

    return Integer.valueOf(value.toString());
  }

  protected Double getDouble(String key) {
    Object value = this.getProperty(key);
    if (value == null) {
      return null;
    }

    if (value instanceof Double) {
      return (Double) value;
    }

    if (value instanceof Integer) {
      return (double) ((Integer) value).intValue();
    }

    return Double.valueOf(value.toString());
  }

  protected String getString(String key) {
    Object value = this.getProperty(key);
    if (value != null) {
      return value.toString();
    }
    return null;
  }

  protected Date getDate(String key) {
    Object value = this.getProperty(key);
    if (value == null) {
      return null;
    }

    if (value instanceof Long) {
      return new Date((Long) value);
    }

    if (value instanceof Double) {
      return new Date((long) ((Double) value).doubleValue());
    }

    return null;
  }

  @ApiModelProperty(hidden = true)
  public Map<String, Object> getProperties() {
    if (properties == null) {
      properties = new HashMap<String, Object>();
    }

    return this.properties;
  }

  public void setProperties(Map<String, Object> properties) {
    this.properties = new HashMap<>(properties);
  }

  public boolean hasProperty(String property) {
    return properties.containsKey(property);
  }

  public Object getProperty(String property) {
    return properties.get(property);
  }

  public Object removeProperty(String property) {
    return properties.remove(property);
  }

  public void setProperty(String property, Object value) {
    properties.put(property, value);
  }

  public T addProperty(String property, Object value) {
    this.setProperty(property, value);
    return (T) this;
  }

  @ApiModelProperty(hidden = true)
  public Iterable<String> getPropertyKeys() {
    return properties.keySet();
  }

  @ApiModelProperty(hidden = true)
  public <V> V getTypedProperty(String property) {
    return (V) getProperty(property);
  }

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
