package com.guidewire.in.service;

import com.guidewire.in.entity.DisruptionSeverity;
import com.guidewire.in.entity.Policy;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.DisruptionRepository;
import com.guidewire.in.repository.PolicyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Workers without an active plan who buy during a recent HIGH-severity disruption window pay +30% on premiums.
 */
@Service
public class WorkerPolicyPricingService {

	public static final int SURCHARGE_PERCENT = 30;

	private final PolicyRepository policyRepository;
	private final DisruptionRepository disruptionRepository;

	@Value("${app.policy.disruption-surcharge-window-hours:72}")
	private int surchargeWindowHours;

	public WorkerPolicyPricingService(PolicyRepository policyRepository, DisruptionRepository disruptionRepository) {
		this.policyRepository = policyRepository;
		this.disruptionRepository = disruptionRepository;
	}

	public boolean hasActivePlan(User worker) {
		Long pid = worker.getActivePolicyId();
		if (pid == null) {
			return false;
		}
		return policyRepository.findById(pid).map(Policy::isActive).orElse(false);
	}

	public boolean hasRecentHighSeverityDisruption(User worker) {
		LocalDateTime since = LocalDateTime.now().minusHours(surchargeWindowHours);
		return disruptionRepository.existsByWorkerAndSeverityAndCreatedAtGreaterThanEqual(
				worker, DisruptionSeverity.HIGH, since);
	}

	/** No active insurance + at least one HIGH disruption in the lookback window → surcharge. */
	public boolean shouldApplyNoPlanDisruptionSurcharge(User worker) {
		if (hasActivePlan(worker)) {
			return false;
		}
		return hasRecentHighSeverityDisruption(worker);
	}

	public BigDecimal effectivePremium(Policy policy, User worker) {
		BigDecimal base = policy.getPremium();
		if (!shouldApplyNoPlanDisruptionSurcharge(worker)) {
			return base;
		}
		return base.multiply(BigDecimal.ONE.add(
						BigDecimal.valueOf(SURCHARGE_PERCENT).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)))
				.setScale(2, RoundingMode.HALF_UP);
	}
}
