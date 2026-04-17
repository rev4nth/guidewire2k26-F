package com.guidewire.in.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
}
