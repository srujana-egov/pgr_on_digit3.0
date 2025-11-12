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