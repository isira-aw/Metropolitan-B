package com.example.met.service;

import com.example.met.dto.request.RegisterRequest;
import com.example.met.dto.request.RegisterRequestAdmin;
import com.example.met.dto.response.EmployeeResponse;
import com.example.met.entity.Employee;
import com.example.met.exception.DuplicateResourceException;
import com.example.met.exception.ResourceNotFoundException;
import com.example.met.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Employee not found with email: " + email));

        return User.builder()
                .username(employee.getEmail())
                .password(employee.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name())))
                .build();
    }

    @Transactional
    public Employee createEmployee(RegisterRequest request) {
        log.info("Creating new employee with email: {}", request.getEmail());

        if (employeeRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Employee with email " + request.getEmail() + " already exists");
        }

        Employee employee = new Employee();
        employee.setName(request.getName());
        employee.setEmail(request.getEmail());
        employee.setContactNumber(request.getContactNumber());
        employee.setRole(request.getRole());
        employee.setPassword(passwordEncoder.encode(request.getPassword()));

        Employee savedEmployee = employeeRepository.save(employee);
        log.info("Employee created successfully with email: {}", savedEmployee.getEmail());
        return savedEmployee;
    }

    public Employee findByEmail(String email) {
        return employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with email: " + email));
    }

    public EmployeeResponse getEmployeeResponse(String email) {
        Employee employee = findByEmail(email);
        return convertToResponse(employee);
    }

    public List<EmployeeResponse> getAllEmployees() {
        log.info("Fetching all employees");
        return employeeRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<EmployeeResponse> getEmployeesByEmails(List<String> emails) {
        log.info("Fetching employees by emails: {}", emails);
        return employeeRepository.findByEmailIn(emails)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public EmployeeResponse updateEmployee(String email, RegisterRequest request) {
        log.info("Updating employee with email: {}", email);

        Employee employee = findByEmail(email);
        employee.setName(request.getName());
        employee.setContactNumber(request.getContactNumber());
        employee.setRole(request.getRole());

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            employee.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        employee = employeeRepository.save(employee);
        log.info("Employee updated successfully with email: {}", employee.getEmail());
        return convertToResponse(employee);
    }

    @Transactional
    public EmployeeResponse updateEmployeeByAdmin(String email, RegisterRequestAdmin request) {
        log.info("Updating employee with email: {}", email);

        Employee employee = findByEmail(email);
        employee.setName(request.getName());
        employee.setContactNumber(request.getContactNumber());
        employee.setRole(request.getRole());

        employee = employeeRepository.save(employee);
        log.info("Employee updated by Admin : {}", employee.getEmail());
        return convertToResponse(employee);
    }

    @Transactional
    public void deleteEmployee(String email) {
        log.info("Deleting employee with email: {}", email);
        Employee employee = findByEmail(email);
        employeeRepository.delete(employee);
        log.info("Employee deleted successfully with email: {}", email);
    }

    private EmployeeResponse convertToResponse(Employee employee) {
        EmployeeResponse response = new EmployeeResponse();
        response.setEmail(employee.getEmail());
        response.setName(employee.getName());
        response.setContactNumber(employee.getContactNumber());
        response.setRole(employee.getRole());
        response.setCreatedAt(employee.getCreatedAt());
        response.setUpdatedAt(employee.getUpdatedAt());
        return response;
    }
}