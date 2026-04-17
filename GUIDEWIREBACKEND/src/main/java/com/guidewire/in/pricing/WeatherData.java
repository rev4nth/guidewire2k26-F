package com.guidewire.in.pricing;

/**
 * Lightweight value object holding the extracted weather info from OpenWeather.
 */
public record WeatherData(
		String main,        // e.g. "Rain", "Clear", "Clouds"
		String description, // e.g. "moderate rain"
		double windSpeed,   // m/s
		boolean hasRain     // true if the "rain" key is present in the response
) {}
