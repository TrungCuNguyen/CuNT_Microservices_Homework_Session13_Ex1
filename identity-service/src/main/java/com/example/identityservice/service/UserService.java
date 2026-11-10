package com.example.identityservice.service;

import com.example.identityservice.dto.RegisterRequest;
import com.example.identityservice.dto.UserResponse;

public interface UserService {
    UserResponse register(RegisterRequest request);
}

