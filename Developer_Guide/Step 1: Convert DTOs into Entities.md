# Step 1: Convert DTOs into Entities

Step 1 — Identify which DTO becomes which Entity

From your PGR use-case:

DTO	Entity Equivalent
AuditDetails	→ AuditDetails (@Embeddable)
CitizenAddress	→ CitizenAddress (@Entity)
CitizenService	→ CitizenService (@Entity)
ServiceResponse	❌ stays DTO
ServiceWrapper	❌ stays DTO

Because only the first three represent actual database rows.

Step 2 — Move Entities to separate package (Optional)
src/main/java/com/example/pgrown30/domain


NEVER put entities inside web.models.
That folder is reserved for OpenAPI-generated DTOs.

Step 3 — Add JPA annotations & design DB structure

Example with your DTO:

DTO version:
public class CitizenService {
    private String serviceRequestId;
    private String tenantId;
    private String serviceCode;
    private String description;
    private AuditDetails auditDetails;
    private CitizenAddress address;
}

Entity version:
@Entity
@Table(name = "citizen_service")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CitizenService {

    @Id
    @Column(name = "service_request_id")
    private String serviceRequestId;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "service_code")
    private String serviceCode;

    @Column(name = "description")
    private String description;

    @Embedded
    private AuditDetails auditDetails;

    @OneToOne(mappedBy = "citizenService", cascade = CascadeType.ALL)
    private CitizenAddress address;
}

You add @Entity, @Table, @Id
You embed AuditDetails
You convert nested objects to proper relationships

Step 4 — Convert nested DTOs into JPA relationships
DTO version:
private CitizenAddress address;

Entity version:
@OneToOne(mappedBy = "citizenService")
private CitizenAddress address;


And inside CitizenAddress entity:

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "service_request_id")
private CitizenService citizenService;


This is EXACTLY how you persisted your manual model.

Step 5 — Fix type mismatches

DTO uses Double latitude
Entity uses BigDecimal latitude

Because:

Double ≠ precise

DB geographic columns need fixed precision

Step 6 — Final entity versions (For this tutorial you can directly replace your generated DTOs with these Entities)

```java
package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditDetails {
    @JsonProperty("createdBy")
    private String createdBy;
    
    @JsonProperty("createdTime")
    private Long createdTime;
    
    @JsonProperty("lastModifiedBy")
    private String lastModifiedBy;
    
    @JsonProperty("lastModifiedTime")
    private Long lastModifiedTime;
}
```

```java
package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "citizen_address")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CitizenAddress {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "service_request_id", nullable = false)
    private String serviceRequestId;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "audit_details")
    private AuditDetails auditDetails;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", insertable = false, updatable = false)
    @JsonBackReference
    private CitizenService citizenService;
}
```

```java
package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Entity
@Table(name = "citizen_service", indexes = {
    @Index(columnList = "tenant_id"),
    @Index(columnList = "service_code"),
    @Index(columnList = "application_status"),
    @Index(columnList = "account_id"),
    @Index(columnList = "boundary_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class CitizenService {

    @Id
    @Column(name = "service_request_id")
    private String serviceRequestId;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "service_code")
    private String serviceCode;

    @Column(name = "description")
    private String description;

    @Column(name = "account_id")
    private String accountId;

    @Column(name = "source")
    private String source;

    @Column(name = "application_status")
    private String applicationStatus;
    
    @Transient
    private String action;

    @Column(name = "file_store_id")
    private String fileStoreId;

    @Column(name = "file_valid")
    private Boolean fileValid;

    @Column(name = "boundary_code")
    private String boundaryCode;

    @Column(name = "boundary_valid")
    private Boolean boundaryValid;

    @Embedded
    private AuditDetails auditDetails;

    @Column(name = "email")
    private String email;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "process_id")
    private String processId;

    @Column(name = "workflow_instance_id")
private String workflowInstanceId;

    @OneToOne(mappedBy = "citizenService", cascade = CascadeType.ALL, fetch = FetchType.LAZY, optional = true)
    @JsonManagedReference
    private CitizenAddress address;

    @PrePersist
    protected void onCreate() {
        if (this.auditDetails == null) {
            this.auditDetails = AuditDetails.builder().build();
        }
        long now = System.currentTimeMillis();
        this.auditDetails.setCreatedTime(now);
        this.auditDetails.setLastModifiedTime(now);
    }

    @PreUpdate
    protected void onUpdate() {
        if (this.auditDetails == null) {
            this.auditDetails = AuditDetails.builder().build();
        }
        this.auditDetails.setLastModifiedTime(System.currentTimeMillis());
    }
}
```

```java
package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {

    @JsonProperty("services")
    @Valid
    private List<CitizenService> services;

    @JsonProperty("serviceWrappers")
    @Valid
    private List<ServiceWrapper> serviceWrappers;
}
```

```java
package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.digit.services.workflow.model.Workflow;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceWrapper {

    @JsonProperty("CitizenService")
    @NotNull
    @Valid
    private CitizenService service;

    @JsonProperty("Workflow")
    @Valid
    private Workflow workflow;
}
```





