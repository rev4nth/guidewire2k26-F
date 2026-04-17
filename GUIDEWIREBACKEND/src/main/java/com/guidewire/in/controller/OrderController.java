package com.guidewire.in.controller;

import com.guidewire.in.dto.OrderResponse;
import com.guidewire.in.entity.Order;
import com.guidewire.in.entity.OrderStatus;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.OrderRepository;
import com.guidewire.in.repository.UserRepository;
import com.guidewire.in.security.JwtFilter;
import com.guidewire.in.service.LocationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class OrderController {

	private final OrderRepository orderRepository;
	private final UserRepository userRepository;
	private final LocationService locationService;

	public OrderController(OrderRepository orderRepository, UserRepository userRepository,
			LocationService locationService) {
		this.orderRepository = orderRepository;
		this.userRepository  = userRepository;
		this.locationService = locationService;
	}

	private boolean isAdmin(HttpServletRequest req) {
		return "ADMIN".equals(req.getAttribute(JwtFilter.ATTR_ROLE));
	}

	private Long currentUserId(HttpServletRequest req) {
		Object attr = req.getAttribute(JwtFilter.ATTR_USER_ID);
		if (attr == null) return null;
		return Long.valueOf(attr.toString());
	}

	private OrderResponse toDto(Order o) {
		return new OrderResponse(
				o.getId(),
				o.getWorker().getId(),
				o.getWorker().getName(),
				o.getStatus().name(),
				o.getCreatedAt()
		);
	}

	/* ── ADMIN ── */

	@PostMapping("/admin/send-order/{workerId}")
	public ResponseEntity<?> sendOrder(HttpServletRequest req, @PathVariable Long workerId) {
		if (!isAdmin(req)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		User worker = userRepository.findById(workerId).orElse(null);
		if (worker == null) return ResponseEntity.notFound().build();
		Order order = new Order();
		order.setWorker(worker);
		order.setStatus(OrderStatus.PENDING);
		orderRepository.save(order);
		return ResponseEntity.status(HttpStatus.CREATED).body(toDto(order));
	}

	/**
	 * POST /admin/send-order/nearest?lat={lat}&lon={lon}&radiusKm={km}
	 *
	 * Finds the nearest available worker within radiusKm (default 50 km) of the
	 * given coordinates and creates a PENDING order for them.
	 */
	@PostMapping("/admin/send-order/nearest")
	public ResponseEntity<?> sendOrderToNearest(HttpServletRequest req,
			@RequestParam double lat,
			@RequestParam double lon,
			@RequestParam(defaultValue = "50") double radiusKm) {
		if (!isAdmin(req)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

		User nearest = locationService
				.findNearestWorkerWithinRadius(lat, lon, radiusKm)
				.orElse(null);

		if (nearest == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND)
					.body(Map.of("error", "No available worker found within " + radiusKm + " km"));
		}

		Order order = new Order();
		order.setWorker(nearest);
		order.setStatus(OrderStatus.PENDING);
		orderRepository.save(order);

		Map<String, Object> result = new java.util.LinkedHashMap<>();
		result.put("order",          toDto(order));
		result.put("workerName",     nearest.getName());
		result.put("workerCity",     nearest.getLocation());
		result.put("workerLat",      nearest.getLatitude());
		result.put("workerLon",      nearest.getLongitude());
		result.put("distanceKm",
				Math.round(LocationService.distanceKm(lat, lon, nearest.getLatitude(), nearest.getLongitude()) * 10.0) / 10.0);
		return ResponseEntity.status(HttpStatus.CREATED).body(result);
	}

	/* ── WORKER ── */

	@GetMapping("/worker/orders")
	public ResponseEntity<?> myOrders(HttpServletRequest req) {
		Long uid = currentUserId(req);
		if (uid == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		User worker = userRepository.findById(uid).orElse(null);
		if (worker == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		List<OrderResponse> list = orderRepository.findByWorkerOrderByCreatedAtDesc(worker)
				.stream().map(this::toDto).collect(Collectors.toList());
		return ResponseEntity.ok(list);
	}

	@PostMapping("/worker/orders/{id}/accept")
	public ResponseEntity<?> accept(HttpServletRequest req, @PathVariable Long id) {
		return transition(req, id, OrderStatus.PENDING, OrderStatus.ACCEPTED);
	}

	@PostMapping("/worker/orders/{id}/pickup")
	public ResponseEntity<?> pickup(HttpServletRequest req, @PathVariable Long id) {
		return transition(req, id, OrderStatus.ACCEPTED, OrderStatus.PICKED_UP);
	}

	@PostMapping("/worker/orders/{id}/deliver")
	public ResponseEntity<?> deliver(HttpServletRequest req, @PathVariable Long id) {
		return transition(req, id, OrderStatus.PICKED_UP, OrderStatus.DELIVERED);
	}

	@PostMapping("/worker/orders/{id}/cancel")
	public ResponseEntity<?> cancel(HttpServletRequest req, @PathVariable Long id) {
		Long uid = currentUserId(req);
		if (uid == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		Order order = orderRepository.findById(id).orElse(null);
		if (order == null) return ResponseEntity.notFound().build();
		if (!order.getWorker().getId().equals(uid))
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		if (order.getStatus() == OrderStatus.DELIVERED)
			return ResponseEntity.badRequest().body("{\"error\":\"Cannot cancel a delivered order\"}");
		order.setStatus(OrderStatus.CANCELLED);
		orderRepository.save(order);
		return ResponseEntity.ok(toDto(order));
	}

	private ResponseEntity<?> transition(HttpServletRequest req, Long orderId,
			OrderStatus from, OrderStatus to) {
		Long uid = currentUserId(req);
		if (uid == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		Order order = orderRepository.findById(orderId).orElse(null);
		if (order == null) return ResponseEntity.notFound().build();
		if (!order.getWorker().getId().equals(uid))
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		if (order.getStatus() != from)
			return ResponseEntity.badRequest()
					.body("{\"error\":\"Order must be in status " + from + " to perform this action\"}");
		order.setStatus(to);
		orderRepository.save(order);
		return ResponseEntity.ok(toDto(order));
	}
}
