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
    
    /**
     * Finds a citizen service by its request ID and tenant ID.
     * @param serviceRequestId The unique identifier of the service request
     * @param tenantId The tenant identifier
     * @return An Optional containing the found CitizenService, or empty if not found
     */
    Optional<CitizenService> findByServiceRequestIdAndTenantId(String serviceRequestId, String tenantId);
    
    // Additional query methods can be added here as needed
    
}
```

The repository layer is implemented.
