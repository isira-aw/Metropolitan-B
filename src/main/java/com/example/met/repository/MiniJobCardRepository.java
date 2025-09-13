package com.example.met.repository;

import com.example.met.entity.MiniJobCard;
import com.example.met.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface MiniJobCardRepository extends JpaRepository<MiniJobCard, UUID> {

    List<MiniJobCard> findByEmployeeEmail(String employeeEmail);

    List<MiniJobCard> findByJobCardJobCardId(UUID jobCardId);

    List<MiniJobCard> findByStatus(JobStatus status);

    List<MiniJobCard> findByDate(LocalDate date);

    List<MiniJobCard> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT m FROM MiniJobCard m WHERE m.employee.email = :email AND m.status = :status")
    List<MiniJobCard> findByEmployeeEmailAndStatus(@Param("email") String email, @Param("status") JobStatus status);

    @Query("SELECT m FROM MiniJobCard m WHERE m.employee.email = :email AND m.date = :date")
    List<MiniJobCard> findByEmployeeEmailAndDate(@Param("email") String email, @Param("date") LocalDate date);

    @Query("SELECT m FROM MiniJobCard m WHERE m.employee.email = :email ORDER BY m.createdAt DESC")
    List<MiniJobCard> findByEmployeeEmailOrderByCreatedAtDesc(@Param("email") String email);

    @Query("SELECT COUNT(m) FROM MiniJobCard m WHERE m.employee.email = :email AND m.status = :status")
    long countByEmployeeEmailAndStatus(@Param("email") String email, @Param("status") JobStatus status);


    @Query("SELECT m FROM MiniJobCard m " +
            "JOIN FETCH m.jobCard " +
            "JOIN FETCH m.employee " +
            "WHERE m.employee.email = :employeeEmail " +
            "AND m.date >= :startDate " +
            "AND m.date <= :endDate " +
            "ORDER BY m.date DESC, m.updatedAt DESC")
    List<MiniJobCard> findByEmployeeEmailAndDateRange(
            @Param("employeeEmail") String employeeEmail,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT COUNT(m) FROM MiniJobCard m " +
            "WHERE m.employee.email = :employeeEmail " +
            "AND m.date >= :startDate " +
            "AND m.date <= :endDate")
    long countByEmployeeEmailAndDateRange(
            @Param("employeeEmail") String employeeEmail,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}