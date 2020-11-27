package org.mltooling.core.api.format;

import io.swagger.annotations.ApiModelProperty;
import java.util.HashMap;
import java.util.Locale;
import javax.annotation.Nullable;
import org.apache.http.HttpStatus;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
import org.mltooling.core.api.format.metadata.UnifiedErrorMessage;
import org.mltooling.core.api.format.metadata.UnifiedFormatMetadata;
import org.mltooling.core.api.format.parser.JsonFormatParser;
import org.mltooling.core.api.utils.DefaultRequestParams;
import org.mltooling.core.utils.LogUtils;

public class UnifiedFormat {
  // ================ Constants =========================================== //

  // ================ Members ============================================= //
  protected UnifiedErrorMessage errors;
  protected HashMap<String, Object> metadata = new HashMap<>();

  // ================ Constructors & Main ================================= //
  public UnifiedFormat() {
    getMetadata().setStatus(HttpStatus.SC_OK);
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  // ================ Private Methods ===================================== //

  // ================ Public Methods ====================================== //

  public UnifiedFormat setErrorStatus(String message, Integer code) {
    setErrorStatus(message, code, null, null);
    return this;
  }

  public UnifiedFormat setErrorStatus(String message, Integer code, Exception ex) {
    setErrorStatus(message, code, ex.getClass().getSimpleName(), LogUtils.getStackTrace(ex));
    return this;
  }

  public UnifiedFormat setErrorStatus(Exception ex) {
    setErrorStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR, ex, true);
    return this;
  }

  public UnifiedFormat setErrorStatus(Integer code, Exception ex, boolean withStackTrace) {
    if (withStackTrace) {
      setErrorStatus(
          ex.getMessage(), code, ex.getClass().getSimpleName(), LogUtils.getStackTrace(ex));
    } else {
      setErrorStatus(ex.getMessage(), code, ex.getClass().getSimpleName(), null);
    }
    return this;
  }

  public UnifiedFormat setErrorStatus(
      String message, Integer code, @Nullable String type, @Nullable String description) {
    errors = new UnifiedErrorMessage();
    errors.setMessage(message);
    errors.setType(type);
    errors.setCode(code);
    errors.setDescription(description);

    setStatus(code);

    return this;
  }

  public UnifiedFormat setErrorMessage(String message) {
    if (errors == null) {
      setErrorStatus(message, HttpStatus.SC_INTERNAL_SERVER_ERROR, null, null);
    } else {
      errors.setMessage(message);
    }

    return this;
  }

  public UnifiedFormat setItemCreatedStatus() {
    setStatus(HttpStatus.SC_CREATED);
    return this;
  }

  public UnifiedFormat setNotImplementedStatus() {
    setErrorStatus(
        EnglishReasonPhraseCatalog.INSTANCE.getReason(
            HttpStatus.SC_NOT_IMPLEMENTED, Locale.ENGLISH),
        HttpStatus.SC_NOT_IMPLEMENTED);
    return this;
  }

  public UnifiedFormat setNoContentStatus() {
    setStatus(HttpStatus.SC_NO_CONTENT);
    return this;
  }

  public UnifiedFormat setNotFoundStatus() {
    setErrorStatus(
        EnglishReasonPhraseCatalog.INSTANCE.getReason(HttpStatus.SC_NOT_FOUND, Locale.ENGLISH),
        HttpStatus.SC_NOT_FOUND);
    return this;
  }

  public UnifiedFormat setSuccessfulStatus() {
    setStatus(HttpStatus.SC_OK);
    return this;
  }

  public UnifiedFormat setStatus(String message, Integer code) {
    getMetadata().setStatus(code);
    getMetadata().setMessage(message);
    return this;
  }

  public UnifiedFormat setStatus(Integer code) {
    getMetadata().setStatus(code);
    getMetadata().setMessage(EnglishReasonPhraseCatalog.INSTANCE.getReason(code, Locale.ENGLISH));
    return this;
  }

  @ApiModelProperty(hidden = true)
  public int getStatus() {
    Integer status = null;
    if (hasError()) {
      status = getErrorMessage().getCode();
    } else if (hasMetadata()) {
      status = getMetadata().getStatus();
    }
    if (status == null) {
      status = HttpStatus.SC_OK;
    }
    return status;
  }

  @ApiModelProperty(name = "errors")
  public UnifiedErrorMessage getErrorMessage() {
    return errors;
  }

  public UnifiedFormat prepareResponse(DefaultRequestParams defaultRequestParams) {
    return this;
  }

  // ================ Getter & Setter ===================================== //

  public UnifiedFormat setUnifiedError(UnifiedErrorMessage error) {
    this.errors = error;
    return this;
  }

  @ApiModelProperty(hidden = true)
  public boolean isSuccessful() {
    return !hasError() && getStatus() >= 200 && getStatus() < 300;
  }

  public boolean hasError() {
    return errors != null;
  }

  public boolean hasMetadata() {
    return metadata != null;
  }

  public void removeMetadata() {
    metadata = null;
  }

  @ApiModelProperty(hidden = true)
  public HashMap<String, Object> getMetadataMap() {
    return metadata;
  }

  public UnifiedFormat addMetadata(String key, Object value) {
    metadata.put(key, value);
    return this;
  }

  public Object getMetadata(String key) {
    return metadata.get(key);
  }

  public UnifiedFormatMetadata getMetadata() {
    return new UnifiedFormatMetadata(this.getMetadataMap());
  }

  public UnifiedFormat setMetadata(UnifiedFormatMetadata unifiedMetadata) {
    metadata = unifiedMetadata.getMetadataMap();
    return this;
  }

  public String toJson() {
    return JsonFormatParser.INSTANCE.toJson(this);
  }

  @Override
  public String toString() {
    return toJson();
  }
  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
