package com.ridelist.dto.response;

import com.ridelist.model.AccountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SellerProfileResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String bio;
    private String sellerSlug;
    private String sellerUrl;
    private AccountType accountType;
    private String state;
    private long activeListingCount;
    private String memberSince;
    private boolean hasPublicPage;
    private String profileImageUrl;
}
