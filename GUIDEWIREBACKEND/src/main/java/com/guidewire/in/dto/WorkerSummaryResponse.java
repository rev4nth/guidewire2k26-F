package com.guidewire.in.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerSummaryResponse {
	private UserListResponse worker;
	private List<OrderResponse> orders;
	private List<ClaimResponse> claims;
	private List<DisruptionResponse> disruptions;
}
