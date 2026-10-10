
package BankManagement.security;

import BankManagement.entity.User;
import BankManagement.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                Claims claims = jwtService.extractClaims(token);
                Long userId = Long.parseLong(claims.getSubject());

                User user = userRepository.findById(userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("User not found"));

                String role = user.getRole();

                if (!"USER".equals(role) && !"ADMIN".equals(role)) {
                    throw new IllegalArgumentException("Invalid role");
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                String.valueOf(userId),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                authentication.setDetails(user.getEmail());
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

            } catch (Exception exception) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Invalid or expired token\"}"
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
