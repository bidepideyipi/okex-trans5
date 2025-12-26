package com.supermancell.common.model;

import java.util.Map;

public class IndicatorResult {
    private Double value;
    private Map<String, Double> values;
    private String timestamp;
    private Integer dataPoints;

    public IndicatorResult() {}

    public IndicatorResult(Double value, Map<String, Double> values, String timestamp, Integer dataPoints) {
        this.value = value;
        this.values = values;
        this.timestamp = timestamp;
        this.dataPoints = dataPoints;
    }

    // Getters and Setters
    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Map<String, Double> getValues() {
        return values;
    }

    public void setValues(Map<String, Double> values) {
        this.values = values;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getDataPoints() {
        return dataPoints;
    }

    public void setDataPoints(Integer dataPoints) {
        this.dataPoints = dataPoints;
    }

    @Override
    public String toString() {
        return "IndicatorResult{" +
                "value=" + value +
                ", values=" + values +
                ", timestamp='" + timestamp + '\'' +
                ", dataPoints=" + dataPoints +
                '}';
    }
}