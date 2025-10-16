package com.example.pgrown30.service;

import com.example.pgrown30.web.models.ServiceResponse;
import com.example.pgrown30.web.models.ServiceWrapper;

public interface ServiceService {
    ServiceResponse createService(ServiceWrapper wrapper);
    ServiceResponse updateService(ServiceWrapper wrapper);
    ServiceResponse searchServices(ServiceWrapper wrapper);
}
