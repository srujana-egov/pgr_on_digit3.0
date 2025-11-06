package com.example.pgrown30.repository;

import com.example.pgrown30.domain.CitizenAddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitizenAddressRepository extends JpaRepository<CitizenAddressEntity, String>, JpaSpecificationExecutor<CitizenAddressEntity> {
    
    List<CitizenAddressEntity> findByServiceRequestId(String serviceRequestId);
    
    void deleteByServiceRequestId(String serviceRequestId);
}
