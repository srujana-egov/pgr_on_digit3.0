package com.example.pgrown30.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Repository
public class FileStoreRepository {

    @Value("${filestore.host}")
    private String fileStoreHost;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isFileValid(String tenantId, String fileStoreId) {
    try {
        String url = String.format(
            "%s/filestore/v1/files/%s?tenantId=%s",
            fileStoreHost, fileStoreId, tenantId
        );

        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);

        // If 200 OK and body has some content → valid
        return response.getStatusCode().is2xxSuccessful()
                && response.getBody() != null
                && response.getBody().length > 0;

    } catch (Exception e) {
        log.error("File validation failed for fileStoreId={} tenantId={}", fileStoreId, tenantId, e);
        return false;
    }
}
}