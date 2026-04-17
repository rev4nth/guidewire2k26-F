package com.guidewire.in.controller;

import com.guidewire.in.dto.PolicyRequest;
import com.guidewire.in.dto.PolicyResponse;
import com.guidewire.in.entity.Policy;
import com.guidewire.in.repository.PolicyRepository;
import com.guidewire.in.security.JwtFilter;
import com.guidewire.in.web.ApiResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/policies")
public class PolicyController {

	private final PolicyRepository policyRepository;

	public PolicyController(PolicyRepository policyRepository) {
		this.policyRepository = policyRepository;
	}

	private boolean isAdmin(HttpServletRequest req) {
		return "ADMIN".equals(req.getAttribute(JwtFilter.ATTR_ROLE));
	}

	private PolicyResponse toDto(Policy p) {
		return new PolicyResponse(p.getId(), p.getName(), p.getPremium(), p.getCoverage(), p.isActive());
	}

	@GetMapping
	public ResponseEntity<?> list(HttpServletRequest req) {
		if (!isAdmin(req)) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
		List<PolicyResponse> list = policyRepository.findAllByOrderByIdAsc()
				.stream().map(this::toDto).collect(Collectors.toList());
		return ApiResponseBuilder.ok("Policies loaded", list);
	}

	@PostMapping
	public ResponseEntity<?> create(HttpServletRequest req, @RequestBody PolicyRequest body) {
		if (!isAdmin(req)) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
		if (body.getName() == null || body.getPremium() == null || body.getCoverage() == null) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "name, premium and coverage are required");
		}
		Policy p = new Policy();
		p.setName(body.getName().trim().toUpperCase());
		p.setPremium(body.getPremium());
		p.setCoverage(body.getCoverage());
		p.setActive(body.getActive() == null || body.getActive());
		policyRepository.save(p);
		return ApiResponseBuilder.created("Policy created", toDto(p));
	}

	@PutMapping("/{id}")
	public ResponseEntity<?> update(HttpServletRequest req, @PathVariable Long id,
			@RequestBody PolicyRequest body) {
		if (!isAdmin(req)) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
		Optional<Policy> opt = policyRepository.findById(id);
		if (opt.isEmpty()) return ApiResponseBuilder.fail(HttpStatus.NOT_FOUND, "Policy not found");
		Policy p = opt.get();
		if (body.getPremium() != null) p.setPremium(body.getPremium());
		if (body.getCoverage() != null) p.setCoverage(body.getCoverage());
		if (body.getActive() != null) p.setActive(body.getActive());
		policyRepository.save(p);
		return ApiResponseBuilder.ok("Policy updated", toDto(p));
	}
}
