package com.ridelist.controller;

import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.ListingSummaryResponse;
import com.ridelist.dto.response.PagedResponse;
import com.ridelist.security.UserPrincipal;
import com.ridelist.service.FavoriteService;
import com.ridelist.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/account/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{listingId}")
    public ResponseEntity<ApiResponse<Void>> addToFavorites(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID listingId) {

        favoriteService.addToFavorites(principal.getId(), listingId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Added to favorites", null));
    }

    @DeleteMapping("/{listingId}")
    public ResponseEntity<ApiResponse<Void>> removeFromFavorites(
            @CurrentUser UserPrincipal principal,
            @PathVariable UUID listingId) {

        favoriteService.removeFromFavorites(principal.getId(), listingId);
        return ResponseEntity.ok(ApiResponse.success("Removed from favorites", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<ListingSummaryResponse>>> getMyFavorites(
            @CurrentUser UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PagedResponse<ListingSummaryResponse> response = favoriteService.getUserFavorites(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
