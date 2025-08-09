package com.example.met.controller;

import com.example.met.dto.request.RegisterRequest;
import com.example.met.dto.response.ApiResponse;
import com.example.met.dto.response.EmployeeResponse;
import com.example.met.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getAllEmployees() {
        log.info("Request to get all employees");

        List<EmployeeResponse> employees = employeeService.getAllEmployees();
        ApiResponse<List<EmployeeResponse>> response = ApiResponse.success(
                "Employees retrieved successfully", employees);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{email}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeByEmail(@PathVariable String email) {
        log.info("Request to get employee by email: {}", email);

        EmployeeResponse employee = employeeService.getEmployeeResponse(email);
        ApiResponse<EmployeeResponse> response = ApiResponse.success(
                "Employee retrieved successfully", employee);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{email}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable String email,
            @Valid @RequestBody RegisterRequest request) {
        log.info("Request to update employee: {}", email);

        EmployeeResponse updatedEmployee = employeeService.updateEmployee(email, request);
        ApiResponse<EmployeeResponse> response = ApiResponse.success(
                "Employee updated successfully", updatedEmployee);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{email}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable String email) {
        log.info("Request to delete employee: {}", email);

        employeeService.deleteEmployee(email);
        ApiResponse<Void> response = ApiResponse.success("Employee deleted successfully");

        return ResponseEntity.ok(response);
    }
}