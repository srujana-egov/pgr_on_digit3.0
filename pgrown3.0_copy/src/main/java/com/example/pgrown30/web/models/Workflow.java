// Workflow.java
package com.example.pgr.client.model;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Workflow {
    private String action;
    private String comment;
    private List<String> assignes;
}
