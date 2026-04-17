package com.guidewire.in.controller;

import com.guidewire.in.dto.LocationRequest;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.UserRepository;
import com.guidewire.in.security.JwtFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/worker/location")
public class WorkerLocationController {

	private final UserRepository userRepository;

	public WorkerLocationController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * POST /worker/location
	 * Body: { "latitude": 17.38, "longitude": 78.48, "city": "Hyderabad" }
	 *
	 * Saves GPS coordinates (and optional city) for the authenticated worker.
	 */
	@PostMapping
	public ResponseEntity<?> updateLocation(HttpServletRequest req,
			@RequestBody LocationRequest body) {
		Long uid = resolveUid(req);
		if (uid == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

		User user = userRepository.findById(uid).orElse(null);
		if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

		if (body.getLatitude() == null || body.getLongitude() == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "latitude and longitude are required"));
		}

		user.setLatitude(body.getLatitude());
		user.setLongitude(body.getLongitude());
		if (body.getCity() != null && !body.getCity().isBlank()) {
			user.setLocation(body.getCity().trim());
		}
		userRepository.save(user);

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("latitude",  user.getLatitude());
		response.put("longitude", user.getLongitude());
		response.put("city",      user.getLocation());
		response.put("message",   "Location updated");
		return ResponseEntity.ok(response);
	}

	/**
	 * GET /worker/location
	 * Returns the current stored location for the authenticated worker.
	 */
	@GetMapping
	public ResponseEntity<?> getLocation(HttpServletRequest req) {
		Long uid = resolveUid(req);
		if (uid == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

		User user = userRepository.findById(uid).orElse(null);
		if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("latitude",  user.getLatitude());
		response.put("longitude", user.getLongitude());
		response.put("city",      user.getLocation());
		return ResponseEntity.ok(response);
	}

	private Long resolveUid(HttpServletRequest req) {
		Object attr = req.getAttribute(JwtFilter.ATTR_USER_ID);
		if (attr == null) return null;
		try { return Long.valueOf(attr.toString()); } catch (NumberFormatException e) { return null; }
	}
}
