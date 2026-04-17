package com.guidewire.in.repository;

import com.guidewire.in.entity.Order;
import com.guidewire.in.entity.OrderStatus;
import com.guidewire.in.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
	List<Order> findByWorkerOrderByCreatedAtDesc(User worker);
	List<Order> findByWorkerAndStatusOrderByCreatedAtDesc(User worker, OrderStatus status);
	Optional<Order> findFirstByWorkerAndStatusIn(User worker, List<OrderStatus> statuses);
	long countByWorker(User worker);
}
