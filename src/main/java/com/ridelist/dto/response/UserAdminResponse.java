package com.ridelist.dto.response;

import com.ridelist.model.AccountType;
import com.ridelist.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserAdminResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String state;
    private AccountType accountType;
    private String profileImageUrl;
    private Role role;
    private boolean enabled;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
}
