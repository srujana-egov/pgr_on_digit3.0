#!/usr/bin/env python3
"""
generate_security_config.py

Usage:
    python3 scripts/generate_security_config.py generated-pgr/src/main/java

Auto-detects the base package.
Creates:
    <base-package>/config/SecurityConfig.java
"""

import sys
from pathlib import Path

TEMPLATE = """package {{PACKAGE}}.config;

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

    private final Map<String, AuthenticationManager> managersByIssuer = new ConcurrentHashMap<>();

    private static final String KEYCLOAK_BASE = "https://digit-lts.digit.org/keycloak/realms/";

    private static String resolveToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null) return null;
        if (auth.startsWith("Bearer ")) return auth.substring(7);
        if (auth.startsWith("bearer ")) return auth.substring(7);
        return null;
    }

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

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeConv = new JwtGrantedAuthoritiesConverter();
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

    @Bean
    public AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver() {
        return request -> {
            String token = resolveToken(request);
            if (token == null) {
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
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer);
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(jwtAuthenticationConverter());
        return provider::authenticate;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationManagerResolver<HttpServletRequest> resolver
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers("/citizen-service/**").authenticated()
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(oauth -> oauth.authenticationManagerResolver(resolver));

        return http.build();
    }
}
"""

def detect_base_package(root: Path):
    for java in root.rglob("*.java"):
        text = java.read_text(errors="ignore")
        for line in text.splitlines():
            if line.strip().startswith("package "):
                pkg = line.strip()[8:].rstrip(";").strip()
                if ".web" in pkg:
                    return pkg.split(".web")[0]
                return pkg
    return None

def main():
    if len(sys.argv) < 2:
        print("Usage: python3 generate_security_config.py <src/main/java>")
        sys.exit(1)

    root = Path(sys.argv[1])
    base_pkg = detect_base_package(root)

    if not base_pkg:
        print("❌ Could not detect base package.")
        sys.exit(1)

    target_dir = root / Path(base_pkg.replace(".", "/")) / "config"
    target_dir.mkdir(parents=True, exist_ok=True)

    target_file = target_dir / "SecurityConfig.java"
    content = TEMPLATE.replace("{{PACKAGE}}", base_pkg)

    target_file.write_text(content)
    print("✅ Generated:", target_file)

if __name__ == "__main__":
    main()
