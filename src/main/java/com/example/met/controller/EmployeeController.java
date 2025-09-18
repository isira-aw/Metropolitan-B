package com.example.met.controller;

import com.example.met.dto.request.RegisterRequest;
import com.example.met.dto.request.RegisterRequestAdmin;
import com.example.met.dto.response.ApiResponse;
import com.example.met.dto.response.EmployeeResponse;
import com.example.met.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> getAllEmployees() {
        try {
            log.info("Request to get all employees");

            List<EmployeeResponse> employees = employeeService.getAllEmployees();
            ApiResponse<List<EmployeeResponse>> response = ApiResponse.success(
                    "Employees retrieved successfully", employees);

            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            log.error("Security error while retrieving all employees", e);
            ApiResponse<List<EmployeeResponse>> response = ApiResponse.error(
                    "Access denied. Insufficient privileges", null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            log.error("Error retrieving all employees", e);
            ApiResponse<List<EmployeeResponse>> response = ApiResponse.error(
                    "Failed to retrieve employees", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/{email}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> getEmployeeByEmail(@PathVariable String email) {
        try {
            log.info("Request to get employee by email: {}", email);

            // Validate email parameter
            if (email == null || email.trim().isEmpty()) {
                ApiResponse<EmployeeResponse> response = ApiResponse.error("Employee email cannot be empty", null);
                return ResponseEntity.badRequest().body(response);
            }

            // Validate email format
            if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
                ApiResponse<EmployeeResponse> response = ApiResponse.error("Invalid email format", null);
                return ResponseEntity.badRequest().body(response);
            }

            EmployeeResponse employee = employeeService.getEmployeeResponse(email);
            ApiResponse<EmployeeResponse> response = ApiResponse.success(
                    "Employee retrieved successfully", employee);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Employee not found with email: {}", email, e);
            ApiResponse<EmployeeResponse> response = ApiResponse.error("Employee not found with the provided email", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error retrieving employee by email: {}", email, e);
            ApiResponse<EmployeeResponse> response = ApiResponse.error(
                    "Failed to retrieve employee", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

//    @PutMapping("/{email}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
//    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
//            @PathVariable String email,
//            @Valid @RequestBody RegisterRequest request) {
//        try {
//            log.info("Request to update employee: {}", email);
//
//            // Validate email parameter
//            if (email == null || email.trim().isEmpty()) {
//                ApiResponse<EmployeeResponse> response = ApiResponse.error("Employee email cannot be empty", null);
//                return ResponseEntity.badRequest().body(response);
//            }
//
//            // Validate email format
//            if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
//                ApiResponse<EmployeeResponse> response = ApiResponse.error("Invalid email format", null);
//                return ResponseEntity.badRequest().body(response);
//            }
//
//            // Additional validation for email consistency
//            if (request.getEmail() != null && !request.getEmail().equals(email)) {
//                ApiResponse<EmployeeResponse> response = ApiResponse.error("Email in request body must match path parameter", null);
//                return ResponseEntity.badRequest().body(response);
//            }
//
//            EmployeeResponse updatedEmployee = employeeService.updateEmployee(email, request);
//            ApiResponse<EmployeeResponse> response = ApiResponse.success(
//                    "Employee updated successfully", updatedEmployee);
//
//            return ResponseEntity.ok(response);
//        } catch (IllegalArgumentException e) {
//            log.error("Employee not found for update with email: {} or invalid request data: {}", email, e.getMessage(), e);
//
//            if (e.getMessage().toLowerCase().contains("not found") || e.getMessage().toLowerCase().contains("does not exist")) {
//                ApiResponse<EmployeeResponse> response = ApiResponse.error("Employee not found with the provided email", null);
//                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
//            } else {
//                ApiResponse<EmployeeResponse> response = ApiResponse.error("Invalid request data: " + e.getMessage(), null);
//                return ResponseEntity.badRequest().body(response);
//            }
//        } catch (DataIntegrityViolationException e) {
//            log.error("Data integrity violation while updating employee: {}", email, e);
//            ApiResponse<EmployeeResponse> response = ApiResponse.error("Email or employee identifier already exists", null);
//            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
//        } catch (SecurityException e) {
//            log.error("Security error while updating employee: {}", email, e);
//            ApiResponse<EmployeeResponse> response = ApiResponse.error("Access denied. Insufficient privileges to update employee", null);
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
//        } catch (Exception e) {
//            log.error("Error updating employee: {}", email, e);
//            ApiResponse<EmployeeResponse> response = ApiResponse.error(
//                    "Failed to update employee", null);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }

    @PutMapping("/{email}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployeeForAdmin(
            @PathVariable String email,
            @Valid @RequestBody RegisterRequestAdmin request) {
        try {
            log.info("Request to update employee: {}", email);

            if (email == null || email.trim().isEmpty()) {
                ApiResponse<EmployeeResponse> response = ApiResponse.error("Employee email cannot be empty", null);
                return ResponseEntity.badRequest().body(response);
            }

            if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
                ApiResponse<EmployeeResponse> response = ApiResponse.error("Invalid email format", null);
                return ResponseEntity.badRequest().body(response);
            }

            EmployeeResponse updatedEmployee = employeeService.updateEmployeeByAdmin(email, request);
            ApiResponse<EmployeeResponse> response = ApiResponse.success(
                    "Employee updated successfully", updatedEmployee);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Employee not found for update with email: {} or invalid request data: {}", email, e.getMessage(), e);

            if (e.getMessage().toLowerCase().contains("not found") || e.getMessage().toLowerCase().contains("does not exist")) {
                ApiResponse<EmployeeResponse> response = ApiResponse.error("Employee not found with the provided email", null);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else {
                ApiResponse<EmployeeResponse> response = ApiResponse.error("Invalid request data: " + e.getMessage(), null);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while updating employee: {}", email, e);
            ApiResponse<EmployeeResponse> response = ApiResponse.error("Email or employee identifier already exists", null);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (SecurityException e) {
            log.error("Security error while updating employee: {}", email, e);
            ApiResponse<EmployeeResponse> response = ApiResponse.error("Access denied. Insufficient privileges to update employee", null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (Exception e) {
            log.error("Error updating employee: {}", email, e);
            ApiResponse<EmployeeResponse> response = ApiResponse.error(
                    "Failed to update employee", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable String email) {
        try {
            log.info("Request to delete employee: {}", email);

            // Validate email parameter
            if (email == null || email.trim().isEmpty()) {
                ApiResponse<Void> response = ApiResponse.error("Employee email cannot be empty", null);
                return ResponseEntity.badRequest().body(response);
            }

            // Validate email format
            if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
                ApiResponse<Void> response = ApiResponse.error("Invalid email format", null);
                return ResponseEntity.badRequest().body(response);
            }

            employeeService.deleteEmployee(email);
            ApiResponse<Void> response = ApiResponse.success("Employee deleted successfully");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Employee not found for deletion with email: {}", email, e);
            ApiResponse<Void> response = ApiResponse.error("Employee not found with the provided email", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation while deleting employee: {}", email, e);
            ApiResponse<Void> response = ApiResponse.error("Cannot delete employee. They are associated with existing job cards or other records", null);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (SecurityException e) {
            log.error("Security error while deleting employee: {}", email, e);
            ApiResponse<Void> response = ApiResponse.error("Access denied. Admin privileges required", null);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        } catch (RuntimeException e) {
            log.error("Business logic error while deleting employee: {}", email, e);
            ApiResponse<Void> response = ApiResponse.error("Cannot delete employee: " + e.getMessage(), null);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (Exception e) {
            log.error("Error deleting employee: {}", email, e);
            ApiResponse<Void> response = ApiResponse.error("Failed to delete employee", null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}