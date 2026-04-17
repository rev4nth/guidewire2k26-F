package com.guidewire.in.controller;

import com.guidewire.in.dto.StatsResponse;
import com.guidewire.in.repository.ClaimRepository;
import com.guidewire.in.repository.DisruptionRepository;
import com.guidewire.in.repository.OrderRepository;
import com.guidewire.in.repository.UserRepository;
import com.guidewire.in.security.JwtFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/stats")
public class StatsController {

	private final UserRepository userRepository;
	private final OrderRepository orderRepository;
	private final DisruptionRepository disruptionRepository;
	private final ClaimRepository claimRepository;

	public StatsController(UserRepository userRepository, OrderRepository orderRepository,
			DisruptionRepository disruptionRepository, ClaimRepository claimRepository) {
		this.userRepository = userRepository;
		this.orderRepository = orderRepository;
		this.disruptionRepository = disruptionRepository;
		this.claimRepository = claimRepository;
	}

	@GetMapping
	public ResponseEntity<?> stats(HttpServletRequest req) {
		if (!"ADMIN".equals(req.getAttribute(JwtFilter.ATTR_ROLE)))
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		return ResponseEntity.ok(new StatsResponse(
				userRepository.count(),
				orderRepository.count(),
				disruptionRepository.count(),
				claimRepository.count()
		));
	}
}
