package com.guidewire.in.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	@Column(unique = true, nullable = false)
	private String email;

	@Column(nullable = false)
	private String password;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	private String location;

	/** GPS coordinates — updated by worker via POST /worker/location */
	private Double latitude;

	private Double longitude;

	@Column(length = 1024)
	private String profileImageUrl;

	/** Wallet balance in INR (claim payouts, top-ups, policy purchases from wallet) */
	private Double walletBalance;

	/** Policy the worker purchased / has active (FK to policies.id) */
	private Long activePolicyId;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (walletBalance == null) {
			walletBalance = 0.0;
		}
	}
}
