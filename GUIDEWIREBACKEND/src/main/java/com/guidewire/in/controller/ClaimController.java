package com.guidewire.in.controller;

import com.guidewire.in.dto.ClaimResponse;
import com.guidewire.in.entity.Claim;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.ClaimRepository;
import com.guidewire.in.repository.UserRepository;
import com.guidewire.in.security.JwtFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/worker/claims")
public class ClaimController {

	private final ClaimRepository claimRepository;
	private final UserRepository userRepository;

	public ClaimController(ClaimRepository claimRepository, UserRepository userRepository) {
		this.claimRepository = claimRepository;
		this.userRepository = userRepository;
	}

	@GetMapping
	public ResponseEntity<?> myClaims(HttpServletRequest req) {
		Object attr = req.getAttribute(JwtFilter.ATTR_USER_ID);
		if (attr == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		Long uid = Long.valueOf(attr.toString());
		User worker = userRepository.findById(uid).orElse(null);
		if (worker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		List<ClaimResponse> list = claimRepository.findByWorkerOrderByCreatedAtDesc(worker)
				.stream()
				.map(c -> new ClaimResponse(c.getId(), c.getWorker().getId(), c.getWorker().getName(),
						c.getAmount(), c.getReason(), c.getCreatedAt()))
				.collect(Collectors.toList());
		return ResponseEntity.ok(list);
	}
}
