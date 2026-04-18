package com.guidewire.in.dto;

import lombok.Data;

@Data
public class AdminClaimPayoutRequest {
	/** One of {@link AdminPayoutTier} names: FULL, HALF, NONE */
	private String payout;
}
