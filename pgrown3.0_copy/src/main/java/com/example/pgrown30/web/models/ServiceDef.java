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

 