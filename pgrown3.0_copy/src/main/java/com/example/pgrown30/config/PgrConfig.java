package com.example.pgrown30.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PgrConfig {

    @Value("${workflow.host}")
    private String workflowHost;

    @Value("${pgr.workflow.processId}")
    private String workflowProcessId;

    public String getWorkflowHost() {
        return workflowHost;
    }

    // FIXED: Added proper getter method
    public String getProcessId() {
        return workflowProcessId;
    }
}
