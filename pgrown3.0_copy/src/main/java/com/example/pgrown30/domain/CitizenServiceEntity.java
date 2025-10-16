package com.example.pgrown30.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "citizen_service")
@Getter
@Setter
@Builder             // ← Adds the builder() method
@NoArgsConstructor
@AllArgsConstructor
public class CitizenServiceEntity {

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

   // @Enumerated(EnumType.STRING)
    @Column(name = "application_status")
    private String applicationStatus;

    @Column(name = "file_store_id")
    private String fileStoreId;

    @Column(name = "file_valid")
    private Boolean fileValid;

    @Column(name = "boundary_code")
    private String boundaryCode;

    @Column(name = "action")
    private String action;

    @Column(name = "created_time")
    private Long createdTime;

    @Column(name = "last_modified_time")
    private Long lastModifiedTime;

    // **Add these fields**
    @Column
    private String email;

    @Column
    private String mobile;

    @Column(name = "workflow_instance_id")
    private String workflowInstanceId;

    @Column(name = "boundary_valid")
    private Boolean boundaryValid;
    
    public boolean isBoundaryValid() {
    return boundaryValid;
}

public void setBoundaryValid(boolean boundaryValid) {
    this.boundaryValid = boundaryValid;
}


    @Column(name = "process_id")
private String processId;

public String getProcessId() {
    return processId;
}

public void setProcessId(String processId) {
    this.processId = processId;
}
}
