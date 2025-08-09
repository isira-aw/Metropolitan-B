package com.example.met.service;

import com.example.met.dto.request.LoginRequest;
import com.example.met.dto.request.RegisterRequest;
import com.example.met.dto.response.LoginResponse;
import com.example.met.entity.Employee;
import com.example.met.exception.UnauthorizedException;
import com.example.met.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final EmployeeService employeeService;
    private final JwtTokenProvider tokenProvider;

    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            String token = tokenProvider.generateToken(authentication);
            Employee employee = employeeService.findByEmail(request.getEmail());

            log.info("Login successful for email: {}", request.getEmail());

            return new LoginResponse(
                    token,
                    employee.getEmail(),
                    employee.getName(),
                    employee.getRole(),
                    employee.getContactNumber()
            );
        } catch (AuthenticationException e) {
            log.error("Login failed for email: {}", request.getEmail());
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    public Employee register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());
        Employee employee = employeeService.createEmployee(request);
        log.info("Registration successful for email: {}", request.getEmail());
        return employee;
    }
}