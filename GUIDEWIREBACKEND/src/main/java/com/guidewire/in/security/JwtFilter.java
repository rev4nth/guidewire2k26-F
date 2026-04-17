package com.guidewire.in.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtFilter extends OncePerRequestFilter {

	public static final String ATTR_USER_ID = "userId";
	public static final String ATTR_ROLE = "role";

	private final JwtUtil jwtUtil;

	public JwtFilter(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			filterChain.doFilter(request, response);
			return;
		}
		String header = request.getHeader("Authorization");
		if (header == null || !header.startsWith("Bearer ")) {
			sendUnauthorized(response);
			return;
		}
		String token = header.substring(7).trim();
		if (token.isEmpty() || !jwtUtil.validateToken(token)) {
			sendUnauthorized(response);
			return;
		}
		try {
			Long userId = jwtUtil.extractUserId(token);
			String role = jwtUtil.extractRole(token);
			request.setAttribute(ATTR_USER_ID, userId);
			request.setAttribute(ATTR_ROLE, role);
		}
		catch (Exception e) {
			sendUnauthorized(response);
			return;
		}
		filterChain.doFilter(request, response);
	}

	private void sendUnauthorized(HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"error\":\"Unauthorized\"}");
	}
}
