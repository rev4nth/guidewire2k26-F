package com.guidewire.in.repository;

import com.guidewire.in.entity.PricingHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PricingHistoryRepository extends JpaRepository<PricingHistory, Long> {
	List<PricingHistory> findTop20ByOrderByCreatedAtDesc();
	List<PricingHistory> findByCityIgnoreCaseOrderByCreatedAtDesc(String city);
}
