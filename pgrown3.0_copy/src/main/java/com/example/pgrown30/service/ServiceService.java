package com.example.pgrown30.service;

import com.example.pgrown30.web.models.ServiceResponse;
import com.example.pgrown30.web.models.ServiceWrapper;

import java.util.List;

public interface ServiceService {

    ServiceResponse createService(ServiceWrapper wrapper, List<String> roles);

    ServiceResponse updateService(ServiceWrapper wrapper, List<String> roles);

    ServiceResponse searchServices(ServiceWrapper wrapper);

    ServiceResponse searchServicesById(String serviceRequestId, String tenantId);
}
