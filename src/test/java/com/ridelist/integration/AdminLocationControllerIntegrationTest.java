package com.ridelist.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.AreaResponse;
import com.ridelist.dto.response.AxisResponse;
import com.ridelist.dto.response.StateResponse;
import com.ridelist.model.Area;
import com.ridelist.model.Axis;
import com.ridelist.model.Role;
import com.ridelist.model.State;
import com.ridelist.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminLocationController.
 * Tests admin CRUD operations for location hierarchy (State -> Axis -> Area).
 *
 * Test IDs: LOC-001 to LOC-022
 */
@DisplayName("AdminLocationController Integration Tests")
class AdminLocationControllerIntegrationTest extends BaseIntegrationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private EntityManager entityManager;

    private String adminToken;

    @BeforeEach
    void setupAdmin() throws Exception {
        adminToken = registerAndGetToken("admin@test.com", "password123");
        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        adminToken = loginAndGetToken("admin@test.com", "password123");
    }

    // ==================== STATE TESTS ====================

    @Nested
    @DisplayName("State CRUD Tests")
    class StateCrudTests {

        @Test
        @DisplayName("LOC-001: Admin creates state - returns 201 with generated slug")
        void adminCreatesState_ReturnsCreated() throws Exception {
            MvcResult result = mockMvc.perform(post("/api/v1/admin/locations/states")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Lagos\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Lagos"))
                    .andExpect(jsonPath("$.data.slug").value("lagos"))
                    .andReturn();

            ApiResponse<StateResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getId()).isNotNull();
        }

        @Test
        @DisplayName("LOC-001b: State with multi-word name generates hyphenated slug")
        void stateWithMultiWordName_GeneratesHyphenatedSlug() throws Exception {
            mockMvc.perform(post("/api/v1/admin/locations/states")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Federal Capital Territory\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.slug").value("federal-capital-territory"));
        }

        @Test
        @DisplayName("LOC-002: Admin creates duplicate state - returns 409 Conflict")
        void adminCreatesDuplicateState_ReturnsConflict() throws Exception {
            // First create
            mockMvc.perform(post("/api/v1/admin/locations/states")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Lagos\"}"))
                    .andExpect(status().isCreated());

            // Duplicate
            mockMvc.perform(post("/api/v1/admin/locations/states")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Lagos\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("LOC-003: Non-admin creates state - returns 403 Forbidden")
        void nonAdminCreatesState_ReturnsForbidden() throws Exception {
            String userToken = registerAndGetToken("user@test.com", "password123");

            mockMvc.perform(post("/api/v1/admin/locations/states")
                            .header("Authorization", authHeader(userToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Lagos\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("LOC-004: Get all states - returns list")
        void getAllStates_ReturnsList() throws Exception {
            createTestState("Lagos");
            createTestState("Abuja");
            createTestState("Kano");

            MvcResult result = mockMvc.perform(get("/api/v1/admin/locations/states")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andReturn();

            ApiResponse<List<StateResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData()).hasSize(3);
        }

        @Test
        @DisplayName("LOC-005: Delete state - cascades to axes and areas")
        void deleteState_CascadesToAxesAndAreas() throws Exception {
            State state = createTestState("Lagos");
            Axis axis = createTestAxis("Mainland", state);
            Area area = createTestArea("Yaba", axis);

            UUID stateId = state.getId();
            UUID axisId = axis.getId();
            UUID areaId = area.getId();

            mockMvc.perform(delete("/api/v1/admin/locations/states/{id}", stateId)
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            entityManager.flush();
            entityManager.clear();

            // Verify cascade
            assertThat(stateRepository.findById(stateId)).isEmpty();
            assertThat(axisRepository.findById(axisId)).isEmpty();
            assertThat(areaRepository.findById(areaId)).isEmpty();
        }

        @Test
        @DisplayName("Update state name")
        void updateStateName_ReturnsOk() throws Exception {
            State state = createTestState("Lagos");

            mockMvc.perform(put("/api/v1/admin/locations/states/{id}", state.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Lagos State\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Lagos State"))
                    .andExpect(jsonPath("$.data.slug").value("lagos-state"));
        }

        @Test
        @DisplayName("Delete non-existent state - returns 404")
        void deleteNonExistentState_ReturnsNotFound() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/locations/states/{id}", UUID.randomUUID())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Create state without name - returns 400")
        void createStateWithoutName_ReturnsBadRequest() throws Exception {
            mockMvc.perform(post("/api/v1/admin/locations/states")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== AXIS TESTS ====================

    @Nested
    @DisplayName("Axis CRUD Tests")
    class AxisCrudTests {

        @Test
        @DisplayName("LOC-010: Create axis under state - returns 201")
        void createAxisUnderState_ReturnsCreated() throws Exception {
            State state = createTestState("Lagos");

            MvcResult result = mockMvc.perform(post("/api/v1/admin/locations/axes")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Mainland\",\"stateId\":\"" + state.getId() + "\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Mainland"))
                    .andExpect(jsonPath("$.data.slug").value("mainland"))
                    .andReturn();

            ApiResponse<AxisResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getId()).isNotNull();
        }

        @Test
        @DisplayName("LOC-011: Create axis with invalid state - returns 404")
        void createAxisWithInvalidState_ReturnsNotFound() throws Exception {
            mockMvc.perform(post("/api/v1/admin/locations/axes")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Mainland\",\"stateId\":\"" + UUID.randomUUID() + "\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("LOC-012: Get axes by state - returns list")
        void getAxesByState_ReturnsList() throws Exception {
            State state = createTestState("Lagos");
            createTestAxis("Mainland", state);
            createTestAxis("Island", state);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/locations/states/{stateId}/axes", state.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andReturn();

            ApiResponse<List<AxisResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData()).hasSize(2);
        }

        @Test
        @DisplayName("Create duplicate axis slug - returns 409")
        void createDuplicateAxisSlug_ReturnsConflict() throws Exception {
            State state = createTestState("Lagos");
            createTestAxis("Mainland", state);

            mockMvc.perform(post("/api/v1/admin/locations/axes")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Mainland\",\"stateId\":\"" + state.getId() + "\"}"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Update axis name")
        void updateAxisName_ReturnsOk() throws Exception {
            State state = createTestState("Lagos");
            Axis axis = createTestAxis("Mainland", state);

            mockMvc.perform(put("/api/v1/admin/locations/axes/{id}", axis.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Lagos Mainland\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Lagos Mainland"))
                    .andExpect(jsonPath("$.data.slug").value("lagos-mainland"));
        }

        @Test
        @DisplayName("Delete axis - cascades to areas")
        void deleteAxis_CascadesToAreas() throws Exception {
            State state = createTestState("Lagos");
            Axis axis = createTestAxis("Mainland", state);
            Area area = createTestArea("Yaba", axis);

            UUID axisId = axis.getId();
            UUID areaId = area.getId();

            mockMvc.perform(delete("/api/v1/admin/locations/axes/{id}", axisId)
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk());

            entityManager.flush();
            entityManager.clear();

            assertThat(axisRepository.findById(axisId)).isEmpty();
            assertThat(areaRepository.findById(areaId)).isEmpty();
            // State should remain
            assertThat(stateRepository.findById(state.getId())).isPresent();
        }

        @Test
        @DisplayName("Delete non-existent axis - returns 404")
        void deleteNonExistentAxis_ReturnsNotFound() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/locations/axes/{id}", UUID.randomUUID())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== AREA TESTS ====================

    @Nested
    @DisplayName("Area CRUD Tests")
    class AreaCrudTests {

        @Test
        @DisplayName("LOC-020: Create area under axis - returns 201")
        void createAreaUnderAxis_ReturnsCreated() throws Exception {
            State state = createTestState("Lagos");
            Axis axis = createTestAxis("Mainland", state);

            MvcResult result = mockMvc.perform(post("/api/v1/admin/locations/areas")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Yaba\",\"axisId\":\"" + axis.getId() + "\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Yaba"))
                    .andExpect(jsonPath("$.data.slug").value("yaba"))
                    .andReturn();

            ApiResponse<AreaResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getId()).isNotNull();
        }

        @Test
        @DisplayName("LOC-021: Create area with invalid axis - returns 404")
        void createAreaWithInvalidAxis_ReturnsNotFound() throws Exception {
            mockMvc.perform(post("/api/v1/admin/locations/areas")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Yaba\",\"axisId\":\"" + UUID.randomUUID() + "\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("LOC-022: Get areas by axis - returns list")
        void getAreasByAxis_ReturnsList() throws Exception {
            State state = createTestState("Lagos");
            Axis axis = createTestAxis("Mainland", state);
            createTestArea("Yaba", axis);
            createTestArea("Surulere", axis);
            createTestArea("Ebute Metta", axis);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/locations/axes/{axisId}/areas", axis.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andReturn();

            ApiResponse<List<AreaResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData()).hasSize(3);
        }

        @Test
        @DisplayName("Create duplicate area slug - returns 409")
        void createDuplicateAreaSlug_ReturnsConflict() throws Exception {
            State state = createTestState("Lagos");
            Axis axis = createTestAxis("Mainland", state);
            createTestArea("Yaba", axis);

            mockMvc.perform(post("/api/v1/admin/locations/areas")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Yaba\",\"axisId\":\"" + axis.getId() + "\"}"))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Update area name")
        void updateAreaName_ReturnsOk() throws Exception {
            State state = createTestState("Lagos");
            Axis axis = createTestAxis("Mainland", state);
            Area area = createTestArea("Yaba", axis);

            mockMvc.perform(put("/api/v1/admin/locations/areas/{id}", area.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Yaba Central\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Yaba Central"))
                    .andExpect(jsonPath("$.data.slug").value("yaba-central"));
        }

        @Test
        @DisplayName("Delete area")
        void deleteArea_ReturnsOk() throws Exception {
            State state = createTestState("Lagos");
            Axis axis = createTestAxis("Mainland", state);
            Area area = createTestArea("Yaba", axis);

            mockMvc.perform(delete("/api/v1/admin/locations/areas/{id}", area.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk());

            assertThat(areaRepository.findById(area.getId())).isEmpty();
            // Axis and State should remain
            assertThat(axisRepository.findById(axis.getId())).isPresent();
            assertThat(stateRepository.findById(state.getId())).isPresent();
        }

        @Test
        @DisplayName("Delete non-existent area - returns 404")
        void deleteNonExistentArea_ReturnsNotFound() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/locations/areas/{id}", UUID.randomUUID())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== AUTHENTICATION TESTS ====================

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Unauthenticated request - returns 401")
        void unauthenticatedRequest_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/admin/locations/states"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Non-admin cannot access any admin location endpoint")
        void nonAdminCannotAccessAdminEndpoints() throws Exception {
            String userToken = registerAndGetToken("user@test.com", "password123");

            mockMvc.perform(get("/api/v1/admin/locations/states")
                            .header("Authorization", authHeader(userToken)))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== PUBLIC LOOKUP TESTS ====================

    @Nested
    @DisplayName("Public Lookup Tests")
    class PublicLookupTests {

        @Test
        @DisplayName("Public can access states lookup")
        void publicCanAccessStatesLookup() throws Exception {
            createTestState("Lagos");
            createTestState("Abuja");

            mockMvc.perform(get("/api/v1/lookup/states"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("Public can access axes lookup by state")
        void publicCanAccessAxesLookup() throws Exception {
            State state = createTestState("Lagos");
            createTestAxis("Mainland", state);
            createTestAxis("Island", state);

            mockMvc.perform(get("/api/v1/lookup/states/{stateId}/axes", state.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("Public can access areas lookup by axis")
        void publicCanAccessAreasLookup() throws Exception {
            State state = createTestState("Lagos");
            Axis axis = createTestAxis("Mainland", state);
            createTestArea("Yaba", axis);

            mockMvc.perform(get("/api/v1/lookup/axes/{axisId}/areas", axis.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1));
        }
    }
}
