package com.guidewire.in.controller;

import com.guidewire.in.dto.ClaimResponse;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.ClaimRepository;
import com.guidewire.in.repository.UserRepository;
import com.guidewire.in.security.JwtFilter;
import com.guidewire.in.web.ApiResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
	@Transactional(readOnly = true)
	public ResponseEntity<?> myClaims(HttpServletRequest req) {
		Object attr = req.getAttribute(JwtFilter.ATTR_USER_ID);
		if (attr == null) return ApiResponseBuilder.fail(HttpStatus.UNAUTHORIZED, "Unauthorized");
		Long uid = Long.valueOf(attr.toString());
		User worker = userRepository.findById(uid).orElse(null);
		if (worker == null) return ApiResponseBuilder.fail(HttpStatus.UNAUTHORIZED, "Unauthorized");
		List<ClaimResponse> list = claimRepository.findByWorkerOrderByCreatedAtDesc(worker)
				.stream()
				.map(ClaimResponse::fromEntity)
				.collect(Collectors.toList());
		return ApiResponseBuilder.ok("Claims loaded", list);
	}
}
