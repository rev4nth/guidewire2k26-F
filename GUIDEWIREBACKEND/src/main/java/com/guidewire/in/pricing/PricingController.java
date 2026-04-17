package com.guidewire.in.pricing;

import com.guidewire.in.entity.PricingHistory;
import com.guidewire.in.entity.Policy;
import com.guidewire.in.repository.PolicyRepository;
import com.guidewire.in.repository.PricingHistoryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pricing")
public class PricingController {

	private final PolicyRepository policyRepository;
	private final PricingService pricingService;
	private final PricingHistoryRepository historyRepository;

	public PricingController(PolicyRepository policyRepository,
			PricingService pricingService,
			PricingHistoryRepository historyRepository) {
		this.policyRepository  = policyRepository;
		this.pricingService    = pricingService;
		this.historyRepository = historyRepository;
	}

	/**
	 * Dynamic pricing for a given city + policy.
	 *
	 * GET /pricing/{city}/{policyId}
	 *
	 * Response:
	 * {
	 *   "city": "Hyderabad",
	 *   "weather": "Rain",
	 *   "weatherDescription": "moderate rain",
	 *   "windSpeed": 4.2,
	 *   "risk": "HIGH",
	 *   "basePremium": 99,
	 *   "adjustedPremium": 119,
	 *   "reason": "..."
	 * }
	 */
	@GetMapping("/{city}/{policyId}")
	public ResponseEntity<?> getPrice(@PathVariable String city, @PathVariable Long policyId) {
		Policy policy = policyRepository.findById(policyId).orElse(null);
		if (policy == null) {
			return ResponseEntity.badRequest().body(Map.of("error", "Policy not found: " + policyId));
		}

		PricingResult result;
		try {
			result = pricingService.price(city, policy);
		} catch (WeatherService.WeatherServiceException ex) {
			return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
		} catch (Exception ex) {
			return ResponseEntity.internalServerError().body(Map.of("error", "Pricing failed: " + ex.getMessage()));
		}

		Map<String, Object> response = new LinkedHashMap<>();
		response.put("city",               result.getCity());
		response.put("weather",            result.getWeather());
		response.put("weatherDescription", result.getWeatherDescription());
		response.put("windSpeed",          result.getWindSpeed());
		response.put("risk",               result.getRisk().name());
		response.put("basePremium",        result.getBasePremium());
		response.put("adjustedPremium",    result.getAdjustedPremium());
		response.put("reason",             result.getReason());
		return ResponseEntity.ok(response);
	}

	/**
	 * GET /pricing/history — latest 20 pricing calculations across all cities.
	 */
	@GetMapping("/history")
	public ResponseEntity<List<PricingHistory>> history() {
		return ResponseEntity.ok(historyRepository.findTop20ByOrderByCreatedAtDesc());
	}

	/**
	 * GET /pricing/history/{city} — history for a specific city.
	 */
	@GetMapping("/history/{city}")
	public ResponseEntity<List<PricingHistory>> historyByCity(@PathVariable String city) {
		return ResponseEntity.ok(historyRepository.findByCityIgnoreCaseOrderByCreatedAtDesc(city));
	}
}
