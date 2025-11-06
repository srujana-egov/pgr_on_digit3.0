package com.example.pgrown30.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "citizen_audit")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenAuditEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "service_request_id", nullable = false)
    private String serviceRequestId;

    @Column(name = "action")
    private String action;

    @Column(name = "status")
    private String status;

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "performed_time")
    private Long performedTime;

    @Column(name = "remarks")
    private String remarks;

    // JPA relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", insertable = false, updatable = false)
    private CitizenServiceEntity citizenService;
}
