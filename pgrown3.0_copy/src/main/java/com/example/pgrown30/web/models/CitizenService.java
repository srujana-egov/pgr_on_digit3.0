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

    @Enumerated(EnumType.STRING)
    @Column(name = "application_status")
    private Status applicationStatus;
    
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