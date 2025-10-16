package com.example.pgrown30.web.controllers;

import com.example.pgrown30.service.ServiceService;
import com.example.pgrown30.web.models.ServiceResponse;
import com.example.pgrown30.web.models.ServiceWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/citizen-service")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceService serviceService;

    private static final Pattern REALM_PATTERN = Pattern.compile(".*/realms/([^/]+)/*$");


    private List<String> getRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return List.of();
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.getOrDefault("roles", List.of());
        return roles;
    }

    private static String getTenantIdFromIssuer(Jwt jwt) {
        if (jwt == null || jwt.getIssuer() == null) return null;
        String iss = jwt.getIssuer().toString(); // e.g. https://.../realms/TRAVISSCOT
        Matcher m = REALM_PATTERN.matcher(iss);
        if (m.matches()) return m.group(1);
        // fallback: try URI parsing if pattern fails
        try {
            String path = URI.create(iss).getPath(); // /keycloak/realms/TRAVISSCOT
            Matcher m2 = REALM_PATTERN.matcher(path);
            if (m2.matches()) return m2.group(1);
        } catch (Exception ignored) {}
        return null;
    }

    @PostMapping("/create")
    public ResponseEntity<ServiceResponse> create(@RequestBody ServiceWrapper wrapper,
                                                  @AuthenticationPrincipal Jwt jwt) {
        List<String> roles = getRealmRoles(jwt);
        String tenantId = getTenantIdFromIssuer(jwt);
        wrapper.getService().setTenantId(tenantId);
        System.out.println(roles);
        // you can pass roles to your service if you need
        ServiceResponse response = serviceService.createService(wrapper, roles);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update")
    public ResponseEntity<ServiceResponse> update(@RequestBody ServiceWrapper wrapper,
                                                  @AuthenticationPrincipal Jwt jwt) {
        List<String> roles = getRealmRoles(jwt);
        String tenantId = getTenantIdFromIssuer(jwt);  // <-- here
        System.out.println(roles);
        wrapper.getService().setTenantId(tenantId);
        ServiceResponse response = serviceService.updateService(wrapper, roles);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ServiceResponse> search(
            @RequestParam("serviceRequestId") String serviceRequestId,
            @AuthenticationPrincipal Jwt jwt) {

        List<String> roles = getRealmRoles(jwt);
        String tenantId = getTenantIdFromIssuer(jwt);
        System.out.println("TENANTID: "+tenantId);
        ServiceResponse response = serviceService.searchServicesById(serviceRequestId, tenantId);
        return ResponseEntity.ok(response);
    }
}
