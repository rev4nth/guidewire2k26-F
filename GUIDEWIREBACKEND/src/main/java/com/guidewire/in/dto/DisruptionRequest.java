package com.guidewire.in.dto;

import lombok.Data;

@Data
public class DisruptionRequest {
	private String type;
	private String severity;
	/** City / area for manual or broadcast triggers */
	private String location;
}
