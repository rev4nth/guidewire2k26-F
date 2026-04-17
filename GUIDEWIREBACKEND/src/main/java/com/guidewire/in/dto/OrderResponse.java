package com.guidewire.in.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
	private Long id;
	private Long workerId;
	private String workerName;
	private String status;
	private LocalDateTime createdAt;
}
