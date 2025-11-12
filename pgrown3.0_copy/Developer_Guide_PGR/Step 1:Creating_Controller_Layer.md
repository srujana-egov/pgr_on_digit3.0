# Step 1: Creating the Controller Layer

## **Overview**

The controller layer is responsible for handling incoming HTTP requests and returning appropriate responses.

## **Steps**

1. Create a ServiceController.java under web > controllers.

<img width="292" height="66" alt="Screenshot 2025-11-13 at 2 07 23 AM" src="https://github.com/user-attachments/assets/570a39f8-f8c4-424c-a816-968da94caef9" />

The controller class reflects the following content -

{% code lineNumbers="true" %}

```java
package com.example.pgrown30.web.controllers;

import com.example.pgrown30.service.ServiceService;
import com.example.pgrown30.web.models.ServiceResponse;
import com.example.pgrown30.web.models.ServiceWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/citizen-service")
@RequiredArgsConstructor
@Slf4j
public class ServiceController {

    private final ServiceService serviceService;

    private static final Pattern REALM_PATTERN = Pattern.compile(".*/realms/([^/]+)/*$");

    private List<String> getRealmRoles(Jwt jwt) {
        if (jwt == null) return List.of();
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

        if (wrapper == null || wrapper.getService() == null) {
            log.warn("create: missing request body");
            return ResponseEntity.badRequest().body(new ServiceResponse(List.of(), List.of()));
        }

        List<String> roles = getRealmRoles(jwt);
        String tenantId = getTenantIdFromIssuer(jwt);

        if (tenantId != null) wrapper.getService().setTenantId(tenantId);
        else log.warn("create: tenantId could not be resolved from JWT issuer");

        log.debug("create: roles={}, tenant={}", roles, tenantId);
        ServiceResponse response = serviceService.createService(wrapper, roles);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/update")
    public ResponseEntity<ServiceResponse> update(@RequestBody ServiceWrapper wrapper,
                                                  @AuthenticationPrincipal Jwt jwt) {

        if (wrapper == null || wrapper.getService() == null) {
            log.warn("update: missing request body");
            return ResponseEntity.badRequest().body(new ServiceResponse(List.of(), List.of()));
        }

        List<String> roles = getRealmRoles(jwt);
        String tenantId = getTenantIdFromIssuer(jwt);
        if (tenantId != null) wrapper.getService().setTenantId(tenantId);
        else log.warn("update: tenantId could not be resolved from JWT issuer");

        log.debug("update: requestId={}, roles={}, tenant={}",
                wrapper.getService().getServiceRequestId(), roles, tenantId);

        ServiceResponse response = serviceService.updateService(wrapper, roles);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<ServiceResponse> search(
            @RequestParam(value = "serviceRequestId", required = false) String serviceRequestId,
            @AuthenticationPrincipal Jwt jwt) {

        if (serviceRequestId == null || serviceRequestId.isBlank()) {
            log.warn("search: missing serviceRequestId");
            return ResponseEntity.badRequest().body(new ServiceResponse(List.of(), List.of()));
        }

        List<String> roles = getRealmRoles(jwt);
        String tenantId = getTenantIdFromIssuer(jwt);
        if (tenantId == null) log.warn("search: tenantId could not be resolved from JWT issuer");

        log.debug("search: serviceRequestId={}, tenant={}, roles={}", serviceRequestId, tenantId, roles);
        ServiceResponse response = serviceService.searchServicesById(serviceRequestId, tenantId);
        return ResponseEntity.ok(response);
    }
}


```

{% endcode %}

{% hint style="info" %}
**NOTE:** At this point, your IDE must be showing a lot of errors, but do not worry, we will add all dependent layers as we progress through this guide, and the errors will go away.
{% endhint %}

The web layer is now set up.
