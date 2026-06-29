package com.example.groupproject.security;

import com.example.groupproject.entity.User;
import com.example.groupproject.entity.enums.UserStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security UserDetails wrapper cho entity User.
 *
 * Mapping:
 *   - authorities: "ROLE_" + user.role (vd: ROLE_ADMIN, ROLE_HR_MANAGER)
 *   - password: user.passwordHash (BCrypt encoded)
 *   - username: user.username
 *   - accountNonLocked: true nếu status != LOCKED
 *   - enabled: true nếu status == ACTIVE
 */
@Getter
public class TalentHubUserDetails implements UserDetails {

    private final User user;

    public TalentHubUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /** Tài khoản INACTIVE cũng bị block (isEnabled = false) */
    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != UserStatus.LOCKED;
    }

    /** Chỉ ACTIVE mới được đăng nhập */
    @Override
    public boolean isEnabled() {
        return user.getStatus() == UserStatus.ACTIVE;
    }
}
