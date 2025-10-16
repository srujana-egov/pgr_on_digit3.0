package com.example.pgrown30.web.controllers;

import com.example.pgrown30.service.ServiceService;
import com.example.pgrown30.web.models.ServiceResponse;
import com.example.pgrown30.web.models.ServiceWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/citizen-service")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    @PostMapping("/create")
    public ResponseEntity<ServiceResponse> create(@RequestBody ServiceWrapper wrapper) {
        ServiceResponse response = serviceService.createService(wrapper);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update")
    public ResponseEntity<ServiceResponse> update(@RequestBody ServiceWrapper wrapper) {
        ServiceResponse response = serviceService.updateService(wrapper);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<ServiceResponse> search(@RequestBody ServiceWrapper wrapper) {
        ServiceResponse response = serviceService.searchServices(wrapper);
        return ResponseEntity.ok(response);
    }
}
