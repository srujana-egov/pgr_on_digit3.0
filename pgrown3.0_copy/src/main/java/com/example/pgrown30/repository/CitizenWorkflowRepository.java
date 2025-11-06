package com.example.pgrown30.repository;

import com.example.pgrown30.domain.CitizenWorkflowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitizenWorkflowRepository extends JpaRepository<CitizenWorkflowEntity, String>, JpaSpecificationExecutor<CitizenWorkflowEntity> {
    
    List<CitizenWorkflowEntity> findByServiceRequestId(String serviceRequestId);
    
    List<CitizenWorkflowEntity> findByServiceRequestIdOrderByCreatedTimeDesc(String serviceRequestId);
    
    void deleteByServiceRequestId(String serviceRequestId);
}
