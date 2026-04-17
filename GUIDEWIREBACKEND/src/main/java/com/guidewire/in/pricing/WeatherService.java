package com.guidewire.in.pricing;

import org.springframework.stereotype.Service;

/**
 * Demo-safe: no external weather APIs. Returns stable mock conditions for pricing and tests.
 */
@Service
public class WeatherService {

	/**
	 * Mock weather for the given city (no HTTP).
	 */
	public WeatherData fetchWeather(String city) {
		return new WeatherData(
				"Clear",
				"Demo weather (no external API)",
				4.5,
				false);
	}

	public static class WeatherServiceException extends RuntimeException {
		public WeatherServiceException(String msg) {
			super(msg);
		}
	}
}
