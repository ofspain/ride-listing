package com.ridelist.service;

import com.ridelist.model.User;
import com.ridelist.repository.UserRepository;
import com.ridelist.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Use native query to bypass @Where filter and check for deleted users
        User user = userRepository.findByEmailIncludingDeleted(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // Check if user is deleted or disabled
        if (!user.isEnabled() || user.getDeletedAt() != null) {
            throw new UsernameNotFoundException("User account is disabled or deleted");
        }

        return new UserPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(UUID id) {
        // Use native query to bypass @Where filter and check for deleted users
        User user = userRepository.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        // Check if user is deleted or disabled
        if (!user.isEnabled() || user.getDeletedAt() != null) {
            throw new UsernameNotFoundException("User account is disabled or deleted");
        }

        return new UserPrincipal(user);
    }
}
