package com.guidewire.in.controller;

import com.guidewire.in.dto.DisruptionRequest;
import com.guidewire.in.dto.GovtClaimRowResponse;
import com.guidewire.in.dto.GovtStatsResponse;
import com.guidewire.in.dto.GovtVerifyRequest;
import com.guidewire.in.entity.ClaimStatus;
import com.guidewire.in.entity.DisruptionSeverity;
import com.guidewire.in.entity.DisruptionSource;
import com.guidewire.in.entity.DisruptionType;
import com.guidewire.in.entity.Role;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.ClaimRepository;
import com.guidewire.in.repository.DisruptionRepository;
import com.guidewire.in.security.JwtFilter;
import com.guidewire.in.service.ClaimVerificationService;
import com.guidewire.in.service.DisruptionService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/govt")
public class GovtController {

	private final LocationService locationService;
	private final DisruptionService disruptionService;
	private final ClaimRepository claimRepository;
	private final DisruptionRepository disruptionRepository;
	private final ClaimVerificationService claimVerificationService;

	public GovtController(LocationService locationService,
			DisruptionService disruptionService,
			ClaimRepository claimRepository,
			DisruptionRepository disruptionRepository,
			ClaimVerificationService claimVerificationService) {
		this.locationService = locationService;
		this.disruptionService = disruptionService;
		this.claimRepository = claimRepository;
		this.disruptionRepository = disruptionRepository;
		this.claimVerificationService = claimVerificationService;
	}

	private boolean isGovt(HttpServletRequest req) {
		return Role.GOVT.name().equals(req.getAttribute(JwtFilter.ATTR_ROLE));
	}

	@Transactional
	@PostMapping("/disruption")
	public ResponseEntity<?> declareDisruption(HttpServletRequest req, @RequestBody DisruptionRequest body) {
		if (!isGovt(req)) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
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
		var results = disruptionService.triggerForWorkers(workers, type, severity, loc, DisruptionSource.GOVT);
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("type", type.name());
		response.put("severity", severity.name());
		response.put("source", DisruptionSource.GOVT.name());
		response.put("affectedCount", workers.size());
		response.put("workers", results);
		return ApiResponseBuilder.ok("Disruption declared", response);
	}

	@GetMapping("/claims")
	@Transactional(readOnly = true)
	public ResponseEntity<?> listClaims(HttpServletRequest req) {
		if (!isGovt(req)) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
		List<GovtClaimRowResponse> list = claimRepository.findAll().stream()
				.sorted(Comparator.comparing(c -> c.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
				.map(c -> new GovtClaimRowResponse(
						c.getId(),
						c.getWorker().getId(),
						c.getWorker().getName(),
						c.getAmount(),
						c.getConfidenceScore(),
						c.getStatus() != null ? c.getStatus().name() : "REVIEW"))
				.collect(Collectors.toList());
		return ApiResponseBuilder.ok("Claims loaded", list);
	}

	@PostMapping("/claim/{id}/verify")
	public ResponseEntity<?> verifyClaim(HttpServletRequest req, @PathVariable Long id, @RequestBody GovtVerifyRequest body) {
		if (!isGovt(req)) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
		if (body.getStatus() == null || body.getStatus().isBlank()) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "status is required");
		}
		ClaimStatus decision;
		try {
			decision = ClaimStatus.valueOf(body.getStatus().trim().toUpperCase());
		} catch (Exception e) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "Invalid status");
		}
		try {
			var updated = claimVerificationService.applyGovernmentDecision(id, decision);
			Map<String, Object> data = new LinkedHashMap<>();
			data.put("claimId", updated.getId());
			data.put("status", updated.getStatus().name());
			data.put("confidenceScore", updated.getConfidenceScore());
			data.put("walletPaid", updated.isWalletPaid());
			return ApiResponseBuilder.ok("Claim updated", data);
		} catch (NoSuchElementException e) {
			return ApiResponseBuilder.fail(HttpStatus.NOT_FOUND, "Claim not found");
		} catch (IllegalStateException e) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, e.getMessage());
		} catch (IllegalArgumentException e) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, e.getMessage());
		}
	}

	@GetMapping("/stats")
	public ResponseEntity<?> stats(HttpServletRequest req) {
		if (!isGovt(req)) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
		long disruptions = disruptionRepository.count();
		long claims = claimRepository.count();
		long workers = disruptionRepository.countDistinctWorkersAffected();
		return ApiResponseBuilder.ok("Stats loaded", new GovtStatsResponse(disruptions, claims, workers));
	}
}
