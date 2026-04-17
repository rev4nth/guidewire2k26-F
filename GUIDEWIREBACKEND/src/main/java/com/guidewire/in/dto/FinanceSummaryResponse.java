package com.guidewire.in.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinanceSummaryResponse {
	private BigDecimal totalRevenue;
	private BigDecimal totalClaimsPaid;
	private BigDecimal profit;
}
