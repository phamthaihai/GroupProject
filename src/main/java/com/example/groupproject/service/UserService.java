package com.example.groupproject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.groupproject.dto.ForgotPasswordDTO;
import com.example.groupproject.dto.LoginDTO;
import com.example.groupproject.dto.ResetPasswordDTO;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserStatus;
import com.example.groupproject.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

}