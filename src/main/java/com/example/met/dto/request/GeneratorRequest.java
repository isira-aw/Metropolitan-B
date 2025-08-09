package com.example.met.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GeneratorRequest {
    @NotBlank(message = "Generator name is required")
    private String name;

    private String capacity;

    private String contactNumber;

    private String email;

    private String description;
}