# Step 5: Creating Config Layer

The config layer is essential for setting up and customizing the application's behavior. Here we have 2 config files:
1. DigitClientConfig.java
2. SecurityConfig.java

## **Steps**

1. Create a config folder.
2. Create DigitClientConfig.java under config folder.

This is the configuration class for Digit Client Library integration in PGR service. Everything else is auto-configured by HeaderPropagationAutoConfiguration.

The contents of DigitClientConfig.java are as follows:

```java
package com.example.pgrown30.config;

import com.digit.config.ApiConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(ApiConfig.class)  
public class DigitClientConfig {
    // That's it! Everything else auto-configured.
}
```

3. Create SecurityConfig.java under config folder.

The SecurityConfig class in the application is responsible for setting up security rules and authentication. It is used for:
1. JWT Authentication
2. Endpoint Protection
3. OAuth2 Resource Server
4. CSRF Protection

The contents of SecurityConfig.java are as follows:

```java
package com.example.pgrown30.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Configuration
public class SecurityConfig {

    /** Map of issuer -> AuthenticationManager (each with its own JwtDecoder) */
    private final Map<String, AuthenticationManager> managersByIssuer = new ConcurrentHashMap<>();

    /** OPTIONAL: if you prefer mapping a tenant header to an issuer, set base URL here */
    private static final String KEYCLOAK_BASE = "https://digit-lts.digit.org/keycloak/realms/";

    /** Extract JWT from Authorization header */
    private static String resolveToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null) return null;
        if (auth.startsWith("Bearer ")) return auth.substring(7);
        if (auth.startsWith("bearer ")) return auth.substring(7);
        return null;
    }

    /** Decode JWT payload (no signature check) to read 'iss' */
    private static Map<String, Object> unsafeDecodePayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return Map.of();
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return new ObjectMapper().readValue(payloadJson, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** realm_access.roles -> ROLE_* authorities */
    private Converter<Jwt, Collection<? extends GrantedAuthority>> realmRolesConverter() {
        return jwt -> {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null) return List.of();
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.getOrDefault("roles", List.of());
            return roles.stream()
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
        };
    }

    /** Combine scope authorities with realm roles */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeConv = new JwtGrantedAuthoritiesConverter(); // SCOPE_*
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<? extends GrantedAuthority> scopeAuths = scopeConv.convert(jwt);
            Collection<? extends GrantedAuthority> realmAuths = realmRolesConverter().convert(jwt);
            return Stream.concat(
                    scopeAuths == null ? Stream.empty() : scopeAuths.stream(),
                    realmAuths == null ? Stream.empty() : realmAuths.stream()
            ).toList();
        });
        return converter;
    }

    /**
     * Dynamic resolver:
     *  - Option 1 (default): read 'iss' from token and use that issuer.
     *  - Option 2 (uncomment): derive issuer from X-Tenant-ID header instead.
     */
    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver() {
        return request -> {
            // ----- Option 2: If you prefer tenant header -> issuer mapping, use this:
            // String tenant = request.getHeader("X-Tenant-ID");
            // if (tenant != null && !tenant.isBlank()) {
            //     String issuer = KEYCLOAK_BASE + tenant;
            //     return managersByIssuer.computeIfAbsent(issuer, this::buildAuthManagerForIssuer);
            // }

            // ----- Option 1: resolve issuer from token 'iss' claim
            String token = resolveToken(request);
            if (token == null) {
                // No token; let Spring handle 401
                return authentication -> { throw new org.springframework.security.core.AuthenticationException("Missing token") {}; };
            }
            Map<String, Object> payload = unsafeDecodePayload(token);
            String issuer = (String) payload.get("iss");
            if (issuer == null || issuer.isBlank()) {
                return authentication -> { throw new org.springframework.security.core.AuthenticationException("Missing iss") {}; };
            }
            return managersByIssuer.computeIfAbsent(issuer, this::buildAuthManagerForIssuer);
        };
    }

    private AuthenticationManager buildAuthManagerForIssuer(String issuer) {
        // Creates a decoder that:
        //  - fetches JWKs from {issuer}/.well-known/openid-configuration -> jwks_uri
        //  - validates signature, exp/nbf, and 'iss' == issuer
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);

        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(jwtAuthenticationConverter());

        return provider::authenticate;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   AuthenticationManagerResolver<HttpServletRequest> resolver) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers("/citizen-service/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .authenticationManagerResolver(resolver)
                );
        return http.build();
    }
}
```
