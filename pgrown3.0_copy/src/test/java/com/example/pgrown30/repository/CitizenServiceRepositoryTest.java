package com.example.pgrown30.repository;

import com.example.pgrown30.domain.CitizenServiceEntity;
import com.example.pgrown30.domain.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test") 
class CitizenServiceRepositoryTest {

    @Autowired
    private CitizenServiceRepository repository;

    @Test
    void testSaveAndFindById() {
        // given
        CitizenServiceEntity entity = CitizenServiceEntity.builder()
                .serviceRequestId("REQ-001")
                .tenantId("tenant.one")
                .serviceCode("PGR001")
                .description("Streetlight not working")
                .accountId("acc-123")
                .source("mobile-app")
                .applicationStatus(Status.ACTIVE)
                .createdTime(System.currentTimeMillis())
                .lastModifiedTime(System.currentTimeMillis())
                .build();

        // when
        repository.save(entity);
        CitizenServiceEntity found = repository.findById("REQ-001").orElse(null);

        // then
        assertThat(found).isNotNull();
        assertThat(found.getTenantId()).isEqualTo("tenant.one");
        assertThat(found.getServiceCode()).isEqualTo("PGR001");
    }

    @Test
    void testFindByTenantId() {
        // given
        CitizenServiceEntity entity1 = CitizenServiceEntity.builder()
                .serviceRequestId("REQ-002")
                .tenantId("tenant.two")
                .serviceCode("PGR002")
                .description("Garbage not collected")
                .applicationStatus(Status.ACTIVE)
                .build();

        repository.save(entity1);

        // when
        List<CitizenServiceEntity> results = repository.findByTenantId("tenant.two");

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDescription()).isEqualTo("Garbage not collected");
    }

    @Test
    void testFindByTenantIdAndServiceCode() {
        // given
        CitizenServiceEntity entity2 = CitizenServiceEntity.builder()
                .serviceRequestId("REQ-003")
                .tenantId("tenant.three")
                .serviceCode("PGR003")
                .description("Water leakage")
                .applicationStatus(Status.ACTIVE)
                .build();

        repository.save(entity2);

        // when
        List<CitizenServiceEntity> results =
                repository.findByTenantIdAndServiceCode("tenant.three", "PGR003");

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getServiceCode()).isEqualTo("PGR003");
    }
}

