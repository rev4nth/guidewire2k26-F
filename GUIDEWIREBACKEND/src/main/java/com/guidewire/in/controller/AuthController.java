package com.guidewire.in.controller;

import com.guidewire.in.dto.LoginRequest;
import com.guidewire.in.dto.LoginResponse;
import com.guidewire.in.dto.RegisterRequest;
import com.guidewire.in.entity.PendingRegistration;
import com.guidewire.in.entity.Role;
import com.guidewire.in.repository.PendingRegistrationRepository;
import com.guidewire.in.repository.UserRepository;
import com.guidewire.in.security.JwtUtil;
import com.guidewire.in.service.CloudinaryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/auth")
public class AuthController {

	private final UserRepository userRepository;
	private final PendingRegistrationRepository pendingRegistrationRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final CloudinaryService cloudinaryService;

	public AuthController(
			UserRepository userRepository,
			PendingRegistrationRepository pendingRegistrationRepository,
			PasswordEncoder passwordEncoder,
			JwtUtil jwtUtil,
			CloudinaryService cloudinaryService) {
		this.userRepository = userRepository;
		this.pendingRegistrationRepository = pendingRegistrationRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtUtil = jwtUtil;
		this.cloudinaryService = cloudinaryService;
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequest body) {
		if (body.getEmail() == null || body.getPassword() == null) {
			return ResponseEntity.badRequest().body("{\"error\":\"email and password required\"}");
		}
		return userRepository.findByEmail(body.getEmail().trim())
				.filter(u -> passwordEncoder.matches(body.getPassword(), u.getPassword()))
				.<ResponseEntity<?>>map(u -> {
					String token = jwtUtil.generateToken(u.getId(), u.getRole().name());
					return ResponseEntity.ok(new LoginResponse(
							token,
							u.getRole().name(),
							u.getId(),
							u.getName(),
							u.getEmail(),
							u.getProfileImageUrl(),
							u.getWalletBalance() != null ? u.getWalletBalance() : 0.0,
							u.getActivePolicyId()));
				})
				.orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"Invalid credentials\"}"));
	}

	@PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> registerJson(@RequestBody RegisterRequest body) {
		return registerInternal(
				body.getName(),
				body.getEmail(),
				body.getPassword(),
				body.getLocation(),
				body.getRole(),
				null);
	}

	@PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> registerMultipart(
			@RequestParam String name,
			@RequestParam String email,
			@RequestParam String password,
			@RequestParam(required = false) String location,
			@RequestParam(required = false, defaultValue = "WORKER") String role,
			@RequestParam(required = false) MultipartFile photo) {
		String imageUrl = null;
		if (photo != null && !photo.isEmpty()) {
			try {
				imageUrl = cloudinaryService.uploadImage(photo, "safeflex/pending-registrations");
			}
			catch (IllegalArgumentException e) {
				return ResponseEntity.badRequest().body("{\"error\":\"Only image uploads are allowed\"}");
			}
			catch (Exception e) {
				return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("{\"error\":\"Could not upload image\"}");
			}
		}
		return registerInternal(name, email, password, location, role, imageUrl);
	}

	private ResponseEntity<?> registerInternal(
			String name,
			String emailRaw,
			String password,
			String location,
			String roleStr,
			String profileImageUrl) {
		if (emailRaw == null || password == null || name == null) {
			return ResponseEntity.badRequest().body("{\"error\":\"name, email, and password are required\"}");
		}
		String email = emailRaw.trim();
		if (userRepository.existsByEmail(email)) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"error\":\"An account with this email already exists\"}");
		}
		if (pendingRegistrationRepository.findByEmail(email).isPresent()) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("{\"error\":\"A registration is already pending for this email\"}");
		}
		Role role = Role.WORKER;
		if (roleStr != null && !roleStr.isBlank()) {
			try {
				Role requested = Role.valueOf(roleStr.trim());
				if (requested == Role.ADMIN) {
					return ResponseEntity.badRequest().body("{\"error\":\"Cannot self-register as ADMIN\"}");
				}
				role = requested;
			}
			catch (IllegalArgumentException e) {
				return ResponseEntity.badRequest().body("{\"error\":\"Invalid role\"}");
			}
		}
		PendingRegistration pending = new PendingRegistration();
		pending.setName(name.trim());
		pending.setEmail(email);
		pending.setPassword(passwordEncoder.encode(password));
		pending.setRole(role);
		pending.setLocation(location);
		pending.setProfileImageUrl(profileImageUrl);
		pendingRegistrationRepository.save(pending);
		return ResponseEntity.status(HttpStatus.CREATED).body("{\"message\":\"Registration submitted. An admin will review it.\"}");
	}
}
