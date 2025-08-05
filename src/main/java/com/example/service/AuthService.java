package com.example.service;

import com.example.dto.UserDTO;
import com.example.entity.User;
import com.example.repository.UserRepository;
import com.example.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String signIn(UserDTO userDTO) {
        // Authenticate the user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userDTO.getEmail(), userDTO.getPassword())
        );

        // After authentication, generate JWT token
        return jwtTokenProvider.generateToken(authentication);
    }

    public String signUp(UserDTO userDTO) {
        // Check if the email already exists
        Optional<User> existingUser = userRepository.findByEmail(userDTO.getEmail());
        if (existingUser.isPresent()) {
            return "User with this email already exists."; // This message is used in controller for error response
        }

        // Encode the password before saving
        String encodedPassword = passwordEncoder.encode(userDTO.getPassword());

        // Default role if not provided
        String role = userDTO.getRole() != null ? userDTO.getRole() : "employee";  // Default to "employee"

        // Create a new User entity with the role
        User user = new User(userDTO.getEmail(), encodedPassword, userDTO.getName(), role);

        // Save the user entity to the database
        userRepository.save(user);

        return "User registered successfully";
    }
}
