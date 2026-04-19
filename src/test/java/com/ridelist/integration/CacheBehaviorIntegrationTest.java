package com.ridelist.integration;

import com.ridelist.cache.InMemoryCache;
import com.ridelist.dto.response.SimpleNode;
import com.ridelist.model.Role;
import com.ridelist.model.State;
import com.ridelist.model.User;
import com.ridelist.service.LocationCacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for cache behavior.
 * Tests the InMemoryCache and LocationCacheService interaction.
 *
 * Test IDs: CACHE-001 to CACHE-004
 */
@DisplayName("Cache Behavior Integration Tests")
class CacheBehaviorIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private InMemoryCache cache;

    @Autowired
    private LocationCacheService locationCacheService;

    @BeforeEach
    void clearCache() {
        cache.evictAll();
    }

    @Test
    @DisplayName("CACHE-001: First request hits database and populates cache")
    void getStates_firstCall_hitsDatabaseAndCaches() {
        // Given: states exist in database
        createTestState("Lagos");
        createTestState("Abuja");

        // Verify cache is empty
        assertThat(cache.containsKey("states:all")).isFalse();

        // When: first call to get states
        List<SimpleNode> states = locationCacheService.getStates();

        // Then: results returned and cache populated
        assertThat(states).hasSize(2);
        assertThat(states).extracting(SimpleNode::getName)
                .containsExactlyInAnyOrder("Lagos", "Abuja");
        assertThat(cache.containsKey("states:all")).isTrue();
    }

    @Test
    @DisplayName("CACHE-002: Second request uses cache, doesn't see DB changes")
    void getStates_secondCall_usesCache() {
        // Given: states cached from first call
        createTestState("Lagos");
        locationCacheService.getStates(); // Populate cache
        assertThat(cache.containsKey("states:all")).isTrue();

        // When: add new state directly to DB (bypassing service/cache eviction)
        State newState = State.builder()
                .name("Kano")
                .slug("kano")
                .build();
        stateRepository.save(newState);

        // Then: cached result doesn't include new state (proves cache hit)
        List<SimpleNode> states = locationCacheService.getStates();
        assertThat(states).hasSize(1);
        assertThat(states.get(0).getName()).isEqualTo("Lagos");
        // Kano exists in DB but not in cached result
    }

    @Test
    @DisplayName("CACHE-003: Admin update via API evicts cache")
    void adminCreateState_evictsCache() throws Exception {
        // Given: states cached
        createTestState("Lagos");
        locationCacheService.getStates();
        assertThat(cache.containsKey("states:all")).isTrue();

        // Create admin user - register first, then update role
        String adminToken = registerAndGetToken("admin@test.com", "password123");
        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        // Need to get a fresh token after role update
        adminToken = loginAndGetToken("admin@test.com", "password123");

        // When: admin creates new state via API
        mockMvc.perform(post("/api/v1/admin/locations/states")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Abuja\"}"))
                .andExpect(status().isCreated());

        // Then: cache evicted
        assertThat(cache.containsKey("states:all")).isFalse();
    }

    @Test
    @DisplayName("CACHE-004: Request after eviction fetches fresh data from DB")
    void getStates_afterEviction_fetchesFreshData() throws Exception {
        // Given: states cached
        createTestState("Lagos");
        locationCacheService.getStates();
        assertThat(cache.containsKey("states:all")).isTrue();
        assertThat(locationCacheService.getStates()).hasSize(1);

        // Create admin user
        String adminToken = registerAndGetToken("admin@test.com", "password123");
        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        adminToken = loginAndGetToken("admin@test.com", "password123");

        // When: admin creates new state (triggers eviction)
        mockMvc.perform(post("/api/v1/admin/locations/states")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Abuja\"}"))
                .andExpect(status().isCreated());

        // Then: subsequent call fetches fresh data including new state
        List<SimpleNode> states = locationCacheService.getStates();
        assertThat(states).hasSize(2);
        assertThat(states).extracting(SimpleNode::getName)
                .containsExactlyInAnyOrder("Lagos", "Abuja");
        assertThat(cache.containsKey("states:all")).isTrue();
    }

    @Test
    @DisplayName("Cache eviction on axis create")
    void adminCreateAxis_evictsCache() throws Exception {
        // Given: state and axes cached
        State state = createTestState("Lagos");
        locationCacheService.getAxesByState(state.getId());
        String axesCacheKey = "state:" + state.getId() + ":axes";
        assertThat(cache.containsKey(axesCacheKey)).isTrue();

        // Create admin user
        String adminToken = registerAndGetToken("admin@test.com", "password123");
        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        adminToken = loginAndGetToken("admin@test.com", "password123");

        // When: admin creates new axis via API
        mockMvc.perform(post("/api/v1/admin/locations/axes")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mainland\",\"stateId\":\"" + state.getId() + "\"}"))
                .andExpect(status().isCreated());

        // Then: cache evicted (evictAll is called)
        assertThat(cache.containsKey(axesCacheKey)).isFalse();
    }

    @Test
    @DisplayName("Cache eviction on area create")
    void adminCreateArea_evictsCache() throws Exception {
        // Given: axis and areas cached
        State state = createTestState("Lagos");
        var axis = createTestAxis("Mainland", state);
        locationCacheService.getAreasByAxis(axis.getId());
        String areasCacheKey = "axis:" + axis.getId() + ":areas";
        assertThat(cache.containsKey(areasCacheKey)).isTrue();

        // Create admin user
        String adminToken = registerAndGetToken("admin@test.com", "password123");
        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        adminToken = loginAndGetToken("admin@test.com", "password123");

        // When: admin creates new area via API
        mockMvc.perform(post("/api/v1/admin/locations/areas")
                        .header("Authorization", authHeader(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Yaba\",\"axisId\":\"" + axis.getId() + "\"}"))
                .andExpect(status().isCreated());

        // Then: cache evicted
        assertThat(cache.containsKey(areasCacheKey)).isFalse();
    }

    @Test
    @DisplayName("Cache eviction on delete operations")
    void adminDeleteState_evictsCache() throws Exception {
        // Given: state cached
        State state = createTestState("Lagos");
        locationCacheService.getStates();
        assertThat(cache.containsKey("states:all")).isTrue();

        // Create admin user
        String adminToken = registerAndGetToken("admin@test.com", "password123");
        User admin = userRepository.findByEmail("admin@test.com").orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        adminToken = loginAndGetToken("admin@test.com", "password123");

        // When: admin deletes state via API
        mockMvc.perform(delete("/api/v1/admin/locations/states/{id}", state.getId())
                        .header("Authorization", authHeader(adminToken)))
                .andExpect(status().isOk());

        // Then: cache evicted
        assertThat(cache.containsKey("states:all")).isFalse();

        // And: subsequent call returns empty
        List<SimpleNode> states = locationCacheService.getStates();
        assertThat(states).isEmpty();
    }

    @Test
    @DisplayName("Non-admin cannot trigger cache eviction via admin endpoints")
    void nonAdminCannotAccessAdminEndpoints() throws Exception {
        // Given: a regular user
        String userToken = registerAndGetToken("user@test.com", "password123");

        // When & Then: trying to access admin endpoint returns 403
        mockMvc.perform(post("/api/v1/admin/locations/states")
                        .header("Authorization", authHeader(userToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Lagos\"}"))
                .andExpect(status().isForbidden());
    }
}
