package com.guidewire.in.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GovtClaimRowResponse {
	private Long claimId;
	private Long workerId;
	private String workerName;
	private BigDecimal amount;
	private int confidenceScore;
	private String status;
}
