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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

	/* ── Self profile update ── */

	@PutMapping("/worker/profile")
	public ResponseEntity<?> updateProfile(HttpServletRequest req, @RequestBody UpdateProfileRequest body) {
		Object attr = req.getAttribute(JwtFilter.ATTR_USER_ID);
		if (attr == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		Long uid = Long.valueOf(attr.toString());
		User user = userRepository.findById(uid).orElse(null);
		if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		if (body.getName() != null && !body.getName().isBlank()) user.setName(body.getName().trim());
		if (body.getLocation() != null) user.setLocation(body.getLocation().trim());
		if (body.getProfileImageUrl() != null) user.setProfileImageUrl(body.getProfileImageUrl().trim());
		userRepository.save(user);
		return ResponseEntity.ok(toUserDto(user));
	}

	/* ── Admin: get worker detail ── */

	@GetMapping("/admin/workers/{id}")
	public ResponseEntity<?> workerDetail(HttpServletRequest req, @PathVariable Long id) {
		if (!"ADMIN".equals(req.getAttribute(JwtFilter.ATTR_ROLE)))
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		User worker = userRepository.findById(id).orElse(null);
		if (worker == null) return ResponseEntity.notFound().build();

		List<OrderResponse> orders = orderRepository.findByWorkerOrderByCreatedAtDesc(worker)
				.stream().map(o -> new OrderResponse(o.getId(), o.getWorker().getId(),
						o.getWorker().getName(), o.getStatus().name(), o.getCreatedAt()))
				.collect(Collectors.toList());

		List<ClaimResponse> claims = claimRepository.findByWorkerOrderByCreatedAtDesc(worker)
				.stream().map(c -> new ClaimResponse(c.getId(), c.getWorker().getId(),
						c.getWorker().getName(), c.getAmount(), c.getReason(), c.getCreatedAt()))
				.collect(Collectors.toList());

		List<DisruptionResponse> disruptions = disruptionRepository.findByWorkerOrderByCreatedAtDesc(worker)
				.stream().map(d -> new DisruptionResponse(d.getId(), d.getType().name(),
						d.getSeverity().name(), d.getWorker().getId(), d.getWorker().getName(), d.getCreatedAt()))
				.collect(Collectors.toList());

		return ResponseEntity.ok(new WorkerSummaryResponse(toUserDto(worker), orders, claims, disruptions));
	}

	/* ── Worker: disruptions for self (for alert banner) ── */

	@GetMapping("/worker/disruptions")
	public ResponseEntity<?> myDisruptions(HttpServletRequest req) {
		Object attr = req.getAttribute(JwtFilter.ATTR_USER_ID);
		if (attr == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		Long uid = Long.valueOf(attr.toString());
		User worker = userRepository.findById(uid).orElse(null);
		if (worker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		List<DisruptionResponse> list = disruptionRepository.findByWorkerOrderByCreatedAtDesc(worker)
				.stream().map(d -> new DisruptionResponse(d.getId(), d.getType().name(),
						d.getSeverity().name(), d.getWorker().getId(), d.getWorker().getName(), d.getCreatedAt()))
				.collect(Collectors.toList());
		return ResponseEntity.ok(list);
	}

	private UserListResponse toUserDto(User u) {
		return new UserListResponse(u.getId(), u.getName(), u.getEmail(),
				u.getRole(), u.getLocation(), u.getProfileImageUrl(), u.getCreatedAt(),
				u.getLatitude(), u.getLongitude());
	}
}
