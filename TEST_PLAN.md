# RideList Integration Test Plan

## 1. Overview

This document outlines the integration test strategy for RideList MVP - a marketplace for motorcycles, tricycles, bicycles, spare parts, and accessories targeting the Nigerian market.

**Objective:** Ensure the system is stable, correct, and resilient enough to support the first 100 active seller users.

**Scope:**
- End-to-end API flows
- Real PostgreSQL database behavior
- Data integrity and constraints
- Security enforcement
- Cache behavior validation

**Out of Scope:**
- Unit tests
- UI/frontend tests
- Full load/stress testing
- External AWS S3 calls (mocked)

---

## 2. Test Environment Setup

### 2.1 Technology Stack

| Component | Technology |
|-----------|------------|
| Test Framework | JUnit 5 + Spring Boot Test |
| Database | PostgreSQL 15 via Testcontainers |
| HTTP Client | MockMvc |
| S3 Mocking | Mock bean / LocalStack |
| Assertions | AssertJ |
| JSON | Jackson ObjectMapper |

### 2.2 Dependencies (pom.xml)

```xml
<dependencies>
    <!-- Test Framework -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Testcontainers (2.x core for Docker socket compatibility, 1.x modules) -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>2.0.4</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <version>1.21.4</version>
        <scope>test</scope>
        <exclusions>
            <exclusion>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>1.21.4</version>
        <scope>test</scope>
        <exclusions>
            <exclusion>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers</artifactId>
            </exclusion>
        </exclusions>
    </dependency>
    
    <!-- Security Test -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

**Note:** We use testcontainers 2.x core for better Docker socket compatibility on Windows (npipe), while the postgresql and junit-jupiter modules remain at 1.x with exclusions to avoid version conflicts.

### 2.3 Test Configuration (application-test.properties)

```properties
# Database - Testcontainers PostgreSQL (properties overridden by @DynamicPropertySource in BaseIntegrationTest)
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Flyway
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration

# JWT Config (test secret)
jwt.secret=test-secret-key-that-is-at-least-256-bits-long-for-testing-purposes
jwt.expiration=3600000
jwt.refresh-expiration=86400000

# AWS S3 (mocked)
aws.access-key=test-access-key
aws.secret-key=test-secret-key
aws.region=us-east-1
aws.s3.bucket=test-bucket

# Image constraints
ridelist.image.min-count=1
ridelist.image.max-count=10
ridelist.image.allowed-types=image/jpeg,image/png,image/webp
ridelist.image.max-size-mb=5

# Logging
logging.level.org.testcontainers=INFO
logging.level.com.ridelist=DEBUG
```

**Note:** Database URL, username, and password are dynamically injected by `@DynamicPropertySource` in `BaseIntegrationTest` using the Testcontainers PostgreSQL container. We use the standard PostgreSQL driver (`org.postgresql.Driver`) instead of `ContainerDatabaseDriver` because the JDBC URL is a standard PostgreSQL URL provided by the container.

### 2.4 Base Test Class

```java
package com.ridelist.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelist.dto.request.LoginRequest;
import com.ridelist.dto.request.RegisterRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.AuthResponse;
import com.ridelist.model.*;
import com.ridelist.repository.*;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
public abstract class BaseIntegrationTest {

    // Windows Docker Desktop: Configure npipe socket for Testcontainers
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
        } else {
            listing.setPartName("Test Part");
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
}
```

**Key differences from initial plan:**
1. **Package:** Tests are in `com.ridelist.integration` package
2. **Docker socket config:** Static block sets Windows Docker Desktop npipe socket for Testcontainers 2.x
3. **Response parsing:** Uses `ApiResponse<AuthResponse>` wrapper with `TypeReference` for proper JSON deserialization
4. **RegisterRequest fields:** Uses `firstName`, `lastName`, `phoneNumber` (matching actual DTO)
5. **HTTP status codes:** Registration returns `201 Created` (not `200 OK`)

### 2.5 S3 Mock Configuration

```java
package com.ridelist.config;

import com.ridelist.service.S3Service;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@TestConfiguration
public class S3MockConfig {

    @Bean
    @Primary
    public S3Service s3Service() {
        S3Service mockS3Service = Mockito.mock(S3Service.class);
        
        // Default behavior: return predictable URL
        when(mockS3Service.uploadFile(any(MultipartFile.class), anyString()))
                .thenAnswer(invocation -> {
                    String folder = invocation.getArgument(1);
                    return "https://test-bucket.s3.us-east-1.amazonaws.com/" 
                           + folder + "/" + UUID.randomUUID() + ".jpg";
                });
        
        // Delete operations succeed silently
        Mockito.doNothing().when(mockS3Service).deleteFile(anyString());
        Mockito.doNothing().when(mockS3Service).deleteFileByUrl(anyString());
        
        return mockS3Service;
    }
}
```

---

## 3. Test Strategy

### 3.1 Test Package Structure

```
src/test/java/com/ridelist/
└── integration/
    ├── BaseIntegrationTest.java
    ├── AuthControllerIntegrationTest.java
    ├── ListingControllerIntegrationTest.java
    ├── FavoriteControllerIntegrationTest.java
    ├── MessageControllerIntegrationTest.java
    ├── AdminLocationControllerIntegrationTest.java
    ├── AdminAttributeControllerIntegrationTest.java
    ├── LookupControllerIntegrationTest.java
    ├── ImageServiceIntegrationTest.java
    ├── CacheBehaviorIntegrationTest.java
    ├── DataIntegrityIntegrationTest.java
    ├── PerformanceSanityTest.java
    ├── AccountDeletionIntegrationTest.java        # Delete account tests
    ├── AuthAfterDeletionIntegrationTest.java      # Auth blocking after deletion
    ├── ListingVisibilityAfterDeletionTest.java    # Listing filtering tests
    ├── DeleteAccountDataIntegrityTest.java        # Data integrity for deletion
    ├── AdminListingControllerIntegrationTest.java # Admin listing management (32 tests)
    └── ImpersonationIntegrationTest.java          # User impersonation (11 tests)
```

**Note:** All integration tests are placed in the `com.ridelist.integration` package for organization.

### 3.2 Naming Conventions

| Type | Convention | Example |
|------|------------|---------|
| Test Class | `{Feature}IntegrationTest` | `AuthControllerIntegrationTest` |
| Test Method | `{action}_{scenario}_{expectedResult}` | `register_validInput_returnsToken` |
| Fixtures | `create{Entity}()` | `createTestUser()` |

### 3.3 Test Isolation

- Each test class uses `@Transactional` for automatic rollback
- Testcontainers provides fresh PostgreSQL instance per test run
- S3 operations mocked to avoid external dependencies
- JWT tokens generated with test-specific secrets

---

## 4. Test Scenarios

### 4.1 Authentication & Security

**Test Class:** `AuthControllerIntegrationTest`

#### Registration Tests

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| AUTH-001 | Register with valid data | Valid RegisterRequest | 201 Created, JWT tokens returned |
| AUTH-002 | Register with duplicate email | Existing email | 409 Conflict |
| AUTH-003 | Register with invalid email format | "notanemail" | 400 Bad Request |
| AUTH-004 | Register with weak password | "123" | 400 Bad Request |
| AUTH-005 | Register with missing required fields | Empty fullName | 400 Bad Request |

```java
@Test
void register_validInput_returnsTokens() throws Exception {
    RegisterRequest request = RegisterRequest.builder()
            .email("newuser@test.com")
            .password("SecurePass123!")
            .firstName("New")
            .lastName("User")
            .phoneNumber("08012345678")
            .build();

    mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.refreshToken").exists())
            .andExpect(jsonPath("$.data.user.email").value("newuser@test.com"));
}

@Test
void register_duplicateEmail_returns409() throws Exception {
    // Given: user already exists
    createTestUser("existing@test.com", Role.USER);

    RegisterRequest request = RegisterRequest.builder()
            .email("existing@test.com")
            .password("SecurePass123!")
            .firstName("Another")
            .lastName("User")
            .phoneNumber("08012345679")
            .build();

    mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
}
```

#### Login Tests

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| AUTH-010 | Login with valid credentials | Correct email/password | 200 OK, JWT tokens |
| AUTH-011 | Login with wrong password | Invalid password | 401 Unauthorized |
| AUTH-012 | Login with non-existent user | Unknown email | 401 Unauthorized |
| AUTH-013 | Login with empty password | Empty string | 400 Bad Request |

```java
@Test
void login_validCredentials_returnsTokens() throws Exception {
    // Given: registered user
    String email = "user@test.com";
    String password = "SecurePass123!";
    registerAndGetToken(email, password);

    LoginRequest request = LoginRequest.builder()
            .email(email)
            .password(password)
            .build();

    mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists());
}

@Test
void login_wrongPassword_returns401() throws Exception {
    String email = "user@test.com";
    registerAndGetToken(email, "CorrectPassword123!");

    LoginRequest request = LoginRequest.builder()
            .email(email)
            .password("WrongPassword123!")
            .build();

    mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
}
```

#### JWT Validation Tests

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| AUTH-020 | Access protected endpoint with valid token | Valid JWT | 200 OK |
| AUTH-021 | Access protected endpoint without token | No Authorization header | 401 Unauthorized |
| AUTH-022 | Access protected endpoint with expired token | Expired JWT | 401 Unauthorized |
| AUTH-023 | Access protected endpoint with malformed token | "Bearer invalid" | 401 Unauthorized |

```java
@Test
void protectedEndpoint_validToken_returns200() throws Exception {
    String token = registerAndGetToken("user@test.com", "Password123!");

    mockMvc.perform(get("/api/v1/account/listings")
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isOk());
}

@Test
void protectedEndpoint_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/account/listings"))
            .andExpect(status().isUnauthorized());
}

@Test
void protectedEndpoint_malformedToken_returns401() throws Exception {
    mockMvc.perform(get("/api/v1/account/listings")
                    .header("Authorization", "Bearer invalid-token"))
            .andExpect(status().isUnauthorized());
}
```

#### Access Control Tests

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| AUTH-030 | USER accessing USER endpoint | USER role token | 200 OK |
| AUTH-031 | USER accessing ADMIN endpoint | USER role token | 403 Forbidden |
| AUTH-032 | ADMIN accessing ADMIN endpoint | ADMIN role token | 200 OK |
| AUTH-033 | Anonymous accessing public endpoint | No token | 200 OK |

```java
@Test
void adminEndpoint_userRole_returns403() throws Exception {
    String userToken = registerAndGetToken("user@test.com", "Password123!");

    mockMvc.perform(post("/api/v1/admin/locations/states")
                    .header("Authorization", authHeader(userToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"Test State\"}"))
            .andExpect(status().isForbidden());
}

@Test
void adminEndpoint_adminRole_returns200() throws Exception {
    // Create admin user directly in DB
    User admin = createTestUser("admin@test.com", Role.ADMIN);
    String adminToken = loginAndGetToken("admin@test.com", "Password123!");

    mockMvc.perform(get("/api/v1/admin/locations/states")
                    .header("Authorization", authHeader(adminToken)))
            .andExpect(status().isOk());
}

@Test
void publicEndpoint_noToken_returns200() throws Exception {
    mockMvc.perform(get("/api/v1/listings"))
            .andExpect(status().isOk());
}
```

---

### 4.2 Listing Lifecycle (Critical Path)

**Test Class:** `ListingControllerIntegrationTest`

#### Create Listing Tests

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| LIST-001 | Create vehicle listing (draft) | Valid vehicle data | 201 Created, status=DRAFT |
| LIST-002 | Create part listing (draft) | Valid part data | 201 Created, status=DRAFT |
| LIST-003 | Create vehicle without vehicleType | Missing vehicleType | 400 Bad Request |
| LIST-004 | Create part without partName | Missing partName | 400 Bad Request |
| LIST-005 | Create listing without authentication | No token | 401 Unauthorized |

```java
@Test
void createListing_validVehicle_returnsDraft() throws Exception {
    String token = registerAndGetToken("seller@test.com", "Password123!");
    State state = createTestState("Lagos");

    CreateListingRequest request = CreateListingRequest.builder()
            .title("Honda CBR 650R")
            .description("Well maintained sports bike")
            .price(BigDecimal.valueOf(2500000))
            .listingType(ListingType.VEHICLE)
            .vehicleType(VehicleType.MOTORCYCLE)
            .condition(ListingCondition.GOOD)
            .stateId(state.getId())
            .build();

    mockMvc.perform(post("/api/v1/account/listings")
                    .header("Authorization", authHeader(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("DRAFT"))
            .andExpect(jsonPath("$.data.listingType").value("VEHICLE"));
}

@Test
void createListing_vehicleWithoutVehicleType_returns400() throws Exception {
    String token = registerAndGetToken("seller@test.com", "Password123!");

    CreateListingRequest request = CreateListingRequest.builder()
            .title("Some Vehicle")
            .description("Description")
            .price(BigDecimal.valueOf(100000))
            .listingType(ListingType.VEHICLE)
            // Missing vehicleType
            .condition(ListingCondition.GOOD)
            .build();

    mockMvc.perform(post("/api/v1/account/listings")
                    .header("Authorization", authHeader(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}

@Test
void createListing_validPart_returnsDraft() throws Exception {
    String token = registerAndGetToken("seller@test.com", "Password123!");

    CreateListingRequest request = CreateListingRequest.builder()
            .title("Motorcycle Chain Set")
            .description("Heavy duty chain set")
            .price(BigDecimal.valueOf(15000))
            .listingType(ListingType.PART)
            .partName("Chain Set")
            .condition(ListingCondition.NEW)
            .build();

    mockMvc.perform(post("/api/v1/account/listings")
                    .header("Authorization", authHeader(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.listingType").value("PART"))
            .andExpect(jsonPath("$.data.partName").value("Chain Set"));
}
```

#### Update Listing Tests

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| LIST-010 | Update own listing | Owner's token | 200 OK, updated fields |
| LIST-011 | Update another user's listing | Non-owner token | 401 Unauthorized |
| LIST-012 | Update non-existent listing | Invalid UUID | 404 Not Found |
| LIST-013 | Update with invalid price | Negative price | 400 Bad Request |

**Note:** LIST-011 returns 401 Unauthorized (not 403 Forbidden) because `MarketplaceListingService.getListingForOwner()` throws `UnauthorizedException` when a non-owner attempts to access a listing.

```java
@Test
void updateListing_owner_returnsUpdated() throws Exception {
    String token = registerAndGetToken("seller@test.com", "Password123!");
    User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
    Listing listing = createTestListing(seller, ListingType.VEHICLE);

    UpdateListingRequest request = UpdateListingRequest.builder()
            .title("Updated Title")
            .price(BigDecimal.valueOf(200000))
            .build();

    mockMvc.perform(put("/api/v1/account/listings/" + listing.getId())
                    .header("Authorization", authHeader(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("Updated Title"))
            .andExpect(jsonPath("$.data.price").value(200000));
}

@Test
void updateListing_notOwner_returns403() throws Exception {
    // Create listing as seller1
    User seller1 = createTestUser("seller1@test.com", Role.USER);
    Listing listing = createTestListing(seller1, ListingType.VEHICLE);

    // Try to update as seller2
    String seller2Token = registerAndGetToken("seller2@test.com", "Password123!");

    UpdateListingRequest request = UpdateListingRequest.builder()
            .title("Hijacked Title")
            .build();

    mockMvc.perform(put("/api/v1/account/listings/" + listing.getId())
                    .header("Authorization", authHeader(seller2Token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
}
```

#### Publish Listing Tests

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| LIST-020 | Publish draft with all required fields | Complete listing | 200 OK, status=ACTIVE |
| LIST-021 | Publish listing without title | Missing title | 400 Bad Request |
| LIST-022 | Publish listing with zero price | price=0 | 400 Bad Request |
| LIST-023 | Publish listing without state | Missing stateId | 400 Bad Request |
| LIST-024 | Publish already active listing | status=ACTIVE | 400 Bad Request |

```java
@Test
void publishListing_validDraft_becomesActive() throws Exception {
    String token = registerAndGetToken("seller@test.com", "Password123!");
    User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
    State state = createTestState("Lagos");
    
    Listing listing = Listing.builder()
            .title("Complete Listing")
            .description("Full description")
            .price(BigDecimal.valueOf(150000))
            .seller(seller)
            .listingType(ListingType.VEHICLE)
            .vehicleType(VehicleType.MOTORCYCLE)
            .status(ListingStatus.DRAFT)
            .condition(ListingCondition.GOOD)
            .state(state)
            .build();
    listing = listingRepository.save(listing);

    mockMvc.perform(post("/api/v1/account/listings/" + listing.getId() + "/publish")
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
}

@Test
void publishListing_missingState_returns400() throws Exception {
    String token = registerAndGetToken("seller@test.com", "Password123!");
    User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
    
    Listing listing = Listing.builder()
            .title("Incomplete Listing")
            .price(BigDecimal.valueOf(150000))
            .seller(seller)
            .listingType(ListingType.VEHICLE)
            .vehicleType(VehicleType.MOTORCYCLE)
            .status(ListingStatus.DRAFT)
            // Missing state
            .build();
    listing = listingRepository.save(listing);

    mockMvc.perform(post("/api/v1/account/listings/" + listing.getId() + "/publish")
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isBadRequest());
}
```

#### Mark as Sold Tests

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| LIST-030 | Mark active listing as sold | status=ACTIVE | 200 OK, status=SOLD |
| LIST-031 | Mark draft as sold | status=DRAFT | 400 Bad Request |
| LIST-032 | Mark another user's listing as sold | Non-owner | 401 Unauthorized |

**Note:** LIST-032 returns 401 Unauthorized (not 403 Forbidden) for the same reason as LIST-011.

```java
@Test
void markAsSold_activeListing_becomesSold() throws Exception {
    String token = registerAndGetToken("seller@test.com", "Password123!");
    User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
    State state = createTestState("Lagos");
    
    Listing listing = Listing.builder()
            .title("Active Listing")
            .price(BigDecimal.valueOf(150000))
            .seller(seller)
            .listingType(ListingType.VEHICLE)
            .vehicleType(VehicleType.MOTORCYCLE)
            .status(ListingStatus.ACTIVE)
            .state(state)
            .build();
    listing = listingRepository.save(listing);

    mockMvc.perform(post("/api/v1/account/listings/" + listing.getId() + "/mark-sold")
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("SOLD"));
}
```

#### Delete Listing Tests

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| LIST-040 | Delete own listing | Owner's token | 200 OK, status=DELETED |
| LIST-041 | Delete another user's listing | Non-owner | 403 Forbidden |

---

### 4.3 Image Upload (S3 Integration)

**Test Class:** `ImageServiceIntegrationTest`

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| IMG-001 | Upload single valid image | JPEG file | 200 OK, image URL returned |
| IMG-002 | Upload multiple images | 3 valid JPEGs | 200 OK, 3 URLs returned |
| IMG-003 | Upload max images (10) | 10 valid files | 200 OK |
| IMG-004 | Exceed max images | 11 files | 400 Bad Request |
| IMG-005 | Upload invalid format | .txt file | 400 Bad Request |
| IMG-006 | Upload oversized file | 6MB file | 400 Bad Request |
| IMG-007 | Upload to non-owned listing | Non-owner token | 403 Forbidden |
| IMG-008 | First image becomes primary | Single image | isPrimary=true |
| IMG-009 | Delete primary reassigns primary | Delete primary | Next image becomes primary |
| IMG-010 | S3 failure handling | Mock S3 exception | 500 Internal Error, clean rollback |

```java
@Test
void uploadImages_validFiles_returnsUrls() throws Exception {
    String token = registerAndGetToken("seller@test.com", "Password123!");
    User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
    Listing listing = createTestListing(seller, ListingType.VEHICLE);

    MockMultipartFile file1 = new MockMultipartFile(
            "files", "image1.jpg", "image/jpeg", "fake-image-data".getBytes());
    MockMultipartFile file2 = new MockMultipartFile(
            "files", "image2.jpg", "image/jpeg", "fake-image-data".getBytes());

    mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                    .file(file1)
                    .file(file2)
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2));
}

@Test
void uploadImages_invalidFormat_returns400() throws Exception {
    String token = registerAndGetToken("seller@test.com", "Password123!");
    User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
    Listing listing = createTestListing(seller, ListingType.VEHICLE);

    MockMultipartFile file = new MockMultipartFile(
            "files", "document.txt", "text/plain", "not an image".getBytes());

    mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                    .file(file)
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isBadRequest());
}

@Test
void uploadImages_firstImageBecomesPrimary() throws Exception {
    String token = registerAndGetToken("seller@test.com", "Password123!");
    User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
    Listing listing = createTestListing(seller, ListingType.VEHICLE);

    MockMultipartFile file = new MockMultipartFile(
            "files", "image.jpg", "image/jpeg", "fake-image-data".getBytes());

    mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                    .file(file)
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].primary").value(true));
}

@Test
void uploadImages_s3Failure_returns500AndRollsBack() throws Exception {
    // Configure mock to throw exception
    when(s3Service.uploadFile(any(), anyString()))
            .thenThrow(new RuntimeException("S3 connection failed"));

    String token = registerAndGetToken("seller@test.com", "Password123!");
    User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
    Listing listing = createTestListing(seller, ListingType.VEHICLE);

    MockMultipartFile file = new MockMultipartFile(
            "files", "image.jpg", "image/jpeg", "fake-image-data".getBytes());

    mockMvc.perform(multipart("/api/v1/account/listings/" + listing.getId() + "/images")
                    .file(file)
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isInternalServerError());

    // Verify no images were persisted
    assertThat(listingImageRepository.findByListingId(listing.getId())).isEmpty();
}
```

---

### 4.4 Location System

**Test Class:** `AdminLocationControllerIntegrationTest`

#### State Tests

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| LOC-001 | Admin creates state | Valid name | 201 Created, slug generated |
| LOC-002 | Admin creates duplicate state | Existing name | 409 Conflict |
| LOC-003 | Non-admin creates state | USER role | 403 Forbidden |
| LOC-004 | Get all states | - | 200 OK, list of states |
| LOC-005 | Delete state | Valid ID | 200 OK, cascades to axes/areas |

```java
@Test
void createState_admin_returnsCreatedWithSlug() throws Exception {
    User admin = createTestUser("admin@test.com", Role.ADMIN);
    String token = loginAndGetToken("admin@test.com", "Password123!");

    CreateStateRequest request = new CreateStateRequest("Lagos");

    mockMvc.perform(post("/api/v1/admin/locations/states")
                    .header("Authorization", authHeader(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("Lagos"))
            .andExpect(jsonPath("$.data.slug").value("lagos"));
}

@Test
void createState_duplicateName_returns409() throws Exception {
    User admin = createTestUser("admin@test.com", Role.ADMIN);
    String token = loginAndGetToken("admin@test.com", "Password123!");
    createTestState("Lagos");

    CreateStateRequest request = new CreateStateRequest("Lagos");

    mockMvc.perform(post("/api/v1/admin/locations/states")
                    .header("Authorization", authHeader(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict());
}
```

#### Axis Tests

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| LOC-010 | Create axis under state | Valid stateId | 201 Created |
| LOC-011 | Create axis with invalid state | Non-existent stateId | 404 Not Found |
| LOC-012 | Get axes by state | Valid stateId | 200 OK, list of axes |

```java
@Test
void createAxis_validState_returns201() throws Exception {
    User admin = createTestUser("admin@test.com", Role.ADMIN);
    String token = loginAndGetToken("admin@test.com", "Password123!");
    State state = createTestState("Lagos");

    CreateAxisRequest request = CreateAxisRequest.builder()
            .name("Mainland")
            .stateId(state.getId())
            .build();

    mockMvc.perform(post("/api/v1/admin/locations/axes")
                    .header("Authorization", authHeader(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("Mainland"));
}

@Test
void createAxis_invalidState_returns404() throws Exception {
    User admin = createTestUser("admin@test.com", Role.ADMIN);
    String token = loginAndGetToken("admin@test.com", "Password123!");

    CreateAxisRequest request = CreateAxisRequest.builder()
            .name("Mainland")
            .stateId(UUID.randomUUID()) // Non-existent
            .build();

    mockMvc.perform(post("/api/v1/admin/locations/axes")
                    .header("Authorization", authHeader(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
}
```

#### Area Tests

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| LOC-020 | Create area under axis | Valid axisId | 201 Created |
| LOC-021 | Create area with invalid axis | Non-existent axisId | 404 Not Found |
| LOC-022 | Get areas by axis | Valid axisId | 200 OK, list of areas |

---

### 4.5 Dynamic Attribute System

**Test Class:** `AdminAttributeControllerIntegrationTest`

#### Admin Attribute Management

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| ATTR-001 | Admin creates attribute | Valid data | 201 Created |
| ATTR-002 | Create duplicate attribute | Same name | 409 Conflict |
| ATTR-003 | Update attribute (deactivate) | active=false | 200 OK |
| ATTR-004 | Get attributes by listing type | VEHICLE | 200 OK, filtered list |
| ATTR-005 | Delete attribute | Valid ID | 200 OK, cascades |

```java
@Test
void createAttribute_admin_returnsCreated() throws Exception {
    User admin = createTestUser("admin@test.com", Role.ADMIN);
    String token = loginAndGetToken("admin@test.com", "Password123!");

    AttributeCreateRequest request = AttributeCreateRequest.builder()
            .name("Engine Type")
            .listingType(ListingType.VEHICLE)
            .filterable(true)
            .required(false)
            .build();

    mockMvc.perform(post("/api/v1/admin/attributes")
                    .header("Authorization", authHeader(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.name").value("Engine Type"))
            .andExpect(jsonPath("$.data.slug").value("engine-type"))
            .andExpect(jsonPath("$.data.filterable").value(true));
}
```

#### Seller Assigns Attributes

**Test Class:** `ListingControllerIntegrationTest` (extended)

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| ATTR-010 | Create listing with attributes | Valid attribute values | 201 Created with attributes |
| ATTR-011 | Assign inactive attribute | inactive attribute | 400 Bad Request |
| ATTR-012 | Assign wrong type attribute | PART attr to VEHICLE | 400 Bad Request |
| ATTR-013 | Missing required attribute | Required attr not provided | 400 Bad Request |
| ATTR-014 | Duplicate attribute in request | Same attr twice | 400 Bad Request |

```java
@Test
void createListing_withAttributes_returnsListingWithAttributes() throws Exception {
    String token = registerAndGetToken("seller@test.com", "Password123!");
    AttributeDefinition engineAttr = createTestAttribute("Engine Type", ListingType.VEHICLE, true);

    CreateListingRequest request = CreateListingRequest.builder()
            .title("Honda CB500X")
            .description("Adventure bike")
            .price(BigDecimal.valueOf(3000000))
            .listingType(ListingType.VEHICLE)
            .vehicleType(VehicleType.MOTORCYCLE)
            .condition(ListingCondition.GOOD)
            .attributes(List.of(
                    new AttributeValueRequest(engineAttr.getId(), "500cc")
            ))
            .build();

    mockMvc.perform(post("/api/v1/account/listings")
                    .header("Authorization", authHeader(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.attributes[0].value").value("500cc"));
}

@Test
void createListing_inactiveAttribute_returns400() throws Exception {
    String token = registerAndGetToken("seller@test.com", "Password123!");
    AttributeDefinition attr = createTestAttribute("Old Attr", ListingType.VEHICLE, true);
    attr.setActive(false);
    attributeRepository.save(attr);

    CreateListingRequest request = CreateListingRequest.builder()
            .title("Test Bike")
            .price(BigDecimal.valueOf(100000))
            .listingType(ListingType.VEHICLE)
            .vehicleType(VehicleType.MOTORCYCLE)
            .attributes(List.of(
                    new AttributeValueRequest(attr.getId(), "value")
            ))
            .build();

    mockMvc.perform(post("/api/v1/account/listings")
                    .header("Authorization", authHeader(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}
```

#### Retrieve Listing with Attributes

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| ATTR-020 | Get listing includes attributes | Listing ID | 200 OK, attributes in response |
| ATTR-021 | Public listing view includes attributes | Public GET | 200 OK, attributes visible |

---

### 4.6 Favorites

**Test Class:** `FavoriteControllerIntegrationTest`

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| FAV-001 | Add listing to favorites | Valid listing ID | 201 Created |
| FAV-002 | Add duplicate favorite | Already favorited | 400 Bad Request |
| FAV-003 | Add own listing to favorites | Owner's listing | 400 Bad Request |
| FAV-004 | Add deleted listing to favorites | DELETED status | 400 Bad Request |
| FAV-005 | Remove from favorites | Favorited listing | 200 OK |
| FAV-006 | Remove non-favorited listing | Not in favorites | 404 Not Found |
| FAV-007 | Get user favorites | - | 200 OK, paginated list |
| FAV-008 | Favorites without auth | No token | 401 Unauthorized |

**Implementation Notes:**
- FAV-001: Returns 201 Created (not 200 OK) as `FavoriteController.addToFavorites()` uses `HttpStatus.CREATED`
- FAV-002: Returns 400 Bad Request (not 409 Conflict) because `FavoriteService` throws `BadRequestException` for duplicates

```java
@Test
void addToFavorites_validListing_returns201() throws Exception {
    User seller = createTestUser("seller@test.com", Role.USER);
    Listing listing = createTestListing(seller, ListingType.VEHICLE);
    listing.setStatus(ListingStatus.ACTIVE);
    listingRepository.save(listing);

    String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");

    mockMvc.perform(post("/api/v1/account/favorites/" + listing.getId())
                    .header("Authorization", authHeader(buyerToken)))
            .andExpect(status().isCreated());
}

@Test
void addToFavorites_ownListing_returns400() throws Exception {
    String token = registerAndGetToken("seller@test.com", "Password123!");
    User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
    Listing listing = createTestListing(seller, ListingType.VEHICLE);

    mockMvc.perform(post("/api/v1/account/favorites/" + listing.getId())
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isBadRequest());
}

@Test
void addToFavorites_duplicate_returns400() throws Exception {
    User seller = createTestUser("seller@test.com", Role.USER);
    Listing listing = createTestListing(seller, ListingType.VEHICLE);
    listing.setStatus(ListingStatus.ACTIVE);
    listingRepository.save(listing);

    String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");

    // First add
    mockMvc.perform(post("/api/v1/account/favorites/" + listing.getId())
                    .header("Authorization", authHeader(buyerToken)))
            .andExpect(status().isCreated());

    // Duplicate add - returns 400 (BadRequestException)
    mockMvc.perform(post("/api/v1/account/favorites/" + listing.getId())
                    .header("Authorization", authHeader(buyerToken)))
            .andExpect(status().isBadRequest());
}

@Test
void getUserFavorites_withFavorites_returnsPaginatedList() throws Exception {
    User seller = createTestUser("seller@test.com", Role.USER);
    Listing listing1 = createTestListing(seller, ListingType.VEHICLE);
    Listing listing2 = createTestListing(seller, ListingType.PART);
    listing1.setStatus(ListingStatus.ACTIVE);
    listing2.setStatus(ListingStatus.ACTIVE);
    listingRepository.saveAll(List.of(listing1, listing2));

    String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");
    User buyer = userRepository.findByEmail("buyer@test.com").orElseThrow();

    favoriteRepository.save(Favorite.builder().user(buyer).listing(listing1).build());
    favoriteRepository.save(Favorite.builder().user(buyer).listing(listing2).build());

    mockMvc.perform(get("/api/v1/account/favorites")
                    .header("Authorization", authHeader(buyerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(2));
}
```

---

### 4.7 Messaging / Inquiry

**Test Class:** `MessageControllerIntegrationTest`

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| MSG-001 | Authenticated user sends inquiry | Valid message | 201 Created |
| MSG-002 | Guest sends inquiry with contact | name, phone, message | 201 Created |
| MSG-003 | Guest inquiry without contact info | Missing name/phone | 400 Bad Request |
| MSG-004 | Inquiry on non-active listing | DRAFT/SOLD listing | 400 Bad Request |
| MSG-005 | Inquiry on own listing | Seller's own listing | 400 Bad Request |
| MSG-006 | Duplicate inquiry (auth user) | Second inquiry | 400 Bad Request |
| MSG-007 | Get seller messages | Seller token | 200 OK, paginated |
| MSG-008 | Message links to correct listing | - | Verify listing ID in response |

```java
@Test
void sendInquiry_authenticatedUser_returns201() throws Exception {
    User seller = createTestUser("seller@test.com", Role.USER);
    Listing listing = createTestListing(seller, ListingType.VEHICLE);
    listing.setStatus(ListingStatus.ACTIVE);
    listingRepository.save(listing);

    String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");

    ContactSellerRequest request = ContactSellerRequest.builder()
            .message("Is this still available?")
            .build();

    mockMvc.perform(post("/api/v1/listings/" + listing.getId() + "/inquire")
                    .header("Authorization", authHeader(buyerToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
}

@Test
void sendInquiry_guestWithContactInfo_returns201() throws Exception {
    User seller = createTestUser("seller@test.com", Role.USER);
    Listing listing = createTestListing(seller, ListingType.VEHICLE);
    listing.setStatus(ListingStatus.ACTIVE);
    listingRepository.save(listing);

    ContactSellerRequest request = ContactSellerRequest.builder()
            .senderName("John Guest")
            .senderPhone("08012345678")
            .message("Is this still available?")
            .build();

    mockMvc.perform(post("/api/v1/listings/" + listing.getId() + "/inquire")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
}

@Test
void sendInquiry_guestWithoutContactInfo_returns400() throws Exception {
    User seller = createTestUser("seller@test.com", Role.USER);
    Listing listing = createTestListing(seller, ListingType.VEHICLE);
    listing.setStatus(ListingStatus.ACTIVE);
    listingRepository.save(listing);

    ContactSellerRequest request = ContactSellerRequest.builder()
            .message("Is this still available?")
            // Missing senderName and senderPhone
            .build();

    mockMvc.perform(post("/api/v1/listings/" + listing.getId() + "/inquire")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}

@Test
void sendInquiry_ownListing_returns400() throws Exception {
    String sellerToken = registerAndGetToken("seller@test.com", "Password123!");
    User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
    Listing listing = createTestListing(seller, ListingType.VEHICLE);
    listing.setStatus(ListingStatus.ACTIVE);
    listingRepository.save(listing);

    ContactSellerRequest request = ContactSellerRequest.builder()
            .message("Testing my own listing")
            .build();

    mockMvc.perform(post("/api/v1/listings/" + listing.getId() + "/inquire")
                    .header("Authorization", authHeader(sellerToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
}

@Test
void getSellerMessages_asSeller_returnsPaginatedMessages() throws Exception {
    String sellerToken = registerAndGetToken("seller@test.com", "Password123!");
    User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
    Listing listing = createTestListing(seller, ListingType.VEHICLE);
    listing.setStatus(ListingStatus.ACTIVE);
    listingRepository.save(listing);

    // Create some inquiries
    User buyer = createTestUser("buyer@test.com", Role.USER);
    contactRequestRepository.save(ContactRequest.builder()
            .listing(listing)
            .buyer(buyer)
            .message("Inquiry 1")
            .build());

    mockMvc.perform(get("/api/v1/account/messages")
                    .header("Authorization", authHeader(sellerToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(1));
}
```

---

### 4.8 Cache Behavior

**Test Class:** `CacheBehaviorIntegrationTest`

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| CACHE-001 | First request hits database | Fresh cache | DB query executed |
| CACHE-002 | Second request uses cache | Same key | No DB query |
| CACHE-003 | Admin update evicts cache | Create new state | Cache invalidated |
| CACHE-004 | Subsequent request after eviction hits DB | Post-eviction | Fresh DB query |

```java
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheBehaviorIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private InMemoryCache cache;

    @Autowired
    private LocationCacheService locationCacheService;

    @Autowired
    private StateRepository stateRepository;

    @BeforeEach
    void clearCache() {
        cache.evictAll();
    }

    @Test
    void getStates_firstCall_hitsDatabaseAndCaches() {
        // Given: states in database
        createTestState("Lagos");
        createTestState("Abuja");

        // When: first call
        List<SimpleNode> states = locationCacheService.getStates();

        // Then: results returned and cached
        assertThat(states).hasSize(2);
        assertThat(cache.containsKey("states:all")).isTrue();
    }

    @Test
    void getStates_secondCall_usesCache() {
        // Given: states cached
        createTestState("Lagos");
        locationCacheService.getStates(); // Cache populated

        // When: add new state directly to DB (bypassing service)
        State newState = State.builder().name("Kano").slug("kano").build();
        stateRepository.save(newState);

        // Then: cached result doesn't include new state (cache hit)
        List<SimpleNode> states = locationCacheService.getStates();
        assertThat(states).hasSize(1); // Only Lagos, cache not aware of Kano
    }

    @Test
    void adminCreateState_evictsCache() throws Exception {
        // Given: states cached
        createTestState("Lagos");
        locationCacheService.getStates();
        assertThat(cache.containsKey("states:all")).isTrue();

        // When: admin creates new state via API
        User admin = createTestUser("admin@test.com", Role.ADMIN);
        String token = loginAndGetToken("admin@test.com", "Password123!");

        mockMvc.perform(post("/api/v1/admin/locations/states")
                        .header("Authorization", authHeader(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Abuja\"}"))
                .andExpect(status().isCreated());

        // Then: cache evicted
        assertThat(cache.containsKey("states:all")).isFalse();

        // And: next call includes new state
        List<SimpleNode> states = locationCacheService.getStates();
        assertThat(states).hasSize(2);
    }
}
```

---

### 4.9 Delete Account (Soft Delete)

**Test Class:** `AccountDeletionIntegrationTest`

The delete account feature uses soft delete to preserve data integrity while making deleted users invisible.

#### Account Deletion Flow

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DEL-001 | Successfully delete own account | Valid token | 200 OK, success message |
| DEL-002 | Verify enabled = false after deletion | Deleted account | enabled = false in DB |
| DEL-003 | Verify deletedAt is set after deletion | Deleted account | deletedAt != null in DB |
| DEL-004 | Attempt to delete already deleted account | Second delete call | 401 Unauthorized |
| DEL-005 | Delete account without authentication | No token | 401 Unauthorized |

```java
@Test
void deleteAccount_validToken_returnsSuccess() throws Exception {
    String token = registerAndGetToken("user@test.com", "Password123!");

    mockMvc.perform(delete("/api/v1/account")
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Account deleted successfully"));
}

@Test
void deleteAccount_verifyEnabledFalseAndDeletedAtSet() throws Exception {
    String token = registerAndGetToken("user@test.com", "Password123!");
    User user = userRepository.findByEmail("user@test.com").orElseThrow();
    UUID userId = user.getId();

    mockMvc.perform(delete("/api/v1/account")
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isOk());

    // Use native query to bypass @Where filter
    User deletedUser = userRepository.findByIdIncludingDeleted(userId).orElseThrow();
    assertThat(deletedUser.isEnabled()).isFalse();
    assertThat(deletedUser.getDeletedAt()).isNotNull();
}
```

#### Authentication After Deletion

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DEL-010 | Deleted user cannot login | Correct credentials | 401 Unauthorized |
| DEL-011 | Deleted user cannot access protected endpoints | Existing JWT | 401 Unauthorized |
| DEL-012 | JWT validation rejects deleted user | Valid token structure | 401 Unauthorized |
| DEL-013 | Re-register with same email after deletion | Same email | 201 Created (new account) |

```java
@Test
void login_deletedUser_returns401() throws Exception {
    String email = "user@test.com";
    String password = "Password123!";
    String token = registerAndGetToken(email, password);

    // Delete account
    mockMvc.perform(delete("/api/v1/account")
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isOk());

    // Attempt login
    LoginRequest request = LoginRequest.builder()
            .email(email)
            .password(password)
            .build();

    mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
}
```

#### Global Filtering Enforcement

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DEL-020 | Deleted user not returned in findByEmail | Standard query | Optional.empty() |
| DEL-021 | Deleted user not returned in findById | Standard query | Optional.empty() |
| DEL-022 | Native query returns deleted user | findByIdIncludingDeleted | User returned |
| DEL-023 | System behaves as if user does not exist | Various queries | User invisible |

```java
@Test
void findByEmail_deletedUser_returnsEmpty() throws Exception {
    String email = "user@test.com";
    String token = registerAndGetToken(email, "Password123!");

    mockMvc.perform(delete("/api/v1/account")
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isOk());

    // Standard query should not find deleted user (@Where filter applied)
    Optional<User> user = userRepository.findByEmail(email);
    assertThat(user).isEmpty();
}
```

#### Listing Visibility After Deletion

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DEL-030 | User's listings marked as DELETED | Delete account | All listings status = DELETED |
| DEL-031 | SOLD listings remain SOLD | Delete account | SOLD status preserved |
| DEL-032 | Listings not visible in public API | GET /listings | Filtered out |
| DEL-033 | Listings not visible in search results | Filter query | Not returned |

```java
@Test
void deleteAccount_listingsMarkedAsDeleted() throws Exception {
    String token = registerAndGetToken("seller@test.com", "Password123!");
    User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
    
    Listing listing = createTestListing(seller, ListingType.VEHICLE);
    listing.setStatus(ListingStatus.ACTIVE);
    listingRepository.save(listing);
    UUID listingId = listing.getId();

    mockMvc.perform(delete("/api/v1/account")
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isOk());

    entityManager.clear();
    Listing deleted = listingRepository.findById(listingId).orElseThrow();
    assertThat(deleted.getStatus()).isEqualTo(ListingStatus.DELETED);
}

@Test
void getListings_deletedSeller_listingsNotVisible() throws Exception {
    String token = registerAndGetToken("seller@test.com", "Password123!");
    User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
    State state = createTestState("Lagos");

    Listing listing = createTestListing(seller, ListingType.VEHICLE);
    listing.setStatus(ListingStatus.ACTIVE);
    listing.setState(state);
    listingRepository.save(listing);

    // Verify listing is visible before deletion
    mockMvc.perform(get("/api/v1/listings"))
            .andExpect(jsonPath("$.data.content.length()").value(1));

    // Delete account
    mockMvc.perform(delete("/api/v1/account")
                    .header("Authorization", authHeader(token)))
            .andExpect(status().isOk());

    // Verify listing is not visible after deletion
    mockMvc.perform(get("/api/v1/listings"))
            .andExpect(jsonPath("$.data.content.length()").value(0));
}
```

#### Favorites & Messages

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DEL-040 | User's favorites are deleted | Delete account | No favorites remain |
| DEL-041 | No orphan favorite records | After deletion | favoriteRepository count = 0 for user |
| DEL-050 | Messages preserved after deletion | Delete buyer/seller | Messages still exist |

```java
@Test
void deleteAccount_favoritesDeleted() throws Exception {
    User seller = createTestUser("seller@test.com", Role.USER);
    Listing listing = createTestListing(seller, ListingType.VEHICLE);
    listing.setStatus(ListingStatus.ACTIVE);
    listingRepository.save(listing);

    String buyerToken = registerAndGetToken("buyer@test.com", "Password123!");
    User buyer = userRepository.findByEmail("buyer@test.com").orElseThrow();

    Favorite favorite = favoriteRepository.save(
            Favorite.builder().user(buyer).listing(listing).build());
    UUID favoriteId = favorite.getId();

    mockMvc.perform(delete("/api/v1/account")
                    .header("Authorization", authHeader(buyerToken)))
            .andExpect(status().isOk());

    entityManager.clear();
    assertThat(favoriteRepository.findById(favoriteId)).isEmpty();
}

@Test
void deleteAccount_messagesPreserved() throws Exception {
    String sellerToken = registerAndGetToken("seller@test.com", "Password123!");
    User seller = userRepository.findByEmail("seller@test.com").orElseThrow();
    Listing listing = createTestListing(seller, ListingType.VEHICLE);
    listing.setStatus(ListingStatus.ACTIVE);
    listingRepository.save(listing);

    User buyer = createTestUser("buyer@test.com", Role.USER);
    ContactRequest inquiry = contactRequestRepository.save(
            ContactRequest.builder()
                    .listing(listing)
                    .buyer(buyer)
                    .message("Is this available?")
                    .build());
    UUID inquiryId = inquiry.getId();

    // Delete seller
    mockMvc.perform(delete("/api/v1/account")
                    .header("Authorization", authHeader(sellerToken)))
            .andExpect(status().isOk());

    // Messages preserved
    entityManager.clear();
    assertThat(contactRequestRepository.findById(inquiryId)).isPresent();
}
```

**Note:** For full test plan details, see `DELETE_ACCOUNT_TEST_PLAN.md`.

---

### 4.10 Data Integrity

**Test Class:** `DataIntegrityIntegrationTest`

#### Cascading Deletes

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DATA-001 | Delete listing cascades to images | Listing with images | Images deleted |
| DATA-002 | Delete listing cascades to favorites | Favorited listing | Favorites deleted |
| DATA-003 | Delete state cascades to axes/areas | State with children | All children deleted |
| DATA-004 | Delete user cascades to listings | User with listings | Listings deleted |

```java
@Test
void deleteListing_cascadesToImages() {
    User seller = createTestUser("seller@test.com", Role.USER);
    Listing listing = createTestListing(seller, ListingType.VEHICLE);

    // Add images
    ListingImage image1 = ListingImage.builder()
            .listing(listing)
            .imageUrl("https://s3.amazonaws.com/test/image1.jpg")
            .s3Key("listings/test/image1.jpg")
            .displayOrder(1)
            .primary(true)
            .build();
    listingImageRepository.save(image1);

    UUID listingId = listing.getId();
    UUID imageId = image1.getId();

    // When: delete listing
    listingRepository.delete(listing);
    listingRepository.flush();

    // Then: images also deleted
    assertThat(listingImageRepository.findById(imageId)).isEmpty();
}

@Test
void deleteListing_cascadesToFavorites() {
    User seller = createTestUser("seller@test.com", Role.USER);
    User buyer = createTestUser("buyer@test.com", Role.USER);
    Listing listing = createTestListing(seller, ListingType.VEHICLE);

    Favorite favorite = Favorite.builder()
            .user(buyer)
            .listing(listing)
            .build();
    favoriteRepository.save(favorite);

    UUID listingId = listing.getId();
    UUID favoriteId = favorite.getId();

    // When: delete listing
    listingRepository.delete(listing);
    listingRepository.flush();

    // Then: favorites also deleted
    assertThat(favoriteRepository.findById(favoriteId)).isEmpty();
}

@Test
void deleteState_cascadesToAxesAndAreas() {
    State state = createTestState("Lagos");
    Axis axis = createTestAxis("Mainland", state);
    Area area = createTestArea("Yaba", axis);

    UUID stateId = state.getId();
    UUID axisId = axis.getId();
    UUID areaId = area.getId();

    // When: delete state
    stateRepository.delete(state);
    stateRepository.flush();

    // Then: axes and areas also deleted
    assertThat(axisRepository.findById(axisId)).isEmpty();
    assertThat(areaRepository.findById(areaId)).isEmpty();
}
```

#### Unique Constraints

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DATA-010 | Duplicate email registration | Same email | Constraint violation |
| DATA-011 | Duplicate favorite | Same user+listing | Constraint violation |
| DATA-012 | Duplicate state slug | Same slug | Constraint violation |
| DATA-013 | Duplicate attribute slug | Same slug | Constraint violation |

```java
@Test
void duplicateEmail_throwsConstraintViolation() {
    createTestUser("duplicate@test.com", Role.USER);

    assertThatThrownBy(() -> {
        createTestUser("duplicate@test.com", Role.USER);
    }).isInstanceOf(DataIntegrityViolationException.class);
}

@Test
void duplicateFavorite_throwsConstraintViolation() {
    User seller = createTestUser("seller@test.com", Role.USER);
    User buyer = createTestUser("buyer@test.com", Role.USER);
    Listing listing = createTestListing(seller, ListingType.VEHICLE);

    favoriteRepository.save(Favorite.builder().user(buyer).listing(listing).build());

    assertThatThrownBy(() -> {
        favoriteRepository.saveAndFlush(Favorite.builder().user(buyer).listing(listing).build());
    }).isInstanceOf(DataIntegrityViolationException.class);
}
```

#### Foreign Key Validation

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| DATA-020 | Create listing with invalid state | Non-existent stateId | Validation error |
| DATA-021 | Create axis with invalid state | Non-existent stateId | Validation error |
| DATA-022 | Create listing with mismatched model/make | modelId not under makeId | Validation error |

```java
@Test
void createListing_invalidStateId_throws() {
    User seller = createTestUser("seller@test.com", Role.USER);
    
    Listing listing = Listing.builder()
            .title("Test")
            .price(BigDecimal.valueOf(100000))
            .seller(seller)
            .listingType(ListingType.VEHICLE)
            .vehicleType(VehicleType.MOTORCYCLE)
            .status(ListingStatus.DRAFT)
            .build();
    
    // Set invalid state reference
    State fakeState = new State();
    fakeState.setId(UUID.randomUUID());
    listing.setState(fakeState);

    assertThatThrownBy(() -> {
        listingRepository.saveAndFlush(listing);
    }).isInstanceOf(DataIntegrityViolationException.class);
}
```

---

### 4.10 Search & Filtering

**Test Class:** `ListingControllerIntegrationTest` (search section)

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| SRCH-001 | Filter by listing type | listingType=VEHICLE | Only vehicles returned |
| SRCH-002 | Filter by vehicle type | vehicleType=MOTORCYCLE | Only motorcycles |
| SRCH-003 | Filter by price range | minPrice=50000&maxPrice=150000 | Listings in range |
| SRCH-004 | Filter by state | stateId=uuid | Listings in state |
| SRCH-005 | Filter by axis | axisId=uuid | Listings in axis |
| SRCH-006 | Filter by area | areaId=uuid | Listings in area |
| SRCH-007 | Filter by attribute | attr_engine-type=150cc | Matching listings |
| SRCH-008 | Combine multiple filters | type + price + state | Intersection of filters |
| SRCH-009 | Empty result handling | Non-matching filters | Empty list, no error |
| SRCH-010 | Pagination | page=1&size=10 | Correct page returned |

```java
@Test
void getListings_filterByListingType_returnsOnlyVehicles() throws Exception {
    User seller = createTestUser("seller@test.com", Role.USER);
    
    Listing vehicle = createTestListing(seller, ListingType.VEHICLE);
    vehicle.setStatus(ListingStatus.ACTIVE);
    
    Listing part = createTestListing(seller, ListingType.PART);
    part.setStatus(ListingStatus.ACTIVE);
    
    listingRepository.saveAll(List.of(vehicle, part));

    mockMvc.perform(get("/api/v1/listings")
                    .param("listingType", "VEHICLE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].listingType").value("VEHICLE"));
}

@Test
void getListings_filterByPriceRange_returnsMatchingListings() throws Exception {
    User seller = createTestUser("seller@test.com", Role.USER);
    
    Listing cheap = createTestListing(seller, ListingType.VEHICLE);
    cheap.setPrice(BigDecimal.valueOf(50000));
    cheap.setStatus(ListingStatus.ACTIVE);
    
    Listing mid = createTestListing(seller, ListingType.VEHICLE);
    mid.setPrice(BigDecimal.valueOf(100000));
    mid.setStatus(ListingStatus.ACTIVE);
    
    Listing expensive = createTestListing(seller, ListingType.VEHICLE);
    expensive.setPrice(BigDecimal.valueOf(500000));
    expensive.setStatus(ListingStatus.ACTIVE);
    
    listingRepository.saveAll(List.of(cheap, mid, expensive));

    mockMvc.perform(get("/api/v1/listings")
                    .param("minPrice", "40000")
                    .param("maxPrice", "150000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(2));
}

@Test
void getListings_filterByState_returnsListingsInState() throws Exception {
    User seller = createTestUser("seller@test.com", Role.USER);
    State lagos = createTestState("Lagos");
    State abuja = createTestState("Abuja");
    
    Listing lagosListing = createTestListing(seller, ListingType.VEHICLE);
    lagosListing.setState(lagos);
    lagosListing.setStatus(ListingStatus.ACTIVE);
    
    Listing abujaListing = createTestListing(seller, ListingType.VEHICLE);
    abujaListing.setState(abuja);
    abujaListing.setStatus(ListingStatus.ACTIVE);
    
    listingRepository.saveAll(List.of(lagosListing, abujaListing));

    mockMvc.perform(get("/api/v1/listings")
                    .param("stateId", lagos.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(1));
}

@Test
void getListings_filterByAttribute_returnsMatchingListings() throws Exception {
    User seller = createTestUser("seller@test.com", Role.USER);
    AttributeDefinition engineAttr = createTestAttribute("Engine Type", ListingType.VEHICLE, true);
    
    Listing listing150cc = createTestListing(seller, ListingType.VEHICLE);
    listing150cc.setStatus(ListingStatus.ACTIVE);
    listingRepository.save(listing150cc);
    
    listingAttributeValueRepository.save(ListingAttributeValue.builder()
            .listing(listing150cc)
            .attribute(engineAttr)
            .value("150cc")
            .build());
    
    Listing listing250cc = createTestListing(seller, ListingType.VEHICLE);
    listing250cc.setStatus(ListingStatus.ACTIVE);
    listingRepository.save(listing250cc);
    
    listingAttributeValueRepository.save(ListingAttributeValue.builder()
            .listing(listing250cc)
            .attribute(engineAttr)
            .value("250cc")
            .build());

    mockMvc.perform(get("/api/v1/listings")
                    .param("attr_engine-type", "150cc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(1));
}

@Test
void getListings_combinedFilters_returnsIntersection() throws Exception {
    User seller = createTestUser("seller@test.com", Role.USER);
    State lagos = createTestState("Lagos");
    
    // Vehicle in Lagos, cheap
    Listing match = createTestListing(seller, ListingType.VEHICLE);
    match.setState(lagos);
    match.setPrice(BigDecimal.valueOf(100000));
    match.setStatus(ListingStatus.ACTIVE);
    
    // Vehicle in Lagos, expensive (no match)
    Listing noMatch1 = createTestListing(seller, ListingType.VEHICLE);
    noMatch1.setState(lagos);
    noMatch1.setPrice(BigDecimal.valueOf(500000));
    noMatch1.setStatus(ListingStatus.ACTIVE);
    
    // Part in Lagos, cheap (no match - wrong type)
    Listing noMatch2 = createTestListing(seller, ListingType.PART);
    noMatch2.setState(lagos);
    noMatch2.setPrice(BigDecimal.valueOf(100000));
    noMatch2.setStatus(ListingStatus.ACTIVE);
    
    listingRepository.saveAll(List.of(match, noMatch1, noMatch2));

    mockMvc.perform(get("/api/v1/listings")
                    .param("listingType", "VEHICLE")
                    .param("stateId", lagos.getId().toString())
                    .param("maxPrice", "200000"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(1));
}

@Test
void getListings_noMatches_returnsEmptyList() throws Exception {
    mockMvc.perform(get("/api/v1/listings")
                    .param("minPrice", "999999999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.content.length()").value(0));
}
```

---

### 4.11 Registration AccountType (GAP 1)

**Test Class:** `RegistrationAccountTypeIntegrationTest`

Tests the optional `accountType` field in registration and its propagation to responses.

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| REG-AT-001 | Register without accountType | No accountType field | 201 Created, accountType=INDIVIDUAL |
| REG-AT-002 | Register with accountType=INDIVIDUAL | accountType=INDIVIDUAL | 201 Created, accountType=INDIVIDUAL |
| REG-AT-003 | Register with accountType=DEALER | accountType=DEALER | 201 Created, accountType=DEALER |
| REG-AT-004 | AuthResponse contains accountType | Registration response | user.accountType present |
| REG-AT-005 | UserResponse contains accountType after login | Login response | user.accountType present |
| REG-AT-006 | Invalid accountType value | accountType=INVALID | 400 Bad Request |
| REG-AT-007 | Null accountType is accepted | accountType=null | 201 Created, defaults to INDIVIDUAL |

```java
@Test
void register_withDealerAccountType_setsDealer() throws Exception {
    RegisterRequest request = RegisterRequest.builder()
            .email("dealer@test.com")
            .password("SecurePass123!")
            .firstName("Dealer")
            .lastName("User")
            .accountType(AccountType.DEALER)
            .build();

    mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.user.accountType").value("DEALER"));
}
```

---

### 4.12 Profile Endpoints (GAP 2)

**Test Class:** `ProfileEndpointsIntegrationTest`

Tests the profile management endpoints at `/api/v1/account/me`.

#### GET /api/v1/account/me

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| PROF-001 | Get profile authenticated | Valid token | 200 OK, user data returned |
| PROF-002 | Get profile unauthenticated | No token | 401 Unauthorized |
| PROF-003 | Get profile returns state | User with state | state field populated |

#### PUT /api/v1/account/me

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| PROF-010 | Update firstName only | firstName field | 200 OK, firstName updated |
| PROF-011 | Update lastName only | lastName field | 200 OK, lastName updated |
| PROF-012 | Update with stateId | Valid stateId | 200 OK, state name set |
| PROF-013 | Update with invalid stateId | Non-existent UUID | 404 Not Found |
| PROF-014 | Update all fields | All optional fields | 200 OK, all fields updated |
| PROF-015 | Update unauthenticated | No token | 401 Unauthorized |
| PROF-016 | Null fields ignored | Empty request body | 200 OK, existing values kept |

#### PUT /api/v1/account/me/password

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| PROF-020 | Change password valid | Correct current + new | 200 OK, password changed |
| PROF-021 | Wrong current password | Incorrect current | 400 Bad Request |
| PROF-022 | Passwords don't match | newPassword != confirm | 400 Bad Request |
| PROF-023 | Weak new password | <8 characters | 400 Bad Request |
| PROF-024 | Unauthenticated | No token | 401 Unauthorized |
| PROF-025 | Old password no longer works | After change | 401 on login with old |

#### DELETE /api/v1/account/me

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| PROF-030 | Delete account | Valid token | 200 OK, account deleted |
| PROF-031 | Delete unauthenticated | No token | 401 Unauthorized |
| PROF-032 | Deleted user cannot login | After deletion | 401 on login attempt |

```java
@Test
void updateProfile_withStateId_setsStateName() throws Exception {
    String token = registerAndGetToken("user@test.com", "Password123!");
    State state = createTestState("Lagos");

    UpdateProfileRequest request = UpdateProfileRequest.builder()
            .stateId(state.getId())
            .build();

    mockMvc.perform(put("/api/v1/account/me")
                    .header("Authorization", authHeader(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.state").value("Lagos"));
}
```

---

### 4.13 Admin Categorization Endpoints (GAP 3)

**Test Class:** `AdminCategorizationControllerIntegrationTest`

Tests admin CRUD for vehicle categorization hierarchy: Make → VehicleModel → ModelYear.

#### Make CRUD

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| CAT-001 | Admin creates make | Valid name | 201 Created, slug generated |
| CAT-002 | Create duplicate make | Existing name | 409 Conflict |
| CAT-003 | Non-admin creates make | USER role | 403 Forbidden |
| CAT-004 | Admin updates make | New name | 200 OK, name/slug updated |
| CAT-005 | Admin deletes make | Valid ID | 200 OK, make deleted |
| CAT-006 | Delete make cascades | Make with models/years | All children deleted |
| CAT-007 | Get all makes | - | 200 OK, list returned |
| CAT-008 | Delete non-existent make | Random UUID | 404 Not Found |

#### VehicleModel CRUD

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| CAT-010 | Admin creates model | Valid makeId | 201 Created |
| CAT-011 | Create model invalid makeId | Non-existent makeId | 404 Not Found |
| CAT-012 | Admin updates model | New name | 200 OK, updated |
| CAT-013 | Delete model cascades to years | Model with years | Years deleted |
| CAT-014 | Get models by make | Valid makeId | 200 OK, filtered list |
| CAT-015 | Get models non-existent make | Random UUID | 404 Not Found |

#### ModelYear CRUD

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| CAT-020 | Admin creates year | Valid vehicleModelId | 201 Created |
| CAT-021 | Create year invalid modelId | Non-existent modelId | 404 Not Found |
| CAT-022 | Admin updates year | New name | 200 OK, updated |
| CAT-023 | Admin deletes year | Valid ID | 200 OK, year deleted |
| CAT-024 | Get years by model | Valid modelId | 200 OK, filtered list |
| CAT-025 | Get years non-existent model | Random UUID | 404 Not Found |

#### Cache Invalidation

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| CAT-030 | Create make invalidates cache | New make | Lookup reflects change |

```java
@Test
void deleteMake_cascadesToModelsAndYears() throws Exception {
    Make make = createTestMake("CascadeTest");
    VehicleModel model = createTestVehicleModel("Model1", make);
    ModelYear year = createTestModelYear("2024", model);

    mockMvc.perform(delete("/api/v1/admin/categorization/makes/" + make.getId())
                    .header("Authorization", authHeader(adminToken)))
            .andExpect(status().isOk());

    entityManager.clear();
    assertThat(vehicleModelRepository.findById(model.getId())).isEmpty();
    assertThat(modelYearRepository.findById(year.getId())).isEmpty();
}
```

---

### 4.14 Admin Listing Management

**Test Class:** `AdminListingControllerIntegrationTest`

Tests admin endpoints for listing management at `/api/v1/admin/listings`.

#### GET /api/v1/admin/listings (10 tests)

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| ADMLIST-001 | Admin gets all listings | Valid admin token | 200 OK, all listings returned |
| ADMLIST-002 | Filter by status | status=ACTIVE | Only ACTIVE listings |
| ADMLIST-003 | Filter by listingType | listingType=VEHICLE | Only VEHICLE listings |
| ADMLIST-004 | Filter by category | category=MOTORCYCLE | Only MOTORCYCLE listings |
| ADMLIST-005 | Search by title | search=honda | Listings with "honda" in title |
| ADMLIST-006 | Search by seller name | search=John | Listings by seller named John |
| ADMLIST-007 | Admin sees DELETED listings | status=DELETED | DELETED listings returned |
| ADMLIST-008 | Pagination support | page=0&size=10 | Paginated results |
| ADMLIST-009 | Non-admin gets 403 | USER role | 403 Forbidden |
| ADMLIST-010 | Unauthenticated gets 401 | No token | 401 Unauthorized |

#### PUT /api/v1/admin/listings/{id}/status (9 tests)

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| ADMLIST-020 | DRAFT → ACTIVE | Valid transition | 200 OK, status changed |
| ADMLIST-021 | ACTIVE → SOLD | Valid transition | 200 OK, status changed |
| ADMLIST-022 | EXPIRED → ACTIVE | Valid transition | 200 OK, status changed |
| ADMLIST-023 | DELETED → ACTIVE (invalid) | Invalid transition | 400 Bad Request |
| ADMLIST-024 | SOLD → ACTIVE (invalid) | Invalid transition | 400 Bad Request |
| ADMLIST-025 | Non-existent listing | Random UUID | 404 Not Found |
| ADMLIST-026 | Non-admin cannot change | USER role | 403 Forbidden |
| ADMLIST-027 | Null status validation | Empty body | 400 Bad Request |

#### DELETE /api/v1/admin/listings/{id} (5 tests)

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| ADMLIST-030 | Soft delete listing | Valid listing ID | 200 OK, status = DELETED |
| ADMLIST-031 | Delete already deleted | DELETED listing | 200 OK (idempotent) |
| ADMLIST-032 | Non-existent listing | Random UUID | 404 Not Found |
| ADMLIST-033 | Non-admin cannot delete | USER role | 403 Forbidden |
| ADMLIST-034 | Unauthenticated | No token | 401 Unauthorized |

#### Status Transition Rules (8 tests)

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| ADMLIST-040 | DRAFT → PUBLISHED | Valid | 200 OK |
| ADMLIST-041 | DRAFT → DELETED | Valid | 200 OK |
| ADMLIST-042 | PUBLISHED → ACTIVE | Valid | 200 OK |
| ADMLIST-043 | PUBLISHED → EXPIRED | Valid | 200 OK |
| ADMLIST-044 | ACTIVE → EXPIRED | Valid | 200 OK |
| ADMLIST-045 | SOLD → DELETED | Valid | 200 OK |
| ADMLIST-046 | EXPIRED → DELETED | Valid | 200 OK |

```java
@Test
void adminCanChangeStatusFromDraftToActive() throws Exception {
    Listing listing = createTestListing(seller, ListingType.VEHICLE);

    ChangeListingStatusRequest request = ChangeListingStatusRequest.builder()
            .status(ListingStatus.ACTIVE)
            .build();

    mockMvc.perform(put("/api/v1/admin/listings/{id}/status", listing.getId())
                    .header("Authorization", authHeader(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACTIVE"));
}
```

---

### 4.15 User Impersonation

**Test Class:** `ImpersonationIntegrationTest`

Tests user impersonation endpoint at `POST /api/v1/admin/users/{userId}/impersonate`.

#### Impersonation Endpoint (6 tests)

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| IMP-001 | Admin impersonates regular user | Valid target userId | 200 OK, token returned |
| IMP-002 | Cannot impersonate admin | Target is admin | 400 Bad Request |
| IMP-003 | Cannot impersonate self | Target is caller | 400 Bad Request |
| IMP-004 | Non-admin cannot impersonate | USER role | 403 Forbidden |
| IMP-005 | Unauthenticated | No token | 401 Unauthorized |
| IMP-006 | Non-existent user | Random UUID | 404 Not Found |

#### Impersonation Token Validity (5 tests)

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| IMP-010 | Token valid for user endpoints | Use on /account/me | 200 OK, target user info |
| IMP-011 | Token accesses user's listings | Use on /account/listings | Target user's listings |
| IMP-012 | Token has isImpersonation claim | Check token claims | isImpersonation=true |
| IMP-013 | Regular token not impersonation | Check regular token | isImpersonation=false |
| IMP-014 | Token cannot access admin endpoints | Use on /admin/* | 403 Forbidden |

```java
@Test
void adminCanImpersonateRegularUser() throws Exception {
    MvcResult result = mockMvc.perform(post("/api/v1/admin/users/{userId}/impersonate", regularUser.getId())
                    .header("Authorization", authHeader(adminToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.accessToken").exists())
            .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.data.expiresIn").value(1800))
            .andReturn();
}

@Test
void impersonationTokenValidForUserEndpoints() throws Exception {
    // Get impersonation token
    MvcResult impersonateResult = mockMvc.perform(
            post("/api/v1/admin/users/{userId}/impersonate", regularUser.getId())
                    .header("Authorization", authHeader(adminToken)))
            .andExpect(status().isOk())
            .andReturn();

    String impersonationToken = extractAccessToken(impersonateResult);

    // Use token on user's own endpoint
    mockMvc.perform(get("/api/v1/account/me")
                    .header("Authorization", authHeader(impersonationToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(regularUser.getId().toString()));
}
```

---

### 4.16 Token Refresh (GAP 4)

**Test Class:** `TokenRefreshIntegrationTest`

Tests the token refresh endpoint at `POST /api/v1/auth/refresh`.

#### Valid Refresh Token

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| REFRESH-001 | Valid refresh token | Valid JWT | 200 OK, new access token |
| REFRESH-005 | New token works | Use new access token | Can access protected endpoints |
| REFRESH-006 | Endpoint is public | No Authorization header | 200 OK (with valid body) |

#### Invalid Refresh Token

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| REFRESH-002 | Invalid token | Invalid JWT string | 401 Unauthorized |
| REFRESH-002b | Malformed JWT | "not-a-jwt" | 401 Unauthorized |
| REFRESH-002c | Empty token | "" | 400 Bad Request |
| REFRESH-002d | Null token | null | 400 Bad Request |
| REFRESH-002e | Access token as refresh | Access token | May work (same signing key) |

#### Deleted/Disabled User

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| REFRESH-004 | Deleted user's token | Valid token, deleted user | 401 Unauthorized |
| REFRESH-004b | Disabled user's token | Valid token, disabled user | 401 Unauthorized |

#### Response Format

| Test ID | Scenario | Input | Expected Result |
|---------|----------|-------|-----------------|
| REFRESH-007 | Response format | Valid request | accessToken, tokenType, expiresIn |
| REFRESH-008 | Token is unique | Multiple refreshes | Different tokens each time |

```java
@Test
void newAccessToken_worksForProtectedEndpoints() throws Exception {
    String refreshToken = registerAndGetRefreshToken("user@test.com", "Password123!");

    RefreshTokenRequest request = RefreshTokenRequest.builder()
            .refreshToken(refreshToken)
            .build();

    MvcResult result = mockMvc.perform(post("/api/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

    String newAccessToken = extractAccessToken(result);

    mockMvc.perform(get("/api/v1/account/listings")
                    .header("Authorization", "Bearer " + newAccessToken))
            .andExpect(status().isOk());
}
```

---

## 5. Edge Cases

### 5.1 Boundary Conditions

| Test ID | Scenario | Expected Behavior |
|---------|----------|-------------------|
| EDGE-001 | Price = 0 | Rejected for publish |
| EDGE-002 | Price = MAX_VALUE | Accepted |
| EDGE-003 | Title = 1 char | Depends on validation rules |
| EDGE-004 | Title = 255 chars | Accepted (max length) |
| EDGE-005 | Description empty | Allowed for draft |
| EDGE-006 | Exactly 10 images | Accepted (max) |
| EDGE-007 | Exactly 5MB image | Accepted (max) |
| EDGE-008 | Image 5MB + 1 byte | Rejected |

### 5.2 Concurrent Operations

| Test ID | Scenario | Expected Behavior |
|---------|----------|-------------------|
| EDGE-010 | Two users favorite same listing simultaneously | Both succeed, count = 2 |
| EDGE-011 | Same user favorites same listing twice (race) | One succeeds, one gets 409 |
| EDGE-012 | Seller deletes listing while buyer is viewing | Buyer sees stale data or 404 |

### 5.3 State Transitions

| Test ID | Scenario | Expected Behavior |
|---------|----------|-------------------|
| EDGE-020 | DRAFT → ACTIVE (valid) | Success |
| EDGE-021 | DRAFT → SOLD (invalid) | 400 Bad Request |
| EDGE-022 | ACTIVE → SOLD (valid) | Success |
| EDGE-023 | SOLD → ACTIVE (invalid) | 400 Bad Request |
| EDGE-024 | DELETED → anything (invalid) | 400 Bad Request |

---

## 6. Performance Considerations

### 6.1 Sanity Tests

**Test Class:** `PerformanceSanityTest`

```java
@SpringBootTest
@Testcontainers
class PerformanceSanityTest extends BaseIntegrationTest {

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void getListings_with100Listings_completesIn5Seconds() throws Exception {
        // Setup: Create 100 listings
        User seller = createTestUser("seller@test.com", Role.USER);
        State state = createTestState("Lagos");

        List<Listing> listings = IntStream.range(0, 100)
                .mapToObj(i -> {
                    Listing listing = createTestListing(seller, ListingType.VEHICLE);
                    listing.setTitle("Listing " + i);
                    listing.setState(state);
                    listing.setStatus(ListingStatus.ACTIVE);
                    return listing;
                })
                .toList();
        listingRepository.saveAll(listings);

        // Execute: Fetch listings
        mockMvc.perform(get("/api/v1/listings")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(20));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void filterListings_with100Listings_completesIn5Seconds() throws Exception {
        // Setup: Create 100 listings with various attributes
        User seller = createTestUser("seller@test.com", Role.USER);
        State state = createTestState("Lagos");
        AttributeDefinition attr = createTestAttribute("Engine", ListingType.VEHICLE, true);

        List<Listing> listings = IntStream.range(0, 100)
                .mapToObj(i -> {
                    Listing listing = createTestListing(seller, ListingType.VEHICLE);
                    listing.setTitle("Listing " + i);
                    listing.setState(state);
                    listing.setPrice(BigDecimal.valueOf(50000 + (i * 1000)));
                    listing.setStatus(ListingStatus.ACTIVE);
                    return listing;
                })
                .toList();
        listingRepository.saveAll(listings);

        // Execute: Filter with multiple criteria
        mockMvc.perform(get("/api/v1/listings")
                        .param("listingType", "VEHICLE")
                        .param("stateId", state.getId().toString())
                        .param("minPrice", "60000")
                        .param("maxPrice", "100000"))
                .andExpect(status().isOk());
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.SECONDS)
    void lookupEndpoints_cached_completeQuickly() throws Exception {
        // Setup: Create location hierarchy
        State state = createTestState("Lagos");
        Axis axis = createTestAxis("Mainland", state);
        createTestArea("Yaba", axis);

        // First call (cache miss)
        mockMvc.perform(get("/api/v1/lookup/states"))
                .andExpect(status().isOk());

        // Subsequent calls (cache hit) - should be very fast
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/v1/lookup/states"))
                    .andExpect(status().isOk());
        }
    }
}
```

### 6.2 Performance Targets

| Operation | Target Response Time | Notes |
|-----------|---------------------|-------|
| GET /listings (paginated) | < 500ms | With 100 listings |
| GET /listings (filtered) | < 1000ms | Multiple filters |
| GET /lookup/* (cached) | < 50ms | After first request |
| POST /listings | < 500ms | Create draft |
| POST /images | < 2000ms | Per image (mocked S3) |

---

## 7. Risks & Gaps

### 7.1 Known Limitations

| Risk | Mitigation |
|------|------------|
| S3 mocked - can't test real upload failures | Add smoke test with real S3 in staging |
| No load testing | Defer to staging environment |
| No email/notification testing | Mock notification service |
| JWT secret exposed in test config | Use different secrets per environment |

### 7.2 Test Coverage Gaps

| Area | Gap | Recommendation |
|------|-----|----------------|
| Full-text search | Not implemented | Add when search feature added |
| Rate limiting | Not tested | Add when rate limiting implemented |

### 7.3 Recommended Additional Tests (Post-MVP)

1. **Load Testing**: Use Gatling/JMeter for realistic traffic simulation
2. **Contract Testing**: API contract validation with consumer tests
3. **Security Testing**: OWASP ZAP scan for vulnerabilities
4. **Chaos Testing**: Resilience testing with database failures

---

## 8. Test Execution

### 8.1 Running Tests

```bash
# Run all integration tests
mvn test -Dspring.profiles.active=test

# Run specific test class
mvn test -Dtest=AuthControllerIntegrationTest

# Run tests with coverage
mvn test jacoco:report

# Run performance tests only
mvn test -Dtest=PerformanceSanityTest
```

### 8.2 CI/CD Integration

```yaml
# .github/workflows/test.yml
name: Integration Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Run tests
        run: mvn test -Dspring.profiles.active=test
      
      - name: Upload test results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-results
          path: target/surefire-reports/
```

### 8.3 Test Data Cleanup

- Tests use `@Transactional` for automatic rollback
- Testcontainers creates fresh database per test run
- No manual cleanup required

---

## 9. Appendix

### 9.1 Test Data Builders

```java
// TestDataBuilder.java
public class TestDataBuilder {
    
    public static RegisterRequest.RegisterRequestBuilder aRegisterRequest() {
        return RegisterRequest.builder()
                .email("test@example.com")
                .password("SecurePass123!")
                .firstName("Test")
                .lastName("User")
                .phoneNumber("08012345678");
    }
    
    public static CreateListingRequest.CreateListingRequestBuilder aVehicleListing() {
        return CreateListingRequest.builder()
                .title("Test Vehicle")
                .description("Test description")
                .price(BigDecimal.valueOf(150000))
                .listingType(ListingType.VEHICLE)
                .vehicleType(VehicleType.MOTORCYCLE)
                .condition(ListingCondition.GOOD);
    }
    
    public static CreateListingRequest.CreateListingRequestBuilder aPartListing() {
        return CreateListingRequest.builder()
                .title("Test Part")
                .description("Test description")
                .price(BigDecimal.valueOf(15000))
                .listingType(ListingType.PART)
                .partName("Chain Set")
                .condition(ListingCondition.NEW);
    }
}
```

### 9.2 Custom Test Assertions

```java
// ListingAssertions.java
public class ListingAssertions {
    
    public static void assertListingIsActive(Listing listing) {
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.ACTIVE);
        assertThat(listing.getTitle()).isNotBlank();
        assertThat(listing.getPrice()).isPositive();
        assertThat(listing.getState()).isNotNull();
    }
    
    public static void assertListingIsDraft(Listing listing) {
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.DRAFT);
    }
}
```

### 9.3 Test Categories (JUnit Tags)

```java
@Tag("integration")
@Tag("auth")
class AuthControllerIntegrationTest { }

@Tag("integration")
@Tag("critical-path")
class ListingControllerIntegrationTest { }

@Tag("integration")
@Tag("performance")
class PerformanceSanityTest { }
```

Run by tag:
```bash
mvn test -Dgroups=critical-path
```

---

## 10. Summary

This test plan covers:

- **140+ test scenarios** across 14 feature areas
- **Full authentication & authorization** testing
- **Critical path coverage** for listing lifecycle
- **Data integrity validation** at database level
- **Cache behavior verification**
- **Performance sanity checks** for MVP scale

**Implementation Priority:**
1. Authentication (AUTH-*) - Foundation for all tests
2. Listing Lifecycle (LIST-*) - Core business flow
3. Data Integrity (DATA-*) - Prevent data corruption
4. Search & Filtering (SRCH-*) - User-facing functionality
5. Everything else - Supporting features

**Estimated Implementation Time:** 3-5 days for an experienced engineer.

---

## 11. Implementation Status

### Implemented Tests

| Test Class | Tests | Status | Notes |
|------------|-------|--------|-------|
| `AuthControllerIntegrationTest` | 14 | Implemented | AUTH-001 to AUTH-033 |
| `ListingControllerIntegrationTest` | 26 | Implemented | LIST-001 to LIST-032, SRCH-001 to SRCH-010 |
| `FavoriteControllerIntegrationTest` | 15 | Implemented | FAV-001 to FAV-008 |
| `ImageServiceIntegrationTest` | 17 | Implemented | IMG-001 to IMG-010 + additional edge cases |
| `MessageControllerIntegrationTest` | 16 | Implemented | MSG-001 to MSG-008 + additional edge cases |
| `AdminLocationControllerIntegrationTest` | 28 | Implemented | LOC-001 to LOC-022 + public lookup |
| `AdminAttributeControllerIntegrationTest` | 22 | Implemented | ATTR-001 to ATTR-005 + public endpoints |
| `CacheBehaviorIntegrationTest` | 8 | Implemented | CACHE-001 to CACHE-004 + additional edge cases |
| `DataIntegrityIntegrationTest` | 20 | Implemented | DATA-001 to DATA-022 + additional edge cases |
| `PerformanceSanityTest` | 5 | Implemented | Performance sanity checks |
| `AccountDeletionIntegrationTest` | TBD | Planned | DEL-001 to DEL-091 (50+ scenarios) |
| `RegistrationAccountTypeIntegrationTest` | 7 | Implemented | REG-AT-001 to REG-AT-007 (GAP 1) |
| `ProfileEndpointsIntegrationTest` | 19 | Implemented | PROF-001 to PROF-032 (GAP 2) |
| `AdminCategorizationControllerIntegrationTest` | 21 | Implemented | CAT-001 to CAT-030 (GAP 3) |
| `TokenRefreshIntegrationTest` | 12 | Implemented | REFRESH-001 to REFRESH-008 (GAP 4) |
| `AdminListingControllerIntegrationTest` | 32 | Implemented | Admin listing management (Session 2026-04-25) |
| `ImpersonationIntegrationTest` | 11 | Implemented | User impersonation (Session 2026-04-25) |

**Total Implemented: 273 tests** (as of 2026-04-25)

**Planned Tests:**
- Delete Account feature: 50+ test scenarios (see `DELETE_ACCOUNT_TEST_PLAN.md`)

### Implementation Differences from Plan

The following deviations from the original plan were discovered during implementation:

1. **Authorization for non-owner access returns 401 (not 403)**
   - `LIST-011`, `LIST-032`, `LIST-041`: Non-owner modification attempts return `401 Unauthorized`
   - Reason: `MarketplaceListingService.getListingForOwner()` throws `UnauthorizedException`
   - This is semantically correct as the user is "not authorized" to access that specific resource

2. **Duplicate favorites return 400 (not 409)**
   - `FAV-002`: Adding duplicate favorite returns `400 Bad Request`
   - Reason: `FavoriteService` throws `BadRequestException` (not `DuplicateResourceException`)

3. **Add to favorites returns 201 (not 200)**
   - `FAV-001`: Returns `201 Created` on success
   - Reason: `FavoriteController.addToFavorites()` uses `HttpStatus.CREATED`

4. **Listing requires `category` field**
   - `createTestListing()` helper must set `category` (NOT NULL constraint in database)
   - Vehicle listings: `ListingCategory.MOTORCYCLE/TRICYCLE/BICYCLE`
   - Part listings: `ListingCategory.SPARE_PART/ACCESSORY`

5. **Send inquiry returns 201 (not 200)**
   - `MSG-001`, `MSG-002`: Returns `201 Created` on success
   - Reason: `MessageController.sendInquiry()` uses `HttpStatus.CREATED`

6. **Duplicate inquiry returns 400 (not 409)**
   - `MSG-006`: Returns `400 Bad Request` for duplicate inquiries
   - Reason: `MessageService` throws `BadRequestException` (not `DuplicateResourceException`)

### Test Execution

```bash
# Run all implemented integration tests
mvn test -Dtest=AuthControllerIntegrationTest,ListingControllerIntegrationTest,FavoriteControllerIntegrationTest

# Run individual test class
mvn test -Dtest=ListingControllerIntegrationTest

# Run with verbose output
mvn test -Dtest=ListingControllerIntegrationTest -Dspring.profiles.active=test
```
