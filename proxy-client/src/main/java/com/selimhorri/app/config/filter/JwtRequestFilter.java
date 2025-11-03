package com.selimhorri.app.config.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.selimhorri.app.jwt.service.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {
	
	private final UserDetailsService userDetailsService;
	private final JwtService jwtService;
	
	@Override
	protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) 
			throws ServletException, IOException {
		
		log.info("**JwtRequestFilter, once per request, validating and extracting token*\n");
		log.info("Request URI: {}", request.getRequestURI());
		
		// Skip JWT validation for authentication endpoints
		String requestPath = request.getRequestURI();
		if (requestPath.contains("/api/authenticate") ||
			requestPath.contains("/app/api/authenticate") ||
		    requestPath.contains("/swagger-ui") || 
		    requestPath.contains("/v3/api-docs") ||
		    requestPath.contains("/api/categories") ||
		    requestPath.contains("/api/products")) {
			log.info("**Skipping JWT filter for public endpoint: {}*\n", requestPath);
			filterChain.doFilter(request, response);
			return;
		}
		
		final var authorizationHeader = request.getHeader("Authorization");
		
		String username = null;
		String jwt = null;
		
		if ( authorizationHeader != null && authorizationHeader.startsWith("Bearer ") ) {
			jwt = authorizationHeader.substring(7);
			username = jwtService.extractUsername(jwt);
		}
		
		if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			
			final UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
			
			if (this.jwtService.validateToken(jwt, userDetails)) {
				final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = 
						new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
				usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
			}
			
		}
		
		filterChain.doFilter(request, response);
		log.info("**Jwt request filtered!*\n");
	}
	
	
	
}










