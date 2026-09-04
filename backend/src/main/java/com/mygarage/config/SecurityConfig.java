package com.mygarage.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security Configuration for MyGarage.
 *
 * Authentication: Form-based login (session-based).
 * No JWT - session cookies are sufficient for a localhost project.
 *
 * Authorization:
 *   Public: /, /login, /register, /css/**, /js/**, /images/**
 *   USER or ADMIN: /dashboard/**, /vehicles/**, /profile/**, /api/**
 *   ADMIN only: /admin/**
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .userDetailsService(userDetailsService)
            .authorizeHttpRequests(auth -> auth
                // Public pages
                .requestMatchers("/", "/login", "/register", "/access-denied").permitAll()
                // Static resources
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                // Actuator health check
                .requestMatchers("/actuator/health").permitAll()
                // Admin routes (Web & REST) - MUST BE EVALUATED BEFORE broad /api/** rule
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Profile Web & REST accessible to any authenticated user
                .requestMatchers("/profile/**", "/api/profile/**").hasAnyRole("NORMAL_USER", "ADMIN")
                // User Web routes
                .requestMatchers("/dashboard/**", "/vehicles/**", "/service/**", "/services/**", "/fuel/**", "/maintenance/**", "/reports/**", "/export/**", "/analytics/**").hasRole("NORMAL_USER")
                // User REST APIs - EVALUATED AFTER specific /api/admin/** and /api/profile/**
                .requestMatchers("/api/**").hasRole("NORMAL_USER")
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/access-denied")
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                // Redirect based on role after login
                .successHandler((request, response, authentication) -> {
                    boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
                    response.sendRedirect(isAdmin ? "/admin/dashboard" : "/dashboard");
                })
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(5)
                .expiredUrl("/login?expired=true")
            )
            .csrf(csrf -> csrf
                // CSRF enabled for MVC forms (Thymeleaf adds token automatically)
                // Disable only for stateless REST testing convenience - NOT in production
                .ignoringRequestMatchers("/api/**")
            );

        return http.build();
    }
}
