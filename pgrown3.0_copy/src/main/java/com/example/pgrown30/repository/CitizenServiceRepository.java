package com.example.pgrown30.repository;

import com.example.pgrown30.domain.CitizenServiceEntity;
import com.example.pgrown30.domain.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CitizenServiceRepository extends JpaRepository<CitizenServiceEntity, String>, JpaSpecificationExecutor<CitizenServiceEntity> {
    List<CitizenServiceEntity> findByTenantId(String tenantId);

    List<CitizenServiceEntity> findByTenantIdAndServiceCode(String tenantId, String serviceCode);

    List<CitizenServiceEntity> findByTenantIdAndServiceCodeAndApplicationStatus(
    String tenantId, String serviceCode, Status applicationStatus
);
    Optional<CitizenServiceEntity> findByServiceRequestIdAndTenantId(String serviceRequestId, String tenantId);

}
