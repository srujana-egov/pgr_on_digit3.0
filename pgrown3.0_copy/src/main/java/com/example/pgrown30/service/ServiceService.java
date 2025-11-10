package com.example.pgrown30.service;

import com.example.pgrown30.web.models.ServiceResponse;
import com.example.pgrown30.web.models.ServiceWrapper;

import java.util.List;

public interface ServiceService {

    // create and update keep the wrapper + roles signature
    ServiceResponse createService(ServiceWrapper wrapper, List<String> roles);

    ServiceResponse updateService(ServiceWrapper wrapper, List<String> roles);

    /**
     * Search with explicit query params (used by controller GET /search).
     * Keep this on the interface because the implementation exposes it and controllers call it.
     */
    ServiceResponse searchService(
            String tenantId,
            String serviceRequestId,
            String serviceCode,
            String applicationStatus,
            String mobileNumber,
            String locality
    );

    /**
     * Convenience / fast-path: search by id + tenant. Implementation may just delegate to searchService.
     */
    ServiceResponse searchServicesById(String serviceRequestId, String tenantId);
}
