package com.ridelist.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ridelist.dto.request.CreateMakeRequest;
import com.ridelist.dto.request.CreateModelYearRequest;
import com.ridelist.dto.request.CreateVehicleModelRequest;
import com.ridelist.dto.request.UpdateCategorizationRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.MakeResponse;
import com.ridelist.dto.response.ModelYearResponse;
import com.ridelist.dto.response.VehicleModelResponse;
import com.ridelist.model.Make;
import com.ridelist.model.ModelYear;
import com.ridelist.model.Role;
import com.ridelist.model.User;
import com.ridelist.model.VehicleModel;
import com.ridelist.repository.VehicleModelRepository;
import com.ridelist.repository.ModelYearRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for GAP 3: Admin categorization endpoints.
 *
 * Tests CRUD operations for Make -> VehicleModel -> ModelYear hierarchy.
 */
@DisplayName("AdminCategorizationController Integration Tests (GAP 3)")
class AdminCategorizationControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private VehicleModelRepository vehicleModelRepository;

    @Autowired
    private ModelYearRepository modelYearRepository;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setupUsers() throws Exception {
        adminToken = registerAndGetToken("admin@test.com", "password123");
        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        adminToken = loginAndGetToken("admin@test.com", "password123");

        userToken = registerAndGetToken("user@test.com", "password123");
    }

    // ==================== MAKE CRUD TESTS ====================

    @Nested
    @DisplayName("Make CRUD Tests")
    class MakeCrudTests {

        @Test
        @DisplayName("CAT-001: Admin creates make - returns 201")
        void adminCreatesMake_returnsCreated() throws Exception {
            CreateMakeRequest request = CreateMakeRequest.builder()
                    .name("Honda")
                    .build();

            mockMvc.perform(post("/api/v1/admin/categorization/makes")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Honda"))
                    .andExpect(jsonPath("$.data.slug").value("honda"));
        }

        @Test
        @DisplayName("CAT-002: Admin creates duplicate make - returns 409")
        void adminCreatesDuplicateMake_returnsConflict() throws Exception {
            createTestMake("TVS");

            CreateMakeRequest request = CreateMakeRequest.builder()
                    .name("TVS")
                    .build();

            mockMvc.perform(post("/api/v1/admin/categorization/makes")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("CAT-003: Non-admin cannot create make - returns 403")
        void nonAdminCreatesMake_returnsForbidden() throws Exception {
            CreateMakeRequest request = CreateMakeRequest.builder()
                    .name("Bajaj")
                    .build();

            mockMvc.perform(post("/api/v1/admin/categorization/makes")
                            .header("Authorization", authHeader(userToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("CAT-004: Admin updates make - returns 200")
        void adminUpdatesMake_returnsOk() throws Exception {
            Make make = createTestMake("OldName");

            UpdateCategorizationRequest request = UpdateCategorizationRequest.builder()
                    .name("NewName")
                    .build();

            mockMvc.perform(put("/api/v1/admin/categorization/makes/" + make.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("NewName"))
                    .andExpect(jsonPath("$.data.slug").value("newname"));
        }

        @Test
        @DisplayName("CAT-005: Admin deletes make - returns 200")
        void adminDeletesMake_returnsOk() throws Exception {
            Make make = createTestMake("ToDelete");

            mockMvc.perform(delete("/api/v1/admin/categorization/makes/" + make.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk());

            assertThat(makeRepository.findById(make.getId())).isEmpty();
        }

        @Test
        @DisplayName("CAT-006: Delete make cascades to models and years")
        void deleteMake_cascadesToModelsAndYears() throws Exception {
            Make make = createTestMake("CascadeTest");
            VehicleModel model = createTestVehicleModel("Model1", make);
            ModelYear year = createTestModelYear("2024", model);

            mockMvc.perform(delete("/api/v1/admin/categorization/makes/" + make.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk());

            entityManager.flush();
            entityManager.clear();
            assertThat(vehicleModelRepository.findById(model.getId())).isEmpty();
            assertThat(modelYearRepository.findById(year.getId())).isEmpty();
        }

        @Test
        @DisplayName("CAT-007: Get all makes - returns list")
        void getAllMakes_returnsList() throws Exception {
            createTestMake("Honda");
            createTestMake("Bajaj");

            mockMvc.perform(get("/api/v1/admin/categorization/makes")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("CAT-008: Delete non-existent make - returns 404")
        void deleteNonExistentMake_returnsNotFound() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/categorization/makes/" + UUID.randomUUID())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== VEHICLE MODEL CRUD TESTS ====================

    @Nested
    @DisplayName("VehicleModel CRUD Tests")
    class VehicleModelCrudTests {

        @Test
        @DisplayName("CAT-010: Admin creates vehicle model - returns 201")
        void adminCreatesVehicleModel_returnsCreated() throws Exception {
            Make make = createTestMake("Honda");

            CreateVehicleModelRequest request = CreateVehicleModelRequest.builder()
                    .name("CBR 650R")
                    .makeId(make.getId())
                    .build();

            mockMvc.perform(post("/api/v1/admin/categorization/models")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name").value("CBR 650R"))
                    .andExpect(jsonPath("$.data.slug").value("cbr-650r"))
                    .andExpect(jsonPath("$.data.make.id").value(make.getId().toString()));
        }

        @Test
        @DisplayName("CAT-011: Create model with invalid makeId - returns 404")
        void createModelWithInvalidMakeId_returnsNotFound() throws Exception {
            CreateVehicleModelRequest request = CreateVehicleModelRequest.builder()
                    .name("SomeModel")
                    .makeId(UUID.randomUUID())
                    .build();

            mockMvc.perform(post("/api/v1/admin/categorization/models")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("CAT-012: Admin updates vehicle model - returns 200")
        void adminUpdatesVehicleModel_returnsOk() throws Exception {
            Make make = createTestMake("Honda");
            VehicleModel model = createTestVehicleModel("OldModel", make);

            UpdateCategorizationRequest request = UpdateCategorizationRequest.builder()
                    .name("UpdatedModel")
                    .build();

            mockMvc.perform(put("/api/v1/admin/categorization/models/" + model.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("UpdatedModel"));
        }

        @Test
        @DisplayName("CAT-013: Admin deletes vehicle model - cascades to years")
        void adminDeletesVehicleModel_cascadesToYears() throws Exception {
            Make make = createTestMake("Honda");
            VehicleModel model = createTestVehicleModel("CBR", make);
            ModelYear year = createTestModelYear("2024", model);

            mockMvc.perform(delete("/api/v1/admin/categorization/models/" + model.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk());

            entityManager.flush();
            entityManager.clear();
            assertThat(vehicleModelRepository.findById(model.getId())).isEmpty();
            assertThat(modelYearRepository.findById(year.getId())).isEmpty();
        }

        @Test
        @DisplayName("CAT-014: Get models by make - returns filtered list")
        void getModelsByMake_returnsFilteredList() throws Exception {
            Make honda = createTestMake("Honda");
            Make bajaj = createTestMake("Bajaj");
            createTestVehicleModel("CBR", honda);
            createTestVehicleModel("Activa", honda);
            createTestVehicleModel("Pulsar", bajaj);

            mockMvc.perform(get("/api/v1/admin/categorization/makes/" + honda.getId() + "/models")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("CAT-015: Get models for non-existent make - returns 404")
        void getModelsForNonExistentMake_returnsNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/admin/categorization/makes/" + UUID.randomUUID() + "/models")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== MODEL YEAR CRUD TESTS ====================

    @Nested
    @DisplayName("ModelYear CRUD Tests")
    class ModelYearCrudTests {

        @Test
        @DisplayName("CAT-020: Admin creates model year - returns 201")
        void adminCreatesModelYear_returnsCreated() throws Exception {
            Make make = createTestMake("Honda");
            VehicleModel model = createTestVehicleModel("CBR 650R", make);

            CreateModelYearRequest request = CreateModelYearRequest.builder()
                    .name("2024")
                    .vehicleModelId(model.getId())
                    .build();

            mockMvc.perform(post("/api/v1/admin/categorization/years")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name").value("2024"))
                    .andExpect(jsonPath("$.data.slug").value("2024"))
                    .andExpect(jsonPath("$.data.vehicleModel.id").value(model.getId().toString()));
        }

        @Test
        @DisplayName("CAT-021: Create year with invalid vehicleModelId - returns 404")
        void createYearWithInvalidVehicleModelId_returnsNotFound() throws Exception {
            CreateModelYearRequest request = CreateModelYearRequest.builder()
                    .name("2024")
                    .vehicleModelId(UUID.randomUUID())
                    .build();

            mockMvc.perform(post("/api/v1/admin/categorization/years")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("CAT-022: Admin updates model year - returns 200")
        void adminUpdatesModelYear_returnsOk() throws Exception {
            Make make = createTestMake("Honda");
            VehicleModel model = createTestVehicleModel("CBR", make);
            ModelYear year = createTestModelYear("2023", model);

            UpdateCategorizationRequest request = UpdateCategorizationRequest.builder()
                    .name("2024")
                    .build();

            mockMvc.perform(put("/api/v1/admin/categorization/years/" + year.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("2024"));
        }

        @Test
        @DisplayName("CAT-023: Admin deletes model year - returns 200")
        void adminDeletesModelYear_returnsOk() throws Exception {
            Make make = createTestMake("Honda");
            VehicleModel model = createTestVehicleModel("CBR", make);
            ModelYear year = createTestModelYear("2024", model);

            mockMvc.perform(delete("/api/v1/admin/categorization/years/" + year.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk());

            assertThat(modelYearRepository.findById(year.getId())).isEmpty();
        }

        @Test
        @DisplayName("CAT-024: Get years by model - returns filtered list")
        void getYearsByModel_returnsFilteredList() throws Exception {
            Make make = createTestMake("Honda");
            VehicleModel cbr = createTestVehicleModel("CBR", make);
            VehicleModel activa = createTestVehicleModel("Activa", make);
            createTestModelYear("2023", cbr);
            createTestModelYear("2024", cbr);
            createTestModelYear("2025", activa);

            mockMvc.perform(get("/api/v1/admin/categorization/models/" + cbr.getId() + "/years")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2));
        }

        @Test
        @DisplayName("CAT-025: Get years for non-existent model - returns 404")
        void getYearsForNonExistentModel_returnsNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/admin/categorization/models/" + UUID.randomUUID() + "/years")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== CACHE INVALIDATION TESTS ====================

    @Nested
    @DisplayName("Cache Invalidation Tests")
    class CacheInvalidationTests {

        @Test
        @DisplayName("CAT-030: Create make invalidates cache")
        void createMake_invalidatesCache() throws Exception {
            mockMvc.perform(get("/api/v1/lookup/makes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(0));

            CreateMakeRequest request = CreateMakeRequest.builder()
                    .name("NewMake")
                    .build();

            mockMvc.perform(post("/api/v1/admin/categorization/makes")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            mockMvc.perform(get("/api/v1/lookup/makes"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1));
        }
    }

    // ==================== HELPER METHODS ====================

    private Make createTestMake(String name) {
        Make make = Make.builder()
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .build();
        return makeRepository.save(make);
    }

    private VehicleModel createTestVehicleModel(String name, Make make) {
        VehicleModel model = VehicleModel.builder()
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .make(make)
                .build();
        return vehicleModelRepository.save(model);
    }

    private ModelYear createTestModelYear(String name, VehicleModel model) {
        ModelYear year = ModelYear.builder()
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .vehicleModel(model)
                .build();
        return modelYearRepository.save(year);
    }
}
