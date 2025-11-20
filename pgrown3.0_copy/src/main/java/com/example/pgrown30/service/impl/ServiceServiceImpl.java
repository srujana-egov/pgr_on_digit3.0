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
import com.digit.services.individual.IndividualClient;
import com.digit.services.mdms.MdmsClient;

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
    private final IndividualClient individualClient;
    private final MdmsClient mdmsClient;

    @Value("${idgen.templateCode}")
    private String templateCode;

    @Value("${pgr.workflow.processCode}")
    private String processCode;

    // ----------------------- CREATE SERVICE -----------------------
    @Override
    @Transactional
    public ServiceResponse createService(ServiceWrapper wrapper, List<String> roles) {
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

        // External validations (boundary, filestore) — evaluate both and if either fails, log a combined failure message
        try {
            boolean boundaryValid = true;
            boolean fileValid = true;

            if (citizenService.getBoundaryCode() != null && !citizenService.getBoundaryCode().isBlank()) {
                boundaryValid = boundaryClient.isValidBoundariesByCodes(List.of(citizenService.getBoundaryCode()));
                citizenService.setBoundaryValid(boundaryValid);
                if (!boundaryValid) {
                    log.warn("Boundary {} is invalid for tenant {}", citizenService.getBoundaryCode(), citizenService.getTenantId());
                }
            }

            if (citizenService.getFileStoreId() != null && !citizenService.getFileStoreId().isBlank()) {
                fileValid = filestoreClient.isFileAvailable(citizenService.getFileStoreId(), citizenService.getTenantId());
                citizenService.setFileValid(fileValid);
                log.info("File validation for fileStoreId={} tenantId={}: {}", citizenService.getFileStoreId(), citizenService.getTenantId(), fileValid ? "VALID" : "INVALID");
                if (!fileValid) {
                    log.warn("FileStoreId {} is invalid or inaccessible for tenant {}", citizenService.getFileStoreId(), citizenService.getTenantId());
                }
            }

            // If either check failed (and was actually run), log a single ERROR-level failure message for easier alerting
            if (!boundaryValid || !fileValid) {
                log.error("External validation FAILED for serviceRequestId={}, tenant={} (boundaryValid={}, fileValid={}, boundaryCode={}, fileStoreId={})",
                        citizenService.getServiceRequestId(),
                        citizenService.getTenantId(),
                        boundaryValid, fileValid,
                        citizenService.getBoundaryCode(), citizenService.getFileStoreId());
            }
        } catch (Exception e) {
            log.error("External validation failed (boundary/filestore) with exception: {}", e.getMessage(), e);
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

        // Send notifications (create)
        sendNotificationsIfNeeded(saved, null);

        return ServiceResponse.builder()
                .services(List.of(responseDto))
                .serviceWrappers(Collections.singletonList(responseWrapper))
                .build();
    }

    // ----------------------- UPDATE SERVICE -----------------------
    @Override
    @Transactional
    public ServiceResponse updateService(ServiceWrapper wrapper, List<String> roles) {
        CitizenService incoming = wrapper.getService();
        log.debug("updateService called for requestId={} tenant={}", incoming.getServiceRequestId(), incoming.getTenantId());

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

        log.debug("Found existing entity id={} status={}", existing.getServiceRequestId(), existing.getApplicationStatus());

        // Apply updates
        applyPartialUpdates(existing, incoming);

        // Handle workflow transition if requested
        WorkflowTransitionResponse workflowResp = handleWorkflowTransition(wrapper, existing, roles);

        // Persist updated entity
        CitizenService saved = citizenServiceRepository.save(existing);
        log.debug("Updated citizen service saved: id={} status={}", saved.getServiceRequestId(), saved.getApplicationStatus());

        // Prepare response
        CitizenService responseDto = createResponseDto(saved, workflowResp);
        ServiceWrapper responseWrapper = ServiceWrapper.builder()
                .service(responseDto)
                .workflow(wrapper.getWorkflow())
                .build();

        // Send notifications (update)
        sendNotificationsIfNeeded(saved, workflowResp != null ? workflowResp.getAction() : null);

        return ServiceResponse.builder()
                .services(List.of(responseDto))
                .serviceWrappers(Collections.singletonList(responseWrapper))
                .build();
    }

    // ----------------------- SEARCH SERVICE BY ID -----------------------
    @Override
    @Transactional(readOnly = true)
    public ServiceResponse searchServicesById(String serviceRequestId, String tenantId) {
        // Controller validates inputs; service assumes valid parameters.
        return citizenServiceRepository.findByServiceRequestIdAndTenantId(serviceRequestId, tenantId)
                .map(service -> ServiceResponse.builder()
                        .services(Collections.singletonList(service))
                        .serviceWrappers(Collections.singletonList(ServiceWrapper.builder().service(service).build()))
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

    // ----------------------- HELPERS -----------------------
    private WorkflowTransitionResponse startWorkflow(CitizenService service, String action, List<String> roles) {
        try {
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("roles", roles != null ? roles : Collections.emptyList());
            attributes.put("tenantId", Collections.singletonList(service.getTenantId()));

            WorkflowTransitionRequest request = WorkflowTransitionRequest.builder()
                    .processId(workflowClient.getProcessByCode(processCode))
                    .entityId(service.getServiceRequestId())
                    .action(action)
                    .comment("Complaint submitted")
                    .attributes(attributes)
                    .build();

            return workflowClient.executeTransition(request);
        } catch (Exception ex) {
            log.error("Failed to start workflow for {}: {}", service.getServiceRequestId(), ex.getMessage(), ex);
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
        if (incoming.getDescription() != null) existing.setDescription(incoming.getDescription());
        if (incoming.getAddress() != null) existing.setAddress(incoming.getAddress());
        if (incoming.getEmail() != null) existing.setEmail(incoming.getEmail());
        if (incoming.getMobile() != null) existing.setMobile(incoming.getMobile());

        if (incoming.getFileStoreId() != null && !incoming.getFileStoreId().equals(existing.getFileStoreId())) {
            updateFileStore(existing, incoming.getFileStoreId());
        }

        if (incoming.getBoundaryCode() != null && !incoming.getBoundaryCode().equals(existing.getBoundaryCode())) {
            updateBoundary(existing, incoming.getBoundaryCode());
        }

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

    private WorkflowTransitionResponse handleWorkflowTransition(ServiceWrapper wrapper, CitizenService existing, List<String> roles) {
        String workflowAction = (wrapper.getWorkflow() != null) ? wrapper.getWorkflow().getAction() : null;

        if (workflowAction == null || workflowAction.isBlank()) {
            return null;
        }

        try {
            Map<String, List<String>> data = new HashMap<>();
            data.put("roles", roles != null ? roles : Collections.emptyList());

            if (wrapper.getWorkflow() != null && wrapper.getWorkflow().getAssignes() != null) {
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
            log.error("Workflow transition failed for {} action={}: {}", existing.getServiceRequestId(), workflowAction, e.getMessage(), e);
            throw new RuntimeException("Workflow transition failed", e);
        }
    }
}