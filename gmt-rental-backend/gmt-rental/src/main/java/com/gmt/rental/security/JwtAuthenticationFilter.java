package com.gmt.rental.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/api/public/") || path.equals("/api/auth/login")) return true;
        if (HttpMethod.GET.matches(request.getMethod())) {
            return path.startsWith("/api/vehicules/") || path.equals("/api/vehicules")
                    || path.startsWith("/api/categories/") || path.equals("/api/categories")
                    || path.startsWith("/api/parametres/") || path.equals("/api/parametres");
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(7);
            try {
                if (jwtUtils.isValid(token)) {
                    UserDetails user = userDetailsService.loadUserByUsername(jwtUtils.getEmail(token));
                    if (user.isEnabled()) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        log.warn("JWT authentication rejected because account is disabled: {}", user.getUsername());
                    }
                } else {
                    log.warn("JWT authentication rejected an invalid or expired token for {}", request.getRequestURI());
                }
            } catch (RuntimeException exception) {
                log.warn("JWT authentication failed for {}: {}", request.getRequestURI(), exception.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
