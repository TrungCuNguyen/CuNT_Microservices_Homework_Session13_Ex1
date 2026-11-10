package com.example.identityservice.service.impl;

import com.example.identityservice.dto.RegisterRequest;
import com.example.identityservice.dto.UserResponse;
import com.example.identityservice.entity.User;
import com.example.identityservice.exception.DuplicateResourceException;
import com.example.identityservice.repository.UserRepository;
import com.example.identityservice.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username '" + request.getUsername() + "' is already registered");
        }

        String role = (request.getRole() != null && !request.getRole().trim().isEmpty())
                ? request.getRole().trim().toUpperCase()
                : "USER";

        // Hash password using BCrypt before storing in database
        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = new User(request.getUsername().trim(), hashedPassword, role);
        User savedUser = userRepository.save(user);

        // Return user info without password
        return new UserResponse(savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
    }
}
