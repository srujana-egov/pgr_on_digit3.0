package com.example.pgrown30.web.controllers;

import com.example.pgrown30.service.ServiceService;
import com.example.pgrown30.web.models.ServiceResponse;
import com.example.pgrown30.web.models.ServiceWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/citizen-service")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    private List<String> getRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return List.of();
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.getOrDefault("roles", List.of());
        return roles;
    }

    @PostMapping("/create")
    public ResponseEntity<ServiceResponse> create(@RequestBody ServiceWrapper wrapper,
                                                  @AuthenticationPrincipal Jwt jwt) {
        List<String> roles = getRealmRoles(jwt);
        System.out.println(roles);
        // you can pass roles to your service if you need
        ServiceResponse response = serviceService.createService(wrapper, roles);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update")
    public ResponseEntity<ServiceResponse> update(@RequestBody ServiceWrapper wrapper,
                                                  @AuthenticationPrincipal Jwt jwt) {
        List<String> roles = getRealmRoles(jwt);
        System.out.println(roles);
        ServiceResponse response = serviceService.updateService(wrapper, roles);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<ServiceResponse> search(@RequestBody ServiceWrapper wrapper,
                                                  @AuthenticationPrincipal Jwt jwt) {
        List<String> roles = getRealmRoles(jwt);
        ServiceResponse response = serviceService.searchServices(wrapper);
        return ResponseEntity.ok(response);
    }
}
