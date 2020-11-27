package org.mltooling.core.api.format.parser;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;
import org.mltooling.core.api.format.UnifiedFormat;
import org.mltooling.core.utils.structures.PropertyContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum JsonFormatParser {
  INSTANCE;
  // ================ Constants =========================================== //
  private static final Logger log = LoggerFactory.getLogger(JsonFormatParser.class);

  // ================ Members ============================================= //
  private JsonSerializer<PropertyContainer> propertyContainerSerializer =
      new JsonSerializer<PropertyContainer>() {

        @Override
        public JsonElement serialize(
            PropertyContainer src, Type typeOfSrc, JsonSerializationContext context) {

          return src == null ? null : context.serialize(src.getProperties());
        }
      };

  private JsonDeserializer<PropertyContainer> propertyContainerDeserializer =
      new JsonDeserializer<PropertyContainer>() {

        @Override
        public PropertyContainer deserialize(
            JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
          if (json == null) {
            return null;
          }

          try {
            Class<?> clazz = Class.forName(typeOfT.getTypeName());
            PropertyContainer item = (PropertyContainer) clazz.newInstance();
            item.setProperties(
                context.deserialize(json, new TypeToken<Map<String, Object>>() {}.getType()));
            return item;
          } catch (Exception e) {
            log.error(
                "Failed to parse object (" + typeOfT.getTypeName() + ") to PropertyContainer.", e);
            return null;
          }
        }
      };

  private JsonSerializer<Class> classSerializer =
      new JsonSerializer<Class>() {

        @Override
        public JsonElement serialize(Class src, Type typeOfSrc, JsonSerializationContext context) {
          return src == null ? null : new JsonPrimitive(src.getSimpleName());
        }
      };

  private JsonSerializer<Date> dateSerializer =
      new JsonSerializer<Date>() {

        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
          return src == null ? null : new JsonPrimitive(src.getTime());
        }
      };

  private JsonDeserializer<Date> dateDeserializer =
      new JsonDeserializer<Date>() {

        @Override
        public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
          return json == null ? null : new Date(json.getAsLong());
        }
      };

  private Gson gson =
      new GsonBuilder()
          .setPrettyPrinting()
          .registerTypeAdapter(Date.class, dateSerializer)
          .registerTypeAdapter(Date.class, dateDeserializer)
          .registerTypeAdapter(Class.class, classSerializer)
          .registerTypeHierarchyAdapter(PropertyContainer.class, propertyContainerSerializer)
          .registerTypeHierarchyAdapter(PropertyContainer.class, propertyContainerDeserializer)
          .create();

  // ================ Constructors & Main ================================= //

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Private Methods ===================================== //
  /* TODO
  private void correctPropertiesMap(HashMap<String, Object> properties) {
      for (String propertyName : properties.keySet()) {
          Object propertyValue = properties.get(propertyName);
          if (propertyValue != null && propertyValue instanceof Double) {
              Property property = Property.from(propertyName);
              if (property.isUnknown()) {
                  continue;
              }
              Class techType = property.getDetails().getTechtype();
              if (techType == null) {
                  continue;
              }

              if (techType == Integer.class) {
                  propertyValue = (Integer) ((Double) propertyValue).intValue();
              } else if (techType == Long.class) {
                  propertyValue = (Long) ((Double) propertyValue).longValue();
              } else if (techType == Float.class) {
                  propertyValue = (Float) ((Double) propertyValue).floatValue();
              } else if (techType == Date.class) {
                  //TODO change to real Date?
                  propertyValue = (Long) ((Double) propertyValue).longValue();
              } else {
                  continue;
              }
              properties.put(propertyName, propertyValue);
          }
      }
  }
  */

  // ================ Public Methods ====================================== //
  public Gson getGson() {
    return gson;
  }

  public <T> T fromJson(String json, Class<T> classType) {
    T parsedObject = gson.fromJson(json, classType);
    return parsedObject;
  }

  public <T> T fromJson(String json, Type typeOfT) {
    T parsedObject = gson.fromJson(json, typeOfT);
    return parsedObject;
  }

  public String toJson(UnifiedFormat unifiedFormat) {
    return gson.toJson(unifiedFormat);
  }

  public String toJson(PropertyContainer item) {
    return gson.toJson(item);
  }

  public String toJson(Object item) {
    return gson.toJson(item);
  }
  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
