package org.mltooling.core.service.params;

import org.mltooling.core.api.utils.DefaultRequestParams;
import io.swagger.annotations.ApiParam;

import javax.ws.rs.QueryParam;


public class DefaultListParams {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //
    @ApiParam(value = "Limit the number of returned entities", required = false)
    @QueryParam(value = DefaultRequestParams.LIMIT)
    public Integer limit;

    @ApiParam(value = "Filter by entity-types, relation-types or origins", required = false)
    @QueryParam(value = DefaultRequestParams.SELECT)
    public String select;

    @ApiParam(value = "Exclude selected entity-types, relation-types or origins", required = false)
    @QueryParam(value = DefaultRequestParams.EXCLUDE)
    public String exclude;
    // ================ Constructors & Main ================================= //

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}
