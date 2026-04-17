package com.guidewire.in.controller;

import com.guidewire.in.dto.MeResponse;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.UserRepository;
import com.guidewire.in.security.JwtFilter;
import com.guidewire.in.service.CloudinaryService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me")
public class ProfileController {

	private final UserRepository userRepository;
	private final CloudinaryService cloudinaryService;

	public ProfileController(UserRepository userRepository, CloudinaryService cloudinaryService) {
		this.userRepository = userRepository;
		this.cloudinaryService = cloudinaryService;
	}

	@GetMapping
	public ResponseEntity<?> me(HttpServletRequest request) {
		Long userId = (Long) request.getAttribute(JwtFilter.ATTR_USER_ID);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		return userRepository.findById(userId)
				.map(u -> ResponseEntity.ok(toMe(u)))
				.orElse(ResponseEntity.notFound().build());
	}

	@PostMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> uploadProfileImage(HttpServletRequest request, @RequestParam("image") MultipartFile image) {
		Long userId = (Long) request.getAttribute(JwtFilter.ATTR_USER_ID);
		if (userId == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		User user = userRepository.findById(userId).orElse(null);
		if (user == null) {
			return ResponseEntity.notFound().build();
		}
		try {
			String url = cloudinaryService.uploadImage(image, "safeflex/profiles/" + userId);
			if (url == null) {
				return ResponseEntity.badRequest().body("{\"error\":\"Image file is required\"}");
			}
			user.setProfileImageUrl(url);
			userRepository.save(user);
			return ResponseEntity.ok(java.util.Map.of("profileImageUrl", url));
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body("{\"error\":\"Invalid image file\"}");
		}
		catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\":\"Image upload failed\"}");
		}
	}

	private static MeResponse toMe(User u) {
		return new MeResponse(
				u.getId(),
				u.getName(),
				u.getEmail(),
				u.getRole(),
				u.getLocation(),
				u.getProfileImageUrl(),
				u.getWalletBalance() != null ? u.getWalletBalance() : 0.0,
				u.getActivePolicyId());
	}
}
