package com.ridelist.controller;

import com.ridelist.dto.mapper.UserMapper;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.SellerProfileResponse;
import com.ridelist.dto.response.SitemapEntry;
import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.model.AccountType;
import com.ridelist.model.ListingStatus;
import com.ridelist.model.User;
import com.ridelist.repository.ListingRepository;
import com.ridelist.repository.UserRepository;
import com.ridelist.service.SellerProfileService;
import com.ridelist.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SellerController {

    private final SellerProfileService sellerProfileService;
    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final UserMapper userMapper;

    /**
     * Get seller profile by SEO-friendly slug.
     * URL format: /sellers/chukwuemeka-autos-lagos-f630be
     */
    @GetMapping("/sellers/{sellerSlug}")
    public ResponseEntity<ApiResponse<SellerProfileResponse>> getSellerProfile(
            @PathVariable String sellerSlug
    ) {
        User seller = sellerProfileService.resolveSellerBySlug(sellerSlug);

        long activeListingCount = listingRepository.countBySellerIdAndStatusIn(
                seller.getId(),
                List.of(ListingStatus.ACTIVE, ListingStatus.PUBLISHED)
        );

        return ResponseEntity.ok(ApiResponse.success(
                userMapper.toProfileResponse(seller, activeListingCount)
        ));
    }

    /**
     * Get seller profile by UUID (backward compatibility).
     * URL format: /seller/f630be62-b1bb-4100-840f-266ef035b307
     */
    @GetMapping("/seller/{id}")
    public ResponseEntity<ApiResponse<SellerProfileResponse>> getSellerByUuid(
            @PathVariable UUID id
    ) {
        User seller = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Seller", "id", id));

        long activeListingCount = listingRepository.countBySellerIdAndStatusIn(
                seller.getId(),
                List.of(ListingStatus.ACTIVE, ListingStatus.PUBLISHED)
        );

        return ResponseEntity.ok(ApiResponse.success(
                userMapper.toProfileResponse(seller, activeListingCount)
        ));
    }

    /**
     * Get seller pages for sitemap generation.
     */
    @GetMapping("/sellers/sitemap-data")
    public ResponseEntity<ApiResponse<List<SitemapEntry>>> getSellerSitemapData() {
        List<SitemapEntry> entries = userRepository
                .findByAccountTypeAndSellerSlugIsNotNull(AccountType.DEALER)
                .stream()
                .filter(u -> u.getSellerSlug() != null)
                .map(u -> new SitemapEntry(
                        SlugUtil.toSellerUrl(u.getSellerSlug()),
                        u.getUpdatedAt()
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(entries));
    }
}
