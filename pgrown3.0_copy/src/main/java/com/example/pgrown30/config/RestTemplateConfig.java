package com.example.pgrown30.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean("pgrRestTemplate")
    public RestTemplate pgrRestTemplate() {
        return new RestTemplate();
    }
}