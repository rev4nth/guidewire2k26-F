package com.guidewire.in.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
	private long totalUsers;
	private long totalOrders;
	private long totalDisruptions;
	private long totalClaims;
}
