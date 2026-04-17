package com.guidewire.in.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisruptionResponse {
	private Long id;
	private String type;
	private String severity;
	private Long workerId;
	private String workerName;
	private LocalDateTime createdAt;
}
