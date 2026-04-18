package com.guidewire.in.controller;

import com.guidewire.in.dto.ClaimResponse;
import com.guidewire.in.entity.Role;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.UserRepository;
import com.guidewire.in.security.JwtFilter;
import com.guidewire.in.service.ClaimVerificationService;
import com.guidewire.in.service.CloudinaryService;
import com.guidewire.in.web.ApiResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class WorkerProofController {

	private final UserRepository userRepository;
	private final CloudinaryService cloudinaryService;
	private final ClaimVerificationService claimVerificationService;

	public WorkerProofController(UserRepository userRepository,
			CloudinaryService cloudinaryService,
			ClaimVerificationService claimVerificationService) {
		this.userRepository = userRepository;
		this.cloudinaryService = cloudinaryService;
		this.claimVerificationService = claimVerificationService;
	}

	private User requireWorker(HttpServletRequest req) {
		Object a = req.getAttribute(JwtFilter.ATTR_USER_ID);
		if (a == null) return null;
		Long id = Long.valueOf(a.toString());
		User u = userRepository.findById(id).orElse(null);
		if (u == null || u.getRole() != Role.WORKER) return null;
		return u;
	}

	/**
	 * POST /worker/claim/{id}/upload-proof — multipart image + optional description (WORKER only).
	 */
	@Transactional
	@PostMapping(value = "/worker/claim/{id}/upload-proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> uploadProofForClaim(HttpServletRequest req,
			@PathVariable("id") Long claimId,
			@RequestParam("image") MultipartFile image,
			@RequestParam(value = "description", required = false) String description) {
		return handleUpload(req, claimId, image, description);
	}

	/** @deprecated Prefer {@code POST /worker/claim/{id}/upload-proof} */
	@Transactional
	@PostMapping(value = "/worker/upload-proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> uploadProofLegacy(HttpServletRequest req,
			@RequestParam("claimId") Long claimId,
			@RequestParam("image") MultipartFile image,
			@RequestParam(value = "description", required = false) String description) {
		return handleUpload(req, claimId, image, description);
	}

	private ResponseEntity<?> handleUpload(HttpServletRequest req, Long claimId, MultipartFile image, String description) {
		User worker = requireWorker(req);
		if (worker == null) {
			return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Only workers can upload claim proof");
		}

		try {
			String url = cloudinaryService.uploadImage(image, "safeflex/claims/" + claimId);
			if (url == null) {
				return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "Image file is required");
			}
			var claim = claimVerificationService.uploadProofAndRescore(claimId, worker.getId(), url, description);
			return ApiResponseBuilder.ok("Proof uploaded", ClaimResponse.fromEntity(claim));
		} catch (IllegalArgumentException e) {
			String msg = e.getMessage() != null ? e.getMessage() : "Invalid request";
			HttpStatus st = "Claim not found".equals(msg) ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
			return ApiResponseBuilder.fail(st, msg);
		} catch (IllegalStateException e) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST,
					e.getMessage() != null ? e.getMessage() : "Cannot upload proof");
		} catch (Exception e) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_GATEWAY, "Upload failed");
		}
	}
}
