package com.guidewire.in.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GovtStatsResponse {
	private long totalDisruptions;
	private long totalClaims;
	private long totalWorkersAffected;
}
