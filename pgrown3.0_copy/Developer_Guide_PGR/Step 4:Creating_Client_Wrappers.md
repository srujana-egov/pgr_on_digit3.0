# Step 4: Creating Client Wrappers 

The DIGIT Client Library is a lightweight helper module designed to simplify communication with various DIGIT microservices. Instead of manually building HTTP requests for each service—like IDGEN, MDMS, FileStore, Project, User, and more—the client library provides pre-built, reusable methods that handle request formation, authentication headers, and response mapping for you.

It acts as an abstraction layer over DIGIT’s REST APIs, so developers can interact with core services through clean, type-safe Java methods. This reduces boilerplate code, improves consistency across services, and speeds up development of DIGIT-based applications.

By using the DIGIT Client Library, you ensure your service integrates seamlessly with the wider DIGIT ecosystem while following recommended patterns for tenant isolation, request validation, tracing, and error handling.

## **Steps**

1. Add a folder called client.
2. Under client add BoundaryService.java, the contents of which are as follows:
   
   ```java
     package com.example.pgrown30.client;

    import com.digit.services.boundary.BoundaryClient;
    import com.digit.services.boundary.model.Boundary;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.stereotype.Component;

    import java.util.List;

    @Slf4j
    @Component
    @RequiredArgsConstructor
    public class BoundaryService {

    private final BoundaryClient boundaryClient;

    public boolean isBoundaryValid(String boundaryCode) {
        if (boundaryCode == null || boundaryCode.isBlank()) return false;

        try {
            // Use digit-client library for boundary search
            // Headers are automatically propagated via HeaderPropagationInterceptor
            List<Boundary> boundaries = boundaryClient.searchBoundariesByCodes(List.of(boundaryCode));
            
            boolean isValid = boundaries != null && !boundaries.isEmpty() &&
                    boundaries.stream().anyMatch(b -> boundaryCode.equals(b.getCode()));
            
            log.info("Boundary validation for code={}: {}", boundaryCode, isValid ? "VALID" : "INVALID");
            return isValid;

        } catch (Exception e) {
            log.warn("Boundary validation failed for boundary={}: {}", boundaryCode, e.getMessage());
            return false;
        }
    }

    public List<Boundary> searchBoundariesByCodes(List<String> codes) {
        try {
            log.info("Searching boundaries for codes: {}", codes);
            List<Boundary> boundaries = boundaryClient.searchBoundariesByCodes(codes);
            log.info("Found {} boundaries", boundaries != null ? boundaries.size() : 0);
            return boundaries;
        } catch (Exception e) {
            log.error("Failed to search boundaries for codes {}: {}", codes, e.getMessage(), e);
            throw e;
        }
    }
   }
```


