package at.ac.tuwien.sepr.groupphase.backend.security;

import at.ac.tuwien.sepr.groupphase.backend.config.properties.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

// @Order(Ordered.LOWEST_PRECEDENCE - 1)
@Service
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final SecurityProperties securityProperties;

    public JwtAuthorizationFilter(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain chain) throws IOException, ServletException {
        try {
            UsernamePasswordAuthenticationToken auth = getAuthToken(request);
            if (auth != null) {
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            LOGGER.warn("JWT Processing failed: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        chain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken getAuthToken(HttpServletRequest request) {
        String header = request.getHeader(securityProperties.getAuthHeader());
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }

        String token = header.substring(7);
        Claims claims = Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(securityProperties.getJwtSecret().getBytes()))
            .build()
            .parseSignedClaims(token)
            .getPayload();

        String username = claims.getSubject();
        List<String> roles = claims.get("rol", List.class);
        List<SimpleGrantedAuthority> authorities = roles.stream()
            .map(SimpleGrantedAuthority::new)
            .toList();

        MDC.put("u", username);

        return new UsernamePasswordAuthenticationToken(username, null, authorities);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        boolean isPublicGetEndpoint = uri.matches("^/api/(v1/)?events(/.*)?$") || uri.matches("^/api/(v1/)?news(/[0-9]+)?$");
        boolean isGetRequest = "GET".equalsIgnoreCase(method);

        System.out.println(">>> shouldNotFilter? uri=" + uri + ", method=" + method + ", skip=" + (isPublicGetEndpoint && isGetRequest));

        return isPublicGetEndpoint && isGetRequest;
    }

}