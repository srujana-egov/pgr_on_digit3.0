package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.digit.services.workflow.model.Workflow;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceWrapper {

    @JsonProperty("CitizenService")
    @NotNull(message = "CitizenService cannot be null")
    @Valid
    private CitizenService service;

    @JsonProperty("Workflow")
    @Valid
    private Workflow workflow;
}
