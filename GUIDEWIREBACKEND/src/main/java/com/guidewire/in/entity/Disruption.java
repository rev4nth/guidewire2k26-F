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

import java.time.LocalDateTime;

@Entity
@Table(name = "disruptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Disruption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private DisruptionType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private DisruptionSeverity severity;

	/** City / area label for this event (e.g. Hyderabad). */
	private String location;

	@Enumerated(EnumType.STRING)
	@Column(name = "disruption_source", nullable = false, length = 32)
	private DisruptionSource source;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "worker_id", nullable = false)
	private User worker;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@PrePersist
	void onCreate() {
		if (createdAt == null) createdAt = LocalDateTime.now();
		if (source == null) source = DisruptionSource.MANUAL;
	}

	@PostLoad
	void onLoad() {
		if (source == null) source = DisruptionSource.MANUAL;
	}
}
