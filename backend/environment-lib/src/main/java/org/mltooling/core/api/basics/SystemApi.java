package org.mltooling.core.api.basics;

import org.mltooling.core.api.BaseApi;
import org.mltooling.core.api.format.StatusMessageFormat;

public interface SystemApi extends BaseApi {

  // ================ Constants =========================================== //
  String ENDPOINT_PATH = "/system";

  String METHOD_IS_HEALTHY = "/healthy";

  // ================ Public Methods ====================================== //
  StatusMessageFormat isHealthy();
}
