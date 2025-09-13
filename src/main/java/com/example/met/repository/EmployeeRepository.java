package com.example.met.repository;

import com.example.met.entity.Employee;
import com.example.met.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
    Optional<Employee> findByEmail(String email);

    List<Employee> findByRole(Role role);

    boolean existsByEmail(String email);

    @Query("SELECT e FROM Employee e WHERE e.name LIKE %:name%")
    List<Employee> findByNameContaining(@Param("name") String name);

    @Query("SELECT e FROM Employee e WHERE e.email IN :emails")
    List<Employee> findByEmailIn(@Param("emails") List<String> emails);

    @Query("SELECT e.name FROM Employee e WHERE e.email = :email")
    Optional<String> findNameByEmail(@Param("email") String email);
}