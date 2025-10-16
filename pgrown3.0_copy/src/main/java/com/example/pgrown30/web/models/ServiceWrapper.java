package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ServiceWrapper {

    @JsonProperty("CitizenService")
    @NotNull(message = "Service cannot be blank")
    @Valid
    private CitizenService service;

    @JsonProperty("Workflow")
    @Valid
    private Workflow workflow;

    @JsonProperty("Notification")
    @Valid
    private Notification notification;  // Add this
    
}


