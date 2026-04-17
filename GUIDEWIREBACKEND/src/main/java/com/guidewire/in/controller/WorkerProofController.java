package com.guidewire.in.controller;

import com.guidewire.in.dto.ClaimResponse;
import com.guidewire.in.entity.Claim;
import com.guidewire.in.entity.ClaimStatus;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.ClaimRepository;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class WorkerProofController {

	private final ClaimRepository claimRepository;
	private final UserRepository userRepository;
	private final CloudinaryService cloudinaryService;
	private final ClaimVerificationService claimVerificationService;

	public WorkerProofController(ClaimRepository claimRepository,
			UserRepository userRepository,
			CloudinaryService cloudinaryService,
			ClaimVerificationService claimVerificationService) {
		this.claimRepository = claimRepository;
		this.userRepository = userRepository;
		this.cloudinaryService = cloudinaryService;
		this.claimVerificationService = claimVerificationService;
	}

	@Transactional
	@PostMapping(value = "/worker/upload-proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> uploadProof(HttpServletRequest req,
			@RequestParam("claimId") Long claimId,
			@RequestParam("image") MultipartFile image) {

		Object attr = req.getAttribute(JwtFilter.ATTR_USER_ID);
		if (attr == null) return ApiResponseBuilder.fail(HttpStatus.UNAUTHORIZED, "Unauthorized");
		Long uid = Long.valueOf(attr.toString());

		Claim claim = claimRepository.findById(claimId).orElse(null);
		if (claim == null) return ApiResponseBuilder.fail(HttpStatus.NOT_FOUND, "Claim not found");
		if (!claim.getWorker().getId().equals(uid)) {
			return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Not your claim");
		}
		if (claim.getStatus() == ClaimStatus.REJECTED) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "Claim was rejected");
		}

		try {
			String url = cloudinaryService.uploadImage(image, "safeflex/claims/" + claimId);
			if (url == null) {
				return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "Image file is required");
			}
			claim.setProofImage(url);
			User worker = userRepository.findById(uid).orElseThrow();
			claimVerificationService.scoreAndSettle(claim, worker);
			return ApiResponseBuilder.ok("Proof uploaded", ClaimResponse.fromEntity(claim));
		} catch (IllegalArgumentException e) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, e.getMessage() != null ? e.getMessage() : "Invalid image file");
		} catch (Exception e) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_GATEWAY, "Upload failed");
		}
	}
}
