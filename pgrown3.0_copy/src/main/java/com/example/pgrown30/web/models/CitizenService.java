// CitizenService.java
package com.example.pgr.client.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CitizenService implements Serializable {
    private String serviceRequestId;
    private String tenantId;
    private String serviceCode;
    @JsonProperty("description")
    private String descriptionText;
    private AddressDetail addressDetail;
    private String firstName;
    private String phone;
    private String email;
    private Status status;

    public enum Status { OPEN, ASSIGNED, RESOLVED, REJECTED, CLOSED }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AddressDetail {
        private String addressLine1;
        private String city;
        private String pincode;
    }
}
