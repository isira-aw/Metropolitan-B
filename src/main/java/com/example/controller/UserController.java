package com.example.controller;

import com.example.dto.CommonResponse;
import com.example.dto.EmployeeDTO;
import com.example.entity.User;
import com.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // Endpoint to get all users with role 'employee'
    @GetMapping("/employees")
    public ResponseEntity<CommonResponse<List<EmployeeDTO>>> getAllEmployees() {
        try {
            List<EmployeeDTO> employees = userService.getAllEmployees();
            return ResponseEntity.ok(new CommonResponse<>("success", "Fetched employees successfully", employees));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new CommonResponse<>("error", "Failed to fetch employees: " + e.getMessage(), null));
        }
    }
}
