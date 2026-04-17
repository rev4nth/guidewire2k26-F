package com.guidewire.in.controller;

import com.guidewire.in.dto.PolicyResponse;
import com.guidewire.in.entity.Policy;
import com.guidewire.in.repository.PolicyRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Public read-only policy catalog (no JWT).
 */
@RestController
@RequestMapping("/policies")
public class PublicPoliciesController {

	private final PolicyRepository policyRepository;

	public PublicPoliciesController(PolicyRepository policyRepository) {
		this.policyRepository = policyRepository;
	}

	@GetMapping
	public ResponseEntity<List<PolicyResponse>> list() {
		List<PolicyResponse> list = policyRepository.findAllByOrderByIdAsc().stream()
				.map(this::toDto)
				.collect(Collectors.toList());
		return ResponseEntity.ok(list);
	}

	private PolicyResponse toDto(Policy p) {
		return new PolicyResponse(p.getId(), p.getName(), p.getPremium(), p.getCoverage(), p.isActive());
	}
}
