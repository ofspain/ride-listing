package com.ridelist.service;

import com.ridelist.exception.BadRequestException;
import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.model.ListingStatus;
import com.ridelist.model.User;
import com.ridelist.repository.FavoriteRepository;
import com.ridelist.repository.ListingRepository;
import com.ridelist.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final FavoriteRepository favoriteRepository;

    /**
     * Soft deletes a user account.
     *
     * Steps:
     * 1. Validate user exists and is not already deleted
     * 2. Disable account (enabled = false, deletedAt = now)
     * 3. Mark all user's listings as DELETED (except already SOLD/DELETED)
     * 4. Delete all user's favorites
     * 5. Messages are preserved for historical records
     *
     * @param userId The ID of the user to delete
     * @throws ResourceNotFoundException if user not found
     * @throws BadRequestException if user is already deleted
     */
    @Transactional
    public void deleteAccount(UUID userId) {
        log.info("Deleting account for user: {}", userId);

        // Use native query to find user including deleted ones (to give proper error message)
        User user = userRepository.findByIdIncludingDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Check if already deleted
        if (user.getDeletedAt() != null || !user.isEnabled()) {
            throw new BadRequestException("Account is already deleted or disabled");
        }

        // Disable account
        user.setEnabled(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Disabled user account: {}", userId);

        // Mark all listings as DELETED (except already SOLD or DELETED)
        List<ListingStatus> excludeStatuses = List.of(ListingStatus.SOLD, ListingStatus.DELETED);
        int updatedListings = listingRepository.updateStatusBySellerIdAndStatusNotIn(
                userId, ListingStatus.DELETED, excludeStatuses);
        log.info("Marked {} listings as DELETED for user: {}", updatedListings, userId);

        // Delete all favorites belonging to user
        favoriteRepository.deleteAllByUserId(userId);
        log.info("Deleted favorites for user: {}", userId);

        log.info("Account deletion completed for user: {}", userId);
    }
}
