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
            IdGenGenerateRequest request = IdGenGenerateRequest.builder()
                    .templateId(templateId)
                    .variables(Map.of("ORG", String.valueOf(orgCode)))
                    .build();


            log.info("Requesting ID from IdGen with templateId={} and orgCode={}", templateId, orgCode);

            // Use digit-client library for ID generation
            // Headers are automatically propagated via HeaderPropagationInterceptor
            String id =  idGenClient.generateId(request);

            return id;
    }
}
