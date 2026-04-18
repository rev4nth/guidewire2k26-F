package com.guidewire.in.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "claims")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Claim {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "worker_id", nullable = false)
	private User worker;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "disruption_id")
	private Disruption disruption;

	/** Snapshot: incident had an active delivery order (disruption path). */
	@Column(nullable = false)
	private boolean hadActiveOrder = true;

	@Column(nullable = false)
	private BigDecimal amount;

	@Column(nullable = false)
	private String reason;

	/** Proof image URL (e.g. Cloudinary). DB column kept as proof_image for compatibility. */
	@Column(name = "proof_image")
	private String proofImageUrl;

	@Column(length = 1024)
	private String proofDescription;

	@Column(nullable = false)
	private int confidenceScore;

	@Convert(converter = ClaimStatusConverter.class)
	@Column(nullable = false, length = 32)
	private ClaimStatus status = ClaimStatus.PENDING_PROOF;

	/** True after wallet credit for this claim (idempotent). */
	@Column(nullable = false)
	private boolean walletPaid;

	/**
	 * Rupees actually credited (equals {@link #amount} for full payout; half of that for partial).
	 * Null for legacy rows before this column existed — treat as {@code amount} when {@link #walletPaid}.
	 */
	@Column(precision = 14, scale = 2)
	private BigDecimal payoutCredited;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) createdAt = LocalDateTime.now();
		if (status == null) status = ClaimStatus.PENDING_PROOF;
	}

	@PostLoad
	void legacyRows() {
		if (status == null) {
			status = ClaimStatus.APPROVED;
			walletPaid = true;
			hadActiveOrder = true;
			if (confidenceScore == 0) {
				confidenceScore = 100;
			}
		}
	}
}
