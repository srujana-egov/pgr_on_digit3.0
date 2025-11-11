package com.example.pgrown30.repository;

import com.example.pgrown30.web.models.CitizenService;
import com.example.pgrown30.web.models.Status;
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
    // Basic CRUD operations are inherited from JpaRepository
    
    // Find by ID with tenant isolation
    Optional<CitizenService> findByServiceRequestIdAndTenantId(
    @Param("serviceRequestId") String serviceRequestId,
    @Param("tenantId") String tenantId
);
    // Find by tenant
    List<CitizenService> findByTenantId(String tenantId);
    Page<CitizenService> findByTenantId(String tenantId, Pageable pageable);
    
    // Find by service code
    List<CitizenService> findByTenantIdAndServiceCode(String tenantId, String serviceCode);
    Page<CitizenService> findByTenantIdAndServiceCode(String tenantId, String serviceCode, Pageable pageable);
    
    // Find by status
    List<CitizenService> findByTenantIdAndApplicationStatus(String tenantId, Status status);
    Page<CitizenService> findByTenantIdAndApplicationStatus(String tenantId, Status status, Pageable pageable);
    
    // Combined queries
    List<CitizenService> findByTenantIdAndServiceCodeAndApplicationStatus(
            String tenantId, String serviceCode, Status applicationStatus);
            
    List<CitizenService> findByTenantIdAndAccountId(String tenantId, String accountId);
    
    List<CitizenService> findByTenantIdAndSource(String tenantId, String source);
    
    List<CitizenService> findByTenantIdAndBoundaryCode(String tenantId, String boundaryCode);
    
    // Search by file store
    List<CitizenService> findByTenantIdAndFileStoreId(String tenantId, String fileStoreId);
    
    List<CitizenService> findByTenantIdAndFileValid(String tenantId, Boolean fileValid);
    
    // Date range queries
    List<CitizenService> findByTenantIdAndCreatedTimeBetween(
            String tenantId, Long startTime, Long endTime);
            
    // Custom query example using @Query
    @Query("SELECT cs FROM CitizenService cs WHERE " +
           "cs.tenantId = :tenantId AND " +
           "(LOWER(cs.description) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(cs.serviceRequestId) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<CitizenService> searchByDescriptionOrRequestId(
            @Param("tenantId") String tenantId,
            @Param("searchTerm") String searchTerm);
    
    // Count queries
    Long countByTenantId(String tenantId);
    
    Long countByTenantIdAndServiceCode(String tenantId, String serviceCode);
    
    Long countByTenantIdAndApplicationStatus(String tenantId, Status status);

    @EntityGraph(attributePaths = {"documents", "audits"})
    Optional<CitizenService> findWithDetailsByServiceRequestIdAndTenantId(
        @Param("serviceRequestId") String serviceRequestId,
        @Param("tenantId") String tenantId
    );
}