package com.ridelist.service;

import com.ridelist.dto.request.RegisterRequest;
import com.ridelist.exception.DuplicateResourceException;
import com.ridelist.model.Role;
import com.ridelist.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DbSeeder implements CommandLineRunner {

    private final AuthService authService;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {

        if (userRepository.existsByEmail("femi.ayeni@ridelist.com")) {
            return;
        }
        RegisterRequest registerAdmin = new RegisterRequest();
        registerAdmin.setEmail("femi.ayeni@ridelist.com");
        registerAdmin.setPassword("ofspain86");
        registerAdmin.setFirstName("Oluwafemi");
        registerAdmin.setLastName("Ayeni");
        registerAdmin.setPhoneNumber("00000000000");
        registerAdmin.setRole(Role.ADMIN);

        authService.register(registerAdmin);
    }
}
