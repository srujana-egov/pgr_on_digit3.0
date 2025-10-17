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

import java.util.HashMap;
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

    @Value("${pgr.workflow.processId}")
    private String processId;

    private String base() {
        return workflowHost + "/workflow/v1";
    }

    private HttpHeaders defaultHeaders(String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Tenant-ID", tenantId);
        return headers;
    }



    // TRANSITION APIS - Using digit-client
    public WorkflowTransitionResponse transition(
                                          String entityId,
                                          String action,
                                          String comment,
                                          Map<String, List<String>> attributes) {
            // For now, let's pass null attributes and let the library handle it
            // We can revisit this once we understand the exact API structure

            WorkflowTransitionRequest request = WorkflowTransitionRequest.builder()
                    .processId(processId)
                    .entityId(entityId)
                    .action(action)
                    .comment(comment)
                    .attributes(attributes)
                    .build();

            // Use digit-client library for workflow transition
            // Headers are automatically propagated via HeaderPropagationInterceptor
            WorkflowTransitionResponse response = workflowClient.executeTransition(request);
            
            log.info("Workflow transition completed successfully for entityId={} processId={} action={}", 
                    entityId, processId, action);
            
          return response;
    }

    public WorkflowTransitionResponse updateProcessInstance( String entityId, String processId, String action, List<String> roles) {
            Map<String, List<String>> attributes = new HashMap<>();
            attributes.put("roles", roles);
            WorkflowTransitionRequest request = WorkflowTransitionRequest.builder()
                    .processId(processId)
                    .entityId(entityId)
                    .action(action)
                    .attributes(attributes) // No attributes for simple updates
                    .build();

            // Use digit-client library for workflow transition
            // Headers are automatically propagated via HeaderPropagationInterceptor
            WorkflowTransitionResponse response = workflowClient.executeTransition(request);
            
            return response;
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

