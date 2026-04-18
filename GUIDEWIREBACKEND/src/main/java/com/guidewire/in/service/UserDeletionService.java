package com.guidewire.in.service;

import com.guidewire.in.entity.User;
import com.guidewire.in.repository.ClaimRepository;
import com.guidewire.in.repository.DisruptionRepository;
import com.guidewire.in.repository.OrderRepository;
import com.guidewire.in.repository.PaymentRepository;
import com.guidewire.in.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deletes a user only after removing dependent rows (FK constraints).
 * Order: claims (reference disruptions) → disruptions → orders → payments → user.
 */
@Service
public class UserDeletionService {

	private final UserRepository userRepository;
	private final ClaimRepository claimRepository;
	private final DisruptionRepository disruptionRepository;
	private final OrderRepository orderRepository;
	private final PaymentRepository paymentRepository;

	public UserDeletionService(UserRepository userRepository,
			ClaimRepository claimRepository,
			DisruptionRepository disruptionRepository,
			OrderRepository orderRepository,
			PaymentRepository paymentRepository) {
		this.userRepository = userRepository;
		this.claimRepository = claimRepository;
		this.disruptionRepository = disruptionRepository;
		this.orderRepository = orderRepository;
		this.paymentRepository = paymentRepository;
	}

	@Transactional
	public void deleteUserById(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
		claimRepository.deleteByWorker(user);
		disruptionRepository.deleteByWorker(user);
		orderRepository.deleteByWorker(user);
		paymentRepository.deleteByUser(user);
		userRepository.delete(user);
	}
}
