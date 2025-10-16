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

