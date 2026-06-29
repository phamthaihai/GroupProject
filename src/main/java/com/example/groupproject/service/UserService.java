package com.example.groupproject.service;

import com.example.groupproject.entity.User;
import com.example.groupproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service cho các thao tác cơ bản trên User.
 * Các thao tác phức tạp hơn (tạo, khóa, deactivate) nằm trong UserManagementService.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public User getByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User getById(Integer id) {
        return userRepository.findById(id).orElse(null);
    }
}
