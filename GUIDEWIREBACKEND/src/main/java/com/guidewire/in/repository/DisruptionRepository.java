package com.guidewire.in.repository;

import com.guidewire.in.entity.Disruption;
import com.guidewire.in.entity.DisruptionSource;
import com.guidewire.in.entity.DisruptionType;
import com.guidewire.in.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface DisruptionRepository extends JpaRepository<Disruption, Long> {
	List<Disruption> findByWorkerOrderByCreatedAtDesc(User worker);
	List<Disruption> findTop1ByWorkerOrderByCreatedAtDesc(User worker);

	boolean existsBySourceAndTypeAndLocationIgnoreCaseAndCreatedAtAfter(
			DisruptionSource source, DisruptionType type, String location, LocalDateTime after);

	@Query("SELECT COUNT(DISTINCT d.worker.id) FROM Disruption d")
	long countDistinctWorkersAffected();
}
