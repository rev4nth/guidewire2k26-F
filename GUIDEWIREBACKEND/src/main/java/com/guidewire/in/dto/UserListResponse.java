package com.guidewire.in.dto;

import com.guidewire.in.entity.Role;

import java.time.LocalDateTime;

public record UserListResponse(
		Long id,
		String name,
		String email,
		Role role,
		String location,
		String profileImageUrl,
		LocalDateTime createdAt,
		Double latitude,
		Double longitude
) {
}
