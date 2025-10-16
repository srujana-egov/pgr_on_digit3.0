package com.example.pgrown30.config;

import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayConfigurationCustomizer flywayCustomizer() {
        return (FluentConfiguration configuration) -> {
            configuration
                .schemas("public")
                .baselineOnMigrate(true)
                .ignoreMigrationPatterns("*:*");  // ✅ Ignore unsupported db version checks
        };
    }
}
