package com.guidewire.in.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletAmountRequest {
	private BigDecimal amount;
}
