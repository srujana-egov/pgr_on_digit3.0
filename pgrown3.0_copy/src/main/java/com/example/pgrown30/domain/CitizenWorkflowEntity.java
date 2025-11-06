package com.example.pgrown30.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
import java.util.Map;

@Entity
@Table(name = "citizen_workflow")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenWorkflowEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "service_request_id", nullable = false)
    private String serviceRequestId;

    @Column(name = "action")
    private String action;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "assignees", columnDefinition = "text[]")
    private List<String> assignees;

    @Column(name = "comments")
    private String comments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "verification_docs", columnDefinition = "jsonb")
    private Map<String, Object> verificationDocs;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Column(name = "created_time")
    private Long createdTime;

    @Column(name = "last_modified_time")
    private Long lastModifiedTime;

    // JPA relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", insertable = false, updatable = false)
    private CitizenServiceEntity citizenService;
}
