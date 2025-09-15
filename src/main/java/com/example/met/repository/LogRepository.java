package com.example.met.repository;

import com.example.met.entity.Employee;
import com.example.met.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LogRepository extends JpaRepository<Log, UUID> {

    List<Log> findByEmployeeEmail(String employeeEmail);

    List<Log> findByDate(LocalDate date);

    List<Log> findByEmployeeEmailAndDate(String employeeEmail, LocalDate date);

    List<Log> findByAction(String action);

    List<Log> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT l FROM Log l WHERE l.employee.email = :email ORDER BY l.createdAt DESC")
    List<Log> findByEmployeeEmailOrderByCreatedAtDesc(@Param("email") String email);

    @Query("SELECT l FROM Log l WHERE l.employee.email = :email AND l.action = :action")
    List<Log> findByEmployeeEmailAndAction(@Param("email") String email, @Param("action") String action);

    @Query("SELECT COUNT(l) FROM Log l WHERE l.employee.email = :email AND l.date = :date")
    long countByEmployeeEmailAndDate(@Param("email") String email, @Param("date") LocalDate date);

    @Query("SELECT l FROM Log l WHERE l.createdAt >= :fromDateTime AND l.time IS NOT NULL ORDER BY l.createdAt DESC")
    List<Log> findRecentLogs(@Param("fromDateTime") LocalDateTime fromDateTime);

    // Also update the findAll equivalent
    @Query("SELECT l FROM Log l WHERE l.time IS NOT NULL ORDER BY l.createdAt DESC")
    List<Log> findAllValidLogs();


    /**
     * Find logs by employee email within a date range
     */
    List<Log> findByEmployeeEmailAndDateBetween(String email, LocalDate startDate, LocalDate endDate);

    List<Log> findByEmployeeAndDateOrderByTimeAsc(Employee employee, LocalDate date);

    /**
     * Find all logs for an employee between dates, ordered by date and time
     */
    @Query("SELECT l FROM Log l WHERE l.employee = :employee AND l.date BETWEEN :startDate AND :endDate ORDER BY l.date ASC, l.time ASC")
    List<Log> findByEmployeeAndDateRangeOrderByDateTime(@Param("employee") Employee employee,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

}