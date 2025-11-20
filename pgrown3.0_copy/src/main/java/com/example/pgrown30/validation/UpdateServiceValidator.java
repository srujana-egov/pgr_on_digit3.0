package com.example.pgrown30.validation;

import com.digit.services.boundary.BoundaryClient;
import com.digit.services.filestore.FilestoreClient;
// import com.digit.services.individual.IndividualClient;
import com.example.pgrown30.repository.CitizenServiceRepository;
import com.example.pgrown30.web.models.CitizenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validator for update service operations.
 * Encapsulates all validation logic for service updates.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateServiceValidator {

    private final CitizenServiceRepository citizenServiceRepository;
    private final FilestoreClient filestoreClient;
    private final BoundaryClient boundaryClient;
    // private final IndividualClient individualClient;

    /**
     * Validates that the service request ID is present.
     *
     * @param serviceRequestId the service request ID to validate
     * @throws RuntimeException if service request ID is null or blank
     */
    public void validateServiceRequestId(String serviceRequestId) {
        if (serviceRequestId == null || serviceRequestId.isBlank()) {
            throw new RuntimeException("serviceRequestId is required");
        }
    }

    /**
     * Validates that the service exists for the given ID and tenant.
     *
     * @param serviceRequestId the service request ID
     * @param tenantId         the tenant ID
     * @return the existing citizen service
     * @throws RuntimeException if service not found
     */
    public CitizenService validateServiceExists(String serviceRequestId, String tenantId) {
        return citizenServiceRepository.findByServiceRequestIdAndTenantId(serviceRequestId, tenantId)
                .orElseThrow(() -> {
                    log.error("Service not found for id={} tenant={}", serviceRequestId, tenantId);
                    return new RuntimeException("Service not found: " + serviceRequestId);
                });
    }

    /**
     * Validates file store when it changes during update.
     *
     * @param service    the citizen service
     * @param fileStoreId the new file store ID
     */
    public void validateFileStore(CitizenService service, String fileStoreId) {
        service.setFileStoreId(fileStoreId);
        try {
            boolean fileValid = filestoreClient.isFileAvailable(fileStoreId, service.getTenantId());
            if (!fileValid) {
                log.warn("File {} invalid for tenant {}", fileStoreId, service.getTenantId());
            }
        } catch (Exception e) {
            log.error("File validation failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Validates boundary when it changes during update.
     *
     * @param service      the citizen service
     * @param boundaryCode the new boundary code
     */
    public void validateBoundary(CitizenService service, String boundaryCode) {
        service.setBoundaryCode(boundaryCode);
        try {
            boolean boundaryValid = boundaryClient.isValidBoundariesByCodes(List.of(boundaryCode));
            if (!boundaryValid) {
                log.warn("Boundary {} invalid for tenant {}", boundaryCode, service.getTenantId());
            }
        } catch (Exception e) {
            log.error("Boundary validation failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Validates and stores individual ID when it changes during update.
     * COMMENTED OUT - Individual client validation disabled
     *
     * @param service      the citizen service
     * @param individualId the new individual ID
     */
    /*
    public void validateIndividual(CitizenService service, String individualId) {
        service.setIndividualId(individualId);
        try {
            boolean individualValid = individualClient.isIndividualExist(individualId);
            log.info("Individual validation for individualId={}: {}", individualId, individualValid ? "VALID" : "INVALID");
            if (!individualValid) {
                log.warn("IndividualId {} invalid or does not exist", individualId);
            }
        } catch (Exception e) {
            log.error("Individual validation failed: {}", e.getMessage(), e);
        }
    }
    */
}

