package com.example.pgrown30.repository;

import com.digit.services.idgen.IdGenClient;
import com.digit.services.idgen.model.IdGenGenerateRequest;
import com.digit.services.idgen.model.GenerateIDResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Repository
@RequiredArgsConstructor
public class IdGenRepository {

    private final IdGenClient idGenClient;

    @Value("${idgen.templateId}")
    private String templateId;

    public String generateId(String orgCode) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("ORG", orgCode); // e.g. "PGX"

            Map<String, String> stringVariables = new HashMap<>();
            variables.forEach((key, value) -> stringVariables.put(key, String.valueOf(value)));

            IdGenGenerateRequest request = IdGenGenerateRequest.builder()
                    .templateId(templateId)
                    .variables(stringVariables)
                    .build();

            log.info("Requesting ID from IdGen with templateId={} and orgCode={}", templateId, orgCode);

            // Use digit-client library for ID generation
            // Headers are automatically propagated via HeaderPropagationInterceptor
            GenerateIDResponse response = idGenClient.generateId(request);

            if (response == null || response.getId() == null) {
                throw new RuntimeException("IdGen did not return a valid ID");
            }

            log.info("Generated ID: {}", response.getId());
            return response.getId();
        } catch (Exception e) {
            log.error("Failed to generate ID with templateId={} and orgCode={}: {}", templateId, orgCode, e.getMessage(), e);
            throw e;
        }
    }
}
