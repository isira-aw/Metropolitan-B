package com.example.repository;

import com.example.entity.JobCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobCardRepository extends JpaRepository<JobCard, Long> {

    // Custom method to find job cards by jobid
    Optional<JobCard> findByJobid(String jobid);

    // Custom method to delete job card by jobid
    void deleteByJobid(String jobid);

    // Custom method to count job cards by jobid
    long countByJobid(String jobid);

    // Custom method to find job cards by assignTo email
    List<JobCard> findByAssignToEmail(String email);
}
