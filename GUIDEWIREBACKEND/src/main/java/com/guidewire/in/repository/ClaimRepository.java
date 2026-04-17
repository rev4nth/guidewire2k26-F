package com.guidewire.in.repository;

import com.guidewire.in.entity.Claim;
import com.guidewire.in.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
	List<Claim> findByWorkerOrderByCreatedAtDesc(User worker);
}
