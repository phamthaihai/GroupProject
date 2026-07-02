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

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder encoder;

    public User login(LoginDTO loginDTO) {

        if (loginDTO == null || loginDTO.getEmail() == null || loginDTO.getPassword() == null) {
            return null;
        }
        String email = loginDTO.getEmail().trim().toLowerCase();
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
        if (optionalUser.isEmpty()) {
            return null;
        }
        User user = optionalUser.get();
        if (!matchesPassword(loginDTO.getPassword(), user.getPasswordHash())) {
            return null;
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            return null;
        }
        return user;
    }
    public String encodePassword(String password) {
        return encoder.encode(password);
    }
    public boolean matchesPassword(String rawPassword, String passwordHash) {

        if (rawPassword == null || passwordHash == null) {
            return false;
        }
        return encoder.matches(rawPassword, passwordHash);
    }
    @Transactional
    public String createResetPasswordOtp(ForgotPasswordDTO dto) {

        String email = dto.getEmail().trim().toLowerCase();

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new RuntimeException("Email not found"));
        String otp = String.format("%06d", (int) (Math.random() * 1000000));
        user.setVerifyToken(otp);
        user.setVerifyTokenExpiresAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        return otp;
    }
    @Transactional
    public void resetPassword(ResetPasswordDTO dto) {

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Password not match");
        }
        User user = userRepository.findByEmailIgnoreCase(dto.getEmail().trim().toLowerCase())
                .filter(u -> dto.getOtp().equals(u.getVerifyToken()))
                .orElseThrow(() -> new RuntimeException("Invalid OTP"));
        if (user.getVerifyTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }
        user.setPasswordHash(encodePassword(dto.getNewPassword()));
        user.setVerifyToken(null);
        user.setVerifyTokenExpiresAt(null);
        userRepository.save(user);
    }
    @Transactional
    public void changePassword(Integer userId, String oldPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!matchesPassword(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Wrong password");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Password not match");
        }
        user.setPasswordHash(encodePassword(newPassword));
        userRepository.save(user);
    }
}