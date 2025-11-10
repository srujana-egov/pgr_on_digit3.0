package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Workflow {

    @JsonProperty("action")
    @Size(min = 1, max = 64)
    private String action;

    @JsonProperty("assignes")
    private List<String> assignes;

    @JsonProperty("comments")
    @Size(min = 1, max = 64)
    private String comments;

    @JsonProperty("verificationDocuments")
    @Valid
    private List<Document> verificationDocuments;
}
