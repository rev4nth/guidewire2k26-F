package com.guidewire.in.service;

import com.guidewire.in.entity.Claim;
import com.guidewire.in.entity.ClaimStatus;
import com.guidewire.in.entity.Disruption;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.ClaimRepository;
import com.guidewire.in.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ClaimVerificationService {

	private final ClaimRepository claimRepository;
	private final UserRepository userRepository;
	private final FinanceService financeService;

	public ClaimVerificationService(ClaimRepository claimRepository,
			UserRepository userRepository,
			FinanceService financeService) {
		this.claimRepository = claimRepository;
		this.userRepository = userRepository;
		this.financeService = financeService;
	}

	/**
	 * Rules: active order +50, location matches disruption +30, proof image +20. Score 70+ → APPROVED.
	 */
	public int computeScore(User worker, Disruption disruption, boolean hadActiveOrder, String proofImage) {
		int score = 0;
		if (hadActiveOrder) {
			score += 50;
		}
		if (disruption != null && locationMatches(worker, disruption)) {
			score += 30;
		}
		if (proofImage != null && !proofImage.isBlank()) {
			score += 20;
		}
		return score;
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

	@Transactional
	public void scoreAndSettle(Claim claim, User worker) {
		User w = userRepository.findById(worker.getId()).orElseThrow();
		int score = computeScore(
				w,
				claim.getDisruption(),
				claim.isHadActiveOrder(),
				claim.getProofImage());
		claim.setConfidenceScore(score);
		claim.setReason(buildVerificationReasonText(w, claim));

		if (score >= 70) {
			claim.setStatus(ClaimStatus.APPROVED);
			if (!claim.isWalletPaid()) {
				financeService.creditWalletForClaim(w, claim.getAmount(), "claim:" + claim.getId());
				claim.setWalletPaid(true);
			}
		} else {
			claim.setStatus(ClaimStatus.REVIEW);
		}
		claimRepository.save(claim);
	}

	/** Single-line explanation for APIs; UI uses {@link #buildVerificationBullets} for checkmarks */
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
		if (claim.getProofImage() != null && !claim.getProofImage().isBlank()) {
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
		if (claim.getProofImage() != null && !claim.getProofImage().isBlank()) {
			bullets.add("✔ Proof uploaded");
		}
		return bullets;
	}

	/**
	 * Government override: APPROVED pays if not yet paid; REJECTED only if not yet paid.
	 */
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
		}
		claimRepository.save(claim);
		return claim;
	}
}
