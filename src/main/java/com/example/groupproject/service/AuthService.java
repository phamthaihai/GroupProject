package com.example.groupproject.service;
import com.example.groupproject.dto.ForgotPasswordDTO;
import com.example.groupproject.dto.LoginDTO;
import com.example.groupproject.dto.ResetPasswordDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.example.groupproject.dto.RegisterDTO;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.entity.enums.UserStatus;
import com.example.groupproject.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
@Service
public class AuthService {
    private final UserRepository userRepo;
    @Autowired
    private  BCryptPasswordEncoder encoder;
    private final EmailService emailService;
    @Autowired
    private UserRepository userRepository;
    public AuthService(UserRepository userRepo,
                       BCryptPasswordEncoder encoder,
                       EmailService emailService) {
        this.userRepo = userRepo;
        this.encoder = encoder;
        this.emailService = emailService;
    }
    @Transactional
    public void register(RegisterDTO dto, String baseUrl) {
        String email = dto.getEmail().trim().toLowerCase();
        if(userRepo.existsByEmail(email)){
            throw new IllegalArgumentException("Email already exists");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        User user = new User();
        user.setEmail(email);
        user.setFullName(dto.getFullname().trim());
        user.setUsername(dto.getEmail().split("@")[0]);
        user.setPasswordHash(encoder.encode(dto.getPassword()));
        user.setRole(UserRole.CANDIDATE);
        user.setStatus(UserStatus.INACTIVE);
        user.setEmailVerified(false);
        user.setVerifyToken(token);
        user.setVerifyTokenExpiresAt(LocalDateTime.now().plusMinutes(30));
        user.setUpdatedAt(Instant.now());
        userRepo.save(user);
        String verifyLink =
                baseUrl + "/verify?token=" + token;
        emailService.sendVerificationEmail(
                user.getEmail(),
                user.getFullName(),
                verifyLink
        );
    }
    @Transactional
    public void verifyEmail(String token){
        User user =
                userRepo.findByVerifyToken(token).orElseThrow(
                        () -> new IllegalArgumentException("Invalid verification token"));
        if(user.getVerifyTokenExpiresAt() == null || user.getVerifyTokenExpiresAt()
                        .isBefore(LocalDateTime.now())){
            throw new IllegalArgumentException("Token expired");
        }
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE
        );
        user.setVerifyToken(null);
        user.setVerifyTokenExpiresAt(null);
        user.setUpdatedAt(Instant.now());
        userRepo.save(user);
    }

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

    public User getCurrentUser(HttpSession session) {
        if (session != null && session.getAttribute("user") != null) {
            return (User) session.getAttribute("user");
        }
        return null;
    }

    public void requireRole(User user, UserRole requiredRole) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        if (user.getRole() != requiredRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    public void requireAnyRole(User user, UserRole... requiredRoles) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        for (UserRole role : requiredRoles) {
            if (user.getRole() == role) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
    }

    public void requireAuthenticated(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
    }
}