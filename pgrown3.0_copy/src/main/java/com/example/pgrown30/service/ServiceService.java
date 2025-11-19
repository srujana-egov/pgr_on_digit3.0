package com.example.pgrown30.service;

import com.example.pgrown30.web.models.ServiceResponse;
import com.example.pgrown30.web.models.ServiceWrapper;

import java.util.List;

public interface ServiceService {

    // create and update keep the wrapper + roles signature
    ServiceResponse createService(ServiceWrapper wrapper, List<String> roles);

    ServiceResponse updateService(ServiceWrapper wrapper, List<String> roles);

    ServiceResponse searchServicesById(String serviceRequestId, String tenantId);
}
