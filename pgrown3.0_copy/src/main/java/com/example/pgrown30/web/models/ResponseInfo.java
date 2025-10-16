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
