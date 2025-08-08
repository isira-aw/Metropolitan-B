package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
public class JobCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String jobid;
    private String generatorid;
    private String title;
    private String description;
    private int hoursnumber;

    @Column(nullable = true)
    private String workstatus ;

    @ManyToOne
    @JoinColumn(name = "assigned_to", referencedColumnName = "email")
    private User assignTo;

    private LocalDate date;
}
