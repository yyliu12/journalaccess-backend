package com.info25.journalindex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * This configures security for the application. Our main goal with authentication
 * is NOT to create a multi user application -- it only supports one journal, and
 * major rearchitecting would be needed in order to support multiple users each with
 * their own journals. Our only goal is to secure the one journal with a password.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        // access react app & api endpoints should req authentication
                        // login pages OK w/o auth
                        .requestMatchers("/app/**").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .requestMatchers("/").authenticated()
                        .requestMatchers("/static/**").permitAll()
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/loginBg.jpg").permitAll()
                )
                .formLogin(f -> f.loginPage("/login").loginProcessingUrl("/login"))
                .headers(headers -> headers.disable())
                .csrf(csrf -> csrf.disable()) // Disable CSRF protection for simplicity
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
                .username("admin")
                .password(passwordEncoder().encode("pass"))
                .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true); // you USUALLY want this
        config.addAllowedOrigin("http://localhost:3000"); // Allow requests from this origin
        config.addAllowedHeader("*");
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("HEAD");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("PATCH");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

}
