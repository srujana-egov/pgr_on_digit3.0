package com.example.pgrown30.service.impl;

import com.digit.services.workflow.model.WorkflowTransitionResponse;
import com.example.pgrown30.domain.CitizenServiceEntity;
import com.example.pgrown30.domain.Status;
import com.example.pgrown30.mapper.CitizenServiceMapper;
import com.example.pgrown30.repository.CitizenServiceRepository;
import com.example.pgrown30.client.BoundaryService;
import com.example.pgrown30.client.IdGenService;
import com.example.pgrown30.client.NotificationService;
import com.example.pgrown30.client.WorkflowService;
import com.example.pgrown30.util.FileStoreUtil;
import com.example.pgrown30.service.ServiceService;
import com.example.pgrown30.web.models.ServiceResponse;
import com.example.pgrown30.web.models.ServiceWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.JoinType;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceServiceImpl implements ServiceService {

    private final CitizenServiceRepository citizenServiceRepository;
    private final IdGenService idGenService;
    private final FileStoreUtil fileStoreUtil;
    private final BoundaryService boundaryService;
    private final NotificationService notificationService;
    private final WorkflowService workflowService;

    // Injected directly from application.properties -> allows deleting PgrConfig
    @Value("${pgr.workflow.processId}")
    private String workflowProcessId;

    // -------------------------
    // CREATE
    // -------------------------
    @Override
    @Transactional
    public ServiceResponse createService(ServiceWrapper wrapper, List<String> roles) {
        // Basic checks
        if (wrapper == null || wrapper.getService() == null) {
            log.error("Invalid request: wrapper or service is null");
            return new ServiceResponse(Collections.emptyList(), Collections.emptyList());
        }

        // Extract DTO
        com.example.pgrown30.web.models.CitizenService citizenService = wrapper.getService();
        log.debug("createService: incoming DTO = {}", citizenService);

        // --- generate id, timestamps, defaults ---
        String newId = idGenService.generateId("service_request");
        long now = Instant.now().toEpochMilli();

        citizenService.setServiceRequestId(newId);
        citizenService.setCreatedTime(now);
        citizenService.setLastModifiedTime(now);

        if (citizenService.getSource() == null || citizenService.getSource().isBlank()) {
            citizenService.setSource("Citizen");
        }

        // --- validate boundary and filestore (if present) ---
        try {
            validateBoundaryIfPresent(citizenService);
            validateFileIfPresent(citizenService);
        } catch (Exception e) {
            log.error("External validation failed (boundary/filestore): {}", e.getMessage(), e);
            // continue for now — policy decision to continue rather than abort
        }

        // --- map DTO -> entity ---
        CitizenServiceEntity entity = CitizenServiceMapper.toEntity(citizenService);

        // --- start workflow ---
        String initialAction = "APPLY";
        Map<String, List<String>> attributes = Map.of("roles", roles == null ? List.of() : roles);
        String processId = workflowProcessId;

        WorkflowTransitionResponse wfResp = null;
        try {
            wfResp = workflowService.transition(
                    citizenService.getServiceRequestId(),
                    initialAction,
                    "Complaint submitted",
                    attributes
            );
            log.debug("Workflow started: id={} state={}", wfResp == null ? null : wfResp.getId(),
                    wfResp == null ? null : wfResp.getCurrentState());
        } catch (Exception ex) {
            log.error("Failed to start workflow for {}: {}", citizenService.getServiceRequestId(), ex.getMessage(), ex);
            wfResp = null;
        }

        // attach workflow info to entity and set status
        if (wfResp != null) {
            entity.setWorkflowInstanceId(wfResp.getId());
            entity.setProcessId(processId);
            try {
                entity.setApplicationStatus(Status.valueOf(wfResp.getCurrentState()));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown workflow state '{}', defaulting to INITIATED", wfResp.getCurrentState());
                entity.setApplicationStatus(Status.INITIATED);
            }
        } else {
            entity.setApplicationStatus(Status.INITIATED);
        }

        // --- persist entity ---
        CitizenServiceEntity saved = citizenServiceRepository.save(entity);
        log.debug("Saved citizen service with id={}", saved.getServiceRequestId());

        // --- map saved entity -> DTO response ---
        com.example.pgrown30.web.models.CitizenService responseDto = CitizenServiceMapper.toDto(saved);

        if (wfResp != null) {
            responseDto.setWorkflowInstanceId(wfResp.getId());
            responseDto.setApplicationStatus(wfResp.getCurrentState() != null ? wfResp.getCurrentState() : responseDto.getApplicationStatus());
            responseDto.setAction(wfResp.getAction());
        } else {
            responseDto.setApplicationStatus(Status.INITIATED.name());
        }

        ServiceWrapper responseWrapper = ServiceWrapper.builder()
                .service(responseDto)
                .workflow(wrapper.getWorkflow())
                .build();

        // --- notifications (async) ---
        try {
            sendCreateNotificationsIfNeeded(saved);
        } catch (Exception e) {
            log.error("Failed to trigger notifications for {}: {}", saved.getServiceRequestId(), e.getMessage(), e);
        }

        // final response
        return new ServiceResponse(List.of(responseDto), List.of(responseWrapper));
    }

    // -------------------------
    // UPDATE
    // -------------------------
    @Override
    @Transactional
    public ServiceResponse updateService(ServiceWrapper wrapper, List<String> roles) {
        // Step 1: basic validation
        if (wrapper == null || wrapper.getService() == null) {
            log.error("Invalid update request: wrapper or service is null");
            return new ServiceResponse(Collections.emptyList(), Collections.emptyList());
        }

        var citizenService = wrapper.getService();
        log.debug("updateService called for requestId={} tenant={}",
                citizenService.getServiceRequestId(), citizenService.getTenantId());

        // Step 2: load existing entity and validate
        String serviceRequestId = citizenService.getServiceRequestId();
        String tenantId = citizenService.getTenantId();

        if (serviceRequestId == null || serviceRequestId.isBlank()) {
            log.error("updateService: serviceRequestId is required");
            throw new RuntimeException("serviceRequestId is required");
        }

        var maybeExisting = citizenServiceRepository.findByServiceRequestIdAndTenantId(serviceRequestId, tenantId);
        if (maybeExisting.isEmpty()) {
            log.error("Service not found for id={} tenant={}", serviceRequestId, tenantId);
            throw new RuntimeException("Service not found: " + serviceRequestId);
        }

        CitizenServiceEntity existing = maybeExisting.get();
        log.debug("Found existing entity id={} status={}", existing.getServiceRequestId(), existing.getApplicationStatus());

        // Step 3: apply incoming changes (only when provided) and refresh validations/timestamps
        applyPartialUpdates(existing, citizenService);

        // Step 4: perform workflow transition when an action is provided
        String workflowAction = (wrapper.getWorkflow() != null) ? wrapper.getWorkflow().getAction() : null;
        WorkflowTransitionResponse workflowResp = null;

        if (workflowAction != null && !workflowAction.isBlank()) {
            try {
                Map<String, List<String>> wfAttrs = Map.of("roles", roles == null ? List.of() : roles);
                workflowResp = workflowService.updateProcessInstance(
                        existing.getServiceRequestId(),
                        workflowProcessId,
                        workflowAction,
                        roles
                );
                log.debug("Workflow transition returned: id={}, state={}", workflowResp.getId(), workflowResp.getCurrentState());
            } catch (Exception e) {
                log.error("Workflow transition failed for {} action={}: {}", existing.getServiceRequestId(), workflowAction, e.getMessage(), e);
            }
        }

        // If WF returned data, update entity status/instance id
        if (workflowResp != null) {
            existing.setWorkflowInstanceId(workflowResp.getId());
            try {
                existing.setApplicationStatus(Status.valueOf(workflowResp.getCurrentState()));
            } catch (IllegalArgumentException ex) {
                log.warn("Unknown workflow state '{}'; leaving previous status", workflowResp.getCurrentState());
            }
        }

        // Step 5: persist updated entity
        CitizenServiceEntity saved = citizenServiceRepository.save(existing);
        log.debug("Updated citizen service saved: id={} status={}", saved.getServiceRequestId(), saved.getApplicationStatus());

        // Step 6: prepare DTO response
        com.example.pgrown30.web.models.CitizenService responseDto = CitizenServiceMapper.toDto(saved);

        if (workflowResp != null) {
            responseDto.setWorkflowInstanceId(workflowResp.getId());
            responseDto.setApplicationStatus(
                    workflowResp.getCurrentState() != null
                            ? workflowResp.getCurrentState()
                            : responseDto.getApplicationStatus());
            responseDto.setAction(workflowResp.getAction());
        }

        ServiceWrapper responseWrapper = ServiceWrapper.builder()
                .service(responseDto)
                .workflow(wrapper.getWorkflow())
                .build();

        // Step 7: notifications (async)
        try {
            sendUpdateNotificationsIfNeeded(saved, workflowAction);
        } catch (Exception e) {
            log.error("Failed to trigger notifications for {}: {}", saved.getServiceRequestId(), e.getMessage(), e);
        }

        // Final: return ServiceResponse
        return new ServiceResponse(List.of(responseDto), List.of(responseWrapper));
    }

    // -------------------------
    // SEARCH
    // -------------------------
    @Override
    @Transactional(readOnly = true)
    public ServiceResponse searchService(String tenantId,
                                         String serviceRequestId,
                                         String serviceCode,
                                         String applicationStatus,
                                         String mobileNumber,
                                         String locality) {
        // Fast path: search by exact serviceRequestId + tenant
        if (serviceRequestId != null && !serviceRequestId.isBlank()) {
            Optional<CitizenServiceEntity> maybe = citizenServiceRepository.findByServiceRequestIdAndTenantId(serviceRequestId, tenantId);
            List<com.example.pgrown30.web.models.CitizenService> dtos = maybe
                    .map(CitizenServiceMapper::toDto)
                    .map(List::of)
                    .orElseGet(List::of);
            return new ServiceResponse(dtos, Collections.emptyList());
        }

        // Build dynamic specification
        Specification<CitizenServiceEntity> spec = Specification.where(
                (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId)
        );

        if (serviceCode != null && !serviceCode.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("serviceCode"), serviceCode));
        }

        if (applicationStatus != null && !applicationStatus.isBlank()) {
            try {
                Status statusEnum = Status.valueOf(applicationStatus);
                spec = spec.and((root, query, cb) -> cb.equal(root.get("applicationStatus"), statusEnum));
            } catch (IllegalArgumentException e) {
                log.warn("searchService: unknown applicationStatus '{}'", applicationStatus);
                return new ServiceResponse(Collections.emptyList(), Collections.emptyList());
            }
        }

        if (mobileNumber != null && !mobileNumber.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("mobile"), mobileNumber));
        }

        if (locality != null && !locality.isBlank()) {
            spec = spec.and((root, query, cb) -> {
                root.join("addresses", JoinType.LEFT);
                query.distinct(true);

                String pattern = "%" + locality.trim().toLowerCase() + "%";
                javax.persistence.criteria.Join<CitizenServiceEntity, ?> addrJoin = root.join("addresses", JoinType.LEFT);

                javax.persistence.criteria.Predicate cityLike = cb.like(cb.lower(addrJoin.get("city")), pattern);
                javax.persistence.criteria.Predicate addressLike = cb.like(cb.lower(addrJoin.get("address")), pattern);
                javax.persistence.criteria.Predicate pincodeEq = cb.equal(addrJoin.get("pincode"), locality.trim());

                return cb.or(cityLike, addressLike, pincodeEq);
            });
        }

        List<CitizenServiceEntity> results = citizenServiceRepository.findAll(spec);

        List<com.example.pgrown30.web.models.CitizenService> dtos = results.stream()
                .map(CitizenServiceMapper::toDto)
                .collect(Collectors.toList());

        return new ServiceResponse(dtos, Collections.emptyList());
    }

    // -------------------------
    // Private helpers
    // -------------------------
    private void validateBoundaryIfPresent(com.example.pgrown30.web.models.CitizenService citizenService) {
        if (citizenService.getBoundaryCode() != null && !citizenService.getBoundaryCode().isBlank()) {
            boolean boundaryValid = boundaryService.isBoundaryValid(citizenService.getBoundaryCode());
            citizenService.setBoundaryValid(boundaryValid);
            if (!boundaryValid) {
                log.warn("Boundary {} is invalid for tenant {}", citizenService.getBoundaryCode(), citizenService.getTenantId());
            }
        }
    }

    private void validateFileIfPresent(com.example.pgrown30.web.models.CitizenService citizenService) {
        if (citizenService.getFileStoreId() != null && !citizenService.getFileStoreId().isBlank()) {
            boolean fileValid = fileStoreUtil.isFileValid(citizenService.getTenantId(), citizenService.getFileStoreId());
            citizenService.setFileValid(fileValid);
            if (!fileValid) {
                log.warn("FileStoreId {} is invalid or inaccessible for tenant {}", citizenService.getFileStoreId(), citizenService.getTenantId());
            }
        }
    }

    private void applyPartialUpdates(CitizenServiceEntity existing, com.example.pgrown30.web.models.CitizenService citizenService) {
        if (citizenService.getDescription() != null) existing.setDescription(citizenService.getDescription());
        if (citizenService.getServiceCode() != null) existing.setServiceCode(citizenService.getServiceCode());
        if (citizenService.getAccountId() != null) existing.setAccountId(citizenService.getAccountId());
        if (citizenService.getSource() != null) existing.setSource(citizenService.getSource());
        if (citizenService.getEmail() != null) existing.setEmail(citizenService.getEmail());
        if (citizenService.getMobile() != null) existing.setMobile(citizenService.getMobile());

        // file changed -> revalidate
        if (citizenService.getFileStoreId() != null && !citizenService.getFileStoreId().equals(existing.getFileStoreId())) {
            existing.setFileStoreId(citizenService.getFileStoreId());
            try {
                boolean fileValid = fileStoreUtil.isFileValid(existing.getTenantId(), existing.getFileStoreId());
                existing.setFileValid(fileValid);
                if (!fileValid) log.warn("File {} invalid for tenant {}", existing.getFileStoreId(), existing.getTenantId());
            } catch (Exception e) {
                log.error("File validation failed: {}", e.getMessage(), e);
            }
        }

        // boundary changed -> revalidate
        if (citizenService.getBoundaryCode() != null && !citizenService.getBoundaryCode().equals(existing.getBoundaryCode())) {
            existing.setBoundaryCode(citizenService.getBoundaryCode());
            try {
                boolean boundaryValid = boundaryService.isBoundaryValid(existing.getBoundaryCode());
                existing.setBoundaryValid(boundaryValid);
                if (!boundaryValid) log.warn("Boundary {} invalid for tenant {}", existing.getBoundaryCode(), existing.getTenantId());
            } catch (Exception e) {
                log.error("Boundary validation failed: {}", e.getMessage(), e);
            }
        }

        existing.setLastModifiedTime(Instant.now().toEpochMilli());
    }

    private void sendCreateNotificationsIfNeeded(CitizenServiceEntity saved) {
        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            List<String> emails = List.of(saved.getEmail());
            Map<String, Object> emailPayload = Map.of(
                    "applicationNo", saved.getServiceRequestId(),
                    "citizenName", saved.getAccountId() == null ? "" : saved.getAccountId(),
                    "serviceName", saved.getDescription(),
                    "statusLabel", saved.getApplicationStatus() == null ? Status.INITIATED.name() : saved.getApplicationStatus().toString(),
                    "trackUrl", "https://pgr.digit.org/track/" + saved.getServiceRequestId()
            );
            List<String> attachments = saved.getFileStoreId() != null ? List.of(saved.getFileStoreId()) : Collections.emptyList();
            notificationService.sendEmail("service-request-received", emails, emailPayload, attachments);
            log.info("Triggered email notification for {}", saved.getServiceRequestId());
        }
    }

    private void sendUpdateNotificationsIfNeeded(CitizenServiceEntity saved, String workflowAction) {
        if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
            List<String> emails = List.of(saved.getEmail());
            Map<String, Object> emailPayload = Map.of(
                    "applicationNo", saved.getServiceRequestId(),
                    "citizenName", saved.getAccountId() == null ? "" : saved.getAccountId(),
                    "serviceName", saved.getDescription(),
                    "statusLabel", saved.getApplicationStatus() == null ? Status.INITIATED.name() : saved.getApplicationStatus().toString(),
                    "action", workflowAction == null ? "" : workflowAction,
                    "trackUrl", "https://pgr.digit.org/track/" + saved.getServiceRequestId()
            );
            List<String> attachments = saved.getFileStoreId() != null ? List.of(saved.getFileStoreId()) : Collections.emptyList();
            notificationService.sendEmail("service-update", emails, emailPayload, attachments);
            log.info("Triggered update email for {}", saved.getServiceRequestId());
        }
    }
}
