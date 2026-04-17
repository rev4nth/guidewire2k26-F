package com.guidewire.in.controller;

import com.guidewire.in.dto.CreateUserRequest;
import com.guidewire.in.dto.PendingRegistrationResponse;
import com.guidewire.in.dto.UserListResponse;
import com.guidewire.in.entity.PendingRegistration;
import com.guidewire.in.entity.Role;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.PendingRegistrationRepository;
import com.guidewire.in.repository.UserRepository;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

	private final UserRepository userRepository;
	private final PendingRegistrationRepository pendingRegistrationRepository;
	private final PasswordEncoder passwordEncoder;
	private final CloudinaryService cloudinaryService;

	public AdminController(
			UserRepository userRepository,
			PendingRegistrationRepository pendingRegistrationRepository,
			PasswordEncoder passwordEncoder,
			CloudinaryService cloudinaryService) {
		this.userRepository = userRepository;
		this.pendingRegistrationRepository = pendingRegistrationRepository;
		this.passwordEncoder = passwordEncoder;
		this.cloudinaryService = cloudinaryService;
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
			return ResponseEntity.badRequest().body("{\"error\":\"Cannot delete your own account\"}");
		}
		if (!userRepository.existsById(id)) {
			return ResponseEntity.notFound().build();
		}
		userRepository.deleteById(id);
		return ResponseEntity.noContent().build();
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
}
