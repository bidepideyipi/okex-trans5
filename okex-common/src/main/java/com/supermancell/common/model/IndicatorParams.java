package com.supermancell.common.model;

import java.util.HashMap;
import java.util.Map;

public class IndicatorParams {
    private Map<String, Object> parameters = new HashMap<>();
    
    public void addParameter(String key, Object value) {
        parameters.put(key, value);
    }
    
    public Object getParameter(String key) {
        return parameters.get(key);
    }
    
    public <T> T getParameter(String key, Class<T> type) {
        Object value = parameters.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
    
    public boolean hasParameter(String key) {
        return parameters.containsKey(key);
    }
    
    public Map<String, Object> getAllParameters() {
        return new HashMap<>(parameters);
    }
    
    public void clear() {
        parameters.clear();
    }
    
    @Override
    public String toString() {
        return "IndicatorParams{" +
                "parameters=" + parameters +
                '}';
    }
}