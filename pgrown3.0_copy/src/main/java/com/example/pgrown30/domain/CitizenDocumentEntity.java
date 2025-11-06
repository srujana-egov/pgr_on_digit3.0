package com.example.pgrown30.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "citizen_document")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitizenDocumentEntity {

    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "service_request_id", nullable = false)
    private String serviceRequestId;

    @Column(name = "document_type")
    private String documentType;

    @Column(name = "file_store_id")
    private String fileStoreId;

    @Column(name = "document_uid")
    private String documentUid;

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
