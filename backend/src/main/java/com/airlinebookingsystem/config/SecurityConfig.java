package com.airlinebookingsystem.config;

import com.airlinebookingsystem.security.JwtAuthFilter;
import com.airlinebookingsystem.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.airlinebookingsystem.security.RestAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration.
 * The @Lazy annotation on JwtAuthFilter breaks the circular dependency:
 *   JwtAuthFilter → UserService → SecurityConfig → JwtAuthFilter
 * Spring creates a proxy for JwtAuthFilter first, then injects the real
 * instance once all beans are fully initialized.

 * Public endpoints (no token needed):
 *   POST /api/v1/auth/register  — create account
 *   POST /api/v1/auth/login     — get JWT
 *   GET  /api/v1/flights/**     — browse flights
 *   GET  /api/v1/airports/**    — airport data
 *   GET  /api/v1/airlines/**    — airline data
 *   GET  /api/v1/currencies/**  — supported display currencies
 *   GET  /api/v1/live-flights/**— live ADS-B traffic (OpenSky proxy)
 *   POST /api/v1/assistant      — natural-language flight search
 *   GET  /api/v1/assistant/status — whether the assistant is configured
 *   GET  /actuator/health       — health check
 *   GET  /swagger-ui/**         — API docs
 * Everything else requires a valid Bearer token.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    // @Lazy on JwtAuthFilter breaks the circular dependency cycle
    public SecurityConfig(@Lazy JwtAuthFilter jwtAuthFilter, UserService userService, PasswordEncoder passwordEncoder,
                          RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/flights/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/flights/search").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/flights/search/multi-city").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/airports/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/airlines/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/currencies/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/live-flights/**").permitAll()
                        .requestMatchers("/api/v1/assistant/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // Without this an unauthenticated request returns 403, which a
                // client cannot tell apart from a genuine permission denial.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Wires UserService + PasswordEncoder into the AuthenticationManager
     * so Spring Security knows how to validate credentials on login.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Explicit CORS config so Spring Security's CorsFilter (enabled via .cors()
     * above) handles preflight OPTIONS requests before the authorization rules
     * ever see them — @CrossOrigin on individual controllers only affects
     * Spring MVC dispatch, which the security filter chain runs ahead of.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173", "https://airline-booking-app-theta.vercel.app"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}