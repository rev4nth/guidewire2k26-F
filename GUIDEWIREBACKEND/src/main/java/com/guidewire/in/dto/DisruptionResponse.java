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
	private String location;
	private String source;
	private Long workerId;
	private String workerName;
	private LocalDateTime createdAt;

	public DisruptionResponse(Long id, String type, String severity, Long workerId, String workerName,
			LocalDateTime createdAt) {
		this(id, type, severity, null, null, workerId, workerName, createdAt);
	}
}
