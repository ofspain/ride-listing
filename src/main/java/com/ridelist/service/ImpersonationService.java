package com.ridelist.service;

import com.ridelist.dto.mapper.UserMapper;
import com.ridelist.dto.response.ImpersonationResponse;
import com.ridelist.exception.BadRequestException;
import com.ridelist.exception.ResourceNotFoundException;
import com.ridelist.model.Role;
import com.ridelist.model.User;
import com.ridelist.repository.UserRepository;
import com.ridelist.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImpersonationService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;

    private static final int IMPERSONATION_EXPIRY_SECONDS = 1800; // 30 minutes

    @Transactional(readOnly = true)
    public ImpersonationResponse impersonateUser(UUID targetUserId, UUID adminId) {
        log.info("Admin {} starting impersonation session for user {}", adminId, targetUserId);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));

        if (target.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot impersonate an admin account");
        }

        if (targetUserId.equals(adminId)) {
            throw new BadRequestException("Cannot impersonate your own account");
        }

        String token = jwtTokenProvider.generateImpersonationToken(targetUserId, adminId);

        log.info("Admin {} started impersonation session for user {}", adminId, targetUserId);

        return ImpersonationResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(IMPERSONATION_EXPIRY_SECONDS)
                .targetUser(userMapper.toResponse(target))
                .build();
    }
}
