package com.guidewire.in.repository;

import com.guidewire.in.entity.Claim;
import com.guidewire.in.entity.ClaimStatus;
import com.guidewire.in.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

	/** Eager worker + disruption for DTO mapping with open-in-view=false */
	@Query("SELECT c FROM Claim c JOIN FETCH c.worker LEFT JOIN FETCH c.disruption WHERE c.worker = :worker ORDER BY c.createdAt DESC")
	List<Claim> findByWorkerWithAssociations(@Param("worker") User worker);

	List<Claim> findByWorkerOrderByCreatedAtDesc(User worker);

	@Query("SELECT c FROM Claim c JOIN FETCH c.worker LEFT JOIN FETCH c.disruption ORDER BY c.createdAt DESC")
	List<Claim> findAllWithWorkerAndDisruptionOrderByCreatedAtDesc();

	@Query("SELECT c FROM Claim c JOIN FETCH c.worker LEFT JOIN FETCH c.disruption WHERE c.id = :id")
	Optional<Claim> findByIdWithWorkerAndDisruption(@Param("id") Long id);

	@Query("SELECT c FROM Claim c JOIN FETCH c.worker LEFT JOIN FETCH c.disruption WHERE c.status = :status ORDER BY c.createdAt DESC")
	List<Claim> findByStatusWithWorkerAndDisruption(@Param("status") ClaimStatus status);

	void deleteByWorker(User worker);
}
