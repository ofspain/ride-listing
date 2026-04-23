package com.ridelist.integration;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelist.dto.request.LoginRequest;
import com.ridelist.dto.request.RegisterRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.AuthResponse;
import com.ridelist.model.*;
import com.ridelist.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
public abstract class BaseIntegrationTest {

    static {
        System.setProperty("docker.host", "npipe:////./pipe/docker_engine");
        System.setProperty(
                "org.testcontainers.dockerclient.strategy",
                "org.testcontainers.dockerclient.NpipeSocketClientProviderStrategy"
        );
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("ridelist_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ListingRepository listingRepository;

    @Autowired
    protected StateRepository stateRepository;

    @Autowired
    protected AxisRepository axisRepository;

    @Autowired
    protected AreaRepository areaRepository;

    @Autowired
    protected MakeRepository makeRepository;

    @Autowired
    protected AttributeDefinitionRepository attributeRepository;

    @Autowired
    protected FavoriteRepository favoriteRepository;

    @Autowired
    protected ContactRequestRepository contactRequestRepository;

    @Autowired
    protected EntityManager entityManager;

    // ==================== HELPER METHODS ====================

    protected String registerAndGetToken(String email, String password) throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email(email)
                .password(password)
                .firstName("Test")
                .lastName("User")
                .phoneNumber("08012345678")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<AuthResponse> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<ApiResponse<AuthResponse>>() {});
        return response.getData().getAccessToken();
    }

    protected String loginAndGetToken(String email, String password) throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password(password)
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        ApiResponse<AuthResponse> response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<ApiResponse<AuthResponse>>() {});
        return response.getData().getAccessToken();
    }

    protected User createTestUser(String email, Role role) {
        User user = User.builder()
                .email(email)
                .password("$2a$10$encrypted")  // BCrypt placeholder
                .firstName("Test")
                .lastName("User")
                .phoneNumber("08012345678")
                .role(role)
                .accountType(AccountType.INDIVIDUAL)
                .build();
        return userRepository.save(user);
    }

    protected Listing createTestListing(User seller, ListingType type) {
        Listing listing = Listing.builder()
                .title("Test Listing")
                .description("Test Description")
                .price(BigDecimal.valueOf(150000))
                .seller(seller)
                .listingType(type)
                .status(ListingStatus.DRAFT)
                .condition(ListingCondition.GOOD)
                .build();

        if (type == ListingType.VEHICLE) {
            listing.setVehicleType(VehicleType.MOTORCYCLE);
            listing.setCategory(ListingCategory.MOTORCYCLE);
        } else {
            listing.setPartName("Test Part");
            listing.setCategory(ListingCategory.SPARE_PART);
        }

        return listingRepository.save(listing);
    }

    protected State createTestState(String name) {
        State state = State.builder()
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .build();
        return stateRepository.save(state);
    }

    protected Axis createTestAxis(String name, State state) {
        Axis axis = Axis.builder()
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .state(state)
                .build();
        return axisRepository.save(axis);
    }

    protected Area createTestArea(String name, Axis axis) {
        Area area = Area.builder()
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .axis(axis)
                .build();
        return areaRepository.save(area);
    }

    protected AttributeDefinition createTestAttribute(String name, ListingType type, boolean filterable) {
        AttributeDefinition attr = AttributeDefinition.builder()
                .name(name)
                .slug(name.toLowerCase().replace(" ", "-"))
                .listingType(type)
                .filterable(filterable)
                .required(false)
                .active(true)
                .build();
        return attributeRepository.save(attr);
    }

    protected String authHeader(String token) {
        return "Bearer " + token;
    }

    protected Favorite createTestFavorite(User user, Listing listing) {
        return favoriteRepository.save(Favorite.builder()
                .user(user)
                .listing(listing)
                .build());
    }

    protected ContactRequest createTestInquiry(Listing listing, User buyer, String message) {
        return contactRequestRepository.save(ContactRequest.builder()
                .listing(listing)
                .buyer(buyer)
                .message(message)
                .build());
    }
}
