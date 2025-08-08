package com.example.controller;

import com.example.dto.AuthResponseDTO;
import com.example.dto.CommonResponse;
import com.example.dto.UserDTO;
import com.example.entity.User;
import com.example.repository.UserRepository;
import com.example.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository; // Inject the UserRepository here



    @PostMapping("/signin")
    public ResponseEntity<CommonResponse<AuthResponseDTO>> signIn(@RequestBody UserDTO userDTO) {
        try {
            // Attempt to sign in the user and generate a token
            String token = authService.signIn(userDTO);

            // Get the role of the user from the database
            User user = userRepository.findByEmail(userDTO.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String role = user.getRole();

            // Create response DTO
            AuthResponseDTO authResponseDTO = new AuthResponseDTO();
            authResponseDTO.setToken(token);
            authResponseDTO.setRole(role);
            authResponseDTO.setName(user.getName());

            // Return successful response with token and role
            CommonResponse<AuthResponseDTO> response = new CommonResponse<>("success", "Login successful", authResponseDTO);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // In case of any errors, return error response
            CommonResponse<AuthResponseDTO> errorResponse = new CommonResponse<>("error", "Login failed: " + e.getMessage(), null);
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

            // Get the role of the user after successful sign up
            User user = userRepository.findByEmail(userDTO.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found after signup"));
            String role = user.getRole();

            // Return success response with role information
            CommonResponse<String> successResponse = new CommonResponse<>("success", "User registered successfully", role);
            return ResponseEntity.ok(successResponse);

        } catch (Exception e) {
            // In case of any errors, return error response
            CommonResponse<String> errorResponse = new CommonResponse<>("error", "Sign-up failed: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);  // Internal Server Error
        }
    }

}
