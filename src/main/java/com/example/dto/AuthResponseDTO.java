package com.example.dto;

import lombok.Data;

@Data
public class AuthResponseDTO {
    private String token;
    private String role;

}
