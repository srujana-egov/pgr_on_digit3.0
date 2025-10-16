package com.example.pgrown30.web.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ServiceRequest {



    @JsonProperty("serviceWrapper")
    @NotNull(message = "ServiceWrapper cannot be null")
    @Valid
    private ServiceWrapper serviceWrapper;
    
}

   