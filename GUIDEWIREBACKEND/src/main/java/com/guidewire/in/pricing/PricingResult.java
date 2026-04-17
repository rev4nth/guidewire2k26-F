package com.guidewire.in.pricing;

import com.guidewire.in.entity.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PricingResult {
	private String city;
	private String weather;
	private String weatherDescription;
	private double windSpeed;
	private RiskLevel risk;
	private BigDecimal basePremium;
	private BigDecimal adjustedPremium;
	private String reason;
}
