package com.example.identityservice.controller;

import com.example.identityservice.dto.RegisterRequest;
import com.example.identityservice.entity.User;
import com.example.identityservice.repository.UserRepository;
import com.example.identityservice.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Autowired
    private JwtUtil jwtUtil;

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

    @Test
    @DisplayName("End-to-End JWT flow: Register user then call /api/auth/test-token and verify payload claims")
    void testGenerateTokenFlow_EndToEnd() throws Exception {
        // Step 1: Register user in DB
        RegisterRequest registerReq = new RegisterRequest("jwt_tester", "StrongPass@2026", "MANAGER");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Step 2: Call GET /api/auth/test-token?username=jwt_tester
        MvcResult result = mockMvc.perform(get("/api/auth/test-token")
                        .param("username", "jwt_tester"))
                .andExpect(status().isOk())
                .andReturn();

        String token = result.getResponse().getContentAsString();
        assertNotNull(token);
        assertFalse(token.trim().isEmpty());

        // Step 3: Verify JWT Token contains sub = username and role in custom claim
        Claims claims = jwtUtil.extractAllClaims(token);
        assertEquals("jwt_tester", claims.getSubject(), "Claim 'sub' must match registered username");
        assertEquals("MANAGER", claims.get("role", String.class), "Custom claim 'role' must match registered role");
        assertTrue(jwtUtil.isTokenValid(token, "jwt_tester"));
    }
}
