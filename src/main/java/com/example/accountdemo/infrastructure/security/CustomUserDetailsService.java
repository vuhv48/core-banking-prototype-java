package com.example.accountdemo.infrastructure.security;

import com.example.accountdemo.infrastructure.persistence.security.UserJpaEntity;
import com.example.accountdemo.infrastructure.persistence.security.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Load UserDetails từ DB khi đăng nhập.
 *
 * <p><b>Vì sao cần class này:</b> gộp quyền từ roles + directPermissions thành authorities để build JWT.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserJpaRepository userJpaRepository;

    /** Load user + authorities (role permissions ∪ direct permissions). */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserJpaEntity entity = userJpaRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();

        // Quyền từ roles → role_permissions
        entity.getRoles().forEach(role ->
                role.getPermissions().forEach(p ->
                        authorities.add(new SimpleGrantedAuthority(p.getName()))
                )
        );

        // Quyền gán trực tiếp cho user → user_permissions
        entity.getDirectPermissions().forEach(p ->
                authorities.add(new SimpleGrantedAuthority(p.getName()))
        );

        return User.builder()
                .username(entity.getUsername())
                .password(entity.getPasswordHash())
                .authorities(authorities)
                .disabled(!entity.isEnabled())
                .build();
    }
}
