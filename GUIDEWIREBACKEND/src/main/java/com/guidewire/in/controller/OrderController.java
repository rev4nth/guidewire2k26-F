package com.guidewire.in.controller;

import com.guidewire.in.dto.OrderResponse;
import com.guidewire.in.entity.Order;
import com.guidewire.in.entity.OrderStatus;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.OrderRepository;
import com.guidewire.in.repository.UserRepository;
import com.guidewire.in.security.JwtFilter;
import com.guidewire.in.service.LocationService;
import com.guidewire.in.web.ApiResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
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

	@PostMapping("/admin/send-order/{workerId}")
	public ResponseEntity<?> sendOrder(HttpServletRequest req, @PathVariable Long workerId) {
		if (!isAdmin(req)) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
		User worker = userRepository.findById(workerId).orElse(null);
		if (worker == null) return ApiResponseBuilder.fail(HttpStatus.NOT_FOUND, "Worker not found");
		Order order = new Order();
		order.setWorker(worker);
		order.setStatus(OrderStatus.PENDING);
		orderRepository.save(order);
		return ApiResponseBuilder.created("Order created", toDto(order));
	}

	@PostMapping("/admin/send-order/nearest")
	public ResponseEntity<?> sendOrderToNearest(HttpServletRequest req,
			@RequestParam double lat,
			@RequestParam double lon,
			@RequestParam(defaultValue = "50") double radiusKm) {
		if (!isAdmin(req)) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");

		User nearest = locationService
				.findNearestWorkerWithinRadius(lat, lon, radiusKm)
				.orElse(null);

		if (nearest == null) {
			return ApiResponseBuilder.fail(HttpStatus.NOT_FOUND,
					"No available worker found within " + radiusKm + " km");
		}

		Order order = new Order();
		order.setWorker(nearest);
		order.setStatus(OrderStatus.PENDING);
		orderRepository.save(order);

		Map<String, Object> result = new LinkedHashMap<>();
		result.put("order", toDto(order));
		result.put("workerName", nearest.getName());
		result.put("workerCity", nearest.getLocation());
		result.put("workerLat", nearest.getLatitude());
		result.put("workerLon", nearest.getLongitude());
		result.put("distanceKm",
				Math.round(LocationService.distanceKm(lat, lon, nearest.getLatitude(), nearest.getLongitude()) * 10.0) / 10.0);
		return ApiResponseBuilder.created("Order assigned to nearest worker", result);
	}

	@GetMapping("/worker/orders")
	public ResponseEntity<?> myOrders(HttpServletRequest req) {
		Long uid = currentUserId(req);
		if (uid == null) return ApiResponseBuilder.fail(HttpStatus.UNAUTHORIZED, "Unauthorized");
		User worker = userRepository.findById(uid).orElse(null);
		if (worker == null) return ApiResponseBuilder.fail(HttpStatus.UNAUTHORIZED, "Unauthorized");
		List<OrderResponse> list = orderRepository.findByWorkerOrderByCreatedAtDesc(worker)
				.stream().map(this::toDto).collect(Collectors.toList());
		return ApiResponseBuilder.ok("Orders loaded", list);
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
		if (uid == null) return ApiResponseBuilder.fail(HttpStatus.UNAUTHORIZED, "Unauthorized");
		Order order = orderRepository.findById(id).orElse(null);
		if (order == null) return ApiResponseBuilder.fail(HttpStatus.NOT_FOUND, "Order not found");
		if (!order.getWorker().getId().equals(uid))
			return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
		if (order.getStatus() == OrderStatus.DELIVERED)
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "Cannot cancel a delivered order");
		order.setStatus(OrderStatus.CANCELLED);
		orderRepository.save(order);
		return ApiResponseBuilder.ok("Order cancelled", toDto(order));
	}

	private ResponseEntity<?> transition(HttpServletRequest req, Long orderId,
			OrderStatus from, OrderStatus to) {
		Long uid = currentUserId(req);
		if (uid == null) return ApiResponseBuilder.fail(HttpStatus.UNAUTHORIZED, "Unauthorized");
		Order order = orderRepository.findById(orderId).orElse(null);
		if (order == null) return ApiResponseBuilder.fail(HttpStatus.NOT_FOUND, "Order not found");
		if (!order.getWorker().getId().equals(uid))
			return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
		if (order.getStatus() != from)
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST,
					"Order must be in status " + from + " to perform this action");
		order.setStatus(to);
		orderRepository.save(order);
		return ApiResponseBuilder.ok("Order updated", toDto(order));
	}
}
