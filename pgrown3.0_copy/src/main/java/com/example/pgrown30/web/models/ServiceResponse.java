package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {

    @JsonProperty("services")
    @Valid
    private List<CitizenService> services;

    @JsonProperty("serviceWrappers")
    @Valid
    private List<ServiceWrapper> serviceWrappers;
}
