package com.guidewire.in.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyResponse {
	private Long id;
	private String name;
	private BigDecimal premium;
	private BigDecimal coverage;
	private boolean active;
}
