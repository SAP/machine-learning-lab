package org.mltooling.core.api.format;

import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import org.mltooling.core.api.format.parser.JsonFormatParser;

public class SingleValueFormat<T> extends UnifiedFormat {
  // ================ Constants =========================================== //

  // ================ Members ============================================= //
  private T data;

  // ================ Constructors & Main ================================= //
  public SingleValueFormat() {}

  public SingleValueFormat(T data) {
    this.data = data;
  }
  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Private Methods ===================================== //

  // ================ Public Methods ====================================== //
  public static <V> SingleValueFormat<V> fromJson(String json) {
    Type collectionType = new TypeToken<SingleValueFormat<V>>() {}.getType();

    return JsonFormatParser.INSTANCE.fromJson(json, collectionType);
  }

  // ================ Getter & Setter ===================================== //
  public T getData() {
    return data;
  }

  public SingleValueFormat setData(T data) {
    this.data = data;
    return this;
  }

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
