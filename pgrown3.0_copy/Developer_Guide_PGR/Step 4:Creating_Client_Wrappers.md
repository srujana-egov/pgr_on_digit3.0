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

3. Under client add FileStoreService.java, the contents of which are as follows:
   
   ```java
   package com.example.pgrown30.client;

   import com.digit.services.filestore.FilestoreClient;
   import lombok.RequiredArgsConstructor;
   import lombok.extern.slf4j.Slf4j;
   import org.springframework.stereotype.Service;

   @Slf4j
   @Service
   @RequiredArgsConstructor
   public class FileStoreService {

    private final FilestoreClient fileStoreClient;

    /**
     * Validates if a file exists and is accessible in the file store
     * @param tenantId The tenant ID
     * @param fileStoreId The file store ID to validate
     * @return true if the file exists and is accessible, false otherwise
     */
    public boolean isFileValid(String fileStoreId, String tenantId) {
        if (fileStoreId == null || fileStoreId.isBlank()) {
            return false;
        }
        try {
            // Use digit-client library for file store operations
            boolean isValid = fileStoreClient.getFile(fileStoreId, tenantId);
            log.info("File validation for fileStoreId={} tenantId={}: {}", 
                    fileStoreId, tenantId, isValid ? "VALID" : "INVALID");
            return isValid;
        } catch (Exception e) {
            log.error("File validation failed for fileStoreId={} tenantId={}", 
                    fileStoreId, tenantId, e);
            return false;
        }
       }
   }

4. Under client add IdGenService.java, the contents of which are as follows:
   
   ```java
   package com.example.pgrown30.client;

   import com.digit.services.idgen.IdGenClient;
   import com.digit.services.idgen.model.IdGenGenerateRequest;
   import lombok.RequiredArgsConstructor;
   import lombok.extern.slf4j.Slf4j;
   import org.springframework.beans.factory.annotation.Value;
   import org.springframework.stereotype.Component;

   import java.util.Map;

   @Slf4j
   @Component
   @RequiredArgsConstructor
   public class IdGenService {

    private final IdGenClient idGenClient;

    @Value("${idgen.templateCode}")
    private String templateCode;

    public String generateId(String orgCode) {
        IdGenGenerateRequest request = IdGenGenerateRequest.builder()
                .templateCode(templateCode)
                .variables(Map.of("ORG", String.valueOf(orgCode)))
                .build();

        log.info("Requesting ID from IdGen with templateCode={} and orgCode={}", templateCode, orgCode);

        // Use digit-client library for ID generation
        // Headers are automatically propagated via HeaderPropagationInterceptor
        String id = idGenClient.generateId(request);

        return id;
    }
   }

5. Under client add NotificationService.java, the contents of which are as follows:
   
   ```java
   package com.example.pgrown30.client;

   import com.digit.services.notification.NotificationClient;
   import com.digit.services.notification.model.SendEmailRequest;
   import com.digit.services.notification.model.SendEmailResponse;
   import com.digit.services.notification.model.SendSMSRequest;
   import com.digit.services.notification.model.SendSMSResponse;
   import lombok.RequiredArgsConstructor;
   import lombok.extern.slf4j.Slf4j;
   import org.springframework.scheduling.annotation.Async;
   import org.springframework.stereotype.Component;

   import java.util.List;
   import java.util.Map;

   @Slf4j
   @Component
   @RequiredArgsConstructor
   public class NotificationService {

    private final NotificationClient notificationClient;

    public void sendEmail(String templateId, List<String> emailIds, Map<String, Object> payload, List<String> attachments) {
        SendEmailRequest request = SendEmailRequest.builder()
                .version("v1")
                .templateId(templateId)
                .emailIds(emailIds)
                .enrich(false)
                .payload(payload)
                .build();

        // Use digit-client library for email sending
        // Headers are automatically propagated via HeaderPropagationInterceptor
        SendEmailResponse response = notificationClient.sendEmail(request);
        log.info("Email sent successfully with templateId: {}", templateId);
    }


    public void sendSms(String templateId, List<String> mobileNumbers, Map<String, Object> payload, String category) {
        // Use default SMS category - let's check what values are available
        SendSMSRequest.SMSCategory smsCategory = null;
        // We'll set this to null and let the library handle the default

        SendSMSRequest request = SendSMSRequest.builder()
                .templateId(templateId)
                .mobileNumbers(mobileNumbers)
                .enrich(false)
                .payload(payload)
                .category(smsCategory)
                .build();

        // Use digit-client library for SMS sending
        // Headers are automatically propagated via HeaderPropagationInterceptor
        SendSMSResponse response = notificationClient.sendSMS(request);
        log.info("SMS sent successfully with templateId: {}", templateId);
    }
   }

6. Under client add WorkflowService.java, the contents of which are as follows:
   
   ```java
   package com.example.pgrown30.client;

   import com.digit.services.workflow.WorkflowClient;
   import com.digit.services.workflow.model.WorkflowProcessResponse;
   import com.digit.services.workflow.model.WorkflowTransitionRequest;
   import com.digit.services.workflow.model.WorkflowTransitionResponse;

   import jakarta.annotation.PostConstruct;
   import lombok.RequiredArgsConstructor;
   import lombok.extern.slf4j.Slf4j;
   import org.springframework.beans.factory.annotation.Value;
   import org.springframework.stereotype.Service;

   import java.util.Collections;
   import java.util.HashMap;
   import java.util.List;
   import java.util.Map;

   @Slf4j
   @Service
   @RequiredArgsConstructor
   public class WorkflowService {
    private final WorkflowClient workflowClient;
    
    @Value("${pgr.workflow.processCode}")
    private String processCode;


    private String getProcessId() {
        System.out.println("Process id: " + workflowClient.getProcessByCode(processCode));
        return workflowClient.getProcessByCode(processCode);
    }

    public WorkflowTransitionResponse transition(
    String entityId, 
    String action, 
    String comment, 
    Map<String, Object> attributes
   ) {
    String processId = getProcessId();
    
    // Create a new Map with the correct type parameters
    Map<String, List<String>> attributesCopy = new HashMap<>();
    
    // Copy all attributes, ensuring values are List<String>
    if (attributes != null) {
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (entry.getValue() instanceof List) {
                // Safe to cast since we know it's a List
                @SuppressWarnings("unchecked")
                List<String> valueList = (List<String>) entry.getValue();
                attributesCopy.put(entry.getKey(), valueList);
            } else {
                // Convert single values to a singleton list
                attributesCopy.put(entry.getKey(), 
                    Collections.singletonList(String.valueOf(entry.getValue())));
            }
        }
    }
    
    WorkflowTransitionRequest request = WorkflowTransitionRequest.builder()
        .processId(getProcessId())
        .entityId(entityId)
        .action(action)
        .comment(comment)
        .attributes(attributesCopy)
        .build();

    log.debug("Initiating workflow transition: {}", request);
    WorkflowTransitionResponse response = workflowClient.executeTransition(request);
    log.info("Workflow transition completed for entityId={} processId={} action={}", 
            entityId, processId, action);
    return response;
   }

   }
