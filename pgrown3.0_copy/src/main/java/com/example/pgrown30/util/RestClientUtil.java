package com.example.pgrown30.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class RestClientUtil {

    private final ObjectMapper mapper;
    private final RestTemplate restTemplate;

    public RestClientUtil(ObjectMapper mapper, RestTemplate restTemplate) {
        this.mapper = mapper;
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> fetchResult(String uri, Object request) {
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                uri,
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("External Service threw an Exception: {}", e.getResponseBodyAsString(), e);
            throw new RuntimeException("External service error: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Exception while fetching from external service: {}", uri, e);
            throw new RuntimeException("Error calling external service", e);
        }
    }
}
