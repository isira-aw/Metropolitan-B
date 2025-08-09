package com.example.met.dto.response;

import com.example.met.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeResponse {
    private String email;
    private String name;
    private String contactNumber;
    private Role role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}