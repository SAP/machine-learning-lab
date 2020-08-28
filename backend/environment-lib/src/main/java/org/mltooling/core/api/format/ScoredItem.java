package org.mltooling.core.api.format;

import java.util.HashMap;
import java.util.Map;


public class ScoredItem {
    // ================ Constants =========================================== //

    // ================ Members ============================================= //
    private String item;
    private Double score;
    private Map<String, String> details;

    // ================ Constructors & Main ================================= //
    public ScoredItem(String item, Double score) {
        this.item = item;
        this.score = score;
        this.details = new HashMap<>();
    }

    public ScoredItem(String item, Double score, Map<String, String> details) {
        this(item, score);
        this.details = details;
    }

    // ================ Methods for/from SuperClass / Interfaces ============ //

    // ================ Public Methods ====================================== //

    // ================ Private Methods ===================================== //

    // ================ Getter & Setter ===================================== //
    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
    // ================ Getter & Setter ===================================== //

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    // ================ Builder Pattern ===================================== //
    public ScoredItem withDetail(String key, String value) {
        if (this.details == null) {
            this.details = new HashMap<>();
        }

        this.details.put(key, value);
        return this;
    }
    // ================ Inner & Anonymous Classes =========================== //
}
