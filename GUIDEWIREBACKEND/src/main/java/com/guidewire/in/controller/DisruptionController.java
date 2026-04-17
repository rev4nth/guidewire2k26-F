package com.guidewire.in.controller;

import com.guidewire.in.dto.DisruptionRequest;
import com.guidewire.in.dto.DisruptionResponse;
import com.guidewire.in.entity.Claim;
import com.guidewire.in.entity.Disruption;
import com.guidewire.in.entity.DisruptionSeverity;
import com.guidewire.in.entity.DisruptionType;
import com.guidewire.in.entity.Order;
import com.guidewire.in.entity.OrderStatus;
import com.guidewire.in.entity.Policy;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.ClaimRepository;
import com.guidewire.in.repository.DisruptionRepository;
import com.guidewire.in.repository.OrderRepository;
import com.guidewire.in.repository.PolicyRepository;
import com.guidewire.in.repository.UserRepository;
import com.guidewire.in.security.JwtFilter;
import com.guidewire.in.service.FinanceService;
import com.guidewire.in.service.LocationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class DisruptionController {

	private final DisruptionRepository disruptionRepository;
	private final OrderRepository orderRepository;
	private final ClaimRepository claimRepository;
	private final PolicyRepository policyRepository;
	private final UserRepository userRepository;
	private final LocationService locationService;
	private final FinanceService financeService;

	public DisruptionController(DisruptionRepository disruptionRepository,
			OrderRepository orderRepository,
			ClaimRepository claimRepository,
			PolicyRepository policyRepository,
			UserRepository userRepository,
			LocationService locationService,
			FinanceService financeService) {
		this.disruptionRepository = disruptionRepository;
		this.orderRepository      = orderRepository;
		this.claimRepository      = claimRepository;
		this.policyRepository     = policyRepository;
		this.userRepository       = userRepository;
		this.locationService      = locationService;
		this.financeService       = financeService;
	}

	private boolean isAdmin(HttpServletRequest req) {
		return "ADMIN".equals(req.getAttribute(JwtFilter.ATTR_ROLE));
	}

	@Transactional
	@PostMapping("/disruption/{workerId}")
	public ResponseEntity<?> triggerDisruption(HttpServletRequest req,
			@PathVariable Long workerId,
			@RequestBody DisruptionRequest body) {
		if (!isAdmin(req)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

		User worker = userRepository.findById(workerId).orElse(null);
		if (worker == null) return ResponseEntity.notFound().build();

		DisruptionType type;
		DisruptionSeverity severity;
		try {
			type = DisruptionType.valueOf(body.getType().toUpperCase());
			severity = DisruptionSeverity.valueOf(body.getSeverity().toUpperCase());
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("{\"error\":\"Invalid type or severity\"}");
		}

		Disruption disruption = new Disruption();
		disruption.setType(type);
		disruption.setSeverity(severity);
		disruption.setWorker(worker);
		disruptionRepository.save(disruption);

		// Cancel active order if present
		List<OrderStatus> activeStatuses = List.of(OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.PICKED_UP);
		Optional<Order> activeOrder = orderRepository.findFirstByWorkerAndStatusIn(worker, activeStatuses);
		Claim claim = null;

		if (activeOrder.isPresent()) {
			Order order = activeOrder.get();
			order.setStatus(OrderStatus.CANCELLED);
			orderRepository.save(order);

			// Find first active policy for coverage amount
			Optional<Policy> policy = policyRepository.findAll().stream()
					.filter(Policy::isActive)
					.findFirst();

			BigDecimal coverage = policy.map(Policy::getCoverage).orElse(BigDecimal.valueOf(100));

			claim = new Claim();
			claim.setWorker(worker);
			claim.setAmount(coverage);
			claim.setReason("Disruption (" + type + " " + severity + ")");
			claimRepository.save(claim);
			financeService.creditWalletForClaim(worker, coverage, "claim:" + claim.getId());
		}

		DisruptionResponse dr = new DisruptionResponse(
				disruption.getId(), disruption.getType().name(), disruption.getSeverity().name(),
				worker.getId(), worker.getName(), disruption.getCreatedAt());

		Map<String, Object> result = new java.util.LinkedHashMap<>();
		result.put("disruption", dr);
		result.put("orderCancelled", activeOrder.isPresent());
		if (claim != null) {
			result.put("claimCreated", true);
			result.put("claimAmount", claim.getAmount());
		} else {
			result.put("claimCreated", false);
		}
		return ResponseEntity.ok(result);
	}

	/**
	 * POST /admin/disruption/location?city={city}
	 *
	 * Triggers a disruption for ALL workers in the given city.
	 * For each worker that has an active order, cancels it and creates a claim.
	 *
	 * Alternatively supply lat/lon + radiusKm to target by GPS radius:
	 *   POST /admin/disruption/location?lat=17.38&lon=78.48&radiusKm=30
	 */
	@Transactional
	@PostMapping("/disruption/location")
	public ResponseEntity<?> triggerLocationDisruption(HttpServletRequest req,
			@RequestBody DisruptionRequest body,
			@RequestParam(required = false) String city,
			@RequestParam(required = false) Double lat,
			@RequestParam(required = false) Double lon,
			@RequestParam(defaultValue = "30") double radiusKm) {

		if (!isAdmin(req)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

		DisruptionType type;
		DisruptionSeverity severity;
		try {
			type     = DisruptionType.valueOf(body.getType().toUpperCase());
			severity = DisruptionSeverity.valueOf(body.getSeverity().toUpperCase());
		} catch (Exception e) {
			return ResponseEntity.badRequest().body("{\"error\":\"Invalid type or severity\"}");
		}

		// Resolve affected workers
		List<User> affectedWorkers;
		if (lat != null && lon != null) {
			affectedWorkers = locationService.findWorkersNearby(lat, lon, radiusKm);
		} else if (city != null && !city.isBlank()) {
			affectedWorkers = locationService.findWorkersByCity(city.trim());
		} else {
			return ResponseEntity.badRequest().body("{\"error\":\"Provide either city or lat+lon\"}");
		}

		if (affectedWorkers.isEmpty()) {
			return ResponseEntity.ok(Map.of("message", "No workers found in the specified area",
					"affectedCount", 0));
		}

		BigDecimal coverage = policyRepository.findAll().stream()
				.filter(Policy::isActive)
				.findFirst()
				.map(Policy::getCoverage)
				.orElse(BigDecimal.valueOf(100));

		List<OrderStatus> activeStatuses = List.of(OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.PICKED_UP);
		List<Map<String, Object>> results = new ArrayList<>();

		for (User worker : affectedWorkers) {
			Disruption disruption = new Disruption();
			disruption.setType(type);
			disruption.setSeverity(severity);
			disruption.setWorker(worker);
			disruptionRepository.save(disruption);

			Optional<Order> activeOrder = orderRepository.findFirstByWorkerAndStatusIn(worker, activeStatuses);
			boolean orderCancelled = false;
			boolean claimCreated   = false;

			if (activeOrder.isPresent()) {
				activeOrder.get().setStatus(OrderStatus.CANCELLED);
				orderRepository.save(activeOrder.get());
				orderCancelled = true;

				Claim claim = new Claim();
				claim.setWorker(worker);
				claim.setAmount(coverage);
				claim.setReason("Location disruption (" + type + " " + severity + ")");
				claimRepository.save(claim);
				financeService.creditWalletForClaim(worker, coverage, "claim:" + claim.getId());
				claimCreated = true;
			}

			Map<String, Object> entry = new java.util.LinkedHashMap<>();
			entry.put("workerId",      worker.getId());
			entry.put("workerName",    worker.getName());
			entry.put("city",          worker.getLocation());
			entry.put("orderCancelled", orderCancelled);
			entry.put("claimCreated",   claimCreated);
			results.add(entry);
		}

		Map<String, Object> response = new java.util.LinkedHashMap<>();
		response.put("type",          type.name());
		response.put("severity",      severity.name());
		response.put("affectedCount", affectedWorkers.size());
		response.put("workers",       results);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/disruptions")
	public ResponseEntity<?> listAll(HttpServletRequest req) {
		if (!isAdmin(req)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		List<DisruptionResponse> list = disruptionRepository.findAll().stream()
				.sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
				.map(d -> new DisruptionResponse(d.getId(), d.getType().name(), d.getSeverity().name(),
						d.getWorker().getId(), d.getWorker().getName(), d.getCreatedAt()))
				.collect(Collectors.toList());
		return ResponseEntity.ok(list);
	}
}
