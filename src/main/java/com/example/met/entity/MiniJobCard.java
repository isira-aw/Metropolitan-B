package com.example.met.entity;

import com.example.met.enums.JobStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "mini_job_cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniJobCard {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "mini_job_card_id")
    private UUID miniJobCardId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_card_id", nullable = false)
    private JobCard jobCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_email", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.PENDING;

    @Column(nullable = false)
    private LocalDate date;

    private String location;

    private LocalTime time;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime updatedAt;

    @PrePersist
    private void setDefaults() {
        if (this.time == null) {
            this.time = LocalTime.now(java.time.ZoneId.of("Asia/Colombo"));
        }
        if (this.date == null) {
            this.date = LocalDate.now(java.time.ZoneId.of("Asia/Colombo"));
        }
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