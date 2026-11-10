package com.example.identityservice.controller;

import com.example.identityservice.dto.RegisterRequest;
import com.example.identityservice.entity.User;
import com.example.identityservice.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RegisterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete registration flow: check response without password and check hashed password in DB")
    void testRegisterFlow_EndToEnd() throws Exception {
        RegisterRequest request = new RegisterRequest("nguyen_van_a", "MySecretPass@123", "ADMIN");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username").value("nguyen_van_a"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());

        // Verify in database
        Optional<User> userOptional = userRepository.findByUsername("nguyen_van_a");
        assertTrue(userOptional.isPresent(), "User should exist in database");

        User savedUser = userOptional.get();
        assertEquals("nguyen_van_a", savedUser.getUsername());
        assertEquals("ADMIN", savedUser.getRole());

        // CRITICAL CHECK: Password must be hashed with BCrypt, not raw
        String storedPassword = savedUser.getPassword();
        assertNotEquals("MySecretPass@123", storedPassword, "Password must not be stored in plaintext");
        assertTrue(storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$"),
                "Password in DB must be BCrypt hashed string (e.g., $2a$10$...)");
        assertTrue(passwordEncoder.matches("MySecretPass@123", storedPassword),
                "BCrypt encoder must match raw password with stored hash");
    }
}

