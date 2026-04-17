package com.guidewire.in.dto;

import com.guidewire.in.entity.Role;

public record MeResponse(
		Long id,
		String name,
		String email,
		Role role,
		String location,
		String profileImageUrl,
		Double walletBalance,
		Long activePolicyId
) {
}
