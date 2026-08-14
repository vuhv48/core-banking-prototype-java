package com.example.accountdemo.infrastructure.security;

import com.example.accountdemo.api.common.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

/**
 * Cấu hình Spring Security: filter chain, public paths, 401/403 JSON.
 *
 * <p><b>Vì sao cần class này:</b> gắn JwtAuthFilter + AuthorizationFilter; public-paths từ yaml,
 * permission từ bảng resources — không {@code @PreAuthorize}.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(SecurityProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthorizationFilter authorizationFilter;
    private final CustomUserDetailsService userDetailsService;
    private final SecurityProperties securityProperties;
    private final JsonErrorWriter jsonErrorWriter;

    /** Filter chain: JWT → authorization theo resources; 401/403 JSON. */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        String[] publicPaths = securityProperties.getPublicPaths().toArray(String[]::new);
        log.info("Security public paths (from yaml): {}", List.of(publicPaths));

        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, authException) ->
                                jsonErrorWriter.write(request, response, ErrorStatus.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                jsonErrorWriter.write(request, response, ErrorStatus.FORBIDDEN))
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicPaths).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(authorizationFilter, JwtAuthFilter.class);

        return http.build();
    }

    /** Provider login username/password + BCrypt. */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /** Expose AuthenticationManager cho AuthController.login. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /** BCrypt encoder cho passwordHash. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
