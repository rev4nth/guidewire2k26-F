package com.guidewire.in.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidewire.in.entity.Policy;
import com.guidewire.in.entity.PricingHistory;
import com.guidewire.in.entity.RiskLevel;
import com.guidewire.in.repository.PricingHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class PricingService {

	private static final Logger log = LoggerFactory.getLogger(PricingService.class);

	private static final BigDecimal HIGH_SURCHARGE   = BigDecimal.valueOf(20);
	private static final BigDecimal MEDIUM_SURCHARGE = BigDecimal.valueOf(10);
	private static final BigDecimal LOW_DISCOUNT     = BigDecimal.valueOf(5);

	private final WeatherService weatherService;
	private final RiskEngineService riskEngine;
	private final PricingHistoryRepository historyRepository;
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Value("${app.openai.api-key:}")
	private String openAiKey;

	public PricingService(WeatherService weatherService,
			RiskEngineService riskEngine,
			PricingHistoryRepository historyRepository,
			RestTemplate restTemplate,
			ObjectMapper objectMapper) {
		this.weatherService    = weatherService;
		this.riskEngine        = riskEngine;
		this.historyRepository = historyRepository;
		this.restTemplate      = restTemplate;
		this.objectMapper      = objectMapper;
	}

	public PricingResult price(String city, Policy policy) {
		// 1. Fetch weather
		WeatherData weather = weatherService.fetchWeather(city);

		// 2. Calculate risk
		RiskLevel risk = riskEngine.calculateRisk(weather);

		// 3. Adjust premium
		BigDecimal base     = policy.getPremium();
		BigDecimal adjusted = applyAdjustment(base, risk);

		// 4. Generate explanation (AI if key available, else rule-based fallback)
		String reason = buildReason(weather, risk, openAiKey);

		// 5. Persist history
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

	/* ── premium math ── */

	private BigDecimal applyAdjustment(BigDecimal base, RiskLevel risk) {
		return switch (risk) {
			case HIGH   -> base.add(HIGH_SURCHARGE);
			case MEDIUM -> base.add(MEDIUM_SURCHARGE);
			case LOW    -> base.subtract(LOW_DISCOUNT).max(BigDecimal.ONE);
		};
	}

	/* ── explanation ── */

	private String buildReason(WeatherData weather, RiskLevel risk, String apiKey) {
		if (apiKey != null && !apiKey.isBlank()) {
			try {
				return callOpenAi(weather, risk, apiKey);
			} catch (Exception ex) {
				log.warn("OpenAI call failed, falling back to rule-based reason: {}", ex.getMessage());
			}
		}
		return ruleBasedReason(weather, risk);
	}

	private String ruleBasedReason(WeatherData weather, RiskLevel risk) {
		return switch (risk) {
			case HIGH -> weather.hasRain()
					? String.format("Heavy %s detected (wind: %.1f m/s). Delivery risk is HIGH — premium increased by ₹%s.",
					        weather.description(), weather.windSpeed(), HIGH_SURCHARGE)
					: String.format("High wind speed of %.1f m/s detected. Delivery risk is HIGH — premium increased by ₹%s.",
					        weather.windSpeed(), HIGH_SURCHARGE);
			case MEDIUM -> String.format("Moderate wind speed of %.1f m/s. Delivery risk is MEDIUM — premium increased by ₹%s.",
					weather.windSpeed(), MEDIUM_SURCHARGE);
			case LOW -> String.format("Clear weather conditions (wind: %.1f m/s). Delivery risk is LOW — premium reduced by ₹%s.",
					weather.windSpeed(), LOW_DISCOUNT);
		};
	}

	private String callOpenAi(WeatherData weather, RiskLevel risk, String apiKey) throws Exception {
		String prompt = String.format(
				"Explain briefly (1-2 sentences) why an insurance premium for a delivery worker was %s "
				+ "given current weather: %s (wind speed %.1f m/s, rain present: %s). Risk level: %s.",
				risk == RiskLevel.HIGH ? "increased" : risk == RiskLevel.MEDIUM ? "slightly increased" : "reduced",
				weather.main(), weather.windSpeed(), weather.hasRain(), risk);

		Map<String, Object> body = Map.of(
				"model", "gpt-3.5-turbo",
				"messages", List.of(Map.of("role", "user", "content", prompt)),
				"max_tokens", 120,
				"temperature", 0.5
		);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(apiKey);

		String response = restTemplate.postForObject(
				"https://api.openai.com/v1/chat/completions",
				new HttpEntity<>(objectMapper.writeValueAsString(body), headers),
				String.class
		);

		JsonNode root = objectMapper.readTree(response);
		return root.path("choices").get(0).path("message").path("content").asText("").trim();
	}
}
