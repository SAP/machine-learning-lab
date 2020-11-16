package org.mltooling.core.api.basics;

import com.google.gson.reflect.TypeToken;
import com.mashape.unirest.http.Unirest;
import java.net.URI;
import java.net.URISyntaxException;
import javax.annotation.Nullable;
import org.mltooling.core.api.client.AbstractApiClient;
import org.mltooling.core.api.format.StatusMessageFormat;

public class SystemApiClient extends AbstractApiClient<SystemApiClient> implements SystemApi {

  // ================ Constants =========================================== //

  // ================ Members ============================================= //

  // ================ Constructors & Main ================================= //
  public SystemApiClient(String serviceUrl) {
    super(serviceUrl, null);
  }

  public SystemApiClient(String serviceUrl, @Nullable String authToken) {
    super(serviceUrl, authToken);
  }

  // ================ Methods for/from SuperClass / Interfaces ============ //

  @Override
  public StatusMessageFormat isHealthy() {
    return executeRequest(
        Unirest.get(getEndpointUrl() + METHOD_IS_HEALTHY).getHttpRequest(),
        new TypeToken<StatusMessageFormat>() {}.getType());
  }

  // ================ Public Methods ====================================== //

  // ================ Private Methods ===================================== //

  // ================ Getter & Setter ===================================== //
  @Override
  public String getEndpointUrl() {
    try {
      return new URI(getServiceUrl()).resolve(ENDPOINT_PATH).toString();
    } catch (URISyntaxException e) {
      log.error("Failed to resolve endpoint URL", e);
      return "";
    }
  }

  // ================ Builder Pattern ===================================== //

  // ================ Inner & Anonymous Classes =========================== //
}
