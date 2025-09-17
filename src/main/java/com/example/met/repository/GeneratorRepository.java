package com.example.met.repository;

import com.example.met.entity.Generator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

@Repository
public interface GeneratorRepository extends JpaRepository<Generator, UUID> {

    @Query("SELECT g FROM Generator g WHERE g.name LIKE %:name%")
    List<Generator> findByNameContaining(@Param("name") String name);

    List<Generator> findByCapacityContaining(String capacity);

    List<Generator> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT COUNT(g) FROM Generator g")
    long getGeneratorCount();

    // Or using JPA method naming (works with all databases):
    List<Generator> findByNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);
}