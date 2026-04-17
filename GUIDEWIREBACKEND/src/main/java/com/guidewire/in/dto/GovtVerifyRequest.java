package com.guidewire.in.dto;

import lombok.Data;

@Data
public class GovtVerifyRequest {
	/** APPROVED or REJECTED */
	private String status;
}
