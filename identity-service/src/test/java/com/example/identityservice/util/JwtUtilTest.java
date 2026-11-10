package com.example.identityservice.util;

import com.example.identityservice.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // 256-bit secret key and 3600000ms expiration (1 hour)
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 3600000L);
    }

    @Test
    @DisplayName("Should generate valid JWT token with username in sub and role in custom claim")
    void generateToken_ContainsUsernameAndRoleClaims() {
        User user = new User("user-123", "nguyen_van_a", "hashed_pwd", "ADMIN");

        String token = jwtUtil.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isEmpty());

        // Extract claims
        Claims claims = jwtUtil.extractAllClaims(token);
        assertEquals("nguyen_van_a", claims.getSubject(), "Claim 'sub' must be username");
        assertEquals("ADMIN", claims.get("role", String.class), "Custom claim 'role' must be user's role");
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());

        // Verify expiration is roughly 1 hour in future
        long duration = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(3600000L, duration);

        assertTrue(jwtUtil.isTokenValid(token, "nguyen_van_a"));
    }
}
