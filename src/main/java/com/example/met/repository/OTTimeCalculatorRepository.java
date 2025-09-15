package com.example.met.repository;

import com.example.met.entity.Employee;
import com.example.met.entity.OTtimeCalculator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OTTimeCalculatorRepository extends JpaRepository<OTtimeCalculator, UUID> {

    // Find by employee and date
    Optional<OTtimeCalculator> findByEmployeeAndDate(Employee employee, LocalDate date);

    // Find by employee email and date (alternative method)
    @Query("SELECT o FROM OTtimeCalculator o WHERE o.employee.email = :email AND o.date = :date")
    Optional<OTtimeCalculator> findByEmployeeEmailAndDate(@Param("email") String email, @Param("date") LocalDate date);

    // Find all entries for an employee
    List<OTtimeCalculator> findByEmployeeOrderByDateDesc(Employee employee);

    // Find all entries for an employee by email
    @Query("SELECT o FROM OTtimeCalculator o WHERE o.employee.email = :email ORDER BY o.date DESC")
    List<OTtimeCalculator> findByEmployeeEmailOrderByDateDesc(@Param("email") String email);

    // Find entries for an employee within date range
    @Query("SELECT o FROM OTtimeCalculator o WHERE o.employee.email = :email AND o.date BETWEEN :startDate AND :endDate ORDER BY o.date ASC")
    List<OTtimeCalculator> findByEmployeeEmailAndDateBetween(
            @Param("email") String email,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Find all entries for a specific date
    List<OTtimeCalculator> findByDate(LocalDate date);

    // Find entries within date range
    List<OTtimeCalculator> findByDateBetweenOrderByDateAsc(LocalDate startDate, LocalDate endDate);

    // Check if entry exists for employee on specific date
    boolean existsByEmployeeAndDate(Employee employee, LocalDate date);

    // Count entries for employee within date range
    @Query("SELECT COUNT(o) FROM OTtimeCalculator o WHERE o.employee.email = :email AND o.date BETWEEN :startDate AND :endDate")
    Long countByEmployeeEmailAndDateBetween(
            @Param("email") String email,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}