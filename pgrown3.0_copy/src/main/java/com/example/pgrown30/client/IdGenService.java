package com.example.pgrown30.client;

import com.digit.services.idgen.IdGenClient;
import com.digit.services.idgen.model.IdGenGenerateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class IdGenService {

    private final IdGenClient idGenClient;

    @Value("${idgen.templateCode}")
    private String templateCode;

    public String generateId(String orgCode) {
        IdGenGenerateRequest request = IdGenGenerateRequest.builder()
                .templateCode(templateCode)
                .variables(Map.of("ORG", String.valueOf(orgCode)))
                .build();

        log.info("Requesting ID from IdGen with templateCode={} and orgCode={}", templateCode, orgCode);

        // Use digit-client library for ID generation
        // Headers are automatically propagated via HeaderPropagationInterceptor
        String id = idGenClient.generateId(request);

        return id;
    }
}