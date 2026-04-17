package com.guidewire.in.pricing;

import com.guidewire.in.entity.Policy;
import com.guidewire.in.entity.PricingHistory;
import com.guidewire.in.entity.RiskLevel;
import com.guidewire.in.repository.PricingHistoryRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PricingService {

	private static final BigDecimal HIGH_SURCHARGE = BigDecimal.valueOf(20);
	private static final BigDecimal MEDIUM_SURCHARGE = BigDecimal.valueOf(10);
	private static final BigDecimal LOW_DISCOUNT = BigDecimal.valueOf(5);

	private final WeatherService weatherService;
	private final RiskEngineService riskEngine;
	private final PricingHistoryRepository historyRepository;

	public PricingService(WeatherService weatherService,
			RiskEngineService riskEngine,
			PricingHistoryRepository historyRepository) {
		this.weatherService = weatherService;
		this.riskEngine = riskEngine;
		this.historyRepository = historyRepository;
	}

	public PricingResult price(String city, Policy policy) {
		WeatherData weather = weatherService.fetchWeather(city);
		RiskLevel risk = riskEngine.calculateRisk(weather);
		BigDecimal base = policy.getPremium();
		BigDecimal adjusted = applyAdjustment(base, risk);
		String reason = ruleBasedReason(weather, risk);

		PricingHistory record = new PricingHistory();
		record.setCity(city);
		record.setPolicyId(policy.getId());
		record.setWeather(weather.main());
		record.setWindSpeed(weather.windSpeed());
		record.setRisk(risk);
		record.setBasePremium(base);
		record.setAdjustedPremium(adjusted);
		record.setReason(reason);
		historyRepository.save(record);

		return new PricingResult(city, weather.main(), weather.description(),
				weather.windSpeed(), risk, base, adjusted, reason);
	}

	private BigDecimal applyAdjustment(BigDecimal base, RiskLevel risk) {
		return switch (risk) {
			case HIGH -> base.add(HIGH_SURCHARGE);
			case MEDIUM -> base.add(MEDIUM_SURCHARGE);
			case LOW -> base.subtract(LOW_DISCOUNT).max(BigDecimal.ONE);
		};
	}

	private String ruleBasedReason(WeatherData weather, RiskLevel risk) {
		return switch (risk) {
			case HIGH -> String.format(
					"Demo risk model: %s conditions (wind %.1f m/s). Premium adjusted +₹%s.",
					weather.main(), weather.windSpeed(), HIGH_SURCHARGE);
			case MEDIUM -> String.format(
					"Demo risk model: moderate factors (wind %.1f m/s). Premium adjusted +₹%s.",
					weather.windSpeed(), MEDIUM_SURCHARGE);
			case LOW -> String.format(
					"Demo risk model: favorable conditions (wind %.1f m/s). Premium adjusted −₹%s.",
					weather.windSpeed(), LOW_DISCOUNT);
		};
	}
}
