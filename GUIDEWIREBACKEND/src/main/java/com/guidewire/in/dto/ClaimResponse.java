package com.guidewire.in.dto;

import com.guidewire.in.entity.Claim;
import com.guidewire.in.entity.ClaimStatus;
import com.guidewire.in.entity.User;
import com.guidewire.in.service.ClaimVerificationService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaimResponse {
	private Long id;
	private Long workerId;
	private String workerName;
	private BigDecimal amount;
	private String reason;
	private LocalDateTime createdAt;
	private String proofImageUrl;
	private String proofDescription;
	private int confidenceScore;
	private String status;
	/** UI lines: ✔ Active order, etc. */
	private List<String> verificationBullets;

	/** From linked disruption; drives severity-based payout rules in UI. */
	private String disruptionSeverity;

	/** Rupees credited to wallet when approved (may be half of {@link #amount}). Null if not paid yet. */
	private BigDecimal amountCredited;

	public static ClaimResponse fromEntity(Claim c) {
		User w = c.getWorker();
		List<String> bullets = ClaimVerificationService.buildVerificationBullets(c, w);
		String sev = null;
		if (c.getDisruption() != null && c.getDisruption().getSeverity() != null) {
			sev = c.getDisruption().getSeverity().name();
		}
		BigDecimal credited = c.getPayoutCredited();
		if (c.isWalletPaid() && c.getStatus() == ClaimStatus.APPROVED && credited == null) {
			credited = c.getAmount();
		}
		return new ClaimResponse(
				c.getId(),
				w.getId(),
				w.getName(),
				c.getAmount(),
				c.getReason(),
				c.getCreatedAt(),
				c.getProofImageUrl(),
				c.getProofDescription(),
				c.getConfidenceScore(),
				c.getStatus() != null ? c.getStatus().name() : "PENDING_PROOF",
				bullets,
				sev,
				credited);
	}
}
