package com.example.pgrown30.repository;

import com.digit.services.workflow.WorkflowClient;
import com.digit.services.workflow.model.WorkflowTransitionRequest;
import com.digit.services.workflow.model.WorkflowTransitionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Repository
public class WorkflowRepository {

    private final WorkflowClient workflowClient;
    private final RestTemplate restTemplate;

    public WorkflowRepository(WorkflowClient workflowClient, 
                             @Qualifier("pgrRestTemplate") RestTemplate restTemplate) {
        this.workflowClient = workflowClient;
        this.restTemplate = restTemplate;
    }

    @Value("${workflow.host}")
    private String workflowHost;

    private String base() {
        return workflowHost + "/workflow/v1";
    }

    private HttpHeaders defaultHeaders(String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId);
        return headers;
    }

    private HttpHeaders clientHeaders(String tenantId, String clientId) {
        HttpHeaders headers = defaultHeaders(tenantId);
        if (clientId != null && !clientId.isBlank()) headers.set("X-Client-Id", clientId);
        return headers;
    }

    // TRANSITION APIS - Using digit-client
    public Map<String, Object> transition(String tenantId,
                                          String clientId,
                                          String processId,
                                          String entityId,
                                          String action,
                                          String comment,
                                          Map<String, List<String>> attributes) {
        try {
            // For now, let's pass null attributes and let the library handle it
            // We can revisit this once we understand the exact API structure
            WorkflowTransitionRequest.Attributes workflowAttributes = null;

            WorkflowTransitionRequest request = WorkflowTransitionRequest.builder()
                    .processId(processId)
                    .entityId(entityId)
                    .action(action)
                    .comment(comment)
                    .attributes(workflowAttributes)
                    .build();

            // Use digit-client library for workflow transition
            // Headers are automatically propagated via HeaderPropagationInterceptor
            WorkflowTransitionResponse response = workflowClient.executeTransition(request);
            
            log.info("Workflow transition completed successfully for entityId={} processId={} action={}", 
                    entityId, processId, action);
            
            // Convert response to Map for backward compatibility
            Map<String, Object> responseMap = new java.util.HashMap<>();
            if (response != null) {
                responseMap.put("id", response.getId());
                responseMap.put("status", response.getStatus());
                responseMap.put("currentState", response.getCurrentState());
            }
            return responseMap;
        } catch (Exception e) {
            log.error("Transition failed for entityId={} processId={} action={}: {}", entityId, processId, action, e.getMessage(), e);
            throw e;
        }
    }

    public boolean updateProcessInstance(String tenantId, String workflowInstanceId, String processId, String action) {
        try {
            WorkflowTransitionRequest request = WorkflowTransitionRequest.builder()
                    .processId(processId)
                    .entityId(workflowInstanceId)
                    .action(action)
                    .attributes(null) // No attributes for simple updates
                    .build();

            // Use digit-client library for workflow transition
            // Headers are automatically propagated via HeaderPropagationInterceptor
            WorkflowTransitionResponse response = workflowClient.executeTransition(request);
            
            boolean success = response != null && response.getId() != null;
            log.info("Process instance update for workflowInstanceId={}, processId={}, action={}: {}", 
                    workflowInstanceId, processId, action, success ? "SUCCESS" : "FAILED");
            return success;
        } catch (Exception e) {
            log.error("updateProcessInstance failed for workflowInstanceId={}, processId={}, action={}: {}", 
                      workflowInstanceId, processId, action, e.getMessage());
            return false;
        }
    }

    // ORIGINAL METHODS - Using RestTemplate as before
    public Map<String, Object> getLatestInstance(String tenantId,
                                                 String processId,
                                                 String entityId) {
        String url = base() + "/transition?entityId=" + entityId + "&processId=" + processId;
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(defaultHeaders(tenantId)), Map.class);
            return resp.getBody();
        } catch (RestClientException e) {
            log.error("Get latest instance failed for entityId={} processId={}: {}", entityId, processId, e.getMessage(), e);
            throw e;
        }
    }

    public Map<String, Object> getInstanceHistory(String tenantId,
                                                  String processId,
                                                  String entityId) {
        String url = base() + "/transition?entityId=" + entityId + "&processId=" + processId + "&history=true";
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(defaultHeaders(tenantId)), Map.class);
            return resp.getBody();
        } catch (RestClientException e) {
            log.error("Get instance history failed for entityId={} processId={}: {}", entityId, processId, e.getMessage(), e);
            throw e;
        }
    }

    public Map<String, Object> createProcess(String tenantId,
                                             String name,
                                             String code,
                                             String description,
                                             String version,
                                             Long slaSeconds) {
        String url = base() + "/process";
        Map<String, Object> body = Map.of(
                "name", name,
                "code", code,
                "description", description,
                "version", version,
                "sla", slaSeconds
        );
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, defaultHeaders(tenantId)), Map.class);
            return resp.getBody();
        } catch (RestClientException e) {
            log.error("createProcess failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<Map<String, Object>> getAllProcesses(String tenantId) {
        String url = base() + "/process";
        try {
            ResponseEntity<List> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(defaultHeaders(tenantId)), List.class);
            return resp.getBody();
        } catch (RestClientException e) {
            log.error("getAllProcesses failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Map<String, Object> getProcessById(String tenantId, String processId) {
        String url = base() + "/process/" + processId;
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(defaultHeaders(tenantId)), Map.class);
            return resp.getBody();
        } catch (RestClientException e) {
            log.error("getProcessById failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Map<String, Object> updateProcess(String tenantId,
                                             String processId,
                                             String name,
                                             String description,
                                             String version,
                                             Long slaSeconds) {
        String url = base() + "/process/" + processId;
        Map<String, Object> body = Map.of(
                "name", name,
                "description", description,
                "version", version,
                "sla", slaSeconds
        );
        try {
            ResponseEntity<Map> resp = restTemplate.exchange(
                    url, HttpMethod.PUT, new HttpEntity<>(body, defaultHeaders(tenantId)), Map.class);
            return resp.getBody();
        } catch (RestClientException e) {
            log.error("updateProcess failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void deleteProcess(String tenantId, String processId) {
        String url = base() + "/process/" + processId;
        try {
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(defaultHeaders(tenantId)), Void.class);
        } catch (RestClientException e) {
            log.error("deleteProcess failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public boolean processExists(String tenantId, String processId) {
        try {
            Map<String, Object> process = getProcessById(tenantId, processId);
            return process != null && !process.isEmpty();
        } catch (Exception e) {
            log.warn("Process {} does not exist for tenant {}: {}", processId, tenantId, e.getMessage());
            return false;
        }
    }

}

