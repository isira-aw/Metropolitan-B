
package com.example.met.dto.request;

import com.example.met.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RegisterRequestAdmin {
    @NotBlank(message = "Name is required")
    private String name;

    private String contactNumber;

    @NotNull(message = "Role is required")
    private Role role;
}