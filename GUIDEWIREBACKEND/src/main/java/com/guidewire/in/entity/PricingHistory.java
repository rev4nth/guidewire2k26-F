package com.guidewire.in.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PricingHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String city;

	@Column(nullable = false)
	private Long policyId;

	@Column(nullable = false)
	private String weather;

	private Double windSpeed;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private RiskLevel risk;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal basePremium;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal adjustedPremium;

	@Column(length = 1024)
	private String reason;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) createdAt = LocalDateTime.now();
	}
}
