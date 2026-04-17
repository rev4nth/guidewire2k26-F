package com.guidewire.in.repository;

import com.guidewire.in.entity.Disruption;
import com.guidewire.in.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisruptionRepository extends JpaRepository<Disruption, Long> {
	List<Disruption> findByWorkerOrderByCreatedAtDesc(User worker);
	List<Disruption> findTop1ByWorkerOrderByCreatedAtDesc(User worker);
}
