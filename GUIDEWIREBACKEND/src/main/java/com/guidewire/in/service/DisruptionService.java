package com.guidewire.in.service;

import com.guidewire.in.entity.Claim;
import com.guidewire.in.entity.Disruption;
import com.guidewire.in.entity.DisruptionSeverity;
import com.guidewire.in.entity.DisruptionSource;
import com.guidewire.in.entity.DisruptionType;
import com.guidewire.in.entity.Order;
import com.guidewire.in.entity.OrderStatus;
import com.guidewire.in.entity.Policy;
import com.guidewire.in.entity.User;
import com.guidewire.in.entity.ClaimStatus;
import com.guidewire.in.repository.ClaimRepository;
import com.guidewire.in.repository.DisruptionRepository;
import com.guidewire.in.repository.OrderRepository;
import com.guidewire.in.repository.PolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DisruptionService {

	private static final List<OrderStatus> ACTIVE_ORDER_STATUSES =
			List.of(OrderStatus.PENDING, OrderStatus.ACCEPTED, OrderStatus.PICKED_UP);

	private final DisruptionRepository disruptionRepository;
	private final OrderRepository orderRepository;
	private final ClaimRepository claimRepository;
	private final PolicyRepository policyRepository;
	private final ClaimVerificationService claimVerificationService;

	public DisruptionService(DisruptionRepository disruptionRepository,
			OrderRepository orderRepository,
			ClaimRepository claimRepository,
			PolicyRepository policyRepository,
			ClaimVerificationService claimVerificationService) {
		this.disruptionRepository = disruptionRepository;
		this.orderRepository = orderRepository;
		this.claimRepository = claimRepository;
		this.policyRepository = policyRepository;
		this.claimVerificationService = claimVerificationService;
	}

	/**
	 * Core flow: persist disruption row, cancel active order if any, create claim and verification scoring / payout.
	 */
	@Transactional
	public SingleDisruptionResult triggerForWorker(User worker, DisruptionType type,
			DisruptionSeverity severity, String location, DisruptionSource source) {

		Disruption disruption = new Disruption();
		disruption.setType(type);
		disruption.setSeverity(severity);
		disruption.setLocation(location);
		disruption.setSource(source);
		disruption.setWorker(worker);
		disruptionRepository.save(disruption);

		Optional<Order> activeOrder = orderRepository.findFirstByWorkerAndStatusIn(worker, ACTIVE_ORDER_STATUSES);
		Claim claim = null;

		if (activeOrder.isPresent()) {
			Order order = activeOrder.get();
			order.setStatus(OrderStatus.CANCELLED);
			orderRepository.save(order);

			BigDecimal coverage = resolveCoverage(worker);
			claim = new Claim();
			claim.setWorker(worker);
			claim.setDisruption(disruption);
			claim.setHadActiveOrder(true);
			claim.setAmount(coverage);
			claim.setReason("");
			claim.setProofImage(null);
			claim.setConfidenceScore(0);
			claim.setStatus(ClaimStatus.REVIEW);
			claim.setWalletPaid(false);
			claimRepository.save(claim);
			claimVerificationService.scoreAndSettle(claim, worker);
		}

		return new SingleDisruptionResult(disruption, activeOrder.isPresent(), claim);
	}

	private BigDecimal resolveCoverage(User worker) {
		if (worker.getActivePolicyId() != null) {
			Optional<Policy> p = policyRepository.findById(worker.getActivePolicyId());
			if (p.isPresent() && p.get().isActive()) {
				return p.get().getCoverage();
			}
		}
		return policyRepository.findAll().stream()
				.filter(Policy::isActive)
				.findFirst()
				.map(Policy::getCoverage)
				.orElse(BigDecimal.valueOf(100));
	}

	/**
	 * Same as manual broadcast: one disruption row per worker in the list.
	 */
	@Transactional
	public List<Map<String, Object>> triggerForWorkers(List<User> workers, DisruptionType type,
			DisruptionSeverity severity, String location, DisruptionSource source) {
		List<Map<String, Object>> results = new ArrayList<>();
		for (User worker : workers) {
			SingleDisruptionResult r = triggerForWorker(worker, type, severity, location, source);
			Map<String, Object> entry = new LinkedHashMap<>();
			entry.put("workerId", worker.getId());
			entry.put("workerName", worker.getName());
			entry.put("city", worker.getLocation());
			entry.put("orderCancelled", r.orderCancelled());
			entry.put("claimCreated", r.claim() != null);
			results.add(entry);
		}
		return results;
	}

	public record SingleDisruptionResult(Disruption disruption, boolean orderCancelled, Claim claim) {}
}
