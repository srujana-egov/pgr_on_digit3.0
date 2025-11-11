package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(
    name = "citizen_service",
    indexes = {
        @Index(columnList = "tenant_id"),
        @Index(columnList = "service_code"),
        @Index(columnList = "application_status"),
        @Index(columnList = "account_id"),
        @Index(columnList = "boundary_code")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

    @Column(name = "created_time")
    private Long createdTime;

    @Column(name = "last_modified_time")
    private Long lastModifiedTime;

    @Column(name = "email")
    private String email;

    @Column(name = "mobile")
    private String mobile;

    @Column(name = "workflow_instance_id")
    private String workflowInstanceId;

    @Column(name = "process_id")
    private String processId;

    // Relationships
    @OneToMany(mappedBy = "citizenService", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CitizenAddress> addresses;

    @OneToMany(mappedBy = "citizenService", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CitizenDocument> documents;

    @OneToMany(mappedBy = "citizenService", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CitizenWorkflow> workflows;

    @OneToMany(mappedBy = "citizenService", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<CitizenAudit> audits;

}
