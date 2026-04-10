package com.ridelist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelist.dto.request.LoginRequest;
import com.ridelist.dto.request.RegisterRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.AuthResponse;
import com.ridelist.model.*;
import com.ridelist.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ListingRepository listingRepository;

    @Autowired
    protected FavoriteRepository favoriteRepository;

    @Autowired
    protected ContactRequestRepository contactRequestRepository;

    @Autowired
    protected ListingImageRepository listingImageRepository;

    @BeforeEach
    void setUp() {
        contactRequestRepository.deleteAll();
        favoriteRepository.deleteAll();
        listingImageRepository.deleteAll();
        listingRepository.deleteAll();
        userRepository.deleteAll();
    }

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
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class)
        );

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
                objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class)
        );

        return response.getData().getAccessToken();
    }

    protected User createTestUser(String email, String encodedPassword) {
        User user = User.builder()
                .email(email)
                .password(encodedPassword)
                .firstName("Test")
                .lastName("User")
                .phoneNumber("08012345678")
                .state("Lagos")
                .accountType(AccountType.INDIVIDUAL)
                .role(Role.USER)
                .enabled(true)
                .build();
        return userRepository.save(user);
    }

    protected Listing createTestListing(User seller, ListingStatus status) {
        Listing listing = Listing.builder()
                .listingType(ListingType.VEHICLE)
                .vehicleType(VehicleType.MOTORCYCLE)
                .title("Test Motorcycle")
                .description("A test motorcycle listing")
                .price(BigDecimal.valueOf(500000))
                .state("Lagos")
                .category(ListingCategory.MOTORCYCLE)
                .condition(ListingCondition.GOOD)
                .status(status)
                .make("Honda")
                .model("CBR")
                .year(2022)
                .location("Ikeja, Lagos")
                .seller(seller)
                .build();
        return listingRepository.save(listing);
    }

    protected Listing createTestPartListing(User seller, ListingStatus status) {
        Listing listing = Listing.builder()
                .listingType(ListingType.PART)
                .title("Test Spare Part")
                .description("A test spare part listing")
                .price(BigDecimal.valueOf(15000))
                .state("Lagos")
                .category(ListingCategory.SPARE_PART)
                .condition(ListingCondition.NEW)
                .status(status)
                .partName("Brake Pad")
                .partCategory("Brakes")
                .compatibility("Honda CBR 2020-2023")
                .location("Ikeja, Lagos")
                .seller(seller)
                .build();
        return listingRepository.save(listing);
    }
}
