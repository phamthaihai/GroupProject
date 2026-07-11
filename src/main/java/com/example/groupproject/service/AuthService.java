package com.example.groupproject.service;
import com.example.groupproject.dto.ForgotPasswordDTO;
import com.example.groupproject.dto.LoginDTO;
import com.example.groupproject.dto.ResetPasswordDTO;
import com.example.groupproject.exception.AdminLockedException;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.example.groupproject.dto.RegisterDTO;
import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserRole;
import com.example.groupproject.entity.enums.UserStatus;
import com.example.groupproject.repository.UserRepository;
import com.example.groupproject.exception.AccountLockedException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
@Service
public class AuthService {
    public static final String SESSION_USER_ID = "loggedInUserId";

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
            throw new IllegalArgumentException("This email address is already registered");
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
    public User login(LoginDTO loginDTO, HttpSession session) {
        if (loginDTO == null ||
                loginDTO.getEmail() == null || loginDTO.getEmail().isBlank() ||
                loginDTO.getPassword() == null || loginDTO.getPassword().isBlank()) {
            throw new IllegalArgumentException("Thông tin điền vào không hợp lệ!");
        }

        String email = loginDTO.getEmail().trim().toLowerCase();
        User user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Incorrect username or password!"));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AdminLockedException("Tài khoản đã bị quản trị viên khóa vĩnh viễn.");
        }

        if (user.getLockedAt() != null && !user.isLoginLocked()) {
            user.setFailedLoginCount((short) 0);
            user.setLockedAt(null);
            userRepo.save(user);
        }

        if (user.isLoginLocked()) {
            throw new AccountLockedException("Your account has\n" +
                    "been temporarily locked after too many failed attempts.\n" +
                    "Try again in 10 minutes or contact your administrator");
        }

        if (!encoder.matches(loginDTO.getPassword(), user.getPasswordHash())) {
            int newFailedCount = user.getFailedLoginCount() + 1;
            user.setFailedLoginCount((short) newFailedCount);
            if (newFailedCount >= 5) {
                user.setLockedAt(Instant.now());
                userRepo.save(user);
                throw new AccountLockedException("Your account has\n" +
                        "been temporarily locked after too many failed attempts.\n" +
                        "Try again in 10 minutes or contact your administrator");
            }

            userRepo.save(user);
            throw new IllegalArgumentException("Incorrect username or password!");
        }

        user.setFailedLoginCount((short) 0);
        user.setLockedAt(null);
        userRepo.save(user);

        session.setAttribute(SESSION_USER_ID, user.getId());
        return user;
    }

    @Transactional
    public void doLogout(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_USER_ID);
            session.invalidate();
        }
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

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);

        if (userOptional.isPresent()) {
            String otp = String.format("%06d", (int) (Math.random() * 1000000));
            User user = userOptional.get();
            user.setVerifyToken(otp);
            user.setVerifyTokenExpiresAt(LocalDateTime.now().plusMinutes(10));
            userRepository.save(user);
            return otp;
        }
        return "NOT_FOUND";
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

    @Transactional
    public void changePassword(Integer userId, String oldPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!matchesPassword(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Incorrect current password.");
        }
        if (matchesPassword(newPassword, user.getPasswordHash())) {
            throw new RuntimeException("New password must be different from your current password.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new RuntimeException("Passwords do not match.");
        }
        user.setPasswordHash(encodePassword(newPassword));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(
            HttpSession session
    ) {
        //Kiểm tra đã đăng nhập?
        if (session == null) {
            return null;
        }
        //Lấy user Id từ session
        Object value = session.getAttribute(SESSION_USER_ID);
        //kiểm tra nếu kiểu của id lưu trong session
        if (!(value instanceof Integer userId)) {
            return null;
        }
        //Trả về User từ database
        return userRepository.findById(userId).orElse(null);
    }

    public void requireAuthenticated(User user) {
        //Kiểm tra đã đăng nhập chưa
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
    }
    public void requireRole(User user, UserRole role) {
        requireAuthenticated(user);
        //kiểm tra 1 role
        if (user.getRole() != role) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    public void requireAnyRole(User user, UserRole... roles) {
        requireAuthenticated(user);
        //kiểm tra nhiều role
        if (!hasAnyRole(user, roles)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    public boolean hasAnyRole(User user, UserRole... roles) {

        if (user == null || roles == null) {
            return false;
        }
        for (UserRole role : roles) {
            if (user.getRole() == role) {
                return true;
            }
        }
        return false;
    }
}