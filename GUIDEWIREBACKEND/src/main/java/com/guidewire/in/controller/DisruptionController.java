package com.guidewire.in.controller;

import com.guidewire.in.dto.DisruptionRequest;
import com.guidewire.in.dto.DisruptionResponse;
import com.guidewire.in.entity.Claim;
import com.guidewire.in.entity.Disruption;
import com.guidewire.in.entity.DisruptionSeverity;
import com.guidewire.in.entity.DisruptionSource;
import com.guidewire.in.entity.DisruptionType;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.DisruptionRepository;
import com.guidewire.in.repository.UserRepository;
import com.guidewire.in.security.JwtFilter;
import com.guidewire.in.service.DisruptionService;
import com.guidewire.in.service.DisruptionService.SingleDisruptionResult;
import com.guidewire.in.service.LocationService;
import com.guidewire.in.web.ApiResponseBuilder;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class DisruptionController {

	private final DisruptionRepository disruptionRepository;
	private final UserRepository userRepository;
	private final LocationService locationService;
	private final DisruptionService disruptionService;

	public DisruptionController(DisruptionRepository disruptionRepository,
			UserRepository userRepository,
			LocationService locationService,
			DisruptionService disruptionService) {
		this.disruptionRepository = disruptionRepository;
		this.userRepository = userRepository;
		this.locationService = locationService;
		this.disruptionService = disruptionService;
	}

	private boolean isAdmin(HttpServletRequest req) {
		return "ADMIN".equals(req.getAttribute(JwtFilter.ATTR_ROLE));
	}

	private static DisruptionResponse toDto(Disruption d) {
		String src = d.getSource() != null ? d.getSource().name() : DisruptionSource.MANUAL.name();
		return new DisruptionResponse(
				d.getId(),
				d.getType().name(),
				d.getSeverity().name(),
				d.getLocation(),
				src,
				d.getWorker().getId(),
				d.getWorker().getName(),
				d.getCreatedAt());
	}

	@Transactional
	@PostMapping("/disruption")
	public ResponseEntity<?> createManualDisruption(HttpServletRequest req, @RequestBody DisruptionRequest body) {
		if (!isAdmin(req)) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");

		if (body.getLocation() == null || body.getLocation().isBlank()) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "location is required");
		}

		DisruptionType type;
		DisruptionSeverity severity;
		try {
			type = DisruptionType.valueOf(body.getType().toUpperCase());
			severity = DisruptionSeverity.valueOf(body.getSeverity().toUpperCase());
		} catch (Exception e) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "Invalid type or severity");
		}

		String loc = body.getLocation().trim();
		List<User> workers = locationService.findWorkersByCity(loc);
		if (workers.isEmpty()) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("message", "No workers found in the specified area");
			empty.put("affectedCount", 0);
			empty.put("workers", List.of());
			return ApiResponseBuilder.ok("No workers in area", empty);
		}

		List<Map<String, Object>> results = disruptionService.triggerForWorkers(workers, type, severity, loc,
				DisruptionSource.MANUAL);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("type", type.name());
		response.put("severity", severity.name());
		response.put("source", DisruptionSource.MANUAL.name());
		response.put("affectedCount", workers.size());
		response.put("workers", results);
		return ApiResponseBuilder.ok("Disruption triggered", response);
	}

	@Transactional
	@PostMapping("/disruption/{workerId}")
	public ResponseEntity<?> triggerDisruption(HttpServletRequest req,
			@PathVariable Long workerId,
			@RequestBody DisruptionRequest body) {
		if (!isAdmin(req)) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");

		User worker = userRepository.findById(workerId).orElse(null);
		if (worker == null) return ApiResponseBuilder.fail(HttpStatus.NOT_FOUND, "Worker not found");

		DisruptionType type;
		DisruptionSeverity severity;
		try {
			type = DisruptionType.valueOf(body.getType().toUpperCase());
			severity = DisruptionSeverity.valueOf(body.getSeverity().toUpperCase());
		} catch (Exception e) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "Invalid type or severity");
		}

		String location = worker.getLocation() != null ? worker.getLocation() : "";
		SingleDisruptionResult result = disruptionService.triggerForWorker(worker, type, severity, location,
				DisruptionSource.MANUAL);

		Claim claim = result.claim();
		DisruptionResponse dr = toDto(result.disruption());

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("disruption", dr);
		res.put("orderCancelled", result.orderCancelled());
		if (claim != null) {
			res.put("claimCreated", true);
			res.put("claimAmount", claim.getAmount());
		} else {
			res.put("claimCreated", false);
		}
		return ApiResponseBuilder.ok("Disruption processed", res);
	}

	@Transactional
	@PostMapping("/disruption/location")
	public ResponseEntity<?> triggerLocationDisruption(HttpServletRequest req,
			@RequestBody DisruptionRequest body,
			@RequestParam(required = false) String city,
			@RequestParam(required = false) Double lat,
			@RequestParam(required = false) Double lon,
			@RequestParam(defaultValue = "30") double radiusKm) {

		if (!isAdmin(req)) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");

		DisruptionType type;
		DisruptionSeverity severity;
		try {
			type = DisruptionType.valueOf(body.getType().toUpperCase());
			severity = DisruptionSeverity.valueOf(body.getSeverity().toUpperCase());
		} catch (Exception e) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "Invalid type or severity");
		}

		List<User> affectedWorkers;
		String locationLabel;
		if (lat != null && lon != null) {
			affectedWorkers = locationService.findWorkersNearby(lat, lon, radiusKm);
			locationLabel = String.format("radius:%.4f,%.4f r=%.0fkm", lat, lon, radiusKm);
		} else if (city != null && !city.isBlank()) {
			locationLabel = city.trim();
			affectedWorkers = locationService.findWorkersByCity(locationLabel);
		} else {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "Provide either city or lat+lon");
		}

		if (affectedWorkers.isEmpty()) {
			Map<String, Object> empty = new LinkedHashMap<>();
			empty.put("message", "No workers found in the specified area");
			empty.put("affectedCount", 0);
			return ApiResponseBuilder.ok("No workers in area", empty);
		}

		List<Map<String, Object>> results = disruptionService.triggerForWorkers(affectedWorkers, type, severity,
				locationLabel, DisruptionSource.MANUAL);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("type", type.name());
		response.put("severity", severity.name());
		response.put("source", DisruptionSource.MANUAL.name());
		response.put("affectedCount", affectedWorkers.size());
		response.put("workers", results);
		return ApiResponseBuilder.ok("Disruption triggered", response);
	}

	@GetMapping("/disruptions")
	public ResponseEntity<?> listAll(HttpServletRequest req) {
		if (!isAdmin(req)) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
		List<DisruptionResponse> list = disruptionRepository.findAll().stream()
				.sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
				.map(DisruptionController::toDto)
				.collect(Collectors.toList());
		return ApiResponseBuilder.ok("Disruptions loaded", list);
	}
}
