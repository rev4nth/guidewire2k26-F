package com.guidewire.in.entity;

/**
 * APPROVED — paid out when wallet credit applies (severity rules + confidence, or manual govt approval).
 * PENDING_PROOF — needs proof upload and/or admin review (typical for LOW, or MEDIUM when score &lt; 70).
 * REJECTED — manually rejected; no wallet credit.
 */
public enum ClaimStatus {
	APPROVED,
	PENDING_PROOF,
	REJECTED
}
