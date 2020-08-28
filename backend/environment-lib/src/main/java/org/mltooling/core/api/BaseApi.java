package org.mltooling.core.api;

import org.mltooling.core.api.utils.DefaultRequestParams;


public interface BaseApi<T extends BaseApi> {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //

    // ================ Constructors & Main ================================= //

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //
    T setDefaultParams(DefaultRequestParams defaultRequestParams);

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
