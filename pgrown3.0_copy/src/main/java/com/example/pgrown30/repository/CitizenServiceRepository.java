package com.example.pgrown30.repository;

import com.example.pgrown30.web.models.CitizenService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitizenServiceRepository extends JpaRepository<CitizenService, String>, JpaSpecificationExecutor<CitizenService> {
    
    Optional<CitizenService> findByServiceRequestIdAndTenantId(String serviceRequestId, String tenantId);
    
}