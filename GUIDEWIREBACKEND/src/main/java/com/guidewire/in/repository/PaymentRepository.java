package com.guidewire.in.repository;

import com.guidewire.in.entity.Payment;
import com.guidewire.in.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
	List<Payment> findByUserOrderByCreatedAtDesc(User user);
}
