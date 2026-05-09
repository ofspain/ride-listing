package com.ridelist.dto.response;

import com.ridelist.model.AccountType;
import com.ridelist.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String profileImageUrl;
    private AccountType accountType;
    private Role role;
    private String state;
    private LocalDateTime createdAt;
    private String bio;
    private String sellerSlug;
    private String sellerUrl;
    private boolean hasPublicPage;
}
