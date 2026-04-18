package com.guidewire.in.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminClaimRowResponse {
	private Long claimId;
	private Long workerId;
	private String workerName;
	/** Active policy name when resolvable (plan tier). */
	private String policyName;
	/** Max coverage for this claim (from plan snapshot). */
	private BigDecimal fullCoverageAmount;
	/** Half of plan coverage (rounded). */
	private BigDecimal halfCoverageAmount;
	private int confidenceScore;
	private String status;
	private String disruptionSeverity;
	private String proofImageUrl;
	private String proofDescription;
	/** True when both image and explanation are present (ready for full/half approval). */
	private boolean proofComplete;
}
