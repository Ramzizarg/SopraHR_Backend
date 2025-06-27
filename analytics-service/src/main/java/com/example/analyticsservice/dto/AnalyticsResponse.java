package com.example.analyticsservice.dto;

import java.util.Map;

/**
 * DTO for returning analytics data to the client
 * Contains aggregated data from all microservices
 */
public class AnalyticsResponse {
    
    private final Map<String, Object> data;
    
    public AnalyticsResponse(Map<String, Object> data) {
        super();
        this.data = data;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
}
