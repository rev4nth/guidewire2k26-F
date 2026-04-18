package com.guidewire.in.repository;

import com.guidewire.in.entity.Disruption;
import com.guidewire.in.entity.DisruptionSeverity;
import com.guidewire.in.entity.DisruptionSource;
import com.guidewire.in.entity.DisruptionType;
import com.guidewire.in.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DisruptionRepository extends JpaRepository<Disruption, Long> {

	/** Eager worker — required when open-in-view=false and mapping to DTOs */
	@Query("SELECT d FROM Disruption d JOIN FETCH d.worker ORDER BY d.createdAt DESC")
	List<Disruption> findAllWithWorkerOrderByCreatedAtDesc();

	@Query("SELECT d FROM Disruption d JOIN FETCH d.worker WHERE d.worker = :worker ORDER BY d.createdAt DESC")
	List<Disruption> findByWorkerWithWorkerFetched(@Param("worker") User worker);

	List<Disruption> findByWorkerOrderByCreatedAtDesc(User worker);
	List<Disruption> findTop1ByWorkerOrderByCreatedAtDesc(User worker);

	boolean existsBySourceAndTypeAndLocationIgnoreCaseAndCreatedAtAfter(
			DisruptionSource source, DisruptionType type, String location, LocalDateTime after);

	@Query("SELECT COUNT(DISTINCT d.worker.id) FROM Disruption d")
	long countDistinctWorkersAffected();

	void deleteByWorker(User worker);

	boolean existsByWorkerAndSeverityAndCreatedAtGreaterThanEqual(User worker, DisruptionSeverity severity, LocalDateTime createdAt);
}
