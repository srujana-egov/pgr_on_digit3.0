package com.example.pgrown30.client;

import com.digit.services.workflow.WorkflowClient;
import com.digit.services.workflow.model.WorkflowProcessResponse;
import com.digit.services.workflow.model.WorkflowTransitionRequest;
import com.digit.services.workflow.model.WorkflowTransitionResponse;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {
    private final WorkflowClient workflowClient;
    
    @Value("${pgr.workflow.processCode}")
    private String processCode;


    private String getProcessId() {
        System.out.println("Process id: " + workflowClient.getProcessByCode(processCode));
        return workflowClient.getProcessByCode(processCode);
    }

    public WorkflowTransitionResponse transition(
    String entityId, 
    String action, 
    String comment, 
    Map<String, Object> attributes
) {
    String processId = getProcessId();
    
    // Create a new Map with the correct type parameters
    Map<String, List<String>> attributesCopy = new HashMap<>();
    
    // Copy all attributes, ensuring values are List<String>
    if (attributes != null) {
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (entry.getValue() instanceof List) {
                // Safe to cast since we know it's a List
                @SuppressWarnings("unchecked")
                List<String> valueList = (List<String>) entry.getValue();
                attributesCopy.put(entry.getKey(), valueList);
            } else {
                // Convert single values to a singleton list
                attributesCopy.put(entry.getKey(), 
                    Collections.singletonList(String.valueOf(entry.getValue())));
            }
        }
    }
    
    WorkflowTransitionRequest request = WorkflowTransitionRequest.builder()
        .processId(getProcessId())
        .entityId(entityId)
        .action(action)
        .comment(comment)
        .attributes(attributesCopy)
        .build();

    log.debug("Initiating workflow transition: {}", request);
    WorkflowTransitionResponse response = workflowClient.executeTransition(request);
    log.info("Workflow transition completed for entityId={} processId={} action={}", 
            entityId, processId, action);
    return response;
}

}