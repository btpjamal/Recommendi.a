package dev.jamal.projetotcc.config;

import dev.jamal.projetotcc.Service.JwtAuthenticationFilter;
import dev.jamal.projetotcc.Service.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;
import org.springframework.http.HttpMethod;
import jakarta.servlet.DispatcherType;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> corsConfigurationSource())
                .authorizeHttpRequests(auth -> auth

                        // Permite despachos internos assíncronos
                        .dispatcherTypeMatchers(
                                DispatcherType.ASYNC,
                                DispatcherType.ERROR
                        ).permitAll()

                        // Auth público
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        .requestMatchers("/api/v1/ai/**").authenticated()

                        .requestMatchers("/api/v1/recommendations/**").authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/users"
                        ).permitAll()

                        // Swagger público
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html"
                        ).permitAll()

                                // Rotas administrativas
                                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                                // =====================================================
                                // CATÁLOGO DE HOBBIES
                                // =====================================================

                                // Consultas administrativas
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/hobbies/paginados",
                                        "/api/v1/hobbies/estatisticas"
                                ).hasRole("ADMIN")

                                // Cadastro de hobbies
                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/v1/hobbies",
                                        "/api/v1/hobbies/"
                                ).hasRole("ADMIN")

                                // Atualização de hobbies
                                .requestMatchers(
                                        HttpMethod.PUT,
                                        "/api/v1/hobbies/*"
                                ).hasRole("ADMIN")

                                // Desativação de hobbies
                                .requestMatchers(
                                        HttpMethod.DELETE,
                                        "/api/v1/hobbies/*"
                                ).hasRole("ADMIN")

                                // Reativação de hobbies
                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/v1/hobbies/*/reativar"
                                ).hasRole("ADMIN")

                                // Consultas gerais do catálogo
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/hobbies",
                                        "/api/v1/hobbies/*"
                                ).authenticated()

                                // Demais endpoints exigem autenticação
                                .anyRequest().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
                        )
                        .accessDeniedHandler(
                                (request, response, exception) ->
                                        response.sendError(
                                                HttpServletResponse.SC_FORBIDDEN
                                        )
                        )
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(userDetailsService);

        provider.setPasswordEncoder(passwordEncoder());

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of("http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT","PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}