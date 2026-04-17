package com.guidewire.in.dto;

import lombok.Data;

@Data
public class LocationRequest {
	private Double latitude;
	private Double longitude;
	/** Optional: reverse-geocoded city name sent from the frontend */
	private String city;
}
