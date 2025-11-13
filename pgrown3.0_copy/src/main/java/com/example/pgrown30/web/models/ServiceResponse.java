// ServiceResponse.java
package com.example.pgr.client.model;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceResponse {
    private List<CitizenService> services;
    private List<WorkflowResponse> workflowResponses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkflowResponse {
        private String status;
        private String message;
    }
}
