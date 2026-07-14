package com.github.wekaito.backend.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http
                // Disabling CSRF allows your unified static frontend POST requests to reach the controller
                .csrf(csrf -> csrf.disable())

                .httpBasic(basic -> basic
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");

                            String message;
                            if (authException.getCause() instanceof UserBannedException) {
                                message = authException.getCause().getMessage();
                            } else if (authException instanceof UserBannedException) {
                                message = authException.getMessage();
                            } else {
                                message = "Wrong username or password.";
                            }

                            response.getWriter().write(message);
                        })
                )

                .sessionManagement(httpSecuritySessionManagementConfigurer ->
                        httpSecuritySessionManagementConfigurer
                                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS))

                .logout(logout -> logout.logoutUrl("/api/user/logout"))

                .authorizeHttpRequests(httpRequests ->
                        httpRequests
                                .requestMatchers(HttpMethod.GET, "/api/profile/cards").permitAll()
                                .requestMatchers("/api/profile/decks").authenticated()
                                .requestMatchers("/api/profile/decks/**").authenticated()

                                .requestMatchers("/api/user/me").permitAll()
                                .requestMatchers("/api/user/login").permitAll()
                                .requestMatchers("/api/user/register").permitAll()
                                .requestMatchers("/api/user/active-deck").authenticated()
                                .requestMatchers("/api/user/active-deck/**").authenticated()
                                .requestMatchers("/api/user/avatar").authenticated()
                                .requestMatchers("/api/user/avatar/**").authenticated()
                                
                                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                                .anyRequest().permitAll()
                )
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}