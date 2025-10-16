package com.example.pgrown30.web.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private String templateId;      // e.g., "service-request-received-new"
    private String version;         // e.g., "1.0.0"
    private String type;            // e.g., "EMAIL"
    private List<String> emailIds;  // email recipients
    private Map<String, Object> payload; // dynamic template variables
    private String subject;         // fallback subject line
    private String message;         // fallback message
    private List<String> channels;  // e.g., ["EMAIL", "SMS"]
}
