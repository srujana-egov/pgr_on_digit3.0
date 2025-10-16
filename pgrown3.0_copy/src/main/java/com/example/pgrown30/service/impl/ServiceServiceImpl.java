package com.example.pgrown30.service.impl;

import com.example.pgrown30.config.PgrConfig;
import com.example.pgrown30.domain.CitizenServiceEntity;
import com.example.pgrown30.domain.Status;
import com.example.pgrown30.mapper.CitizenServiceMapper;
import com.example.pgrown30.repository.*;
import com.example.pgrown30.service.ServiceService;
import com.example.pgrown30.web.models.CitizenService;
import com.example.pgrown30.web.models.ResponseInfo;
import com.example.pgrown30.web.models.ServiceResponse;
import com.example.pgrown30.web.models.ServiceWrapper;
import com.example.pgrown30.web.models.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ServiceServiceImpl implements ServiceService {

    private final CitizenServiceRepository citizenServiceRepository;
    private final IdGenRepository idGenRepository;
    private final FileStoreRepository fileStoreRepository;
    private final BoundaryRepository boundaryRepository;
    private final NotificationRepository notificationRepository;
    private final WorkflowRepository workflowRepository;
    private final PgrConfig pgrConfig;

    public ServiceServiceImpl(
            CitizenServiceRepository citizenServiceRepository,
            IdGenRepository idGenRepository,
            FileStoreRepository fileStoreRepository,
            BoundaryRepository boundaryRepository,
            NotificationRepository notificationRepository,
            WorkflowRepository workflowRepository,
            PgrConfig pgrConfig) {

        this.citizenServiceRepository = citizenServiceRepository;
        this.idGenRepository = idGenRepository;
        this.fileStoreRepository = fileStoreRepository;
        this.boundaryRepository = boundaryRepository;
        this.notificationRepository = notificationRepository;
        this.workflowRepository = workflowRepository;
        this.pgrConfig = pgrConfig;
    }

    @Override
public ServiceResponse createService(ServiceWrapper wrapper) {
    CitizenService dto = wrapper.getService();

    CitizenServiceEntity service = CitizenServiceMapper.toEntity(dto);
    String newId = idGenRepository.generateId("service_request");
    long now = Instant.now().toEpochMilli();

    if (dto.getSource() == null || dto.getSource().isEmpty()) {
        log.warn("Source not provided, defaulting to 'Citizen' for serviceRequestId={}", newId);
        service.setSource("Citizen");
    }

    service.setServiceRequestId(newId);
    service.setCreatedTime(now);
    service.setLastModifiedTime(now);

    validateBoundary(service);
    validateFileStore(service);

    String processId = pgrConfig.getProcessId();
    WorkflowResult workflowResult = startWorkflow(service, newId, processId);

    service.setWorkflowInstanceId(workflowResult.getInstanceId());
    service.setProcessId(processId);
    service.setAction(workflowResult.getInitialAction());
    service.setApplicationStatus(workflowResult.getStatus());

    citizenServiceRepository.save(service);

    // Send notifications only if service is valid
    if (isServiceValid(service)) {
        sendNotifications(service);
    } else {
        log.warn("Notifications skipped for serviceRequestId={} due to validation failures", service.getServiceRequestId());
    }

    CitizenService responseDto = CitizenServiceMapper.toDto(service);
    responseDto.setApplicationStatus(workflowResult.getStatus().name());
    responseDto.setWorkflowInstanceId(workflowResult.getInstanceId());
    responseDto.setAction(workflowResult.getInitialAction());

    // Build Notification object for response
    Notification notification = Notification.builder()
    .templateId("service-request-received-new")
    .version("1.0.0")
    .type("EMAIL")
    .emailIds(List.of(service.getEmail()))
    .payload(Map.of(
        "serviceName", service.getDescription(),
        "serviceRequestId", service.getServiceRequestId()
    ))
    .subject("Service request created")
    .message("Your request for " + service.getDescription() + " has been registered.")
    .channels(List.of("EMAIL"))
    .build();

    ServiceWrapper responseWrapper = ServiceWrapper.builder()
            .service(responseDto)
            .workflow(wrapper.getWorkflow())
            .notification(notification)
            .build();

    return new ServiceResponse(List.of(responseDto), ResponseInfo.success(), List.of(responseWrapper));
}

 @Override
public ServiceResponse updateService(ServiceWrapper wrapper) {
    CitizenService dto = wrapper.getService();
    CitizenServiceEntity service = CitizenServiceMapper.toEntity(dto);

    CitizenServiceEntity existing = citizenServiceRepository
            .findById(service.getServiceRequestId())
            .orElseThrow(() -> new RuntimeException("Service not found: " + service.getServiceRequestId()));

    if (dto.getBoundaryCode() != null) {
        boolean isValid = boundaryRepository.isBoundaryValid(dto.getBoundaryCode());
        service.setBoundaryValid(isValid);
    }

    String workflowAction = wrapper.getWorkflow() != null ? wrapper.getWorkflow().getAction() : null;

    if (existing.getWorkflowInstanceId() != null && workflowAction != null) {
        String workflowProcessId = existing.getProcessId();

        if (!isWorkflowProcessValid(existing.getTenantId(), workflowProcessId)) {
            throw new RuntimeException("Workflow process not found: " + workflowProcessId);
        }

        boolean success = workflowRepository.updateProcessInstance(
                existing.getTenantId(),
                existing.getWorkflowInstanceId(),
                workflowProcessId,
                workflowAction
        );

        if (!success) {
            throw new RuntimeException("Workflow update failed for " + existing.getWorkflowInstanceId());
        }

        existing.setAction(workflowAction);
    }

    existing.setDescription(service.getDescription());

    if (dto.getApplicationStatus() != null) {
        existing.setApplicationStatus(Status.valueOf(dto.getApplicationStatus()));
    }

    existing.setLastModifiedTime(Instant.now().toEpochMilli());

    if (existing.getFileStoreId() != null) {
        existing.setFileValid(fileStoreRepository.isFileValid(existing.getTenantId(), existing.getFileStoreId()));
    }

    citizenServiceRepository.save(existing);

    if (isServiceValid(existing)) {
        sendNotifications(existing);
    } else {
        log.warn("Notifications skipped for serviceRequestId={} due to validation failures", existing.getServiceRequestId());
    }

    CitizenService responseDto = CitizenServiceMapper.toDto(existing);

    Notification notification = Notification.builder()
            .templateId("service-request-received-new")
            .version("1.0.0")
            .type("EMAIL")
            .emailIds(List.of(service.getEmail()))
            .payload(Map.of(
                "serviceName", service.getDescription(),
                "serviceRequestId", service.getServiceRequestId()
            ))
            .subject("Service request updated")
            .message("Your request for " + service.getDescription() + " has been updated.")
            .channels(List.of("EMAIL"))
            .build();

    // ✅ FIX: build responseWrapper before returning
    ServiceWrapper responseWrapper = ServiceWrapper.builder()
            .service(responseDto)
            .workflow(wrapper.getWorkflow())
            .notification(notification)
            .build();

    return new ServiceResponse(List.of(responseDto), ResponseInfo.success(), List.of(responseWrapper));
}



    @Override
public ServiceResponse searchServices(ServiceWrapper wrapper) {
    CitizenService dto = wrapper.getService();
    if (dto == null || dto.getTenantId() == null || dto.getTenantId().isEmpty()) {
        throw new RuntimeException("tenantId is required for searching services");
    }

    Specification<CitizenServiceEntity> spec = Specification.where(
        (root, query, cb) -> cb.equal(root.get("tenantId"), dto.getTenantId())
    );

    if (dto.getServiceCode() != null && !dto.getServiceCode().isEmpty()) {
        spec = spec.and((root, query, cb) -> cb.equal(root.get("serviceCode"), dto.getServiceCode()));
    }

    if (dto.getApplicationStatus() != null && !dto.getApplicationStatus().isEmpty()) {
        spec = spec.and((root, query, cb) ->
            cb.equal(root.get("applicationStatus"), Status.valueOf(dto.getApplicationStatus()))
        );
    }

    List<CitizenServiceEntity> results = citizenServiceRepository.findAll(spec);

    List<CitizenService> serviceDTOs = results.stream()
            .map(CitizenServiceMapper::toDto)
            .collect(Collectors.toList());

    return new ServiceResponse(serviceDTOs, ResponseInfo.success(), Collections.emptyList());

}


    // --- Helper Methods ---

    private void validateBoundary(CitizenServiceEntity service) {
    if (service.getBoundaryCode() != null) {
        boolean isValid = boundaryRepository.isBoundaryValid(
                service.getBoundaryCode()
        );
        service.setBoundaryValid(isValid);

        if (!isValid) {
            log.warn("Boundary code {} is invalid for tenant {}", 
                     service.getBoundaryCode(), service.getTenantId());
        }
    }
    }

    private void validateFileStore(CitizenServiceEntity service) {
        if (service.getFileStoreId() != null) {
            service.setFileValid(fileStoreRepository.isFileValid(service.getTenantId(), service.getFileStoreId()));
        }
    }

    private WorkflowResult startWorkflow(CitizenServiceEntity service, String complaintNumber, String processId) {
        String tenantId = service.getTenantId();
        String initialAction = "APPLY";

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("channel", List.of("Citizen"));

        Map<String, Object> transitionResp = workflowRepository.transition(
                tenantId, "pgr-citizen", processId, complaintNumber,
                initialAction, "Complaint submitted", attributes
        );

        Map<String, Object> latest = workflowRepository.getLatestInstance(tenantId, processId, complaintNumber);
        String wfInstanceId = extractWorkflowInstanceId(transitionResp, latest);
        String currentState = extractWorkflowState(latest);

        if (wfInstanceId == null) {
            throw new RuntimeException("Workflow started but instance id could not be determined for " + complaintNumber);
        }

        Status status = currentState != null ? Status.valueOf(currentState) : Status.INITIATED;
        return new WorkflowResult(wfInstanceId, initialAction, status);
    }

    private String extractWorkflowInstanceId(Map<String, Object> transitionResp, Map<String, Object> latest) {
        if (latest != null) {
            if (latest.get("id") != null) return String.valueOf(latest.get("id"));
            if (latest.get("data") instanceof Map) {
                Map<?, ?> data = (Map<?, ?>) latest.get("data");
                if (data.get("id") != null) return String.valueOf(data.get("id"));
            }
        }
        if (transitionResp != null && transitionResp.get("id") != null) {
            return String.valueOf(transitionResp.get("id"));
        }
        return null;
    }

    private String extractWorkflowState(Map<String, Object> latest) {
        if (latest != null) {
            if (latest.get("state") != null) return String.valueOf(latest.get("state"));
            if (latest.get("data") instanceof Map) {
                Map<?, ?> data = (Map<?, ?>) latest.get("data");
                if (data.get("state") != null) return String.valueOf(data.get("state"));
            }
        }
        return null;
    }

    private boolean isWorkflowProcessValid(String tenantId, String processId) {
        return workflowRepository.processExists(tenantId, processId);
    }

    private static class WorkflowResult {
        private final String instanceId;
        private final String initialAction;
        private final Status status;

        public WorkflowResult(String instanceId, String initialAction, Status status) {
            this.instanceId = instanceId;
            this.initialAction = initialAction;
            this.status = status;
        }

        public String getInstanceId() { return instanceId; }
        public String getInitialAction() { return initialAction; }
        public Status getStatus() { return status; }
    }

private void sendNotifications(CitizenServiceEntity service) {
    if (service.getTenantId() == null) return;

    // Determine workflow action for dynamic templates
    String workflowAction = service.getAction() != null ? service.getAction() : "APPLY";

    // Map workflow actions to SMS templates
    Map<String, String> smsTemplates = Map.of(
        "APPLY", "service-initiated",
        "ASSIGN", "service-assigned",
        "RESOLVE", "service-resolved",
        "CLOSE", "service-closed"
    );
    String smsTemplate = smsTemplates.getOrDefault(workflowAction, "service-update");

    // --- Email Notification ---
    if (service.getEmail() != null && !service.getEmail().isEmpty()) {
        List<String> emails = List.of(service.getEmail());
        Map<String, Object> emailPayload = Map.of(
                "applicationNo", service.getServiceRequestId(),
                "citizenName", service.getAccountId(),
                "serviceName", service.getDescription(),
                "statusLabel", service.getApplicationStatus().name(),
                "action", workflowAction,
                "trackUrl", "https://pgr.digit.org/track/" + service.getServiceRequestId(),
                "ulbName", "Hyderabad Municipal Corporation"
        );

        List<String> attachments = service.getFileStoreId() != null ? List.of(service.getFileStoreId()) : Collections.emptyList();

        try {
            notificationRepository.sendEmail(
                    "service-request-received-new",
                    emails,
                    emailPayload,
                    attachments
            );
            log.info("Email notification triggered for serviceRequestId={}", service.getServiceRequestId());
        } catch (Exception e) {
            log.error("Failed to send email for serviceRequestId={}: {}", service.getServiceRequestId(), e.getMessage());
        }
    }

    // --- SMS Notification ---
    if (service.getMobile() != null && !service.getMobile().isEmpty()) {
        List<String> mobiles = List.of(service.getMobile());
        Map<String, Object> smsPayload = Map.of(
                "applicationNo", service.getServiceRequestId(),
                "serviceName", service.getDescription(),
                "statusLabel", service.getApplicationStatus().name(),
                "action", workflowAction
        );

        try {
            notificationRepository.sendSms(
                    smsTemplate,
                    mobiles,
                    smsPayload,
                    "INFO"
            );
            log.info("SMS notification triggered for serviceRequestId={}", service.getServiceRequestId());
        } catch (Exception e) {
            log.error("Failed to send SMS for serviceRequestId={}: {}", service.getServiceRequestId(), e.getMessage());
        }
    }
}


// --- Helper Method ---
private boolean isServiceValid(CitizenServiceEntity service) {
    return Boolean.TRUE.equals(service.getBoundaryValid()) &&
           (service.getFileStoreId() == null || Boolean.TRUE.equals(service.getFileValid()));
}


}