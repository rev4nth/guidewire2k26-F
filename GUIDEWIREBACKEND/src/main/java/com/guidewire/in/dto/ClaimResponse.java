package com.guidewire.in.dto;

import com.guidewire.in.entity.Claim;
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
	private String proofImage;
	private int confidenceScore;
	private String status;
	/** UI lines: ✔ Active order, etc. */
	private List<String> verificationBullets;

	public static ClaimResponse fromEntity(Claim c) {
		User w = c.getWorker();
		List<String> bullets = ClaimVerificationService.buildVerificationBullets(c, w);
		return new ClaimResponse(
				c.getId(),
				w.getId(),
				w.getName(),
				c.getAmount(),
				c.getReason(),
				c.getCreatedAt(),
				c.getProofImage(),
				c.getConfidenceScore(),
				c.getStatus() != null ? c.getStatus().name() : "REVIEW",
				bullets);
	}
}
