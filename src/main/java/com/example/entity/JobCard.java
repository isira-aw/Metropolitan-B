package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class JobCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
}
