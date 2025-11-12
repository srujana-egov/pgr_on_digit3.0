package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditDetails {
    @JsonProperty("createdBy")
    private String createdBy;
    
    @JsonProperty("createdTime")
    private Long createdTime;
    
    @JsonProperty("lastModifiedBy")
    private String lastModifiedBy;
    
    @JsonProperty("lastModifiedTime")
    private Long lastModifiedTime;
}
