// ServiceWrapper.java
package com.example.pgr.client.model;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ServiceWrapper {
    // Required property per spec
    private CitizenService CitizenService;
    private Workflow Workflow;
}
