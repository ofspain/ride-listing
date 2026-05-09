package com.ridelist.dto.mapper;

import com.ridelist.dto.request.RegisterRequest;
import com.ridelist.dto.response.SellerProfileResponse;
import com.ridelist.dto.response.UserResponse;
import com.ridelist.dto.response.UserSummaryResponse;
import com.ridelist.model.AccountType;
import com.ridelist.model.User;
import com.ridelist.util.SlugUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "profileImageUrl", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "listings", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "accountType", ignore = true)
    @Mapping(target = "favorites", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "bio", ignore = true)
    @Mapping(target = "sellerSlug", ignore = true)
    @Mapping(target = "sellerSlugUpdatedAt", ignore = true)
    User toEntity(RegisterRequest request);

    @Mapping(target = "sellerUrl", ignore = true)
    @Mapping(target = "hasPublicPage", ignore = true)
    UserResponse toResponse(User user);

    @AfterMapping
    default void populateSellerFields(User user, @MappingTarget UserResponse response) {
        if (user.getAccountType() == AccountType.DEALER && user.getSellerSlug() != null) {
            response.setSellerUrl(SlugUtil.toSellerUrl(user.getSellerSlug()));
            response.setHasPublicPage(true);
        } else {
            response.setHasPublicPage(false);
        }
    }

    @Mapping(target = "sellerUrl", ignore = true)
    UserSummaryResponse toSummaryResponse(User user);

    @AfterMapping
    default void populateSummarySeller(User user, @MappingTarget UserSummaryResponse response) {
        if (user.getSellerSlug() != null) {
            response.setSellerUrl(SlugUtil.toSellerUrl(user.getSellerSlug()));
        }
    }

    @Mapping(target = "activeListingCount", ignore = true)
    @Mapping(target = "memberSince", ignore = true)
    @Mapping(target = "sellerUrl", ignore = true)
    @Mapping(target = "hasPublicPage", ignore = true)
    SellerProfileResponse toProfileResponse(User user);

    default SellerProfileResponse toProfileResponse(User user, long activeListingCount) {
        SellerProfileResponse response = toProfileResponse(user);
        response.setActiveListingCount(activeListingCount);

        if (user.getSellerSlug() != null) {
            response.setSellerUrl(SlugUtil.toSellerUrl(user.getSellerSlug()));
        }

        response.setHasPublicPage(
                user.getAccountType() == AccountType.DEALER && activeListingCount > 0
        );

        if (user.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
            response.setMemberSince("Member since " + user.getCreatedAt().format(formatter));
        }

        return response;
    }
}