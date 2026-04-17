package com.guidewire.in.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

	private String token;
	private String role;
	private Long userId;
	private String name;
	private String email;
	private String profileImageUrl;
	/** Present for workers — wallet balance in INR */
	private Double walletBalance;
	private Long activePolicyId;
}
