package com.ridelist.controller;

import com.ridelist.dto.request.ContactSellerRequest;
import com.ridelist.dto.response.ApiResponse;
import com.ridelist.dto.response.ContactRequestResponse;
import com.ridelist.dto.response.PagedResponse;
import com.ridelist.security.UserPrincipal;
import com.ridelist.service.MessageService;
import com.ridelist.util.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/api/v1/listings/{id}/inquire")
    public ResponseEntity<ApiResponse<ContactRequestResponse>> sendInquiry(
            @PathVariable UUID id,
            @Valid @RequestBody ContactSellerRequest request,
            @CurrentUser UserPrincipal principal) {

        UUID buyerId = principal != null ? principal.getId() : null;
        ContactRequestResponse response = messageService.sendInquiry(id, request, buyerId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inquiry sent successfully", response));
    }

    @GetMapping("/api/v1/account/messages")
    public ResponseEntity<ApiResponse<PagedResponse<ContactRequestResponse>>> getMyMessages(
            @CurrentUser UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PagedResponse<ContactRequestResponse> response = messageService.getMessagesForSeller(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
