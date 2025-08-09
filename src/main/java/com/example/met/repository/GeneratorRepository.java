package com.example.met.repository;

import com.example.met.entity.Generator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GeneratorRepository extends JpaRepository<Generator, UUID> {

    @Query("SELECT g FROM Generator g WHERE g.name LIKE %:name%")
    List<Generator> findByNameContaining(@Param("name") String name);

    List<Generator> findByCapacityContaining(String capacity);

    @Query("SELECT g FROM Generator g ORDER BY g.createdAt DESC")
    List<Generator> findAllOrderByCreatedAtDesc();
}