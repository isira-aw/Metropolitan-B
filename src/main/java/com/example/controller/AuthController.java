package com.example.controller;

import com.example.dto.CommonResponse;
import com.example.dto.UserDTO;
import com.example.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<CommonResponse<String>> signIn(@RequestBody UserDTO userDTO) {
        try {
            // Attempt to sign in the user and generate a token
            String token = authService.signIn(userDTO);
            // Return successful response with token
            CommonResponse<String> response = new CommonResponse<>("success", "Login successful", token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // In case of any errors, return error response
            CommonResponse<String> errorResponse = new CommonResponse<>("error", "Login failed: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);  // Internal Server Error
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<String>> signUp(@RequestBody UserDTO userDTO) {
        try {
            // Attempt to sign up the user
            String response = authService.signUp(userDTO);

            // If the user already exists, return an error response
            if (response.equals("User with this email already exists.")) {
                CommonResponse<String> errorResponse = new CommonResponse<>("error", response);
                return ResponseEntity.status(409).body(errorResponse);  // Conflict status code
            }

            // Return success response if user is registered successfully
            CommonResponse<String> successResponse = new CommonResponse<>("success", "User registered successfully", null);
            return ResponseEntity.ok(successResponse);

        } catch (Exception e) {
            // In case of any errors, return error response
            CommonResponse<String> errorResponse = new CommonResponse<>("error", "Sign-up failed: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);  // Internal Server Error
        }
    }
}
