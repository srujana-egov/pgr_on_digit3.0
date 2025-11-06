# Create Models

## **Steps**
0. AuditDetails.java -

```java
package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditDetails {

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("lastModifiedBy")
    private String lastModifiedBy;

    @JsonProperty("createdTime")
    private LocalDateTime createdTime;

    @JsonProperty("lastModifiedTime")
    private LocalDateTime lastModifiedTime;
}
```

1. CitizenService.java -

```java   
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
```

2. Document.java -

```java
package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @JsonProperty("id")
    private String id;

    @JsonProperty("documentType")
    private String documentType;

    @JsonProperty("fileStoreId")
    private String fileStoreId;

    @JsonProperty("documentUid")
    private String documentUid;

    @JsonProperty("auditDetails")
    private AuditDetails auditDetails;
}
```

3. ResponseInfo.java -

```java
package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseInfo {

    @JsonProperty("apiId")
    private String apiId;

    @JsonProperty("ver")
    private String ver;

    @JsonProperty("ts")
    private Long ts;

    @JsonProperty("resMsgId")
    private String resMsgId;

    @JsonProperty("msgId")
    private String msgId;

    @JsonProperty("status")
    private String status;

    /**
     * Convenience factory for a success response.
     */
    public static ResponseInfo success() {
        return ResponseInfo.builder()
                .apiId("pgrown-service")
                .ver("1.0")
                .ts(System.currentTimeMillis())
                .status("successful")
                .build();
    }
}
```

4. ServiceDef.java -

```java
package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ServiceDef {

    @JsonProperty("tenantId")
    @NotBlank(message = "TenantId cannot be blank")
    @Size(min = 2, max = 50, message = "TenantId should not be more than 50 characters or less than 2 characters.")  
    private String tenantId;       

    @JsonProperty("serviceCode")
    @NotBlank(message = "ServiceCode cannot be blank")
    @Size(min = 2, max = 64, message = "ServiceCode should not be more than 64 characters or less than 2 characters.") 
    private String serviceCode;    
     

    @JsonProperty("tag")
    private String tag;     

    @JsonProperty("group")
    private String group;   

    @JsonProperty("slaHours")
    private Integer slaHours;   

    
}
```

5. ServiceRequest.java -

```java
package com.example.pgrown30.web.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ServiceRequest {



    @JsonProperty("serviceWrapper")
    @NotNull(message = "ServiceWrapper cannot be null")
    @Valid
    private ServiceWrapper serviceWrapper;
    
}
```

6. ServiceResponse.java -

```java
package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

7. ServiceWrapper.java -

```java
package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ServiceWrapper {

    @JsonProperty("CitizenService")
    @NotNull(message = "Service cannot be blank")
    @Valid
    private CitizenService service;

    @JsonProperty("Workflow")
    @Valid
    private Workflow workflow;    
}
```

8. Workflow.java -

```java
package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.Valid;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Workflow {

    @JsonProperty("action")
    @Size(min = 1, max = 64, message = "Action should not be more than 64 characters or less than 2 characters.")
    private String action;

    @JsonProperty("assignes")
    private java.util.List<String> assignes;            

    @JsonProperty("comments")
    @Size(min = 1, max = 64, message = "Comments should not be more than 64 characters or less than 2 characters.")
    private String comments;

    @JsonProperty("verificationDocuments")
    @Valid
    private java.util.List<Document> verificationDocuments;        

    
}
```

All the necessary models are ready!
