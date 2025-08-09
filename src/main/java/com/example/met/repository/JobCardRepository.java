package com.example.met.repository;

import com.example.met.entity.JobCard;
import com.example.met.enums.JobCardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface JobCardRepository extends JpaRepository<JobCard, UUID> {

    List<JobCard> findByJobType(JobCardType jobType);

    List<JobCard> findByGeneratorGeneratorId(UUID generatorId);

    List<JobCard> findByDate(LocalDate date);


    @Query("SELECT j FROM JobCard j WHERE :email MEMBER OF j.employeeEmails")
    List<JobCard> findByEmployeeEmail(@Param("email") String email);

    @Query("SELECT j FROM JobCard j ORDER BY j.createdAt DESC")
    List<JobCard> findAllOrderByCreatedAtDesc();

    @Query("SELECT j FROM JobCard j WHERE j.jobType = :jobType AND j.date >= :fromDate")
    List<JobCard> findByJobTypeAndDateAfter(@Param("jobType") JobCardType jobType, @Param("fromDate") LocalDate fromDate);
}