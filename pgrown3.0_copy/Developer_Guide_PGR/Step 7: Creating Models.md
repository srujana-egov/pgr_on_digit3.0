# Create Models

## **Steps**

0. AuditDetails.java -

```java
package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
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

1. CitizenAddress.java -

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

2. CitizenService.java -

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

3. ServiceResponse.java -

```java
package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.*;
import java.util.List;
import com.digit.services.workflow.model.Workflow;

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

4. ServiceWrapper.java -

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

All the necessary models are ready!
