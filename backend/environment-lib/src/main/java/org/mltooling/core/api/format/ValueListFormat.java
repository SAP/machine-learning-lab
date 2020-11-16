package org.mltooling.core.api.format;

import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import org.mltooling.core.api.format.metadata.ValueListFormatMetadata;
import org.mltooling.core.api.format.parser.JsonFormatParser;
import org.mltooling.core.api.utils.DefaultRequestParams;
import org.mltooling.core.utils.ListUtils;

public class ValueListFormat<T> extends UnifiedFormat {
  // ================ Constants =========================================== //

  // ================ Members ============================================= //
  private List<T> data = new ArrayList<>();

  // ================ Constructors & Main ================================= //
  public ValueListFormat() {}

  // ================ Methods for/from SuperClass / Interfaces ============ //
  public static ValueListFormat fromJson(String json) {
    return JsonFormatParser.INSTANCE.fromJson(json, ValueListFormat.class);
  }

  public static ValueListFormat fromJson(String json, TypeToken typeToken) {
    return JsonFormatParser.INSTANCE.fromJson(json, typeToken.getType());
  }

  // ================ Private Methods ===================================== //

  // ================ Public Methods ====================================== //
  public ValueListFormatMetadata getMetadata() {
    return new ValueListFormatMetadata(this.getMetadataMap());
  }

  public UnifiedFormat prepareResponse(DefaultRequestParams defaultRequestParams) {

    ValueListFormatMetadata metadata = this.getMetadata();
    if (metadata != null) {
      metadata.setLimit(defaultRequestParams.getLimit());

      if (data != null) {
        metadata.setItemCount(data.size());
      }
    }

    // shorten by limit
    if (defaultRequestParams.getLimit() != null) {
      if (data != null) {
        data = ListUtils.shorten(data, defaultRequestParams.getLimit());
      }
    }

    if (defaultRequestParams.isDebug()) {
      data = null;
    }

    return this;
  }

  public void addData(T data) {
    if (this.data == null) {
      this.data = new ArrayList<>();
    }

    this.data.add(data);
  }

  public void addData(T... data) {
    for (T item : data) {
      addData(item);
    }
  }

  public List<T> getData() {
    return data;
  }

  public void setData(List<T> data) {
    for (T item : data) {
      addData(item);
    }
  }
  // ================ Getter & Setter ===================================== //

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
