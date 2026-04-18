package com.guidewire.in.controller;

import com.guidewire.in.dto.AdminClaimPayoutRequest;
import com.guidewire.in.dto.AdminClaimRowResponse;
import com.guidewire.in.dto.AdminPayoutTier;
import com.guidewire.in.dto.CreateUserRequest;
import com.guidewire.in.dto.PendingRegistrationResponse;
import com.guidewire.in.dto.UserListResponse;
import com.guidewire.in.entity.Claim;
import com.guidewire.in.entity.ClaimStatus;
import com.guidewire.in.entity.PendingRegistration;
import com.guidewire.in.entity.Role;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.ClaimRepository;
import com.guidewire.in.repository.PendingRegistrationRepository;
import com.guidewire.in.repository.PolicyRepository;
import com.guidewire.in.repository.UserRepository;
import com.guidewire.in.service.ClaimVerificationService;
import com.guidewire.in.service.UserDeletionService;
import com.guidewire.in.security.JwtFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import com.guidewire.in.service.CloudinaryService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

	private final UserRepository userRepository;
	private final PendingRegistrationRepository pendingRegistrationRepository;
	private final PasswordEncoder passwordEncoder;
	private final CloudinaryService cloudinaryService;
	private final ClaimRepository claimRepository;
	private final PolicyRepository policyRepository;
	private final ClaimVerificationService claimVerificationService;
	private final UserDeletionService userDeletionService;

	public AdminController(
			UserRepository userRepository,
			PendingRegistrationRepository pendingRegistrationRepository,
			PasswordEncoder passwordEncoder,
			CloudinaryService cloudinaryService,
			ClaimRepository claimRepository,
			PolicyRepository policyRepository,
			ClaimVerificationService claimVerificationService,
			UserDeletionService userDeletionService) {
		this.userRepository = userRepository;
		this.pendingRegistrationRepository = pendingRegistrationRepository;
		this.passwordEncoder = passwordEncoder;
		this.cloudinaryService = cloudinaryService;
		this.claimRepository = claimRepository;
		this.policyRepository = policyRepository;
		this.claimVerificationService = claimVerificationService;
		this.userDeletionService = userDeletionService;
	}

	private static boolean isAdmin(HttpServletRequest request) {
		String role = (String) request.getAttribute(JwtFilter.ATTR_ROLE);
		return Role.ADMIN.name().equals(role);
	}

	@GetMapping("/pending-registrations")
	public ResponseEntity<?> listPending(HttpServletRequest request) {
		if (!isAdmin(request)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		List<PendingRegistrationResponse> list = pendingRegistrationRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(p -> new PendingRegistrationResponse(
						p.getId(),
						p.getName(),
						p.getEmail(),
						p.getRole(),
						p.getLocation(),
						p.getProfileImageUrl(),
						p.getCreatedAt()))
				.toList();
		return ResponseEntity.ok(list);
	}

	@PostMapping("/pending-registrations/{id}/approve")
	@Transactional
	public ResponseEntity<?> approvePending(HttpServletRequest request, @PathVariable Long id) {
		if (!isAdmin(request)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		PendingRegistration pending = pendingRegistrationRepository.findById(id).orElse(null);
		if (pending == null) {
			return ResponseEntity.notFound().build();
		}
		if (userRepository.existsByEmail(pending.getEmail())) {
			pendingRegistrationRepository.delete(pending);
			return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"error\":\"User with this email already exists\"}");
		}
		User user = new User();
		user.setName(pending.getName());
		user.setEmail(pending.getEmail());
		user.setPassword(pending.getPassword());
		user.setRole(pending.getRole());
		user.setLocation(pending.getLocation());
		user.setProfileImageUrl(pending.getProfileImageUrl());
		try {
			userRepository.save(user);
			pendingRegistrationRepository.delete(pending);
		}
		catch (DataIntegrityViolationException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"error\":\"Could not create user\"}");
		}
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@PostMapping("/pending-registrations/{id}/reject")
	public ResponseEntity<?> rejectPending(HttpServletRequest request, @PathVariable Long id) {
		if (!isAdmin(request)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		return pendingRegistrationRepository.findById(id)
				.map(p -> {
					pendingRegistrationRepository.delete(p);
					return ResponseEntity.noContent().build();
				})
				.orElse(ResponseEntity.notFound().build());
	}

	@GetMapping("/users")
	public ResponseEntity<?> listUsers(HttpServletRequest request) {
		if (!isAdmin(request)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		List<UserListResponse> list = userRepository.findAll().stream()
				.map(u -> new UserListResponse(
						u.getId(),
						u.getName(),
						u.getEmail(),
						u.getRole(),
						u.getLocation(),
						u.getProfileImageUrl(),
						u.getCreatedAt(),
						u.getLatitude(),
						u.getLongitude()))
				.toList();
		return ResponseEntity.ok(list);
	}

	@DeleteMapping("/users/{id}")
	public ResponseEntity<?> deleteUser(HttpServletRequest request, @PathVariable Long id) {
		if (!isAdmin(request)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		Long adminId = (Long) request.getAttribute(JwtFilter.ATTR_USER_ID);
		if (adminId != null && adminId.equals(id)) {
			return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete your own account"));
		}
		if (!userRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		try {
			userDeletionService.deleteUserById(id);
			return ResponseEntity.noContent().build();
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.notFound().build();
		}
		catch (DataIntegrityViolationException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Could not delete user (related records)"));
		}
	}

	@PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> uploadImage(HttpServletRequest request,
			@RequestParam("image") MultipartFile image) {
		if (!isAdmin(request)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		try {
			String url = cloudinaryService.uploadImage(image, "safeflex/profiles/admin-upload");
			if (url == null) {
				return ResponseEntity.badRequest().body("{\"error\":\"Image file is required\"}");
			}
			return ResponseEntity.ok(Map.of("profileImageUrl", url));
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("{\"error\":\"Only image files are allowed\"}");
		}
		catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\":\"Image upload failed\"}");
		}
	}

	@PostMapping("/create-user")
	public ResponseEntity<?> createUser(HttpServletRequest request, @RequestBody CreateUserRequest body) {
		if (!isAdmin(request)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		Role newRole;
		try {
			newRole = Role.valueOf(body.getRole());
		}
		catch (IllegalArgumentException | NullPointerException e) {
			return ResponseEntity.badRequest().body("{\"error\":\"Invalid role\"}");
		}
		if (body.getEmail() == null || body.getPassword() == null || body.getName() == null) {
			return ResponseEntity.badRequest().body("{\"error\":\"Missing required fields\"}");
		}
		User user = new User();
		user.setName(body.getName());
		user.setEmail(body.getEmail().trim());
		user.setPassword(passwordEncoder.encode(body.getPassword()));
		user.setRole(newRole);
		user.setLocation(body.getLocation());
		user.setProfileImageUrl(body.getProfileImageUrl());
		try {
			userRepository.save(user);
		}
		catch (DataIntegrityViolationException e) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"error\":\"Email already exists\"}");
		}
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	/** Claims waiting for proof review (PENDING_PROOF) — admin pays full/half plan coverage or rejects. */
	@GetMapping("/claims/pending-review")
	@Transactional(readOnly = true)
	public ResponseEntity<?> pendingReviewClaims(HttpServletRequest request) {
		if (!isAdmin(request)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		List<AdminClaimRowResponse> list = claimRepository.findByStatusWithWorkerAndDisruption(ClaimStatus.PENDING_PROOF).stream()
				.map(this::toAdminClaimRow)
				.toList();
		return ResponseEntity.ok(list);
	}

	private AdminClaimRowResponse toAdminClaimRow(Claim c) {
		User w = c.getWorker();
		String policyName = null;
		if (w.getActivePolicyId() != null) {
			policyName = policyRepository.findById(w.getActivePolicyId()).map(p -> p.getName()).orElse(null);
		}
		BigDecimal full = c.getAmount();
		BigDecimal half = full.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
		String sev = c.getDisruption() != null && c.getDisruption().getSeverity() != null
				? c.getDisruption().getSeverity().name()
				: null;
		boolean proofComplete = c.getProofImageUrl() != null && !c.getProofImageUrl().isBlank()
				&& c.getProofDescription() != null && !c.getProofDescription().isBlank();
		return new AdminClaimRowResponse(
				c.getId(),
				w.getId(),
				w.getName(),
				policyName,
				full,
				half,
				c.getConfidenceScore(),
				c.getStatus().name(),
				sev,
				c.getProofImageUrl(),
				c.getProofDescription(),
				proofComplete);
	}

	@PostMapping("/claim/{id}/payout")
	@Transactional
	public ResponseEntity<?> settleClaimPayout(HttpServletRequest request, @PathVariable Long id,
			@RequestBody(required = false) AdminClaimPayoutRequest body) {
		if (!isAdmin(request)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		if (body == null || body.getPayout() == null || body.getPayout().isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "payout is required: FULL, HALF, or NONE"));
		}
		AdminPayoutTier tier;
		try {
			tier = AdminPayoutTier.valueOf(body.getPayout().trim().toUpperCase());
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("error", "Invalid payout; use FULL, HALF, or NONE"));
		}
		try {
			Claim updated = claimVerificationService.applyAdminPayoutDecision(id, tier);
			Map<String, Object> out = new LinkedHashMap<>();
			out.put("claimId", updated.getId());
			out.put("status", updated.getStatus().name());
			out.put("walletPaid", updated.isWalletPaid());
			out.put("payoutCredited", updated.getPayoutCredited() != null ? updated.getPayoutCredited() : BigDecimal.ZERO);
			return ResponseEntity.ok(out);
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Not found"));
		}
		catch (IllegalStateException e) {
			return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Bad request"));
		}
	}
}
