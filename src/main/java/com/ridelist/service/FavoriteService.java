package com.ridelist.service;

import com.ridelist.dto.mapper.ListingMapper;
import com.ridelist.dto.response.ListingSummaryResponse;
import com.ridelist.dto.response.PagedResponse;
import com.ridelist.exception.BadRequestException;
import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.model.Favorite;
import com.ridelist.model.Listing;
import com.ridelist.model.ListingStatus;
import com.ridelist.model.User;
import com.ridelist.repository.FavoriteRepository;
import com.ridelist.repository.ListingRepository;
import com.ridelist.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final ListingMapper listingMapper;

    @Transactional
    public void addToFavorites(UUID userId, UUID listingId) {
        log.info("Adding listing {} to favorites for user {}", listingId, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        if (listing.getStatus() == ListingStatus.DELETED) {
            throw new BadRequestException("Cannot favorite a deleted listing");
        }

        if (listing.getSeller().getId().equals(userId)) {
            throw new BadRequestException("Cannot favorite your own listing");
        }

        if (favoriteRepository.existsByUserIdAndListingId(userId, listingId)) {
            throw new BadRequestException("Listing is already in favorites");
        }

        Favorite favorite = Favorite.builder()
                .user(user)
                .listing(listing)
                .build();

        favoriteRepository.save(favorite);
        log.info("Listing {} added to favorites for user {}", listingId, userId);
    }

    @Transactional
    public void removeFromFavorites(UUID userId, UUID listingId) {
        log.info("Removing listing {} from favorites for user {}", listingId, userId);

        if (!favoriteRepository.existsByUserIdAndListingId(userId, listingId)) {
            throw new ResourceNotFoundException("Favorite", "listingId", listingId);
        }

        favoriteRepository.deleteByUserIdAndListingId(userId, listingId);
        log.info("Listing {} removed from favorites for user {}", listingId, userId);
    }

    public PagedResponse<ListingSummaryResponse> getUserFavorites(UUID userId, Pageable pageable) {
        log.debug("Fetching favorites for user {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        Page<Favorite> favoritesPage = favoriteRepository.findByUserId(userId, pageable);

        return PagedResponse.<ListingSummaryResponse>builder()
                .content(favoritesPage.getContent().stream()
                        .map(favorite -> listingMapper.toSummaryResponse(favorite.getListing()))
                        .toList())
                .page(favoritesPage.getNumber())
                .size(favoritesPage.getSize())
                .totalElements(favoritesPage.getTotalElements())
                .totalPages(favoritesPage.getTotalPages())
                .first(favoritesPage.isFirst())
                .last(favoritesPage.isLast())
                .build();
    }

    public boolean isFavorite(UUID userId, UUID listingId) {
        return favoriteRepository.existsByUserIdAndListingId(userId, listingId);
    }

    public long getFavoriteCount(UUID listingId) {
        return favoriteRepository.countByListingId(listingId);
    }
}
