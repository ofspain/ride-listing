package com.ridelist.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ridelist.dto.request.AttributeCreateRequest;
import com.ridelist.dto.request.AttributeUpdateRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.AttributeDefinitionResponse;
import com.ridelist.model.AttributeDefinition;
import com.ridelist.model.ListingType;
import com.ridelist.model.Role;
import com.ridelist.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AdminAttributeController.
 * Tests admin CRUD operations for dynamic attributes.
 *
 * Test IDs: ATTR-001 to ATTR-021
 */
@DisplayName("AdminAttributeController Integration Tests")
class AdminAttributeControllerIntegrationTest extends BaseIntegrationTest {

    private String adminToken;

    @BeforeEach
    void setupAdmin() throws Exception {
        // Register and promote to admin
        adminToken = registerAndGetToken("admin@test.com", "password123");
        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        adminToken = loginAndGetToken("admin@test.com", "password123");
    }

    // ==================== CREATE ATTRIBUTE TESTS ====================

    @Nested
    @DisplayName("Create Attribute Tests")
    class CreateAttributeTests {

        @Test
        @DisplayName("ATTR-001: Admin creates attribute - returns 201 Created")
        void adminCreatesAttribute_ReturnsCreated() throws Exception {
            AttributeCreateRequest request = AttributeCreateRequest.builder()
                    .name("Engine Type")
                    .listingType(ListingType.VEHICLE)
                    .filterable(true)
                    .required(false)
                    .build();

            MvcResult result = mockMvc.perform(post("/api/v1/admin/attributes")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Engine Type"))
                    .andExpect(jsonPath("$.data.slug").value("engine-type"))
                    .andExpect(jsonPath("$.data.listingType").value("VEHICLE"))
                    .andExpect(jsonPath("$.data.filterable").value(true))
                    .andExpect(jsonPath("$.data.required").value(false))
                    .andExpect(jsonPath("$.data.active").value(true))
                    .andReturn();

            ApiResponse<AttributeDefinitionResponse> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData().getId()).isNotNull();
        }

        @Test
        @DisplayName("ATTR-001b: Create attribute for PART listing type")
        void createAttributeForPart_ReturnsCreated() throws Exception {
            AttributeCreateRequest request = AttributeCreateRequest.builder()
                    .name("Compatibility")
                    .listingType(ListingType.PART)
                    .filterable(true)
                    .required(true)
                    .build();

            mockMvc.perform(post("/api/v1/admin/attributes")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.listingType").value("PART"))
                    .andExpect(jsonPath("$.data.required").value(true));
        }

        @Test
        @DisplayName("ATTR-002: Create duplicate attribute - returns 409 Conflict")
        void createDuplicateAttribute_ReturnsConflict() throws Exception {
            // First create
            AttributeCreateRequest request = AttributeCreateRequest.builder()
                    .name("Engine Type")
                    .listingType(ListingType.VEHICLE)
                    .build();

            mockMvc.perform(post("/api/v1/admin/attributes")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            // Second create with same name (same slug)
            mockMvc.perform(post("/api/v1/admin/attributes")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("Create attribute without name - returns 400 Bad Request")
        void createAttributeWithoutName_ReturnsBadRequest() throws Exception {
            AttributeCreateRequest request = AttributeCreateRequest.builder()
                    .listingType(ListingType.VEHICLE)
                    .build();

            mockMvc.perform(post("/api/v1/admin/attributes")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Create attribute without listing type - returns 400 Bad Request")
        void createAttributeWithoutListingType_ReturnsBadRequest() throws Exception {
            String json = "{\"name\":\"Test Attribute\"}";

            mockMvc.perform(post("/api/v1/admin/attributes")
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Non-admin cannot create attribute - returns 403 Forbidden")
        void nonAdminCannotCreateAttribute_ReturnsForbidden() throws Exception {
            String userToken = registerAndGetToken("user@test.com", "password123");

            AttributeCreateRequest request = AttributeCreateRequest.builder()
                    .name("Engine Type")
                    .listingType(ListingType.VEHICLE)
                    .build();

            mockMvc.perform(post("/api/v1/admin/attributes")
                            .header("Authorization", authHeader(userToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== UPDATE ATTRIBUTE TESTS ====================

    @Nested
    @DisplayName("Update Attribute Tests")
    class UpdateAttributeTests {

        @Test
        @DisplayName("ATTR-003: Update attribute (deactivate) - returns 200 OK")
        void updateAttributeDeactivate_ReturnsOk() throws Exception {
            // Create attribute first
            AttributeDefinition attr = createTestAttribute("Engine Type", ListingType.VEHICLE, true);

            AttributeUpdateRequest request = AttributeUpdateRequest.builder()
                    .active(false)
                    .build();

            mockMvc.perform(put("/api/v1/admin/attributes/{id}", attr.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.active").value(false));
        }

        @Test
        @DisplayName("Update attribute name - generates new slug")
        void updateAttributeName_GeneratesNewSlug() throws Exception {
            AttributeDefinition attr = createTestAttribute("Engine Type", ListingType.VEHICLE, true);

            AttributeUpdateRequest request = AttributeUpdateRequest.builder()
                    .name("Motor Type")
                    .build();

            mockMvc.perform(put("/api/v1/admin/attributes/{id}", attr.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Motor Type"))
                    .andExpect(jsonPath("$.data.slug").value("motor-type"));
        }

        @Test
        @DisplayName("Update attribute to duplicate slug - returns 409 Conflict")
        void updateAttributeToDuplicateSlug_ReturnsConflict() throws Exception {
            createTestAttribute("Engine Type", ListingType.VEHICLE, true);
            AttributeDefinition attr2 = createTestAttribute("Fuel Type", ListingType.VEHICLE, true);

            // Try to rename "Fuel Type" to "Engine Type"
            AttributeUpdateRequest request = AttributeUpdateRequest.builder()
                    .name("Engine Type")
                    .build();

            mockMvc.perform(put("/api/v1/admin/attributes/{id}", attr2.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Update non-existent attribute - returns 404 Not Found")
        void updateNonExistentAttribute_ReturnsNotFound() throws Exception {
            AttributeUpdateRequest request = AttributeUpdateRequest.builder()
                    .active(false)
                    .build();

            mockMvc.perform(put("/api/v1/admin/attributes/{id}", java.util.UUID.randomUUID())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Update filterable and required flags")
        void updateFilterableAndRequired_ReturnsOk() throws Exception {
            AttributeDefinition attr = createTestAttribute("Engine Type", ListingType.VEHICLE, true);

            AttributeUpdateRequest request = AttributeUpdateRequest.builder()
                    .filterable(false)
                    .required(true)
                    .build();

            mockMvc.perform(put("/api/v1/admin/attributes/{id}", attr.getId())
                            .header("Authorization", authHeader(adminToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.filterable").value(false))
                    .andExpect(jsonPath("$.data.required").value(true));
        }
    }

    // ==================== GET ATTRIBUTES TESTS ====================

    @Nested
    @DisplayName("Get Attributes Tests")
    class GetAttributesTests {

        @Test
        @DisplayName("ATTR-004: Get attributes by listing type - returns filtered list")
        void getAttributesByListingType_ReturnsFilteredList() throws Exception {
            // Create attributes for different types
            createTestAttribute("Engine Type", ListingType.VEHICLE, true);
            createTestAttribute("Fuel Type", ListingType.VEHICLE, true);
            createTestAttribute("Compatibility", ListingType.PART, true);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/attributes")
                            .header("Authorization", authHeader(adminToken))
                            .param("listingType", "VEHICLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andReturn();

            ApiResponse<List<AttributeDefinitionResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData()).hasSize(2);
            assertThat(response.getData()).allMatch(a -> a.getListingType() == ListingType.VEHICLE);
        }

        @Test
        @DisplayName("Get all attributes without filter")
        void getAllAttributes_ReturnsAll() throws Exception {
            createTestAttribute("Engine Type", ListingType.VEHICLE, true);
            createTestAttribute("Fuel Type", ListingType.VEHICLE, true);
            createTestAttribute("Compatibility", ListingType.PART, true);

            MvcResult result = mockMvc.perform(get("/api/v1/admin/attributes")
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andReturn();

            ApiResponse<List<AttributeDefinitionResponse>> response = objectMapper.readValue(
                    result.getResponse().getContentAsString(),
                    new TypeReference<>() {});

            assertThat(response.getData()).hasSize(3);
        }

        @Test
        @DisplayName("Get attribute by ID")
        void getAttributeById_ReturnsAttribute() throws Exception {
            AttributeDefinition attr = createTestAttribute("Engine Type", ListingType.VEHICLE, true);

            mockMvc.perform(get("/api/v1/admin/attributes/{id}", attr.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(attr.getId().toString()))
                    .andExpect(jsonPath("$.data.name").value("Engine Type"));
        }

        @Test
        @DisplayName("Get non-existent attribute - returns 404")
        void getNonExistentAttribute_ReturnsNotFound() throws Exception {
            mockMvc.perform(get("/api/v1/admin/attributes/{id}", java.util.UUID.randomUUID())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== DELETE ATTRIBUTE TESTS ====================

    @Nested
    @DisplayName("Delete Attribute Tests")
    class DeleteAttributeTests {

        @Test
        @DisplayName("ATTR-005: Delete attribute - returns 204 No Content")
        void deleteAttribute_ReturnsNoContent() throws Exception {
            AttributeDefinition attr = createTestAttribute("Engine Type", ListingType.VEHICLE, true);

            mockMvc.perform(delete("/api/v1/admin/attributes/{id}", attr.getId())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isNoContent());

            // Verify deleted
            assertThat(attributeRepository.findById(attr.getId())).isEmpty();
        }

        @Test
        @DisplayName("Delete non-existent attribute - returns 404 Not Found")
        void deleteNonExistentAttribute_ReturnsNotFound() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/attributes/{id}", java.util.UUID.randomUUID())
                            .header("Authorization", authHeader(adminToken)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Non-admin cannot delete attribute - returns 403 Forbidden")
        void nonAdminCannotDeleteAttribute_ReturnsForbidden() throws Exception {
            AttributeDefinition attr = createTestAttribute("Engine Type", ListingType.VEHICLE, true);
            String userToken = registerAndGetToken("user@test.com", "password123");

            mockMvc.perform(delete("/api/v1/admin/attributes/{id}", attr.getId())
                            .header("Authorization", authHeader(userToken)))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== PUBLIC ATTRIBUTE ENDPOINT TESTS ====================

    @Nested
    @DisplayName("Public Attribute Endpoint Tests")
    class PublicAttributeTests {

        @Test
        @DisplayName("Public can get active attributes by listing type")
        void publicCanGetActiveAttributes() throws Exception {
            // Create mix of active and inactive
            createTestAttribute("Engine Type", ListingType.VEHICLE, true);
            AttributeDefinition inactive = createTestAttribute("Old Attr", ListingType.VEHICLE, true);
            inactive.setActive(false);
            attributeRepository.save(inactive);

            // Public endpoint - no auth required
            mockMvc.perform(get("/api/v1/attributes")
                            .param("listingType", "VEHICLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Engine Type"));
        }

        @Test
        @DisplayName("Public can get filterable attributes")
        void publicCanGetFilterableAttributes() throws Exception {
            createTestAttribute("Engine Type", ListingType.VEHICLE, true); // filterable
            AttributeDefinition notFilterable = createTestAttribute("Internal ID", ListingType.VEHICLE, false);

            mockMvc.perform(get("/api/v1/attributes/filterable")
                            .param("listingType", "VEHICLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].filterable").value(true));
        }
    }

    // ==================== AUTHENTICATION TESTS ====================

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Unauthenticated request to admin endpoint - returns 401")
        void unauthenticatedRequest_ReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/admin/attributes"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Non-admin cannot access admin attributes endpoint")
        void nonAdminCannotAccessAdminEndpoint() throws Exception {
            String userToken = registerAndGetToken("user@test.com", "password123");

            mockMvc.perform(get("/api/v1/admin/attributes")
                            .header("Authorization", authHeader(userToken)))
                    .andExpect(status().isForbidden());
        }
    }
}
