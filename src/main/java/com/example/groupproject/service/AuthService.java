package com.example.groupproject.service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.example.groupproject.dto.RegisterDTO;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.entity.enums.UserStatus;
import com.example.groupproject.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
@Service
public class AuthService {
    private final UserRepository userRepo;
    private final BCryptPasswordEncoder encoder;
    private final EmailService emailService;
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
}