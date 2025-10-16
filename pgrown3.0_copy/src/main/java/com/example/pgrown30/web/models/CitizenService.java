package com.example.pgrown30.web.models;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenService {

    private String serviceRequestId;
    private String tenantId;
    private String serviceCode;
    private String description;
    private String accountId;
    private String source;
    private String applicationStatus; // String representation of Status enum
    private String fileStoreId;
    private boolean fileValid;
    private String boundaryCode;
    private boolean boundaryValid;
    private String action;
    private String workflowInstanceId;
    private Long createdTime;
    private Long lastModifiedTime;
    private String email;       // <-- add this
    private String mobile; 

}
