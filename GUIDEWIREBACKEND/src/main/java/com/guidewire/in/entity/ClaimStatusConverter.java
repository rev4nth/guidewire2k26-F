package com.guidewire.in.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps legacy DB value {@code REVIEW} to {@link ClaimStatus#PENDING_PROOF}.
 */
@Converter(autoApply = false)
public class ClaimStatusConverter implements AttributeConverter<ClaimStatus, String> {

	@Override
	public String convertToDatabaseColumn(ClaimStatus attribute) {
		return attribute == null ? ClaimStatus.PENDING_PROOF.name() : attribute.name();
	}

	@Override
	public ClaimStatus convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) {
			return ClaimStatus.PENDING_PROOF;
		}
		if ("REVIEW".equalsIgnoreCase(dbData)) {
			return ClaimStatus.PENDING_PROOF;
		}
		return ClaimStatus.valueOf(dbData.trim().toUpperCase());
	}
}
