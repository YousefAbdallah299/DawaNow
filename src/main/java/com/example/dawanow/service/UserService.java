package com.example.dawanow.service;

import com.example.dawanow.dtos.request.UpdateUserRequest;
import com.example.dawanow.dtos.response.PaginatedResponse;
import com.example.dawanow.dtos.response.UserResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    public UserResponse getCurrentUser() {
        return null;
    }

    public PaginatedResponse<UserResponse> getAllUsers(Pageable pageable) {
        return PaginatedResponse.empty(pageable);
    }

    public UserResponse getUserById(Long id) {
        return null;
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        return null;
    }

    public void deleteUser(Long id) {
    }
}
