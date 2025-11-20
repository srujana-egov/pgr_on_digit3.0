package com.example.pgrown30.validation;

import com.digit.services.boundary.BoundaryClient;
import com.digit.services.filestore.FilestoreClient;
// import com.digit.services.individual.IndividualClient;
import com.example.pgrown30.web.models.CitizenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validator for create service operations.
 * Encapsulates all validation logic for service creation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateServiceValidator {

    private final FilestoreClient filestoreClient;
    private final BoundaryClient boundaryClient;
    // private final IndividualClient individualClient;

    /**
     * Validates external resources (boundary, filestore) for service creation.
     * Throws RuntimeException if any validation fails.
     *
     * @param citizenService the citizen service to validate
     * @throws RuntimeException if any validation fails
     */
    public void validate(CitizenService citizenService) {
        // External validations (boundary, filestore) — evaluate all and if any fails, throw exception
        boolean fileValid = filestoreClient.isFileAvailable(citizenService.getFileStoreId(), citizenService.getTenantId());
        boolean boundaryValid = boundaryClient.isValidBoundariesByCodes(List.of(citizenService.getBoundaryCode()));
        // Individual validation commented out
        // boolean individualValid = individualClient.isIndividualExist(citizenService.getIndividualId());

        if (!fileValid || !boundaryValid) {
            log.error("External validation FAILED for serviceRequestId={}, tenant={} (boundaryValid={}, fileValid={}, boundaryCode={}, fileStoreId={})",
                    citizenService.getServiceRequestId(),
                    citizenService.getTenantId(),
                    boundaryValid, fileValid,
                    citizenService.getBoundaryCode(), citizenService.getFileStoreId());
            throw new RuntimeException("External validation failed: boundaryValid=" + boundaryValid + ", fileValid=" + fileValid);
        }
    }
}

