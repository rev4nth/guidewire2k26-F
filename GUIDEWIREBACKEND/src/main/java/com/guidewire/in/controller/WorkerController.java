package com.guidewire.in.controller;

import com.guidewire.in.dto.ClaimResponse;
import com.guidewire.in.dto.DisruptionResponse;
import com.guidewire.in.dto.OrderResponse;
import com.guidewire.in.dto.UpdateProfileRequest;
import com.guidewire.in.dto.UserListResponse;
import com.guidewire.in.dto.WorkerSummaryResponse;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.ClaimRepository;
import com.guidewire.in.repository.DisruptionRepository;
import com.guidewire.in.repository.OrderRepository;
import com.guidewire.in.repository.UserRepository;
import com.guidewire.in.security.JwtFilter;
import com.guidewire.in.web.ApiResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class WorkerController {

	private final UserRepository userRepository;
	private final OrderRepository orderRepository;
	private final ClaimRepository claimRepository;
	private final DisruptionRepository disruptionRepository;

	public WorkerController(UserRepository userRepository, OrderRepository orderRepository,
			ClaimRepository claimRepository, DisruptionRepository disruptionRepository) {
		this.userRepository = userRepository;
		this.orderRepository = orderRepository;
		this.claimRepository = claimRepository;
		this.disruptionRepository = disruptionRepository;
	}

	@PutMapping("/worker/profile")
	public ResponseEntity<?> updateProfile(HttpServletRequest req, @RequestBody UpdateProfileRequest body) {
		Object attr = req.getAttribute(JwtFilter.ATTR_USER_ID);
		if (attr == null) return ApiResponseBuilder.fail(HttpStatus.UNAUTHORIZED, "Unauthorized");
		Long uid = Long.valueOf(attr.toString());
		User user = userRepository.findById(uid).orElse(null);
		if (user == null) return ApiResponseBuilder.fail(HttpStatus.UNAUTHORIZED, "Unauthorized");
		if (body.getName() != null && !body.getName().isBlank()) user.setName(body.getName().trim());
		if (body.getLocation() != null) user.setLocation(body.getLocation().trim());
		if (body.getProfileImageUrl() != null) user.setProfileImageUrl(body.getProfileImageUrl().trim());
		userRepository.save(user);
		return ApiResponseBuilder.ok("Profile updated", toUserDto(user));
	}

	@GetMapping("/admin/workers/{id}")
	@Transactional(readOnly = true)
	public ResponseEntity<?> workerDetail(HttpServletRequest req, @PathVariable Long id) {
		if (!"ADMIN".equals(req.getAttribute(JwtFilter.ATTR_ROLE)))
			return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
		User worker = userRepository.findById(id).orElse(null);
		if (worker == null) return ApiResponseBuilder.fail(HttpStatus.NOT_FOUND, "Worker not found");

		List<OrderResponse> orders = orderRepository.findByWorkerWithWorkerFetched(worker)
				.stream().map(o -> new OrderResponse(o.getId(), o.getWorker().getId(),
						o.getWorker().getName(), o.getStatus().name(), o.getCreatedAt()))
				.collect(Collectors.toList());

		List<ClaimResponse> claims = claimRepository.findByWorkerWithAssociations(worker)
				.stream().map(ClaimResponse::fromEntity)
				.collect(Collectors.toList());

		List<DisruptionResponse> disruptions = disruptionRepository.findByWorkerWithWorkerFetched(worker)
				.stream().map(d -> new DisruptionResponse(d.getId(), d.getType().name(),
						d.getSeverity().name(), d.getLocation(),
						d.getSource() != null ? d.getSource().name() : "MANUAL",
						d.getWorker().getId(), d.getWorker().getName(), d.getCreatedAt()))
				.collect(Collectors.toList());

		return ApiResponseBuilder.ok("Worker detail",
				new WorkerSummaryResponse(toUserDto(worker), orders, claims, disruptions));
	}

	@GetMapping("/worker/disruptions")
	@Transactional(readOnly = true)
	public ResponseEntity<?> myDisruptions(HttpServletRequest req) {
		Object attr = req.getAttribute(JwtFilter.ATTR_USER_ID);
		if (attr == null) return ApiResponseBuilder.fail(HttpStatus.UNAUTHORIZED, "Unauthorized");
		Long uid = Long.valueOf(attr.toString());
		User worker = userRepository.findById(uid).orElse(null);
		if (worker == null) return ApiResponseBuilder.fail(HttpStatus.UNAUTHORIZED, "Unauthorized");
		List<DisruptionResponse> list = disruptionRepository.findByWorkerWithWorkerFetched(worker)
				.stream().map(d -> new DisruptionResponse(d.getId(), d.getType().name(),
						d.getSeverity().name(), d.getLocation(),
						d.getSource() != null ? d.getSource().name() : "MANUAL",
						d.getWorker().getId(), d.getWorker().getName(), d.getCreatedAt()))
				.collect(Collectors.toList());
		return ApiResponseBuilder.ok("Disruptions loaded", list);
	}

	private UserListResponse toUserDto(User u) {
		return new UserListResponse(u.getId(), u.getName(), u.getEmail(),
				u.getRole(), u.getLocation(), u.getProfileImageUrl(), u.getCreatedAt(),
				u.getLatitude(), u.getLongitude());
	}
}
