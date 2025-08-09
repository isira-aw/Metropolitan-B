package com.example.met.service;

import com.example.met.dto.request.GeneratorRequest;
import com.example.met.dto.response.GeneratorResponse;
import com.example.met.entity.Generator;
import com.example.met.exception.ResourceNotFoundException;
import com.example.met.repository.GeneratorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeneratorService {

    private final GeneratorRepository generatorRepository;

    @Transactional
    public GeneratorResponse createGenerator(GeneratorRequest request) {
        log.info("Creating new generator with name: {}", request.getName());

        Generator generator = new Generator();
        generator.setName(request.getName());
        generator.setCapacity(request.getCapacity());
        generator.setContactNumber(request.getContactNumber());
        generator.setEmail(request.getEmail());
        generator.setDescription(request.getDescription());

        Generator savedGenerator = generatorRepository.save(generator);
        log.info("Generator created successfully with ID: {}", savedGenerator.getGeneratorId());
        return convertToResponse(savedGenerator);
    }

    public Generator findById(UUID id) {
        return generatorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Generator not found with id: " + id));
    }

    public GeneratorResponse getGeneratorResponse(UUID id) {
        Generator generator = findById(id);
        return convertToResponse(generator);
    }

    public List<GeneratorResponse> getAllGenerators() {
        log.info("Fetching all generators");
        return generatorRepository.findAllOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<GeneratorResponse> searchGeneratorsByName(String name) {
        log.info("Searching generators by name: {}", name);
        return generatorRepository.findByNameContaining(name)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public GeneratorResponse updateGenerator(UUID id, GeneratorRequest request) {
        log.info("Updating generator with ID: {}", id);

        Generator generator = findById(id);
        generator.setName(request.getName());
        generator.setCapacity(request.getCapacity());
        generator.setContactNumber(request.getContactNumber());
        generator.setEmail(request.getEmail());
        generator.setDescription(request.getDescription());

        generator = generatorRepository.save(generator);
        log.info("Generator updated successfully with ID: {}", generator.getGeneratorId());
        return convertToResponse(generator);
    }

    @Transactional
    public void deleteGenerator(UUID id) {
        log.info("Deleting generator with ID: {}", id);
        Generator generator = findById(id);
        generatorRepository.delete(generator);
        log.info("Generator deleted successfully with ID: {}", id);
    }

    private GeneratorResponse convertToResponse(Generator generator) {
        GeneratorResponse response = new GeneratorResponse();
        response.setGeneratorId(generator.getGeneratorId());
        response.setName(generator.getName());
        response.setCapacity(generator.getCapacity());
        response.setContactNumber(generator.getContactNumber());
        response.setEmail(generator.getEmail());
        response.setDescription(generator.getDescription());
        response.setCreatedAt(generator.getCreatedAt());
        response.setUpdatedAt(generator.getUpdatedAt());
        return response;
    }
}