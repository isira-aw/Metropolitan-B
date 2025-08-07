package com.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Skip JWT validation for the "/get-logs" endpoint as it's publicly accessible
        if (request.getRequestURI().startsWith("/api/job-event-logs/get-logs")) {
            filterChain.doFilter(request, response);
            return; // Skip JWT processing for this request
        }

        // Extract the token from the Authorization header
        String header = request.getHeader("Authorization");

        // If the token is present and starts with "Bearer"
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);  // Extract token from header
            String email = jwtTokenProvider.getUsernameFromToken(token);  // Get email from token

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Load user details by email
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Validate the token
                if (jwtTokenProvider.validateToken(token)) {
                    // Create authentication token
                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    // Set details for the authentication object
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    // Set the authentication object in the security context
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }

        // Proceed with the filter chain
        filterChain.doFilter(request, response);
    }
}
