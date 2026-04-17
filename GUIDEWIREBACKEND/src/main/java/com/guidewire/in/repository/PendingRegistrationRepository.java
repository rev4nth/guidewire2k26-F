package com.guidewire.in.repository;

import com.guidewire.in.entity.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {

	Optional<PendingRegistration> findByEmail(String email);

	List<PendingRegistration> findAllByOrderByCreatedAtDesc();
}
