# Step 3: Implement Repository Layer

Methods in the service layer, upon performing all the business logic, call methods in the repository layer to persist or lookup data i.e. it interacts with the configured data store. For executing the queries, JdbcTemplate class is used. JdbcTemplate takes care of the creation and release of resources such as creating and closing the connection etc. All database operations namely insert, update, search and delete can be performed on the database using methods of JdbcTemplate class.

## **Steps**

1. Create CitizenServiceRepository.java under repository folder.

The contents of CitizenServiceRepository.java are as follows:

```java
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
    
    @Query("SELECT cs FROM CitizenService cs WHERE cs.tenantId = :tenantId AND cs.auditDetails.createdTime BETWEEN :startDate AND :endDate")
    List<CitizenService> findByTenantIdAndCreatedTimeBetween(
        @Param("tenantId") String tenantId, 
        @Param("startDate") Long startDate, 
        @Param("endDate") Long endDate
    );
    // Find by tenant
    List<CitizenService> findByTenantId(String tenantId);
    Page<CitizenService> findByTenantId(String tenantId, Pageable pageable);
    
    // Find by service code
    List<CitizenService> findByTenantIdAndServiceCode(String tenantId, String serviceCode);
    Page<CitizenService> findByTenantIdAndServiceCode(String tenantId, String serviceCode, Pageable pageable);
    
    // Find by status
    List<CitizenService> findByTenantIdAndApplicationStatus(String tenantId, String status);
    Page<CitizenService> findByTenantIdAndApplicationStatus(String tenantId, String status, Pageable pageable);
    
    // Combined queries
    List<CitizenService> findByTenantIdAndServiceCodeAndApplicationStatus(
            String tenantId, String serviceCode, String applicationStatus);
            
    List<CitizenService> findByTenantIdAndAccountId(String tenantId, String accountId);
    
    List<CitizenService> findByTenantIdAndSource(String tenantId, String source);
    
    List<CitizenService> findByTenantIdAndBoundaryCode(String tenantId, String boundaryCode);
    
    // Search by file store
    List<CitizenService> findByTenantIdAndFileStoreId(String tenantId, String fileStoreId);
    
    List<CitizenService> findByTenantIdAndFileValid(String tenantId, Boolean fileValid);
    
}
```

The repository layer is implemented.
