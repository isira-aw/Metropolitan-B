package com.example.service;

import com.example.dto.EmployeeDTO;
import com.example.entity.User;
import com.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Fetch all users with role 'employee'
    public List<EmployeeDTO> getAllEmployees() {
        List<User> users = userRepository.findByRole("employee");

        // Convert the List<User> to List<EmployeeDTO>
        List<EmployeeDTO> employeeDTOs = users.stream().map(user -> {
            EmployeeDTO dto = new EmployeeDTO();
            dto.setId(user.getId());
            dto.setEmail(user.getEmail());
            dto.setName(user.getName());  // Add other fields you want to expose
            return dto;
        }).collect(Collectors.toList());

        return employeeDTOs;
    }
}
