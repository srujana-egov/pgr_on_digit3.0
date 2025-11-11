package com.example.pgrown30.service.impl;

import com.digit.services.workflow.model.WorkflowTransitionResponse;
import com.example.pgrown30.client.BoundaryService;
import com.example.pgrown30.client.IdGenService;
import com.example.pgrown30.client.NotificationService;
import com.example.pgrown30.client.WorkflowService;
import com.example.pgrown30.repository.CitizenServiceRepository;
import com.example.pgrown30.service.ServiceService;
import com.example.pgrown30.util.FileStoreUtil;
import com.example.pgrown30.web.models.CitizenService;
import com.example.pgrown30.web.models.ServiceResponse;
import com.example.pgrown30.web.models.ServiceWrapper;
import com.example.pgrown30.web.models.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.data.jpa.domain.Specification.where;

@Slf4j
@Service
@Transactional(timeout = 30)
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
        if (wrapper == null || wrapper.getService() == null) {
            log.error("Invalid request: wrapper or service is null");
            return new ServiceResponse(Collections.emptyList(), Collections.emptyList());
        }

        CitizenService citizenService = wrapper.getService();
        log.debug("createService: incoming DTO = {}", citizenService);

        // generate id, timestamps, defaults
        String newId = idGenService.generateId("pgr");
        long now = Instant.now().toEpochMilli();

        citizenService.setServiceRequestId(newId);
        citizenService.setCreatedTime(now);
        citizenService.setLastModifiedTime(now);

        if (citizenService.getSource() == null || citizenService.getSource().isBlank()) {
            citizenService.setSource("Citizen");
        }

        // external validations
        try {
            validateBoundaryIfPresent(citizenService);
            validateFileIfPresent(citizenService);
        } catch (Exception e) {
            log.error("External validation failed (boundary/filestore): {}", e.getMessage(), e);
            // continuing by design
        }

        // start workflow
        String initialAction = "APPLY";
        Map<String, List<String>> attributes = Map.of("roles", roles == null ? List.of() : roles);

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

        // attach workflow info and set status
        if (wfResp != null) {
            citizenService.setWorkflowInstanceId(wfResp.getId());
            citizenService.setProcessId(workflowProcessId);
            try {
                citizenService.setApplicationStatus(Status.valueOf(wfResp.getCurrentState()));
            } catch (IllegalArgumentException e) {
                log.warn("Unknown workflow state '{}', defaulting to INITIATED", wfResp.getCurrentState());
                citizenService.setApplicationStatus(Status.INITIATED);
            }
        } else {
            citizenService.setApplicationStatus(Status.INITIATED);
        }

        // persist entity (CitizenService is now the @Entity)
        CitizenService saved = citizenServiceRepository.save(citizenService);
        log.debug("Saved citizen service with id={}", saved.getServiceRequestId());

        // prepare response DTO (entity == dto now)
        CitizenService responseDto = saved;
        if (wfResp != null) {
            responseDto.setWorkflowInstanceId(wfResp.getId());
            responseDto.setApplicationStatus(wfResp.getCurrentState() != null ? Status.valueOf(wfResp.getCurrentState()) : responseDto.getApplicationStatus());
            responseDto.setAction(wfResp.getAction());
        } else {
            responseDto.setApplicationStatus(Status.INITIATED);
        }

        ServiceWrapper responseWrapper = ServiceWrapper.builder()
                .service(responseDto)
                .workflow(wrapper.getWorkflow())
                .build();

        // notifications (async)
        try {
            sendCreateNotificationsIfNeeded(saved);
        } catch (Exception e) {
            log.error("Failed to trigger notifications for {}: {}", saved.getServiceRequestId(), e.getMessage(), e);
        }

        return new ServiceResponse(List.of(responseDto), List.of(responseWrapper));
    }

    // -------------------------
    // UPDATE
    // -------------------------
    @Override
    @Transactional
    public ServiceResponse updateService(ServiceWrapper wrapper, List<String> roles) {
        if (wrapper == null || wrapper.getService() == null) {
            log.error("Invalid update request: wrapper or service is null");
            return new ServiceResponse(Collections.emptyList(), Collections.emptyList());
        }

        var incoming = wrapper.getService();
        log.debug("updateService called for requestId={} tenant={}",
                incoming.getServiceRequestId(), incoming.getTenantId());

        String serviceRequestId = incoming.getServiceRequestId();
        String tenantId = incoming.getTenantId();

        if (serviceRequestId == null || serviceRequestId.isBlank()) {
            log.error("updateService: serviceRequestId is required");
            throw new RuntimeException("serviceRequestId is required");
        }

        var maybeExisting = citizenServiceRepository.findByServiceRequestIdAndTenantId(serviceRequestId, tenantId);
        if (maybeExisting.isEmpty()) {
            log.error("Service not found for id={} tenant={}", serviceRequestId, tenantId);
            throw new RuntimeException("Service not found: " + serviceRequestId);
        }

        CitizenService existing = maybeExisting.get();
        log.debug("Found existing entity id={} status={}", existing.getServiceRequestId(), existing.getApplicationStatus());

        // apply partial updates from incoming -> existing
        applyPartialUpdates(existing, incoming);

        // workflow transition if requested
        String workflowAction = (wrapper.getWorkflow() != null) ? wrapper.getWorkflow().getAction() : null;
        WorkflowTransitionResponse workflowResp = null;

        if (workflowAction != null && !workflowAction.isBlank()) {
            try {
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

        // update workflow info on entity
        if (workflowResp != null) {
            existing.setWorkflowInstanceId(workflowResp.getId());
            try {
                existing.setApplicationStatus(Status.valueOf(workflowResp.getCurrentState()));
            } catch (IllegalArgumentException ex) {
                log.warn("Unknown workflow state '{}'; leaving previous status", workflowResp.getCurrentState());
            }
        }

        // persist updated entity
        CitizenService saved = citizenServiceRepository.save(existing);
        log.debug("Updated citizen service saved: id={} status={}", saved.getServiceRequestId(), saved.getApplicationStatus());

        // prepare response DTO
        CitizenService responseDto = saved;
        if (workflowResp != null) {
            responseDto.setWorkflowInstanceId(workflowResp.getId());
            try {
                responseDto.setApplicationStatus(Status.valueOf(workflowResp.getCurrentState()));
            } catch (Exception ignore) {}
            responseDto.setAction(workflowResp.getAction());
        }

        ServiceWrapper responseWrapper = ServiceWrapper.builder()
                .service(responseDto)
                .workflow(wrapper.getWorkflow())
                .build();

        // notifications
        try {
            sendUpdateNotificationsIfNeeded(saved, workflowAction);
        } catch (Exception e) {
            log.error("Failed to trigger notifications for {}: {}", saved.getServiceRequestId(), e.getMessage(), e);
        }

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

        // Fast path: exact id + tenant
        if (serviceRequestId != null && !serviceRequestId.isBlank()) {
            Optional<CitizenService> maybe = citizenServiceRepository.findByServiceRequestIdAndTenantId(serviceRequestId, tenantId);
            List<CitizenService> dtos = maybe.map(List::of).orElseGet(List::of);
            return new ServiceResponse(dtos, Collections.emptyList());
        }

        Specification<CitizenService> spec = Specification.where(
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
                var addrJoin = root.join("addresses", JoinType.LEFT);

                Predicate cityLike = cb.like(cb.lower(addrJoin.get("city")), pattern);
                Predicate addressLike = cb.like(cb.lower(addrJoin.get("address")), pattern);
                Predicate pincodeEq = cb.equal(addrJoin.get("pincode"), locality.trim());

                return cb.or(cityLike, addressLike, pincodeEq);
            });
        }

        List<CitizenService> results = citizenServiceRepository.findAll(spec);

        List<CitizenService> dtos = results.stream().collect(Collectors.toList());
        return new ServiceResponse(dtos, Collections.emptyList());
    }

    // -------------------------
    // Search by ID
    // -------------------------
    @Override
    @Transactional(readOnly = true)
    public ServiceResponse searchServicesById(String serviceRequestId, String tenantId) {
        if (serviceRequestId == null || serviceRequestId.isBlank()) {
            log.warn("searchServicesById called with null/empty serviceRequestId");
            return new ServiceResponse(Collections.emptyList(), Collections.emptyList());
        }
        
        Optional<CitizenService> service = citizenServiceRepository.findByServiceRequestIdAndTenantId(serviceRequestId, tenantId);
        if (service.isEmpty()) {
            return new ServiceResponse(Collections.emptyList(), Collections.emptyList());
        }
        
        return new ServiceResponse(Collections.singletonList(service.get()), Collections.emptyList());
    }
    
    // -------------------------
    // Private helpers
    // -------------------------
    private void validateBoundaryIfPresent(CitizenService citizenService) {
        if (citizenService.getBoundaryCode() != null && !citizenService.getBoundaryCode().isBlank()) {
            boolean boundaryValid = boundaryService.isBoundaryValid(citizenService.getBoundaryCode());
            citizenService.setBoundaryValid(boundaryValid);
            if (!boundaryValid) {
                log.warn("Boundary {} is invalid for tenant {}", citizenService.getBoundaryCode(), citizenService.getTenantId());
            }
        }
    }

    private void validateFileIfPresent(CitizenService citizenService) {
        if (citizenService.getFileStoreId() != null && !citizenService.getFileStoreId().isBlank()) {
            boolean fileValid = fileStoreUtil.isFileValid(citizenService.getTenantId(), citizenService.getFileStoreId());
            citizenService.setFileValid(fileValid);
            if (!fileValid) {
                log.warn("FileStoreId {} is invalid or inaccessible for tenant {}", citizenService.getFileStoreId(), citizenService.getTenantId());
            }
        }
    }

    private void applyPartialUpdates(CitizenService existing, CitizenService incoming) {
        if (incoming.getDescription() != null) existing.setDescription(incoming.getDescription());
        if (incoming.getServiceCode() != null) existing.setServiceCode(incoming.getServiceCode());
        if (incoming.getAccountId() != null) existing.setAccountId(incoming.getAccountId());
        if (incoming.getSource() != null) existing.setSource(incoming.getSource());
        if (incoming.getEmail() != null) existing.setEmail(incoming.getEmail());
        if (incoming.getMobile() != null) existing.setMobile(incoming.getMobile());

        // file changed -> revalidate
        if (incoming.getFileStoreId() != null && !incoming.getFileStoreId().equals(existing.getFileStoreId())) {
            existing.setFileStoreId(incoming.getFileStoreId());
            try {
                boolean fileValid = fileStoreUtil.isFileValid(existing.getTenantId(), existing.getFileStoreId());
                existing.setFileValid(fileValid);
                if (!fileValid) log.warn("File {} invalid for tenant {}", existing.getFileStoreId(), existing.getTenantId());
            } catch (Exception e) {
                log.error("File validation failed: {}", e.getMessage(), e);
            }
        }

        // boundary changed -> revalidate
        if (incoming.getBoundaryCode() != null && !incoming.getBoundaryCode().equals(existing.getBoundaryCode())) {
            existing.setBoundaryCode(incoming.getBoundaryCode());
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

    private void sendCreateNotificationsIfNeeded(CitizenService saved) {
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

    private void sendUpdateNotificationsIfNeeded(CitizenService saved, String workflowAction) {
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
