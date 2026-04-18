package com.guidewire.in.service;

import com.guidewire.in.dto.AdminPayoutTier;
import com.guidewire.in.entity.Claim;
import com.guidewire.in.entity.ClaimStatus;
import com.guidewire.in.entity.Disruption;
import com.guidewire.in.entity.DisruptionSeverity;
import com.guidewire.in.entity.User;
import com.guidewire.in.pricing.PricingService;
import com.guidewire.in.repository.ClaimRepository;
import com.guidewire.in.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class ClaimVerificationService {

	private static final int PROOF_UPLOAD_BONUS = 20;

	private final ClaimRepository claimRepository;
	private final UserRepository userRepository;
	private final FinanceService financeService;
	private final PricingService pricingService;

	public ClaimVerificationService(ClaimRepository claimRepository,
			UserRepository userRepository,
			FinanceService financeService,
			PricingService pricingService) {
		this.claimRepository = claimRepository;
		this.userRepository = userRepository;
		this.financeService = financeService;
		this.pricingService = pricingService;
	}

	/**
	 * Active order +50, location match +30, proof on file +20 (max 100).
	 */
	public int calculateConfidence(User worker, Disruption disruption, boolean hadActiveOrder, String proofImageUrl) {
		int score = 0;
		if (hadActiveOrder) {
			score += 50;
		}
		if (disruption != null && locationMatches(worker, disruption)) {
			score += 30;
		}
		if (proofImageUrl != null && !proofImageUrl.isBlank()) {
			score += PROOF_UPLOAD_BONUS;
		}
		return Math.min(100, score);
	}

	/** @deprecated use {@link #calculateConfidence} */
	public int computeScore(User worker, Disruption disruption, boolean hadActiveOrder, String proofImageUrl) {
		return calculateConfidence(worker, disruption, hadActiveOrder, proofImageUrl);
	}

	public static boolean locationMatches(User worker, Disruption disruption) {
		if (worker.getLocation() == null || disruption.getLocation() == null) {
			return false;
		}
		String wl = worker.getLocation().trim().toLowerCase();
		String dl = disruption.getLocation().trim().toLowerCase();
		if (wl.isEmpty() || dl.isEmpty()) {
			return false;
		}
		return wl.equals(dl) || wl.contains(dl) || dl.contains(wl);
	}

	private void creditIfEligible(User worker, Claim claim) {
		if (claim.isWalletPaid()) {
			return;
		}
		BigDecimal pay = claim.getAmount();
		financeService.creditWalletForClaim(worker, pay, "claim:" + claim.getId());
		claim.setWalletPaid(true);
		claim.setPayoutCredited(pay);
	}

	/**
	 * Severity-based settlement after a claim is created (no proof yet).
	 * LOW → always PENDING_PROOF, no auto credit.
	 * MEDIUM → ≥70 APPROVED+credit, else PENDING_PROOF.
	 * HIGH → APPROVED+credit + dynamic price surge ({@code increasePrices}).
	 */
	@Transactional
	public void scoreAndSettle(Claim claim, User worker) {
		User w = userRepository.findById(worker.getId()).orElseThrow();
		Disruption disruption = claim.getDisruption();
		DisruptionSeverity severity = disruption != null ? disruption.getSeverity() : DisruptionSeverity.MEDIUM;

		int confidence = calculateConfidence(w, disruption, claim.isHadActiveOrder(), claim.getProofImageUrl());
		claim.setConfidenceScore(confidence);
		claim.setReason(buildVerificationReasonText(w, claim));

		if (severity == DisruptionSeverity.LOW) {
			claim.setStatus(ClaimStatus.PENDING_PROOF);
		} else if (severity == DisruptionSeverity.MEDIUM) {
			if (confidence >= 70) {
				claim.setStatus(ClaimStatus.APPROVED);
				creditIfEligible(w, claim);
			} else {
				claim.setStatus(ClaimStatus.PENDING_PROOF);
			}
		} else {
			claim.setStatus(ClaimStatus.APPROVED);
			creditIfEligible(w, claim);
			String loc = disruption != null ? disruption.getLocation() : w.getLocation();
			pricingService.increasePrices(loc != null ? loc : "", 30);
		}

		claimRepository.save(claim);
	}

	/**
	 * After proof upload: store image + required explanation, rescore confidence (includes proof bonus).
	 * Payout is decided by an admin (full / half / none) — no automatic wallet credit here.
	 */
	@Transactional
	public Claim uploadProofAndRescore(Long claimId, Long workerUserId, String imageUrl, String proofDescription) {
		Claim claim = claimRepository.findByIdWithWorkerAndDisruption(claimId)
				.orElseThrow(() -> new IllegalArgumentException("Claim not found"));
		if (!claim.getWorker().getId().equals(workerUserId)) {
			throw new IllegalArgumentException("Not your claim");
		}
		if (claim.getStatus() == ClaimStatus.REJECTED) {
			throw new IllegalStateException("Claim was rejected");
		}
		if (claim.getStatus() != ClaimStatus.PENDING_PROOF) {
			throw new IllegalStateException("Proof upload is only allowed when status is PENDING_PROOF");
		}
		if (proofDescription == null || proofDescription.isBlank()) {
			throw new IllegalArgumentException("Written explanation is required with your proof image");
		}
		claim.setProofImageUrl(imageUrl);
		claim.setProofDescription(proofDescription.trim());
		User w = userRepository.findById(workerUserId).orElseThrow();
		int confidence = calculateConfidence(w, claim.getDisruption(), claim.isHadActiveOrder(), claim.getProofImageUrl());
		claim.setConfidenceScore(confidence);
		claim.setReason(buildVerificationReasonText(w, claim));
		claim.setStatus(ClaimStatus.PENDING_PROOF);
		claimRepository.save(claim);
		return claim;
	}

	/**
	 * Admin settles a pending claim after reviewing proof: full plan payout, half, or reject.
	 */
	@Transactional
	public Claim applyAdminPayoutDecision(Long claimId, AdminPayoutTier tier) {
		Claim claim = claimRepository.findByIdWithWorkerAndDisruption(claimId)
				.orElseThrow(() -> new IllegalArgumentException("Claim not found"));
		if (claim.isWalletPaid()) {
			throw new IllegalStateException("Claim was already paid");
		}
		if (claim.getStatus() != ClaimStatus.PENDING_PROOF) {
			throw new IllegalStateException("Only claims pending review can be settled here");
		}
		if (tier == AdminPayoutTier.FULL || tier == AdminPayoutTier.HALF) {
			if (claim.getProofImageUrl() == null || claim.getProofImageUrl().isBlank()) {
				throw new IllegalStateException("Worker must upload a proof image before approval");
			}
			if (claim.getProofDescription() == null || claim.getProofDescription().isBlank()) {
				throw new IllegalStateException("Worker must add an explanation before approval");
			}
		}
		User w = userRepository.findById(claim.getWorker().getId()).orElseThrow();
		if (tier == AdminPayoutTier.NONE) {
			claim.setStatus(ClaimStatus.REJECTED);
			claimRepository.save(claim);
			return claim;
		}
		if (tier == AdminPayoutTier.HALF) {
			BigDecimal half = claim.getAmount().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
			claim.setStatus(ClaimStatus.APPROVED);
			claim.setConfidenceScore(100);
			financeService.creditWalletForClaim(w, half, "claim:" + claim.getId());
			claim.setWalletPaid(true);
			claim.setPayoutCredited(half);
			claimRepository.save(claim);
			return claim;
		}
		claim.setStatus(ClaimStatus.APPROVED);
		claim.setConfidenceScore(100);
		financeService.creditWalletForClaim(w, claim.getAmount(), "claim:" + claim.getId());
		claim.setWalletPaid(true);
		claim.setPayoutCredited(claim.getAmount());
		claimRepository.save(claim);
		return claim;
	}

	public static String buildVerificationReasonText(User worker, Claim claim) {
		List<String> parts = new ArrayList<>();
		if (claim.isHadActiveOrder()) {
			parts.add("Active order");
		}
		if (claim.getDisruption() != null) {
			parts.add("Disruption");
		}
		if (claim.getDisruption() != null && locationMatches(worker, claim.getDisruption())) {
			parts.add("Location match");
		}
		if (claim.getProofImageUrl() != null && !claim.getProofImageUrl().isBlank()) {
			parts.add("Proof uploaded");
		}
		return String.join(" + ", parts);
	}

	public static List<String> buildVerificationBullets(Claim claim, User worker) {
		List<String> bullets = new ArrayList<>();
		if (claim.isHadActiveOrder()) {
			bullets.add("✔ Active order");
		}
		if (claim.getDisruption() != null) {
			bullets.add("✔ Disruption");
		}
		if (claim.getDisruption() != null && locationMatches(worker, claim.getDisruption())) {
			bullets.add("✔ Location match");
		}
		if (claim.getProofImageUrl() != null && !claim.getProofImageUrl().isBlank()) {
			bullets.add("✔ Proof uploaded");
		}
		if (claim.getDisruption() != null && claim.getDisruption().getSeverity() != null) {
			bullets.add("✔ Severity: " + claim.getDisruption().getSeverity().name());
		}
		return bullets;
	}

	@Transactional
	public Claim applyGovernmentDecision(Long claimId, ClaimStatus decision) {
		Claim claim = claimRepository.findById(claimId).orElseThrow();
		if (decision != ClaimStatus.APPROVED && decision != ClaimStatus.REJECTED) {
			throw new IllegalArgumentException("Only APPROVED or REJECTED allowed");
		}
		if (decision == ClaimStatus.REJECTED) {
			if (claim.isWalletPaid()) {
				throw new IllegalStateException("Cannot reject a claim that was already paid");
			}
			claim.setStatus(ClaimStatus.REJECTED);
			claimRepository.save(claim);
			return claim;
		}
		claim.setStatus(ClaimStatus.APPROVED);
		if (claim.getConfidenceScore() < 100) {
			claim.setConfidenceScore(100);
		}
		User w = userRepository.findById(claim.getWorker().getId()).orElseThrow();
		if (!claim.isWalletPaid()) {
			financeService.creditWalletForClaim(w, claim.getAmount(), "claim:" + claim.getId());
			claim.setWalletPaid(true);
			claim.setPayoutCredited(claim.getAmount());
		}
		claimRepository.save(claim);
		return claim;
	}
}
