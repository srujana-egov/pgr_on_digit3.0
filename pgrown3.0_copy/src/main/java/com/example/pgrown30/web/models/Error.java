// Error.java
package com.example.pgr.client.model;

import lombok.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Error {
    private String code;
    private String message;
    private List<ErrorDetail> details;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorDetail {
        private String code;
        private String message;
    }
}
