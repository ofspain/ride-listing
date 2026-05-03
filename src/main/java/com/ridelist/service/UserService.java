package com.ridelist.service;

import com.ridelist.dto.mapper.UserMapper;
import com.ridelist.dto.request.ChangePasswordRequest;
import com.ridelist.dto.request.UpdateProfileRequest;
import com.ridelist.dto.response.PagedResponse;
import com.ridelist.dto.response.UserAdminResponse;
import com.ridelist.dto.response.UserResponse;
import com.ridelist.exception.BadRequestException;
import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.model.*;
import com.ridelist.repository.FavoriteRepository;
import com.ridelist.repository.ListingRepository;
import com.ridelist.repository.StateRepository;
import com.ridelist.repository.UserRepository;
import com.ridelist.repository.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final StateRepository stateRepository;
    private final ListingRepository listingRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserResponse getProfile(UUID userId) {
        log.info("Fetching profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("Updating profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }

        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }

        if (request.getStateId() != null) {
            State state = stateRepository.findById(request.getStateId())
                    .orElseThrow(() -> new ResourceNotFoundException("State", "id", request.getStateId()));
            user.setState(state.getName());
        }

        User savedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", userId);

        return userMapper.toResponse(savedUser);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.info("Changing password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }

    @Transactional
    public void deleteAccount(UUID userId) {
        log.info("Deleting account for user: {}", userId);

        User user = userRepository.findByIdIncludingDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (user.getDeletedAt() != null || !user.isEnabled()) {
            throw new BadRequestException("Account is already deleted or disabled");
        }

        user.setEnabled(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Disabled user account: {}", userId);

        List<ListingStatus> excludeStatuses = List.of(ListingStatus.SOLD, ListingStatus.DELETED);
        int updatedListings = listingRepository.updateStatusBySellerIdAndStatusNotIn(
                userId, ListingStatus.DELETED, excludeStatuses);
        log.info("Marked {} listings as DELETED for user: {}", updatedListings, userId);

        favoriteRepository.deleteAllByUserId(userId);
        log.info("Deleted favorites for user: {}", userId);

        log.info("Account deletion completed for user: {}", userId);
    }


    public PagedResponse<UserAdminResponse> getUsers(
            String search,
            AccountType accountType,
            Boolean enabled,
            LocalDateTime deletedFrom,
            LocalDateTime deletedTo,
            Pageable pageable) {

        Page<User> usersPaged = userRepository.findAll(
                UserSpecification.searchUsers(
                        search,
                        accountType,
                        enabled,
                        deletedFrom,
                        deletedTo
                ),
                pageable
        );

        Page<UserAdminResponse> mapped = usersPaged.map(this::mapToResponse);

        return PagedResponse.<UserAdminResponse>builder()
                .content(mapped.getContent())
                .page(mapped.getNumber())
                .size(mapped.getSize())
                .totalElements(mapped.getTotalElements())
                .totalPages(mapped.getTotalPages())
                .first(mapped.isFirst())
                .last(mapped.isLast())
                .build();


    }

    private UserAdminResponse mapToResponse(User user) {
        return UserAdminResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .state(user.getState())
                .accountType(user.getAccountType())
                .profileImageUrl(user.getProfileImageUrl())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .deletedAt(user.getDeletedAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
