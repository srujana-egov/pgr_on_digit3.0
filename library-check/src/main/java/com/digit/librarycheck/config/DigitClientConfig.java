package com.digit.librarycheck.config;

import com.digit.config.ApiConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Minimal configuration class for Digit Client Library integration.
 * Everything else is auto-configured by HeaderPropagationAutoConfiguration.
 */
@Configuration
@Import(ApiConfig.class)  // Import digit-client configuration (RestTemplate with interceptor)
public class DigitClientConfig {
    // That's it! Everything else auto-configured:
    // - ApiProperties (from application.properties)
    // - BoundaryClient (with RestTemplate + ApiProperties)
    // - AccountClient (with RestTemplate + ApiProperties)
    // - WorkflowClient (with RestTemplate + ApiProperties)
    // - HeaderPropagationInterceptor (automatic header propagation)
}