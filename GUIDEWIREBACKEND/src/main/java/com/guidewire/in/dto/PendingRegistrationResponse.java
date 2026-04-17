package com.guidewire.in.dto;

import com.guidewire.in.entity.Role;

import java.time.LocalDateTime;

public record PendingRegistrationResponse(
		Long id,
		String name,
		String email,
		Role role,
		String location,
		String profileImageUrl,
		LocalDateTime createdAt
) {
}
