package org.mltooling.core.api.format.metadata;

import org.mltooling.core.api.utils.DefaultRequestParams;

import java.util.HashMap;
import java.util.Map;


public class ValueListFormatMetadata extends UnifiedFormatMetadata {

    // ================ Constants =========================================== //
    private static final String ITEM_COUNT = "itemCount";
    private static final String STATS = "stats";

    // ================ Members ============================================= //

    // ================ Constructors & Main ================================= //

    // ================ Methods for/from SuperClass / Interfaces ============ //
    public ValueListFormatMetadata() {
    }

    public ValueListFormatMetadata(HashMap<String, Object> metadata) {
        super(metadata);
    }

    // ================ Private Methods ===================================== //

    // ================ Public Methods ====================================== //

    // ================ Getter & Setter ===================================== //
    public Integer getItemCount() {
        return (Integer) getMetadata(ITEM_COUNT);
    }

    public void setItemCount(Integer itemCount) {
        addMetadata(ITEM_COUNT, itemCount);
    }

    public Integer getLimit() {
        return (Integer) getMetadata(DefaultRequestParams.LIMIT);
    }

    public void setLimit(Integer limit) {
        addMetadata(DefaultRequestParams.LIMIT, limit);
    }

    public Map<String, Object> getStats() {
        return (Map<String, Object>) getMetadata(STATS);
    }

    public void setStats(Map<String, Object> stats) {
        addMetadata(STATS, stats);
    }

    // ================ Builder Pattern ===================================== //

    // ================ Inner & Anonymous Classes =========================== //
}