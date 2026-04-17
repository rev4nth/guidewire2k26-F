package com.guidewire.in.scheduler;

import com.guidewire.in.entity.DisruptionSeverity;
import com.guidewire.in.entity.DisruptionSource;
import com.guidewire.in.entity.DisruptionType;
import com.guidewire.in.entity.User;
import com.guidewire.in.pricing.WeatherData;
import com.guidewire.in.pricing.WeatherService;
import com.guidewire.in.repository.DisruptionRepository;
import com.guidewire.in.service.DisruptionService;
import com.guidewire.in.service.LocationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.disruption.auto.enabled", havingValue = "true", matchIfMissing = false)
public class AutoDisruptionScheduler {

	private static final Logger log = LoggerFactory.getLogger(AutoDisruptionScheduler.class);

	private static final int DEDUP_MINUTES = 10;

	private final WeatherService weatherService;
	private final LocationService locationService;
	private final DisruptionService disruptionService;
	private final DisruptionRepository disruptionRepository;

	@Value("${app.disruption.monitor-cities:Hyderabad}")
	private String monitorCitiesRaw;

	public AutoDisruptionScheduler(WeatherService weatherService,
			LocationService locationService,
			DisruptionService disruptionService,
			DisruptionRepository disruptionRepository) {
		this.weatherService = weatherService;
		this.locationService = locationService;
		this.disruptionService = disruptionService;
		this.disruptionRepository = disruptionRepository;
	}

	@Scheduled(fixedRate = 300_000)
	public void pollWeatherAndTriggerRain() {
		List<String> cities = Arrays.stream(monitorCitiesRaw.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList();
		for (String city : cities) {
			try {
				WeatherData w = weatherService.fetchWeather(city);
				if (!isRain(w)) {
					continue;
				}
				LocalDateTime since = LocalDateTime.now().minusMinutes(DEDUP_MINUTES);
				if (disruptionRepository.existsBySourceAndTypeAndLocationIgnoreCaseAndCreatedAtAfter(
						DisruptionSource.AUTO, DisruptionType.RAIN, city, since)) {
					continue;
				}
				List<User> workers = locationService.findWorkersByCity(city);
				if (workers.isEmpty()) {
					log.debug("Auto rain for {}: no workers in city, skipping claims", city);
					continue;
				}
				disruptionService.triggerForWorkers(workers, DisruptionType.RAIN, DisruptionSeverity.HIGH, city,
						DisruptionSource.AUTO);
				log.info("Auto disruption triggered: RAIN HIGH {}", city);
			} catch (WeatherService.WeatherServiceException e) {
				log.warn("Weather check failed for {}: {}", city, e.getMessage());
			} catch (Exception e) {
				log.error("Auto disruption scheduler error for {}: {}", city, e.getMessage(), e);
			}
		}
	}

	private static boolean isRain(WeatherData w) {
		if (w == null) return false;
		if (w.hasRain()) return true;
		String main = w.main() == null ? "" : w.main();
		return main.equalsIgnoreCase("Rain")
				|| main.equalsIgnoreCase("Drizzle")
				|| main.equalsIgnoreCase("Thunderstorm");
	}
}
