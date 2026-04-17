package com.guidewire.in.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class WeatherService {

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;

	@Value("${app.openweather.api-key}")
	private String apiKey;

	@Value("${app.openweather.base-url}")
	private String baseUrl;

	public WeatherService(RestTemplate restTemplate, ObjectMapper objectMapper) {
		this.restTemplate = restTemplate;
		this.objectMapper = objectMapper;
	}

	/**
	 * Fetches current weather for the given city from OpenWeather.
	 *
	 * @throws WeatherServiceException if the city is not found or the API call fails.
	 */
	public WeatherData fetchWeather(String city) {
		String url = baseUrl + "?q={city}&appid={key}&units=metric";

		String raw;
		try {
			raw = restTemplate.getForObject(url, String.class, city, apiKey);
		} catch (HttpClientErrorException.NotFound e) {
			throw new WeatherServiceException("City not found: " + city);
		} catch (Exception e) {
			throw new WeatherServiceException("Failed to call OpenWeather API: " + e.getMessage());
		}

		try {
			JsonNode root = objectMapper.readTree(raw);

			// weather[0].main  → "Rain", "Clear", etc.
			JsonNode weatherArr = root.path("weather");
			String main = weatherArr.isArray() && weatherArr.size() > 0
					? weatherArr.get(0).path("main").asText("Clear")
					: "Clear";
			String description = weatherArr.isArray() && weatherArr.size() > 0
					? weatherArr.get(0).path("description").asText("")
					: "";

			// wind.speed in m/s
			double windSpeed = root.path("wind").path("speed").asDouble(0.0);

			// presence of "rain" key
			boolean hasRain = root.has("rain") || "Rain".equalsIgnoreCase(main);

			return new WeatherData(main, description, windSpeed, hasRain);
		} catch (Exception e) {
			throw new WeatherServiceException("Failed to parse weather response: " + e.getMessage());
		}
	}

	public static class WeatherServiceException extends RuntimeException {
		public WeatherServiceException(String msg) { super(msg); }
	}
}
