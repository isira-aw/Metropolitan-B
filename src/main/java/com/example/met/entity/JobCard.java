package com.example.met.entity;

import com.example.met.enums.JobCardType;
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
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobCard {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "job_card_id")
    private UUID jobCardId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generator_id", nullable = false)
    private Generator generator;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false)
    private JobCardType jobType;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "estimated_time")
    private LocalTime estimatedTime;

    @ElementCollection
    @CollectionTable(name = "job_card_employees", joinColumns = @JoinColumn(name = "job_card_id"))
    @Column(name = "employee_email")
    private List<String> employeeEmails;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime updatedAt;

    @PrePersist
    private void generateJobId() {
        if (this.jobId == null) {
            this.jobId = UUID.randomUUID();
        }
        // Set default estimated time if not provided
        if (this.estimatedTime == null) {
            this.estimatedTime = LocalTime.now(java.time.ZoneId.of("Asia/Colombo"));
        }
        // Fix datetime nanosecond issue
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