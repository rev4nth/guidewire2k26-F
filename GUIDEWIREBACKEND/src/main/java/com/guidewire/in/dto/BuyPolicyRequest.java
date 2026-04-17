package com.guidewire.in.dto;

import lombok.Data;

@Data
public class BuyPolicyRequest {
	/** WALLET | SIMULATE | RAZORPAY — demo mode treats all as simulated success (no real gateway). */
	private String source;
	private String razorpayPaymentId;
}
