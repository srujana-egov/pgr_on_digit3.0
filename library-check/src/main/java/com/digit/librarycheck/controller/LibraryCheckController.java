package com.digit.librarycheck.controller;

import com.digit.librarycheck.model.BoundarySearchRequest;
import com.digit.services.account.AccountClient;
import com.digit.services.account.model.Tenant;
import com.digit.services.boundary.BoundaryClient;
import com.digit.services.boundary.model.Boundary;
import com.digit.services.workflow.WorkflowClient;
import com.digit.services.workflow.model.WorkflowTransitionRequest;
import com.digit.services.workflow.model.WorkflowTransitionResponse;
import com.digit.services.idgen.IdGenClient;
import com.digit.services.idgen.model.IdGenGenerateRequest;
import com.digit.services.idgen.model.GenerateIDResponse;
import com.digit.services.notification.NotificationClient;
import com.digit.services.notification.model.SendEmailRequest;
import com.digit.services.notification.model.SendEmailResponse;
import com.digit.services.notification.model.SendSMSRequest;
import com.digit.services.notification.model.SendSMSResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/library-check")
@RequiredArgsConstructor
@Slf4j
public class LibraryCheckController {

    private final BoundaryClient boundaryClient;
    private final AccountClient accountClient;
    private final WorkflowClient workflowClient;
    private final IdGenClient idGenClient;
    private final NotificationClient notificationClient;

    /**
     * Test boundary client with automatic header propagation
     */
    @PostMapping("/boundary")
    public ResponseEntity<?> checkBoundary(
            @RequestBody BoundarySearchRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("=== Testing Boundary Client with Header Propagation ===");
        log.info("Received boundary check request with {} codes", request.getCodes().size());
        
        // Log incoming headers to verify they're present
        logIncomingHeaders(httpRequest);
        
        try {
            // Call boundary search using the digit-client library
            // Headers are automatically propagated via HeaderPropagationInterceptor
            List<Boundary> boundaryResponse = boundaryClient.searchBoundariesByCodes(request.getCodes());
            
            log.info("✅ Boundary search completed successfully!");
            log.info("Response size: {} boundaries found", 
                    boundaryResponse != null ? boundaryResponse.size() : 0);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Header propagation working correctly");
            response.put("boundariesFound", boundaryResponse != null ? boundaryResponse.size() : 0);
            response.put("boundaries", boundaryResponse);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error calling boundary service: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error calling boundary service: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Test account client with automatic header propagation
     */
    @GetMapping("/tenant/{code}")
    public ResponseEntity<?> checkTenant(
            @PathVariable String code,
            HttpServletRequest httpRequest) {
        
        log.info("=== Testing Account Client with Header Propagation ===");
        log.info("Received tenant search request for code: {}", code);
        
        // Log incoming headers to verify they're present
        logIncomingHeaders(httpRequest);
        
        try {
            // Call tenant search using the digit-client library
            // Headers are automatically propagated via HeaderPropagationInterceptor
            Tenant tenant = accountClient.searchTenantByCode(code);
            
            log.info("✅ Tenant search completed successfully!");
            log.info("Tenant found: {}", tenant != null ? tenant.getName() : "null");
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Header propagation working correctly");
            response.put("tenantFound", tenant != null);
            response.put("tenant", tenant);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error calling account service: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error calling account service: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Test creating a tenant with automatic header propagation
     */
    @PostMapping("/tenant")
    public ResponseEntity<?> createTenant(
            @RequestBody Tenant tenant,
            HttpServletRequest httpRequest) {
        
        log.info("=== Testing Tenant Creation with Header Propagation ===");
        log.info("Received tenant creation request for: {}", tenant.getName());
        
        // Log incoming headers to verify they're present
        logIncomingHeaders(httpRequest);
        
        try {
            // Call tenant creation using the digit-client library
            // Headers are automatically propagated via HeaderPropagationInterceptor
            Tenant createdTenant = accountClient.createTenant(tenant);
            
            log.info("✅ Tenant creation completed successfully!");
            log.info("Created tenant: {}", createdTenant != null ? createdTenant.getName() : "null");
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Header propagation working correctly");
            response.put("tenantCreated", createdTenant != null);
            response.put("tenant", createdTenant);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Error creating tenant: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Error creating tenant: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Health check endpoint to verify library integration
     */
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        log.info("=== Library Health Check ===");
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "healthy");
        response.put("message", "Digit client library is properly integrated");
        response.put("features", List.of(
            "Automatic header propagation",
            "No authToken parameters required",
            "Thread-safe request context handling"
        ));
        
        return ResponseEntity.ok(response);
    }

    private void logIncomingHeaders(HttpServletRequest request) {
        log.info("📋 Incoming Headers:");
        String authorization = request.getHeader("Authorization");
        String tenantId = request.getHeader("X-Tenant-ID");
        String clientId = request.getHeader("X-Client-Id");
        String correlationId = request.getHeader("X-Correlation-ID");
        String requestId = request.getHeader("X-Request-ID");
        
        log.info("  Authorization: {}", authorization != null ? 
            authorization.substring(0, Math.min(20, authorization.length())) + "..." : "null");
        log.info("  X-Tenant-ID: {}", tenantId);
        log.info("  X-Client-Id: {}", clientId);
        log.info("  X-Correlation-ID: {}", correlationId);
        log.info("  X-Request-ID: {}", requestId);
        
        if (authorization == null && tenantId == null && clientId == null) {
            log.warn("⚠️  No authentication headers found! Header propagation may not work correctly.");
        } else {
            log.info("✅ Headers detected - automatic propagation should work");
        }
    }

    /**
     * Test workflow client with automatic header propagation
     */
    @PostMapping("/workflow/transition")
    public ResponseEntity<?> testWorkflowTransition(
            @RequestBody WorkflowTransitionRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("=== Testing Workflow Client with Header Propagation ===");
        log.info("Received workflow transition request - processId: {}, entityId: {}, action: {}", 
                request.getProcessId(), request.getEntityId(), request.getAction());
        
        // Log incoming headers to verify they're present
        logIncomingHeaders(httpRequest);
        
        try {
            // Call workflow transition using the digit-client library
            // Headers are automatically propagated via HeaderPropagationInterceptor
            WorkflowTransitionResponse workflowResponse = workflowClient.executeTransition(request);
            
            log.info("✅ Workflow transition completed successfully!");
            log.info("Response ID: {}, Status: {}, Current State: {}", 
                    workflowResponse != null ? workflowResponse.getId() : "null",
                    workflowResponse != null ? workflowResponse.getStatus() : "null",
                    workflowResponse != null ? workflowResponse.getCurrentState() : "null");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Workflow transition executed successfully via digit-client library");
            response.put("workflowResponse", workflowResponse);
            response.put("headerPropagationWorking", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Workflow transition failed", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Workflow transition failed: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("headerPropagationWorking", false);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Test IdGen client with automatic header propagation
     */
    @PostMapping("/idgen/generate")
    public ResponseEntity<?> testIdGenerate(
            @RequestBody IdGenGenerateRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("=== Testing IdGen Client with Header Propagation ===");
        log.info("Received ID generation request - templateId: {}, variables: {}", 
                request.getTemplateId(), request.getVariables());
        
        // Log incoming headers to verify they're present
        logIncomingHeaders(httpRequest);
        
        try {
            // Call ID generation using the digit-client library
            // Headers are automatically propagated via HeaderPropagationInterceptor
            GenerateIDResponse idResponse = idGenClient.generateId(request);
            
            log.info("✅ ID generation completed successfully!");
            log.info("Generated ID: {}", idResponse != null ? idResponse.getId() : "null");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "ID generated successfully via digit-client library");
            response.put("idResponse", idResponse);
            response.put("headerPropagationWorking", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ ID generation failed", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "ID generation failed: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("headerPropagationWorking", false);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Test Notification client - Send Email with automatic header propagation
     */
    @PostMapping("/notification/email/send")
    public ResponseEntity<?> testSendEmail(
            @RequestBody SendEmailRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("=== Testing Notification Client - Send Email with Header Propagation ===");
        log.info("Received email request - templateId: {}, recipients: {}", 
                request.getTemplateId(), request.getEmailIds().size());
        
        // Log incoming headers to verify they're present
        logIncomingHeaders(httpRequest);
        
        try {
            // Call email sending using the digit-client library
            // Headers are automatically propagated via HeaderPropagationInterceptor
            SendEmailResponse emailResponse = notificationClient.sendEmail(request);
            
            log.info("✅ Email sent successfully!");
            log.info("Email status: {}", emailResponse != null ? emailResponse.getStatus() : "null");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Email sent successfully via digit-client library");
            response.put("emailResponse", emailResponse);
            response.put("headerPropagationWorking", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ Email sending failed", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Email sending failed: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("headerPropagationWorking", false);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * Test Notification client - Send SMS with automatic header propagation
     */
    @PostMapping("/notification/sms/send")
    public ResponseEntity<?> testSendSMS(
            @RequestBody SendSMSRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("=== Testing Notification Client - Send SMS with Header Propagation ===");
        log.info("Received SMS request - templateId: {}, recipients: {}", 
                request.getTemplateId(), request.getMobileNumbers().size());
        
        // Log incoming headers to verify they're present
        logIncomingHeaders(httpRequest);
        
        try {
            // Call SMS sending using the digit-client library
            // Headers are automatically propagated via HeaderPropagationInterceptor
            SendSMSResponse smsResponse = notificationClient.sendSMS(request);
            
            log.info("✅ SMS sent successfully!");
            log.info("SMS status: {}", smsResponse != null ? smsResponse.getStatus() : "null");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "SMS sent successfully via digit-client library");
            response.put("smsResponse", smsResponse);
            response.put("headerPropagationWorking", true);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ SMS sending failed", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "SMS sending failed: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());
            errorResponse.put("headerPropagationWorking", false);
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
