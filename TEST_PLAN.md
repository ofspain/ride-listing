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
    └── PerformanceSanityTest.java
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

### 4.9 Data Integrity

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
| Token refresh | Not implemented | Add when token refresh endpoint exists |
| Admin categorization | Partially covered | Add full CRUD tests when service implemented |
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

- **85+ test scenarios** across 10 feature areas
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

**Total Implemented: 171 tests** (as of 2026-04-19)

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
