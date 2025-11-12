# Step 2: Creating & Implementing the Service Layer

The service layer contains business logic and interacts with repositories to manage data.

## **Steps**

Follow the steps below to create the service layer.

1. Create ServiceService.java under service folder.
2. Create a folder 'impl' under the service folder.
3. Add ServiceServiceImpl.java under the impl folder.

<img width="365" height="86" alt="Screenshot 2025-11-13 at 3 33 31 AM" src="https://github.com/user-attachments/assets/371bd312-c032-4067-8415-8df1cbf9bf1e" />

The contents ServiceService.java are as follows:
   ```java
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
   ```
To implement these functions from ServiceService.java, we have ServiceServiceImpl.java with contents as follows:

```java
package com.example.pgrown30.service.impl;

import com.digit.services.workflow.model.WorkflowTransitionResponse;
import com.example.pgrown30.client.BoundaryService;
import com.example.pgrown30.web.models.AuditDetails;
import com.example.pgrown30.web.models.CitizenService;
import com.example.pgrown30.web.models.ServiceResponse;
import com.example.pgrown30.web.models.ServiceWrapper;
import com.example.pgrown30.client.NotificationService;
import com.example.pgrown30.client.WorkflowService;
import com.example.pgrown30.client.IdGenService;
import com.example.pgrown30.repository.CitizenServiceRepository;
import com.example.pgrown30.service.ServiceService;
import com.example.pgrown30.client.FileStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceServiceImpl implements ServiceService {
    private final CitizenServiceRepository citizenServiceRepository;
    private final IdGenService idGenService;
    private final FileStoreService fileStoreService;
    private final BoundaryService boundaryService;
    private final NotificationService notificationService;
    private final WorkflowService workflowService;


    @Override
    @Transactional
    public ServiceResponse createService(ServiceWrapper wrapper, List<String> roles) {
        if (wrapper == null || wrapper.getService() == null) {
            log.error("Invalid request: wrapper or service is null");
            return ServiceResponse.builder()
                .services(Collections.emptyList())
                .serviceWrappers(Collections.singletonList(
                    ServiceWrapper.builder()
                        .service(CitizenService.builder()
                            .description("Error: Invalid request")
                            .build())
                        .build()
                ))
                .build();
        }

        CitizenService citizenService = wrapper.getService();
        log.debug("createService: incoming DTO = {}", citizenService);

        // Generate ID and timestamps
        String newId = idGenService.generateId("pgr");
        long now = Instant.now().toEpochMilli();

        citizenService.setServiceRequestId(newId);
        AuditDetails auditDetails = new AuditDetails();
        auditDetails.setCreatedTime(now);
        auditDetails.setLastModifiedTime(now);
        citizenService.setAuditDetails(auditDetails);

        if (citizenService.getSource() == null || citizenService.getSource().isBlank()) {
            citizenService.setSource("Citizen");
        }

        // External validations
        try {
            validateBoundaryIfPresent(citizenService);
            validateFileIfPresent(citizenService);
        } catch (Exception e) {
            log.error("External validation failed (boundary/filestore): {}", e.getMessage(), e);
        }

        // Start workflow
        WorkflowTransitionResponse wfResp = startWorkflow(citizenService, "APPLY", roles);
        
        // Attach workflow info and set 'status'
        System.out.println("workflow response---------------------");
        System.out.println(wfResp);

        if (wfResp != null) {
            updateCitizenServiceWithWorkflow(citizenService, wfResp);
        } 
        // Persist entity
        CitizenService saved = citizenServiceRepository.save(citizenService);
        log.debug("Saved citizen service with id={}", saved.getServiceRequestId());

        // Prepare response
        CitizenService responseDto = createResponseDto(saved, wfResp);
        ServiceWrapper responseWrapper = ServiceWrapper.builder()
                .service(responseDto)
                .workflow(wrapper.getWorkflow())
                .build();

        // Send notifications
        sendCreateNotificationsIfNeeded(saved);

        return ServiceResponse.builder()
            .services(List.of(responseDto))
            .serviceWrappers(Collections.singletonList(responseWrapper))
            .build();
    }

    @Override
    @Transactional
    public ServiceResponse updateService(ServiceWrapper wrapper, List<String> roles) {
        if (wrapper == null || wrapper.getService() == null) {
            log.error("Invalid update request: wrapper or service is null");
            return ServiceResponse.builder()
                .services(Collections.emptyList())
                .serviceWrappers(Collections.singletonList(
                    ServiceWrapper.builder()
                        .service(CitizenService.builder()
                            .description("Error: Invalid request")
                            .build())
                        .build()
                ))
                .build();
        }

        CitizenService incoming = wrapper.getService();
        log.debug("updateService called for requestId={} tenant={}",
                incoming.getServiceRequestId(), incoming.getTenantId());

        String serviceRequestId = incoming.getServiceRequestId();
        String tenantId = incoming.getTenantId();

        if (serviceRequestId == null || serviceRequestId.isBlank()) {
            throw new RuntimeException("serviceRequestId is required");
        }

        CitizenService existing = citizenServiceRepository.findByServiceRequestIdAndTenantId(serviceRequestId, tenantId)
                .orElseThrow(() -> {
                    log.error("Service not found for id={} tenant={}", serviceRequestId, tenantId);
                    return new RuntimeException("Service not found: " + serviceRequestId);
                });

        log.debug("Found existing entity id={} status={}", 
                existing.getServiceRequestId(), existing.getApplicationStatus());

        // Apply updates
        applyPartialUpdates(existing, incoming);

        // Handle workflow transition if requested
        WorkflowTransitionResponse workflowResp = handleWorkflowTransition(wrapper, existing, roles);

        // Persist updated entity
        CitizenService saved = citizenServiceRepository.save(existing);
        log.debug("Updated citizen service saved: id={} status={}", 
                saved.getServiceRequestId(), saved.getApplicationStatus());

        // Prepare response
        CitizenService responseDto = createResponseDto(saved, workflowResp);
        ServiceWrapper responseWrapper = ServiceWrapper.builder()
                .service(responseDto)
                .workflow(wrapper.getWorkflow())
                .build();

        // Send notifications
        sendUpdateNotificationsIfNeeded(saved, 
                workflowResp != null ? workflowResp.getAction() : null);

        return ServiceResponse.builder()
            .services(List.of(responseDto))
            .serviceWrappers(Collections.singletonList(responseWrapper))
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResponse searchServicesById(String serviceRequestId, String tenantId) {
        if (serviceRequestId == null || serviceRequestId.isBlank()) {
            log.warn("searchServicesById called with null/empty serviceRequestId");
            return ServiceResponse.builder()
                .services(Collections.emptyList())
                .serviceWrappers(Collections.singletonList(
                    ServiceWrapper.builder()
                        .service(CitizenService.builder()
                            .description("Error: Invalid request")
                            .build())
                        .build()
                ))
                .build();
        }
        
        return citizenServiceRepository.findByServiceRequestIdAndTenantId(serviceRequestId, tenantId)
                .map(service -> ServiceResponse.builder()
                    .services(Collections.singletonList(service))
                    .serviceWrappers(Collections.singletonList(
                        ServiceWrapper.builder()
                            .service(service)
                            .build()))
                    .build())
                .orElse(ServiceResponse.builder()
                    .services(Collections.emptyList())
                    .serviceWrappers(Collections.singletonList(
                        ServiceWrapper.builder()
                            .service(CitizenService.builder()
                                .description("Error: Service not found")
                                .build())
                            .build()))
                    .build());
    }

    private WorkflowTransitionResponse startWorkflow(CitizenService service, String action, List<String> roles) {
        try {
            Map<String, Object> attributes = new HashMap<>();
            // Add roles and tenant ID to workflow attributes
            attributes.put("roles", roles != null ? roles : Collections.emptyList());
            attributes.put("tenantId", service.getTenantId());
            
            // The workflow client will handle the tenant ID from the attributes
            return workflowService.transition(
                    service.getServiceRequestId(),
                    action,
                    "Complaint submitted",
                    attributes
            );
        } catch (Exception ex) {
            log.error("Failed to start workflow for {}: {}", 
                    service.getServiceRequestId(), ex.getMessage(), ex);
            return null;
        }
    }

    private void updateCitizenServiceWithWorkflow(CitizenService service, WorkflowTransitionResponse wfResp) {
        service.setWorkflowInstanceId(wfResp.getId());
        service.setProcessId(wfResp.getProcessId());
        try {
            service.setApplicationStatus(wfResp.getCurrentState());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown workflow state '{}', defaulting to INITIATED", wfResp.getCurrentState());
        }
    }

    private CitizenService createResponseDto(CitizenService saved, WorkflowTransitionResponse wfResp) {
        if (wfResp != null) {
            saved.setWorkflowInstanceId(wfResp.getId());
            if (wfResp.getCurrentState() != null) {
                try {
                    saved.setApplicationStatus(wfResp.getCurrentState());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid status from workflow: {}", wfResp.getCurrentState());
                }
            }
            saved.setAction(wfResp.getAction());
        }
        return saved;
    }

        List<CitizenService> dtos = maybe.map(List::of).orElseGet(List::of);
        
        return ServiceResponse.builder()
            .services(dtos)
            .serviceWrappers(dtos.stream()
                .map(service -> ServiceWrapper.builder()
                    .service(service)
                    .build())
                .collect(Collectors.toList()))
            .build();
    }

    private void validateBoundaryIfPresent(CitizenService citizenService) {
        if (citizenService.getBoundaryCode() != null && !citizenService.getBoundaryCode().isBlank()) {
            boolean boundaryValid = boundaryService.isBoundaryValid(citizenService.getBoundaryCode());
            citizenService.setBoundaryValid(boundaryValid);
            if (!boundaryValid) {
                log.warn("Boundary {} is invalid for tenant {}", 
                        citizenService.getBoundaryCode(), citizenService.getTenantId());
            }
        }
    }

    private void validateFileIfPresent(CitizenService citizenService) {
        if (citizenService.getFileStoreId() != null && !citizenService.getFileStoreId().isBlank()) {
            boolean fileValid = fileStoreService.isFileValid(
                    citizenService.getFileStoreId(), citizenService.getTenantId());
            citizenService.setFileValid(fileValid);
            if (!fileValid) {
                log.warn("FileStoreId {} is invalid or inaccessible for tenant {}", 
                        citizenService.getFileStoreId(), citizenService.getTenantId());
            }
        }
    }

    private void applyPartialUpdates(CitizenService existing, CitizenService incoming) {
        // Update basic fields
        if (incoming.getDescription() != null) existing.setDescription(incoming.getDescription());
        if (incoming.getAddress() != null) existing.setAddress(incoming.getAddress());
        if (incoming.getEmail() != null) existing.setEmail(incoming.getEmail());
        if (incoming.getMobile() != null) existing.setMobile(incoming.getMobile());

        // Handle file updates
        if (incoming.getFileStoreId() != null && 
                !incoming.getFileStoreId().equals(existing.getFileStoreId())) {
            updateFileStore(existing, incoming.getFileStoreId());
        }

        // Handle boundary updates
        if (incoming.getBoundaryCode() != null && 
                !incoming.getBoundaryCode().equals(existing.getBoundaryCode())) {
            updateBoundary(existing, incoming.getBoundaryCode());
        }

        // Update audit details
        if (existing.getAuditDetails() == null) {
            existing.setAuditDetails(new AuditDetails());
        }   
        existing.getAuditDetails().setLastModifiedTime(Instant.now().toEpochMilli());
    }

    private void updateFileStore(CitizenService service, String fileStoreId) {
        service.setFileStoreId(fileStoreId);
        try {
            boolean fileValid = fileStoreService.isFileValid(fileStoreId, service.getTenantId());
            service.setFileValid(fileValid);
            if (!fileValid) {
                log.warn("File {} invalid for tenant {}", fileStoreId, service.getTenantId());
            }
        } catch (Exception e) {
            log.error("File validation failed: {}", e.getMessage(), e);
        }
    }

    private void updateBoundary(CitizenService service, String boundaryCode) {
        service.setBoundaryCode(boundaryCode);
        try {
            boolean boundaryValid = boundaryService.isBoundaryValid(boundaryCode);
            service.setBoundaryValid(boundaryValid);
            if (!boundaryValid) {
                log.warn("Boundary {} invalid for tenant {}", boundaryCode, service.getTenantId());
            }
        } catch (Exception e) {
            log.error("Boundary validation failed: {}", e.getMessage(), e);
        }
    }

    private void sendCreateNotificationsIfNeeded(CitizenService saved) {
        if (saved.getEmail() == null || saved.getEmail().isBlank()) {
            return;
        }

        Map<String, Object> emailPayload = createEmailPayload(saved, null);
        List<String> attachments = getAttachments(saved);
        notificationService.sendEmail("my-template-new", 
                List.of(saved.getEmail()), emailPayload, attachments);
        log.info("Triggered email notification for {}", saved.getServiceRequestId());
    }

    private void sendUpdateNotificationsIfNeeded(CitizenService saved, String workflowAction) {
        if (saved.getEmail() == null || saved.getEmail().isBlank()) {
            return;
        }

        Map<String, Object> emailPayload = createEmailPayload(saved, workflowAction);
        List<String> attachments = getAttachments(saved);
        notificationService.sendEmail("my-template-new", 
                List.of(saved.getEmail()), emailPayload, attachments);
        log.info("Triggered update email for {}", saved.getServiceRequestId());
    }

    private Map<String, Object> createEmailPayload(CitizenService saved, String workflowAction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("applicationNo", saved.getServiceRequestId());
        payload.put("citizenName", saved.getAccountId() != null ? saved.getAccountId() : "");
        payload.put("serviceName", saved.getDescription() != null ? saved.getDescription() : "");
        payload.put("statusLabel", saved.getApplicationStatus());
        payload.put("trackUrl", "https://pgr.digit.org/track/" + saved.getServiceRequestId());
        
        if (workflowAction != null) {
            payload.put("action", workflowAction);
        }
        
        return payload;
    }

    private List<String> getAttachments(CitizenService saved) {
        return saved.getFileStoreId() != null ? 
                List.of(saved.getFileStoreId()) : 
                Collections.emptyList();
    }

    private WorkflowTransitionResponse handleWorkflowTransition(
        ServiceWrapper wrapper, CitizenService existing, List<String> roles) {
    String workflowAction = (wrapper.getWorkflow() != null) ? 
            wrapper.getWorkflow().getAction() : null;

    if (workflowAction == null || workflowAction.isBlank()) {
        return null;
    }

    try {
        Map<String, Object> data = new HashMap<>();
        // This is fine because List<String> is an Object
        data.put("roles", roles != null ? roles : Collections.emptyList());
        
        if (wrapper.getWorkflow() != null && wrapper.getWorkflow().getAssignes() != null) {
            // This is fine because getAssignes() returns List<String> which is an Object
            data.put("assignes", wrapper.getWorkflow().getAssignes());
        }
        
        return workflowService.transition(
            existing.getServiceRequestId(),
            workflowAction,
            "Updating service request",
            data
        );
    } catch (Exception e) {
        log.error("Workflow transition failed for {} action={}: {}", 
            existing.getServiceRequestId(), workflowAction, e.getMessage(), e);
        throw new RuntimeException("Workflow transition failed", e);
    }
}
}
```
**NOTE:** If you are curious on how we got this implementation of service layer or want more details, click here.
