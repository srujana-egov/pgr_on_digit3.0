package com.example.pgrown30.config;

import com.digit.config.ApiConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Configuration class for Digit Client Library integration in PGR service.
 * Everything else is auto-configured by HeaderPropagationAutoConfiguration.
 */
@Configuration
@Import(ApiConfig.class)  // Import digit-client configuration (RestTemplate with interceptor)
public class DigitClientConfig {
}
