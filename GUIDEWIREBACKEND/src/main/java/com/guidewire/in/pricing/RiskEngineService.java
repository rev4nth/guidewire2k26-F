package com.guidewire.in.pricing;

import com.guidewire.in.entity.RiskLevel;
import org.springframework.stereotype.Service;

@Service
public class RiskEngineService {

	private static final double HIGH_WIND_THRESHOLD   = 10.0; // m/s  ≈ 36 km/h
	private static final double MEDIUM_WIND_THRESHOLD =  5.0; // m/s

	/**
	 * Derive risk level from the current weather snapshot.
	 *
	 * Rules (in priority order):
	 *  1. Rain present                     → HIGH
	 *  2. wind_speed > HIGH_WIND_THRESHOLD  → HIGH
	 *  3. wind_speed > MEDIUM_WIND_THRESHOLD→ MEDIUM
	 *  4. otherwise                         → LOW
	 */
	public RiskLevel calculateRisk(WeatherData weather) {
		if (weather.hasRain() || "Rain".equalsIgnoreCase(weather.main())) {
			return RiskLevel.HIGH;
		}
		if (weather.windSpeed() > HIGH_WIND_THRESHOLD) {
			return RiskLevel.HIGH;
		}
		if (weather.windSpeed() > MEDIUM_WIND_THRESHOLD) {
			return RiskLevel.MEDIUM;
		}
		return RiskLevel.LOW;
	}
}
