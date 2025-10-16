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
public class UserInfo {

    @JsonProperty("id")
    private String id;

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("mobileNumber")
    private String mobileNumber;

    @JsonProperty("emailId")
    private String emailId;

    @JsonProperty("roles")
    private java.util.List<Role> roles;

    @JsonProperty("tenantId")
    private String tenantId;
}
