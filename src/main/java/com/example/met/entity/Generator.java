package com.example.met.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt != null) {
            createdAt = createdAt.truncatedTo(ChronoUnit.MILLIS);
        }
        if (updatedAt != null) {
            updatedAt = updatedAt.truncatedTo(ChronoUnit.MILLIS);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (updatedAt != null) {
            updatedAt = updatedAt.truncatedTo(ChronoUnit.MILLIS);
        }
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt != null ? createdAt.truncatedTo(ChronoUnit.MILLIS) : null;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt != null ? updatedAt.truncatedTo(ChronoUnit.MILLIS) : null;
    }
}