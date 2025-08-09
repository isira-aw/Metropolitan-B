package com.example.met.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "generators")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Generator {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "generator_id")
    private UUID generatorId;

    @Column(nullable = false)
    private String name;

    private String capacity;

    @Column(name = "contact_number")
    private String contactNumber;

    private String email;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}