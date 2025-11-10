package com.example.pgrown30.web.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "citizen_address")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CitizenAddress {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "service_request_id", nullable = false)
    private String serviceRequestId;

    @Column(name = "address")
    private String address;

    @Column(name = "city")
    private String city;

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Column(name = "created_time")
    private Long createdTime;

    @Column(name = "last_modified_time")
    private Long lastModifiedTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", insertable = false, updatable = false)
    @JsonBackReference
    private CitizenService citizenService;
}
