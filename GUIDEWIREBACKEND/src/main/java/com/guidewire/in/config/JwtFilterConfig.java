package com.guidewire.in.config;

import com.guidewire.in.security.JwtFilter;
import com.guidewire.in.security.JwtUtil;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class JwtFilterConfig {

	@Bean
	public JwtFilter jwtFilter(JwtUtil jwtUtil) {
		return new JwtFilter(jwtUtil);
	}

	@Bean
	public FilterRegistrationBean<JwtFilter> jwtFilterRegistration(JwtFilter jwtFilter) {
		FilterRegistrationBean<JwtFilter> registration = new FilterRegistrationBean<>();
		registration.setFilter(jwtFilter);
		registration.addUrlPatterns("/api/*", "/admin/*", "/worker/*", "/govt/*");
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
		return registration;
	}
}
