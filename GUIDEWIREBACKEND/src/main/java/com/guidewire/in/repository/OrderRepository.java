package com.guidewire.in.repository;

import com.guidewire.in.entity.Order;
import com.guidewire.in.entity.OrderStatus;
import com.guidewire.in.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

	@Query("SELECT o FROM Order o JOIN FETCH o.worker WHERE o.worker = :worker ORDER BY o.createdAt DESC")
	List<Order> findByWorkerWithWorkerFetched(@Param("worker") User worker);

	@Query("SELECT o FROM Order o JOIN FETCH o.worker WHERE o.id = :id")
	Optional<Order> findByIdWithWorker(@Param("id") Long id);

	List<Order> findByWorkerOrderByCreatedAtDesc(User worker);
	List<Order> findByWorkerAndStatusOrderByCreatedAtDesc(User worker, OrderStatus status);
	Optional<Order> findFirstByWorkerAndStatusIn(User worker, List<OrderStatus> statuses);
	long countByWorker(User worker);

	void deleteByWorker(User worker);
}
