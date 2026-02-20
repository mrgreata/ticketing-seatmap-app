package at.ac.tuwien.sepr.groupphase.backend.config;

import at.ac.tuwien.sepr.groupphase.backend.security.JwtAuthorizationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableWebSecurity
@EnableMethodSecurity(
    securedEnabled = true,
    jsr250Enabled = true,
    prePostEnabled = true
)
@Configuration
public class SecurityConfig {

    private final JwtAuthorizationFilter jwtAuthorizationFilter;

    @Autowired
    public SecurityConfig(JwtAuthorizationFilter jwtAuthorizationFilter) {
        this.jwtAuthorizationFilter = jwtAuthorizationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // -------------------------
            // CORS
            // -------------------------
            .cors(cors -> cors.configurationSource(request -> {
                var config = new org.springframework.web.cors.CorsConfiguration();
                config.setAllowCredentials(true);
                config.addAllowedOriginPattern("http://localhost:4200");
                config.addAllowedHeader("*");
                config.addAllowedMethod("*");
                return config;
            }))

            // -------------------------
            // CSRF (JWT → disable OK)
            // -------------------------
            .csrf(AbstractHttpConfigurer::disable)

            // -------------------------
            // Stateless JWT
            // -------------------------
            .sessionManagement(s ->
                s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // -------------------------
            // H2 / Frames
            // -------------------------
            .headers(h ->
                h.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable)
            )

            // -------------------------
            // Authorization
            // -------------------------
            .authorizeHttpRequests(auth -> auth

                // Preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Swagger / OpenAPI
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()

                // Public API
                .requestMatchers(
                    "/api/v1/users/registration",
                    "/api/v1/users/password-reset/request",
                    "/api/v1/users/password-reset/confirmation",
                    "/api/v1/authentication",
                    "/api/v1/news",
                    "/api/v1/news/*",
                    "/api/v1/news/*/image",
                    "/api/v1/events/**",
                    "/api/v1/locations/**",
                    "/api/v1/artists/**",
                    "/api/v1/merchandise",
                    "/api/v1/merchandise/*",
                    "/api/v1/merchandise/*/image",
                    "/api/v1/price-categories/**",
                    "/health/**",
                    "/h2-console/**"
                ).permitAll()

                .requestMatchers(HttpMethod.GET, "/api/v1/news/unread").hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/api/v1/news/read").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/v1/news/*/mark-read").hasRole("USER")

                .requestMatchers(HttpMethod.POST, "/api/v1/news").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/news/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/news/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/admin/users/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/news/unpublished").hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/v1/admin/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/admin/users/locked").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/admin/users").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/admin/users/*/password-reset").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/admin/users/*/lock-state").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/admin/users/*/role").hasRole("ADMIN")

                .anyRequest().authenticated()
            )

            // -------------------------
            // JWT Filter
            // -------------------------
            .addFilterBefore(
                jwtAuthorizationFilter,
                UsernamePasswordAuthenticationFilter.class
            )

            .build();
    }

    // ----------------------------------------------------
    // MVC CORS (Angular)
    // ----------------------------------------------------
    @Configuration
    public static class CorsConfig implements WebMvcConfigurer {

        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                .allowedOriginPatterns(
                    "http://localhost:4200",
                    "https://*.apps.student.inso-w.at"
                )
                .allowedMethods(
                    "GET", "POST", "PUT", "PATCH", "DELETE", "HEAD"
                )
                .allowedHeaders("*")
                .allowCredentials(true);
        }
    }
}