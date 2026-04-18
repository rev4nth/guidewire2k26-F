package com.guidewire.in.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkerPolicyResponse {
	private Long id;
	private String name;
	/** Amount to charge (may include +30% disruption surcharge). */
	private BigDecimal premium;
	private BigDecimal basePremium;
	private BigDecimal coverage;
	private boolean active;
	private boolean disruptionSurchargeApplied;
	/** e.g. 30 when surcharge applies */
	private Integer surchargePercent;
}
