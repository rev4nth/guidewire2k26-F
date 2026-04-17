package com.guidewire.in.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

	/** Cloudinary or other URL */
	private String proofImage;

	@Column(nullable = false)
	private int confidenceScore;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ClaimStatus status = ClaimStatus.REVIEW;

	/** True after wallet credit for this claim (idempotent). */
	@Column(nullable = false)
	private boolean walletPaid;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) createdAt = LocalDateTime.now();
		if (status == null) status = ClaimStatus.REVIEW;
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
