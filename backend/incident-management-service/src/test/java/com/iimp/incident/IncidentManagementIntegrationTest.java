package com.iimp.incident;

import com.iimp.incident.domain.Incident;
import com.iimp.incident.dto.CreateIncidentRequest;
import com.iimp.incident.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class IncidentManagementIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("iimp_test")
        .withUsername("iimp")
        .withPassword("iimp_test");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.6.0")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired MockMvc mockMvc;
    @Autowired IncidentRepository incidentRepository;
    @Autowired ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        incidentRepository.deleteAll();
    }

    @Test
    void createIncident_shouldPersistAndReturn201() throws Exception {
        CreateIncidentRequest request = new CreateIncidentRequest();
        request.setTitle("Database connection exhausted");
        request.setSeverity(Incident.Severity.CRITICAL);
        request.setAffectedServices(List.of("order-service", "payment-service"));

        mockMvc.perform(post("/api/v1/incidents")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + generateTestToken())
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.severity").value("CRITICAL"))
            .andExpect(jsonPath("$.data.status").value("OPEN"));

        assertThat(incidentRepository.count()).isEqualTo(1);
    }

    @Test
    void getIncidents_shouldReturnPagedList() throws Exception {
        mockMvc.perform(get("/api/v1/incidents")
                .header("Authorization", "Bearer " + generateTestToken())
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getIncident_withInvalidId_shouldReturn404() throws Exception {
        mockMvc.perform(get("/api/v1/incidents/00000000-0000-0000-0000-000000000000")
                .header("Authorization", "Bearer " + generateTestToken()))
            .andExpect(status().isNotFound());
    }

    private String generateTestToken() {
        // Returns a test JWT signed with the test secret
        return "test-jwt-token";  // Replace with actual token generation in test setup
    }
}
