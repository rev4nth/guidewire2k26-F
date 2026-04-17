package com.guidewire.in.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PolicyRequest {
	private String name;
	private BigDecimal premium;
	private BigDecimal coverage;
	private Boolean active;
}
