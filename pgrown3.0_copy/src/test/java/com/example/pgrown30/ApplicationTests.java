package com.example.pgrown30;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean; // ✅ Add this import
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@Testcontainers
class ApplicationTests {

    @Container
    public static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
            .asCompatibleSubstituteFor("confluentinc/cp-kafka")
    );

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    // --- Mock missing dependencies ---
    @MockBean
    private com.example.pgrown30.repository.NotificationRepository notificationRepository;

    @MockBean
    private com.example.pgrown30.repository.WorkflowRepository workflowRepository;

    @MockBean
    private com.example.pgrown30.repository.IdGenRepository idGenRepository;

    @MockBean
    private com.example.pgrown30.repository.FileStoreRepository fileStoreRepository;

    @MockBean
    private com.example.pgrown30.repository.BoundaryRepository boundaryRepository;

    @Test
    void contextLoads() {
        // Context will now start successfully
    }
}
