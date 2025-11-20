# Step 1: Implementing your Service logic 

The service layer contains business logic and interacts with repositories to manage data. 
Use the client library available as shown below to integrate with DIGIT services. 
To learn more about the client library and available functions, click here. 

## **Steps**
For the example use the below file, ServiceServiceImpl.java:
   ```java
   package com.example.pgrown30.service.impl;

import com.digit.services.boundary.BoundaryClient;
import com.example.pgrown30.web.models.AuditDetails;
import com.example.pgrown30.web.models.CitizenService;
import com.example.pgrown30.web.models.ServiceResponse;
import com.example.pgrown30.web.models.ServiceWrapper;
import com.digit.services.idgen.IdGenClient;
import com.digit.services.idgen.model.IdGenGenerateRequest;
import com.digit.services.filestore.FilestoreClient;
import com.digit.services.notification.NotificationClient;
import com.digit.services.notification.model.SendEmailRequest;
import com.digit.services.workflow.WorkflowClient;
import com.digit.services.workflow.model.WorkflowTransitionRequest;
import com.digit.services.workflow.model.WorkflowTransitionResponse;
import com.example.pgrown30.repository.CitizenServiceRepository;
import com.example.pgrown30.service.ServiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 private final IdGenClient idGenClient;
 private final FilestoreClient filestoreClient;
 private final BoundaryClient boundaryClient;
 private final NotificationClient notificationClient;
 private final WorkflowClient workflowClient;
 
 @Value("${idgen.templateCode}")
 private String templateCode;
 
 @Value("${pgr.workflow.processCode}")
 private String processCode;

 //CREATE SERVICE
 @Override
 @Transactional
 public ServiceResponse citizenServiceCreate(ServiceWrapper wrapper, List<String> roles) {
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
     IdGenGenerateRequest request = IdGenGenerateRequest.builder()
             .templateCode(templateCode)
             .variables(Map.of("ORG", "pgr"))
             .build();
     
     log.info("Requesting ID from IdGen with templateCode={} and orgCode={}", templateCode, "pgr");
     String newId = idGenClient.generateId(request);
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
         if (citizenService.getBoundaryCode() != null && !citizenService.getBoundaryCode().isBlank()) {
             boolean boundaryValid = boundaryClient.isValidBoundariesByCodes(List.of(citizenService.getBoundaryCode()));
             citizenService.setBoundaryValid(boundaryValid);
             if (!boundaryValid) {
                 log.warn("Boundary {} is invalid for tenant {}", 
                         citizenService.getBoundaryCode(), citizenService.getTenantId());
             }
         }
         
         if (citizenService.getFileStoreId() != null && !citizenService.getFileStoreId().isBlank()) {
             boolean fileValid = filestoreClient.isFileAvailable(
                     citizenService.getFileStoreId(), citizenService.getTenantId());
             log.info("File validation for fileStoreId={} tenantId={}: {}", 
                     citizenService.getFileStoreId(), citizenService.getTenantId(), fileValid ? "VALID" : "INVALID");
             citizenService.setFileValid(fileValid);
             if (!fileValid) {
                 log.warn("FileStoreId {} is invalid or inaccessible for tenant {}", 
                         citizenService.getFileStoreId(), citizenService.getTenantId());
             }
         }
     } catch (Exception e) {
         log.error("External validation failed (boundary/filestore): {}", e.getMessage(), e);
     }

     // Start workflow
     WorkflowTransitionResponse wfResp = startWorkflow(citizenService, "APPLY", roles);
     
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
     sendNotificationsIfNeeded(saved, null);

     return ServiceResponse.builder()
         .services(List.of(responseDto))
         .serviceWrappers(Collections.singletonList(responseWrapper))
         .build();
 }

 //UPDATE SERVICE
 @Override
 @Transactional
 public ServiceResponse citizenServiceUpdate(ServiceWrapper wrapper, List<String> roles) {
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
     sendNotificationsIfNeeded(saved, 
             workflowResp != null ? workflowResp.getAction() : null);

     return ServiceResponse.builder()
         .services(List.of(responseDto))
         .serviceWrappers(Collections.singletonList(responseWrapper))
         .build();
 }

 //SEARCH SERVICE BY ID
 @Override
 @Transactional(readOnly = true)
 public ServiceResponse citizenServiceSearch(String serviceRequestId, String tenantId) {
     if (serviceRequestId == null || serviceRequestId.isBlank()) {
         log.warn("citizenServiceSearch called with null/empty serviceRequestId");
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

 //HELPER FUNCTIONS
 private WorkflowTransitionResponse startWorkflow(CitizenService service, String action, List<String> roles) {
     try {
         Map<String, List<String>> attributes = new HashMap<>();
         // Add roles and tenant ID to workflow attributes
         attributes.put("roles", roles != null ? roles : Collections.emptyList());
         attributes.put("tenantId", Collections.singletonList(service.getTenantId()));
         
         WorkflowTransitionRequest request = WorkflowTransitionRequest.builder()
                 .processId(workflowClient.getProcessByCode(processCode))
                 .entityId(service.getServiceRequestId())
                 .action(action)
                 .comment("Complaint submitted")
                 .attributes(attributes)
                 .build();
         
         // The workflow client will handle the tenant ID from the attributes
         return workflowClient.executeTransition(request);
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
         saved.setAction(wfResp.getAction());
     }
     return saved;
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
         boolean fileValid = filestoreClient.isFileAvailable(fileStoreId, service.getTenantId());
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
         boolean boundaryValid = boundaryClient.isValidBoundariesByCodes(List.of(boundaryCode));
         service.setBoundaryValid(boundaryValid);
         if (!boundaryValid) {
             log.warn("Boundary {} invalid for tenant {}", boundaryCode, service.getTenantId());
         }
     } catch (Exception e) {
         log.error("Boundary validation failed: {}", e.getMessage(), e);
     }
 }

 private void sendNotificationsIfNeeded(CitizenService saved, String workflowAction) {
     if (saved.getEmail() == null || saved.getEmail().isBlank()) {
         return;
     }

     Map<String, Object> emailPayload = createEmailPayload(saved, workflowAction);
     
     SendEmailRequest request = SendEmailRequest.builder()
             .version("v1")
             .templateId("my-template-new")
             .emailIds(List.of(saved.getEmail()))
             .enrich(false)
             .payload(emailPayload)
             .build();
             
     notificationClient.sendEmail(request);
     String notificationType = workflowAction != null ? "update" : "create";
     log.info("Triggered {} email notification for {}", notificationType, saved.getServiceRequestId());
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

 
 private WorkflowTransitionResponse handleWorkflowTransition(
     ServiceWrapper wrapper, CitizenService existing, List<String> roles) {
 String workflowAction = (wrapper.getWorkflow() != null) ? 
         wrapper.getWorkflow().getAction() : null;

 if (workflowAction == null || workflowAction.isBlank()) {
     return null;
 }

 try {
     Map<String, List<String>> data = new HashMap<>();
     // This is fine because List<String> is an Object
     data.put("roles", roles != null ? roles : Collections.emptyList());
     
     if (wrapper.getWorkflow() != null && wrapper.getWorkflow().getAssignes() != null) {
         // This is fine because getAssignes() returns List<String> which is an Object
         data.put("assignes", wrapper.getWorkflow().getAssignes());
     }
     
     WorkflowTransitionRequest request = WorkflowTransitionRequest.builder()
             .processId(workflowClient.getProcessByCode(processCode))
             .entityId(existing.getServiceRequestId())
             .action(workflowAction)
             .comment("Updating service request")
             .attributes(data)
             .build();
     
     return workflowClient.executeTransition(request);
 } catch (Exception e) {
     log.error("Workflow transition failed for {} action={}: {}", 
         existing.getServiceRequestId(), workflowAction, e.getMessage(), e);
     throw new RuntimeException("Workflow transition failed", e);
 }
}
}
   ```
**NOTE:** If you are curious on how we got this implementation of service layer or want more details, click here.
