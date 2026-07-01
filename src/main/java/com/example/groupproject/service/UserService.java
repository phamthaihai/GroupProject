package com.example.groupproject.service;

import com.example.groupproject.entity.User;
import com.example.groupproject.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service cho các thao tác cơ bản trên User.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User getById(Integer id) {
        return userRepository.findById(id).orElse(null);
    }
}
