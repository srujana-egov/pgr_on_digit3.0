# PGR Service Developer Guide (Based on pgrown3.0_copy)

## Table of Contents
1. [Project Setup](#1-project-setup)
2. [Core Implementation](#2-core-implementation)
3. [Digit Client Integration](#3-digit-client-integration)
4. [Workflow Management](#4-workflow-management)
5. [Security Configuration](#5-security-configuration)
6. [Service Implementation](#6-service-implementation)
7. [Testing](#7-testing)
8. [Deployment](#8-deployment)

## 1. Project Setup

### 1.1 Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 13+
- Docker & Docker Compose
- Keycloak (for authentication)
- DIGIT Infrastructure Services (Workflow, FileStore, etc.)

### 1.2 Project Structure
```
src/main/java/com/example/pgr/
├── config/              # Configuration classes
├── domain/              # JPA entities
├── dto/                 # Data Transfer Objects
├── mapper/              # DTO-Entity mappers
├── repository/          # JPA repositories
├── service/             # Service interfaces
│   └── impl/            # Service implementations
└── web/                 # Web layer
    ├── controllers/     # REST controllers
    └── models/          # Request/Response models
```

## 2. Core Implementation

### 2.1 Domain Model
```java
// src/main/java/com/example/pgr/domain/CitizenServiceEntity.java
@Entity
@Table(name = "citizen_service")
@Getter
@Setter
public class CitizenServiceEntity {
    @Id
    private String serviceRequestId;
    
    private String tenantId;
    private String description;
    private String source = "Citizen";
    private String applicationStatus;
    private String workflowInstanceId;
    private String processId;
    private String action;
    private String boundaryCode;
    private Long createdTime;
    private Long lastModifiedTime;
    private String email;
}
```

## 3. Digit Client Integration

### 3.1 Configuration
```java
@Configuration
@ConfigurationProperties(prefix = "digit.services")
@Getter
@Setter
public class DigitServiceConfig {
    private String workflowHost;
    private String filestoreHost;
    private String notificationHost;
    private String boundaryHost;
    private String idgenHost;
}
```

## 4. Workflow Management

### 4.1 Workflow Integration
```java
@Service
@RequiredArgsConstructor
public class WorkflowRepository {
    private final WebClient webClient;
    private final DigitServiceConfig config;
    
    public WorkflowTransitionResponse startWorkflow(CitizenServiceEntity service, 
                                                  String businessService,
                                                  String action,
                                                  List<String> roles) {
        // Implementation for workflow initiation
    }
}
```

## 5. Security Configuration

### 5.1 Keycloak Configuration
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Value("${keycloak.jwk-set-uri}")
    private String jwkSetUri;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
           .authorizeHttpRequests(auth -> auth
               .requestMatchers("/service/v1/_create", "/service/v1/_search").permitAll()
               .anyRequest().authenticated()
           )
           .oauth2ResourceServer(oauth2 -> oauth2
               .jwt(jwt -> jwt.decoder(jwtDecoder()))
           );
        return http.build();
    }
    
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}
```

## 6. Service Implementation

### 6.1 Service Implementation
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceServiceImpl implements ServiceService {
    
    private final CitizenServiceRepository citizenServiceRepository;
    private final WorkflowRepository workflowRepository;
    
    @Override
    @Transactional
    public ServiceResponse createService(ServiceWrapper wrapper, List<String> roles) {
        CitizenService dto = wrapper.getService();
        CitizenServiceEntity service = CitizenServiceMapper.toEntity(dto);
        
        // Generate service request ID and set timestamps
        String newId = "PGR" + System.currentTimeMillis();
        long now = Instant.now().toEpochMilli();
        
        service.setServiceRequestId(newId);
        service.setCreatedTime(now);
        service.setLastModifiedTime(now);
        
        // Start workflow
        WorkflowTransitionResponse workflowResponse = workflowRepository.startWorkflow(
            service, 
            "PGR",
            wrapper.getWorkflow().getAction(),
            roles
        );
        
        // Update service with workflow details
        service.setWorkflowInstanceId(workflowResponse.getId());
        service.setProcessId(workflowResponse.getProcessId());
        service.setApplicationStatus(workflowResponse.getCurrentState());
        
        // Save service
        citizenServiceRepository.save(service);
        
        // Prepare response
        CitizenService responseDto = CitizenServiceMapper.toDto(service);
        return new ServiceResponse(List.of(responseDto), List.of(wrapper));
    }
}
```

## 7. Testing

### 7.1 Unit Tests
```java
@ExtendWith(MockitoExtension.class)
class ServiceServiceImplTest {
    
    @Mock
    private CitizenServiceRepository repository;
    
    @Mock
    private WorkflowRepository workflowRepository;
    
    @InjectMocks
    private ServiceServiceImpl service;
    
    @Test
    void createService_ValidRequest_ReturnsServiceResponse() {
        // Test implementation
    }
}
```

## 8. Deployment

### 8.1 Docker Compose
```yaml
version: '3.8'

services:
  pgr-service:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/pgr
      - SPRING_DATASOURCE_USERNAME=pgr_user
      - SPRING_DATASOURCE_PASSWORD=pgr_password
    depends_on:
      - postgres
    
  postgres:
    image: postgres:13
    environment:
      - POSTGRES_DB=pgr
      - POSTGRES_USER=pgr_user
      - POSTGRES_PASSWORD=pgr_password
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```
- Git

### Steps to Generate Code

1. **Create a new Maven project** and add the OpenAPI Generator plugin to your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.openapitools</groupId>
            <artifactId>openapi-generator-maven-plugin</artifactId>
            <version>6.6.0</version>
            <executions>
                <execution>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                    <configuration>
                        <inputSpec>${project.basedir}/src/main/resources/api-spec/pgr-api.yaml</inputSpec>
                        <generatorName>spring</generatorName>
                        <apiPackage>com.example.pgr.api</apiPackage>
```bash
mkdir pgr-service
cd pgr-service
```

### 1.2 Create Maven Project
Create a `pom.xml` with the following content (based on pgrown3.0_copy):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.6</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>pgr-service</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>PGR Service</name>
    <description>Public Grievance Redressal Service</description>

    <properties>
        <java.version>17</java.version>
        <lombok.version>1.18.32</lombok.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>1.20.1</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.3</version>
        </dependency>

        <!-- Flyway (PostgreSQL) -->
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
            <version>11.7.2</version>
        </dependency>

        <!-- Digit Client Library -->
        <dependency>
            <groupId>com.digit</groupId>
            <artifactId>digit-client</artifactId>
            <version>1.0.0</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/lib/digit-client-1.0.0.jar</systemPath>
        </dependency>

        <!-- Security + JWT -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-oauth2-jose</artifactId>
        </dependency>

        <!-- Testing -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.flywaydb</groupId>
                <artifactId>flyway-maven-plugin</artifactId>
                <version>11.7.2</version>
                <configuration>
                    <url>jdbc:postgresql://localhost:5432/pgr</url>
                    <user>postgres</user>
                    <password>password</password>
                    <schemas>
                        <schema>public</schema>
                    </schemas>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>org.postgresql</groupId>
                        <artifactId>postgresql</artifactId>
                        <version>42.7.3</version>
                    </dependency>
                </dependencies>
            </plugin>
        </plugins>
    </build>
</project>
```

### 1.3 Application Properties
Create `src/main/resources/application.yml`:

```yaml
# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /pgr-service/v1

# Spring Configuration
spring:
  application:
    name: pgr-service
  datasource:
    url: jdbc:postgresql://localhost:5432/pgr
    username: postgres
    password: password
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
    show-sql: true
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

# FileStore Service
filestore:
  host: http://localhost:8102
  file:
    endpoint: /filestore/v1/files/metadata

# Boundary Service
boundary:
  host: http://localhost:8093
  search:
    endpoint: /boundary/v1

# Notification Service
notification:
  host: http://localhost:8091
  email:
    endpoint: /notification/v1/email/send
  sms:
    endpoint: /notification/sms/send

# Workflow Service
workflow:
  host: http://localhost:8085
  transition:
    post: /workflow/v1/transition
  process:
    base: workflow/v1/process
pgr:
  workflow:
    processId: 4a1c8a61-44fa-43c9-bb59-fe2478b41cf7

# Digit Client Configuration
digit:
  services:
    boundary:
      base-url: http://localhost:8093
    account:
      base-url: http://localhost:8081
    workflow:
      base-url: http://localhost:8085
    idgen:
      base-url: http://localhost:8100
    notification:
      base-url: http://localhost:8091
    timeout:
      read: 30000
  propagate:
    headers:
      allow: authorization,x-correlation-id,x-request-id,x-tenant-id,x-client-id
      prefixes: x-ctx-,x-trace-

# Logging
logging:
  level:
    root: INFO
    com.example.pgr: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    com.digit: DEBUG
```

## 2. Project Structure

```
src/main/java/com/example/pgr/
├── config/              # Configuration classes
│   ├── PgrConfig.java
│   └── SecurityConfig.java
├── controller/          # REST controllers
│   └── PgrController.java
├── model/               # JPA entities
│   ├── ServiceRequest.java
│   └── Citizen.java
├── repository/          # JPA repositories
│   └── ServiceRequestRepository.java
├── service/             # Business logic
│   ├── PgrService.java
│   └── impl/
│       └── PgrServiceImpl.java
├── dto/                 # DTOs
│   ├── ServiceRequestDTO.java
│   └── ServiceResponse.java
├── exception/           # Exception handling
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
└── Application.java     # Main application class

src/main/resources/
├── db/
│   └── migration/       # Flyway migrations
│       └── V1__Initial_schema.sql
└── application.yml      # Application properties
```

## 3. Implementation Steps

### 3.1 Main Application Class
Create `src/main/java/com/example/pgr/Application.java`:
```java
package com.example.pgr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableJpaAuditing
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3.2 Database Configuration
Create Flyway migration `src/main/resources/db/migration/V1__Initial_schema.sql`:
```sql
-- Create tables
CREATE TABLE IF NOT EXISTS service_requests (
    id UUID PRIMARY KEY,
    service_code VARCHAR(255) NOT NULL,
    service_request_id VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(20),
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP,
    tenant_id VARCHAR(50) NOT NULL,
    citizen_id VARCHAR(255),
    additional_details JSONB
);

CREATE INDEX IF NOT EXISTS idx_service_requests_tenant ON service_requests(tenant_id);
CREATE INDEX IF NOT EXISTS idx_service_requests_status ON service_requests(status);
```

### 3.3 JPA Entity
Create `src/main/java/com/example/pgr/model/ServiceRequest.java`:
```java
package com.example.pgr.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "service_requests")
public class ServiceRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_code", nullable = false)
    private String serviceCode;

    @Column(name = "service_request_id", unique = true, nullable = false)
    private String serviceRequestId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String status;

    private String priority;

    @CreationTimestamp
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

#### ServiceRequestRepository.java
```java
package com.example.pgr.repository;

import com.example.pgr.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, UUID>, 
                                               JpaSpecificationExecutor<ServiceRequest> {
    
    boolean existsByServiceCodeAndAccountId(String serviceCode, String accountId);
    
    // Add custom query methods as needed
}
```

## 6. Service Implementation <a name="service-implementation"></a>

### 1. Service Interface

```java
package com.example.pgr.service;

import com.example.pgr.model.CreateServiceRequest;
import com.example.pgr.model.ServiceResponse;
import com.example.pgr.model.UpdateServiceRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PgrService {
    ServiceResponse createService(CreateServiceRequest request, String tenantId);
    ServiceResponse updateService(UpdateServiceRequest request, String tenantId);
    ServiceResponse getServiceById(String id, String tenantId);
    Page<ServiceResponse> searchServices(String serviceCode, String mobileNumber, 
                                       String applicationStatus, Pageable pageable, String tenantId);
}
```

### 2. Service Implementation

```java
package com.example.pgr.service.impl;

import com.example.pgr.entity.ServiceRequest;
import com.example.pgr.mapper.ServiceMapper;
import com.example.pgr.model.CreateServiceRequest;
import com.example.pgr.model.ServiceResponse;
import com.example.pgr.model.UpdateServiceRequest;
import com.example.pgr.repository.ServiceRequestRepository;
import com.example.pgr.service.PgrService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PgrServiceImpl implements PgrService {
    
    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceMapper serviceMapper;
    
    @Override
    @Transactional
    public ServiceResponse createService(CreateServiceRequest request, String tenantId) {
        log.info("Creating new service request for tenant: {}", tenantId);
        
        // Map DTO to entity
        ServiceRequest serviceRequest = serviceMapper.toEntity(request);
        
        // Set tenant-specific data
        serviceRequest.setStatus("CREATED");
        
        // Save to database
        ServiceRequest savedRequest = serviceRequestRepository.save(serviceRequest);
        
        // Map back to DTO and return
        return serviceMapper.toDto(savedRequest);
    }
    
    @Override
    @Transactional
    public ServiceResponse updateService(UpdateServiceRequest request, String tenantId) {
        // Implementation for update
        return null;
    }
    
    // Other method implementations...
}
```

## 7. Exception Handling <a name="exception-handling"></a>

### 1. Global Exception Handler

```java
package com.example.pgr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .findFirst()
            .orElse("Validation failed");
            
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            errorMessage,
            System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred: " + ex.getMessage(),
            System.currentTimeMillis()
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

## 8. Testing <a name="testing"></a>

### 1. Unit Tests

```java
package com.example.pgr.service;

import com.example.pgr.dto.ServiceRequestDto;
import com.example.pgr.entity.ServiceRequest;
import com.example.pgr.mapper.ServiceMapper;
import com.example.pgr.repository.ServiceRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PgrServiceTest {
    
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    
    @Mock
    private ServiceMapper serviceMapper;
    
    @InjectMocks
    private PgrServiceImpl pgrService;
    
    @Test
    void createService_ValidRequest_ReturnsServiceResponse() {
        // Test implementation
    }
}
```

### 2. Integration Tests with TestContainers

```java
package com.example.pgr.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PgrServiceIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    void createService_ValidRequest_ReturnsCreated() {
        // Test implementation
    }
}
```

## 9. API Documentation <a name="api-documentation"></a>

### 1. Swagger Configuration

```java
package com.example.pgr.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    
    @Bean
    public OpenAPI pgrOpenAPI() {
        return new OpenAPI()
            .info(new Info().title("PGR Service API")
                .description("Public Grievance Redressal Service API")
                .version("v1.0.0")
                .license(new License().name("Apache 2.0").url("http://springdoc.org")));
    }
}
```

### 2. Accessing Swagger UI

After starting the application, access the Swagger UI at:
```
http://localhost:8080/pgr-service/swagger-ui.html
```

And the OpenAPI documentation at:
```
http://localhost:8080/pgr-service/api-docs
```

## 10. Deployment <a name="deployment"></a>

### 1. Build the Application

```bash
mvn clean package -DskipTests
```

### 2. Run with Docker

#### Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/pgr-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

#### docker-compose.yml
```yaml
version: '3.8'

services:
  pgr-service:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/pgr_db
      - SPRING_DATASOURCE_USERNAME=pgr_user
      - SPRING_DATASOURCE_PASSWORD=secure_password
    depends_on:
      - postgres
    
  postgres:
    image: postgres:13
    environment:
      - POSTGRES_DB=pgr_db
      - POSTGRES_USER=pgr_user
      - POSTGRES_PASSWORD=secure_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

Start the application with:
```bash
docker-compose up --build
```

## Conclusion

This guide has walked you through the complete process of developing a PGR service from a Swagger contract. The application follows best practices for Spring Boot development, including:

1. Clean architecture with clear separation of concerns
2. Comprehensive error handling
3. API documentation with Swagger/OpenAPI
4. Database integration with JPA
5. Containerization with Docker
6. Testing with JUnit and TestContainers

To extend this application, consider adding:
- Authentication and authorization
- Caching for better performance
- Asynchronous processing for long-running operations
- Monitoring and metrics with Spring Boot Actuator
- Logging and distributed tracing
                    <interfaceOnly>true</interfaceOnly>
                    <useSpringBoot3>true</useSpringBoot3>
                    <useBeanValidation>true</useBeanValidation>
                    <openApiNullable>false</openApiNullable>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>
```

2. Save the Swagger YAML as `src/main/resources/api/pgr-api.yaml`

3. Generate the code:
```bash
mvn clean compile
```

## Project Structure

```
src/main/java/com/example/pgr/
├── config/           # Configuration classes
│   ├── SecurityConfig.java
│   ├── SwaggerConfig.java
│   └── WebClientConfig.java
├── controller/       # REST controllers
│   └── PgrController.java
├── dto/              # Data Transfer Objects
│   ├── request/
│   └── response/
├── model/            # JPA Entities
│   ├── ServiceRequest.java
│   ├── Citizen.java
│   └── Address.java
├── repository/       # JPA Repositories
│   ├── ServiceRequestRepository.java
│   └── CitizenRepository.java
├── service/          # Business logic
│   ├── impl/
│   │   └── PgrServiceImpl.java
│   └── PgrService.java
└── util/             # Utility classes
    └── Constants.java
```

## Implementation Steps

### 1. Create Domain Models

#### ServiceRequest.java
```java
@Entity
@Table(name = "service_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    private String serviceCode;
    private String description;
    private String accountId;
    private String source;
    private String applicationStatus;
    
    @OneToOne(cascade = CascadeType.ALL)
    private Citizen citizen;
    
    @OneToOne(cascade = CascadeType.ALL)
    private Address address;
    
    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> additionalDetail;
    
    @CreationTimestamp
    private LocalDateTime createdTime;
    
    @UpdateTimestamp
    private LocalDateTime lastModifiedTime;
}
```

### 2. Implement Repository Layer

#### ServiceRequestRepository.java
```java
@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, String> {
    Optional<ServiceRequest> findById(String id);
    
    @Query("SELECT s FROM ServiceRequest s WHERE " +
           "(:id IS NULL OR s.id = :id) AND " +
           "(:serviceCode IS NULL OR s.serviceCode = :serviceCode) AND " +
           "(:mobileNumber IS NULL OR s.citizen.mobileNumber = :mobileNumber) AND " +
           "(:applicationStatus IS NULL OR s.applicationStatus = :applicationStatus)")
    Page<ServiceRequest> search(
        @Param("id") String id,
        @Param("serviceCode") String serviceCode,
        @Param("mobileNumber") String mobileNumber,
        @Param("applicationStatus") String applicationStatus,
        Pageable pageable
    );
}
```

### 3. Implement Service Layer

#### PgrService.java
```java
public interface PgrService {
    ServiceResponse createService(ServiceCreate serviceCreate, Workflow workflow);
    ServiceResponse updateService(ServiceUpdate serviceUpdate, Workflow workflow);
    Page<Service> searchServices(String id, String serviceCode, String mobileNumber, 
                               String applicationStatus, Pageable pageable);
}
```

#### PgrServiceImpl.java
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PgrServiceImpl implements PgrService {
    private final ServiceRequestRepository serviceRequestRepository;
    private final ModelMapper modelMapper;
    
    @Override
    @Transactional
    public ServiceResponse createService(ServiceCreate serviceCreate, Workflow workflow) {
        // Map DTO to entity
        ServiceRequest serviceRequest = modelMapper.map(serviceCreate, ServiceRequest.class);
        
        // Apply workflow actions
        applyWorkflow(serviceRequest, workflow);
        
        // Save to database
        ServiceRequest savedRequest = serviceRequestRepository.save(serviceRequest);
        
        // Return response
        return buildServiceResponse(savedRequest, workflow);
    }
    
    // Other method implementations...
}
```

### 4. Implement Controller

#### PgrController.java
```java
@RestController
@RequestMapping("/v3")
@RequiredArgsConstructor
@Tag(name = "PGR", description = "Public Grievance Redressal APIs")
public class PgrController {
    private final PgrService pgrService;
    
    @PostMapping("/request")
    @Operation(summary = "Create a new PGR service request")
    public ResponseEntity<ServiceResponse> createRequest(
            @RequestHeader("X-Client-ID") String clientId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody CreateServiceRequest request) {
        
        ServiceResponse response = pgrService.createService(
            request.getService(), 
            request.getWorkflow()
        );
        
        return ResponseEntity.ok(response);
    }
    
    // Other endpoints...
}
```

## Database Setup

1. Create PostgreSQL database:
```sql
CREATE DATABASE pgr_db;
CREATE USER pgr_user WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE pgr_db TO pgr_user;
```

2. Configure `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pgr_db
    username: pgr_user
    password: secure_password
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          lob:
            non_contextual_creation: true

# OpenAPI/Swagger Configuration
springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    operationsSorter: method

# Server Configuration
server:
  port: 8080
  servlet:
    context-path: /pgr-service
```

## API Documentation

Access the Swagger UI at: `http://localhost:8080/pgr-service/swagger-ui.html`

## Testing

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class PgrServiceTest {
    
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    
    @InjectMocks
    private PgrServiceImpl pgrService;
    
    @Test
    void createService_ValidRequest_ReturnsServiceResponse() {
        // Given
        ServiceCreate serviceCreate = new ServiceCreate()
            .serviceCode("PGR-001")
            .description("Garbage not collected")
            .citizen(new Citizen().mobileNumber("9876543210"));
            
        Workflow workflow = new Workflow().action("CREATE");
        
        // When
        ServiceResponse response = pgrService.createService(serviceCreate, workflow);
        
        // Then
        assertNotNull(response);
        assertEquals("PGR-001", response.getService().getServiceCode());
    }
}
```

### Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PgrControllerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Test
    void createService_ValidRequest_ReturnsCreated() {
        // Test implementation
    }
}
```

## Deployment

### 1. Build the Application

```bash
mvn clean package -DskipTests
```

### 2. Run with Docker

#### Dockerfile
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/pgr-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

#### docker-compose.yml
```yaml
version: '3.8'

services:
  pgr-service:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/pgr_db
      - SPRING_DATASOURCE_USERNAME=pgr_user
      - SPRING_DATASOURCE_PASSWORD=secure_password
    depends_on:
      - postgres
    
  postgres:
    image: postgres:13
    environment:
      - POSTGRES_DB=pgr_db
      - POSTGRES_USER=pgr_user
      - POSTGRES_PASSWORD=secure_password
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

### 3. Deploy to Kubernetes

#### deployment.yaml
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: pgr-service
  labels:
    app: pgr-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: pgr-service
  template:
    metadata:
      labels:
        app: pgr-service
    spec:
      containers:
      - name: pgr-service
        image: pgr-service:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: pgr-db-secret
              key: url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: pgr-db-secret
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: pgr-db-secret
              key: password
---
apiVersion: v1
kind: Service
metadata:
  name: pgr-service
spec:
  selector:
    app: pgr-service
  ports:
    - protocol: TCP
      port: 80
      targetPort: 8080
  type: LoadBalancer
```

## Conclusion

This guide provides a comprehensive walkthrough for developing a PGR service from scratch. The implementation follows RESTful principles, domain-driven design, and best practices for Spring Boot applications. The service is containerized and ready for deployment in various environments.
