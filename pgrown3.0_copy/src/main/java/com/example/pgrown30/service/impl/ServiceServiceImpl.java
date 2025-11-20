package com.example.pgrown30.service.impl;

import com.example.pgrown30.client.NotificationServiceClient;
import com.example.pgrown30.client.WorkflowServiceClient;
import com.example.pgrown30.validation.CreateServiceValidator;
import com.example.pgrown30.validation.UpdateServiceValidator;
import com.example.pgrown30.web.models.AuditDetails;
import com.example.pgrown30.web.models.CitizenService;
import com.example.pgrown30.web.models.ServiceResponse;
import com.example.pgrown30.web.models.ServiceWrapper;
import com.digit.services.idgen.IdGenClient;
import com.digit.services.idgen.model.IdGenGenerateRequest;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceServiceImpl implements ServiceService {

    private final CitizenServiceRepository citizenServiceRepository;
    private final IdGenClient idGenClient;
    private final WorkflowServiceClient workflowServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final CreateServiceValidator createServiceValidator;
    private final UpdateServiceValidator updateServiceValidator;


    @Value("${idgen.templateCode}")
    private String templateCode;

    // ----------------------- CREATE SERVICE -----------------------
    @Override
    @Transactional
    public ServiceResponse createService(ServiceWrapper wrapper, List<String> roles) {
        CitizenService citizenService = wrapper.getService();
        log.debug("createService: incoming DTO = {}", citizenService);

        // Initialize new service (ID generation, audit details, default values)
        initializeNewService(citizenService);

        // Validate service creation
        createServiceValidator.validate(citizenService);

        // Start workflow
        WorkflowTransitionResponse wfResp = workflowServiceClient.startWorkflow(citizenService, "APPLY", roles);
        if (wfResp != null) {
            workflowServiceClient.updateCitizenServiceWithWorkflow(citizenService, wfResp);
        }

        // Persist entity
        CitizenService saved = citizenServiceRepository.save(citizenService);
        log.debug("Saved citizen service with id={}", saved.getServiceRequestId());

        // Prepare and return response
        return buildServiceResponse(saved, wfResp, wrapper.getWorkflow(), null);
    }

    // ----------------------- UPDATE SERVICE -----------------------
    @Override
    @Transactional
    public ServiceResponse updateService(ServiceWrapper wrapper, List<String> roles) {
        CitizenService incoming = wrapper.getService();
        log.debug("updateService called for requestId={} tenant={}", incoming.getServiceRequestId(), incoming.getTenantId());

        String serviceRequestId = incoming.getServiceRequestId();
        String tenantId = incoming.getTenantId();

        // Validate service request ID
        updateServiceValidator.validateServiceRequestId(serviceRequestId);

        // Validate service exists
        CitizenService existing = updateServiceValidator.validateServiceExists(serviceRequestId, tenantId);

        log.debug("Found existing entity id={} status={}", existing.getServiceRequestId(), existing.getApplicationStatus());

        // Apply updates
        applyPartialUpdates(existing, incoming);

        // Handle workflow transition if requested
        WorkflowTransitionResponse workflowResp = workflowServiceClient.handleWorkflowTransition(wrapper, existing, roles);

        // Persist updated entity
        CitizenService saved = citizenServiceRepository.save(existing);
        log.debug("Updated citizen service saved: id={} status={}", saved.getServiceRequestId(), saved.getApplicationStatus());

        // Prepare and return response
        String workflowAction = workflowResp != null ? workflowResp.getAction() : null;
        return buildServiceResponse(saved, workflowResp, wrapper.getWorkflow(), workflowAction);
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
    /**
     * Initializes a new service with ID, audit details, and default values.
     */
    private void initializeNewService(CitizenService citizenService) {
        // Generate service request ID
        String serviceRequestId = generateServiceRequestId();
        citizenService.setServiceRequestId(serviceRequestId);

        // Set audit details
        AuditDetails auditDetails = createAuditDetails();
        citizenService.setAuditDetails(auditDetails);

        // Set default source if not provided
        setDefaultSource(citizenService);
    }

    /**
     * Generates a new service request ID using IdGen service.
     */
    private String generateServiceRequestId() {
        IdGenGenerateRequest request = IdGenGenerateRequest.builder()
                .templateCode(templateCode)
                .variables(Map.of("ORG", "pgr"))
                .build();

        log.info("Requesting ID from IdGen with templateCode={} and orgCode={}", templateCode, "pgr");
        return idGenClient.generateId(request);
    }

    /**
     * Creates audit details with current timestamp for both created and last modified times.
     */
    private AuditDetails createAuditDetails() {
        long now = Instant.now().toEpochMilli();
        AuditDetails auditDetails = new AuditDetails();
        auditDetails.setCreatedTime(now);
        auditDetails.setLastModifiedTime(now);
        return auditDetails;
    }

    /**
     * Sets default source to "Citizen" if not provided.
     */
    private void setDefaultSource(CitizenService citizenService) {
        if (citizenService.getSource() == null || citizenService.getSource().isBlank()) {
            citizenService.setSource("Citizen");
        }
    }

    /**
     * Builds and returns a service response with workflow information and notifications.
     */
    private ServiceResponse buildServiceResponse(CitizenService saved, WorkflowTransitionResponse workflowResp,
                                                  com.digit.services.workflow.model.Workflow workflow, String workflowAction) {
        // Enrich response with workflow information
        CitizenService responseDto = workflowServiceClient.enrichResponseWithWorkflow(saved, workflowResp);

        // Build response wrapper
        ServiceWrapper responseWrapper = ServiceWrapper.builder()
                .service(responseDto)
                .workflow(workflow)
                .build();

        // Send notifications
        notificationServiceClient.sendNotificationIfNeeded(saved, workflowAction);

        // Build and return response
        return ServiceResponse.builder()
                .services(List.of(responseDto))
                .serviceWrappers(Collections.singletonList(responseWrapper))
                .build();
    }

    private void applyPartialUpdates(CitizenService existing, CitizenService incoming) {
        if (incoming.getDescription() != null) existing.setDescription(incoming.getDescription());
        if (incoming.getAddress() != null) existing.setAddress(incoming.getAddress());
        if (incoming.getEmail() != null) existing.setEmail(incoming.getEmail());
        if (incoming.getMobile() != null) existing.setMobile(incoming.getMobile());

        if (incoming.getFileStoreId() != null && !incoming.getFileStoreId().equals(existing.getFileStoreId())) {
            updateServiceValidator.validateFileStore(existing, incoming.getFileStoreId());
        }

        if (incoming.getBoundaryCode() != null && !incoming.getBoundaryCode().equals(existing.getBoundaryCode())) {
            updateServiceValidator.validateBoundary(existing, incoming.getBoundaryCode());
        }

        // Individual validation commented out
        // Store individualId if provided and different from existing (without validation)
        if (incoming.getIndividualId() != null && !incoming.getIndividualId().equals(existing.getIndividualId())) {
            existing.setIndividualId(incoming.getIndividualId());
            // updateServiceValidator.validateIndividual(existing, incoming.getIndividualId());
        }

        // Audit details are automatically handled by @PreUpdate lifecycle callback in CitizenService entity
    }


}