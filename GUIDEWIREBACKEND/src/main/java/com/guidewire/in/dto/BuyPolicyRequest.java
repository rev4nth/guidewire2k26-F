package com.guidewire.in.dto;

import lombok.Data;

@Data
public class BuyPolicyRequest {
	/** WALLET — deduct from wallet. RAZORPAY — external payment (dummy / test success from client). */
	private String source;
	private String razorpayPaymentId;
}
