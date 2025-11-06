package com.example.pgrown30.repository;

import com.example.pgrown30.domain.CitizenDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitizenDocumentRepository extends JpaRepository<CitizenDocumentEntity, String>, JpaSpecificationExecutor<CitizenDocumentEntity> {
    
    List<CitizenDocumentEntity> findByServiceRequestId(String serviceRequestId);
    
    List<CitizenDocumentEntity> findByServiceRequestIdAndDocumentType(String serviceRequestId, String documentType);
    
    void deleteByServiceRequestId(String serviceRequestId);
}
