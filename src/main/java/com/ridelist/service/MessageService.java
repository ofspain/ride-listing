package com.ridelist.service;

import com.ridelist.dto.mapper.ContactRequestMapper;
import com.ridelist.dto.request.ContactSellerRequest;
import com.ridelist.dto.response.ContactRequestResponse;
import com.ridelist.dto.response.PagedResponse;
import com.ridelist.exception.BadRequestException;
import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.model.ContactRequest;
import com.ridelist.model.Listing;
import com.ridelist.model.ListingStatus;
import com.ridelist.model.User;
import com.ridelist.repository.ContactRequestRepository;
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
public class MessageService {

    private final ContactRequestRepository contactRequestRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ContactRequestMapper contactRequestMapper;

    @Transactional
    public ContactRequestResponse sendInquiry(UUID listingId, ContactSellerRequest request, UUID buyerId) {
        log.info("Sending inquiry for listing: {} from user: {}", listingId, buyerId);

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new BadRequestException("Cannot send inquiry to a non-active listing");
        }

        User buyer = null;
        if (buyerId != null) {
            buyer = userRepository.findById(buyerId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", buyerId));

            if (listing.getSeller().getId().equals(buyerId)) {
                throw new BadRequestException("Cannot send inquiry to your own listing");
            }

            if (contactRequestRepository.existsByListingIdAndBuyerId(listingId, buyerId)) {
                throw new BadRequestException("You have already sent an inquiry for this listing");
            }
        } else {
            validateGuestInquiry(request);
        }

        ContactRequest contactRequest = ContactRequest.builder()
                .listing(listing)
                .buyer(buyer)
                .senderName(buyer != null ? buyer.getFullName() : request.getSenderName())
                .senderPhone(buyer != null ? buyer.getPhoneNumber() : request.getSenderPhone())
                .message(request.getMessage())
                .build();

        ContactRequest savedRequest = contactRequestRepository.save(contactRequest);
        log.info("Inquiry sent successfully with id: {}", savedRequest.getId());

        return contactRequestMapper.toResponse(savedRequest);
    }

    public PagedResponse<ContactRequestResponse> getMessagesForSeller(UUID sellerId, Pageable pageable) {
        log.debug("Fetching messages for seller: {}", sellerId);

        Page<ContactRequest> messagesPage = contactRequestRepository.findBySellerIdThroughListing(sellerId, pageable);

        return PagedResponse.<ContactRequestResponse>builder()
                .content(messagesPage.getContent().stream()
                        .map(contactRequestMapper::toResponse)
                        .toList())
                .page(messagesPage.getNumber())
                .size(messagesPage.getSize())
                .totalElements(messagesPage.getTotalElements())
                .totalPages(messagesPage.getTotalPages())
                .first(messagesPage.isFirst())
                .last(messagesPage.isLast())
                .build();
    }

    public PagedResponse<ContactRequestResponse> getMessagesForListing(UUID listingId, UUID sellerId, Pageable pageable) {
        log.debug("Fetching messages for listing: {}", listingId);

        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Listing", "id", listingId));

        if (!listing.getSeller().getId().equals(sellerId)) {
            throw new BadRequestException("You are not the owner of this listing");
        }

        Page<ContactRequest> messagesPage = contactRequestRepository.findByListingId(listingId, pageable);

        return PagedResponse.<ContactRequestResponse>builder()
                .content(messagesPage.getContent().stream()
                        .map(contactRequestMapper::toResponse)
                        .toList())
                .page(messagesPage.getNumber())
                .size(messagesPage.getSize())
                .totalElements(messagesPage.getTotalElements())
                .totalPages(messagesPage.getTotalPages())
                .first(messagesPage.isFirst())
                .last(messagesPage.isLast())
                .build();
    }

    private void validateGuestInquiry(ContactSellerRequest request) {
        if (request.getSenderName() == null || request.getSenderName().isBlank()) {
            throw new BadRequestException("Sender name is required for guest inquiries");
        }
        if (request.getSenderPhone() == null || request.getSenderPhone().isBlank()) {
            throw new BadRequestException("Sender phone is required for guest inquiries");
        }
    }
}
