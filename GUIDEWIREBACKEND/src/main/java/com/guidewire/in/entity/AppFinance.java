package com.guidewire.in.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Singleton row (id = 1) tracking platform revenue vs claim payouts.
 */
@Entity
@Table(name = "app_finance")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppFinance {

	public static final Long SINGLETON_ID = 1L;

	@Id
	private Long id = SINGLETON_ID;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal totalRevenue = BigDecimal.ZERO;

	@Column(nullable = false, precision = 14, scale = 2)
	private BigDecimal totalClaimsPaid = BigDecimal.ZERO;
}
