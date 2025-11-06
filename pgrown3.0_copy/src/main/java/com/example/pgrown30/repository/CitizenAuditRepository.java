package com.example.pgrown30.repository;

import com.example.pgrown30.domain.CitizenAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitizenAuditRepository extends JpaRepository<CitizenAuditEntity, String>, JpaSpecificationExecutor<CitizenAuditEntity> {
    
    List<CitizenAuditEntity> findByServiceRequestId(String serviceRequestId);
    
    List<CitizenAuditEntity> findByServiceRequestIdOrderByPerformedTimeDesc(String serviceRequestId);
    
    List<CitizenAuditEntity> findByPerformedBy(String performedBy);
    
    void deleteByServiceRequestId(String serviceRequestId);
}
