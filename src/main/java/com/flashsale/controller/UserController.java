package com.flashsale.controller;

import com.flashsale.dto.UserRequest;
import com.flashsale.dto.UserResponse;
import com.flashsale.entity.User;
import com.flashsale.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public UserResponse createUser(
            @Valid @RequestBody UserRequest request
    ) {

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();

        User savedUser = userService.createUser(user);

        return UserResponse.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .createdAt(savedUser.getCreatedAt())
                .build();
    }

    @GetMapping
    public List<User> getUsers() {
        return userService.getAllUsers();
    }
}