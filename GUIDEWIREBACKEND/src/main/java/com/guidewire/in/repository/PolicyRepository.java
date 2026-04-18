package com.guidewire.in.repository;

import com.guidewire.in.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
	Optional<Policy> findByNameIgnoreCase(String name);
	java.util.List<Policy> findAllByOrderByIdAsc();

	java.util.List<Policy> findAllByActiveTrueOrderByIdAsc();
}
