package com.ridelist.service;

import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.model.AccountType;
import com.ridelist.model.User;
import com.ridelist.repository.UserRepository;
import com.ridelist.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class SellerProfileService {

    private final UserRepository userRepository;

    /**
     * Generates and saves seller slug when user becomes a DEALER.
     * Called from:
     * 1. AuthService when accountType is set to DEALER on register
     * 2. MarketplaceListingService when first listing is created (auto-upgrade to DEALER)
     * 3. UserService when bio/name updated
     */
    @Transactional
    public void generateSellerSlug(User user) {
        if (user.getAccountType() != AccountType.DEALER) {
            return; // Only dealers get slugs
        }

        String stateSlug = null;
        if (user.getState() != null && !user.getState().isBlank()) {
            stateSlug = SlugUtil.toSlug(user.getState());
        }

        String slug = SlugUtil.toSellerSlug(
                user.getFirstName(),
                user.getLastName(),
                stateSlug,
                user.getId()
        );

        user.setSellerSlug(slug);
        user.setSellerSlugUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Seller slug generated for user {}: {}", user.getId(), slug);
    }

    /**
     * Resolves a seller by their slug.
     * Extracts the UUID fragment and queries by it — name portion ignored.
     */
    @Transactional(readOnly = true)
    public User resolveSellerBySlug(String sellerSlug) {
        String uuidFragment = SlugUtil.extractSellerUuidFragment(sellerSlug);

        if (uuidFragment == null) {
            throw new ResourceNotFoundException("Seller", "sellerSlug", sellerSlug);
        }

        return userRepository
                .findBySellerSlugFragment(uuidFragment)
                .orElseThrow(() -> new ResourceNotFoundException("Seller", "sellerSlug",sellerSlug));
    }
}
