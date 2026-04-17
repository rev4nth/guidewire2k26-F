package com.guidewire.in.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Older DBs may have {@code disruption_source} as ENUM('AUTO','MANUAL') or a short VARCHAR,
 * which rejects {@code GOVT}. Widen to VARCHAR so all enum names fit.
 */
@Component
@Order(1)
public class DisruptionMysqlColumnPatch implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DisruptionMysqlColumnPatch.class);

	private final JdbcTemplate jdbcTemplate;

	public DisruptionMysqlColumnPatch(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public void run(ApplicationArguments args) {
		try {
			jdbcTemplate.execute(
					"ALTER TABLE disruptions MODIFY COLUMN disruption_source VARCHAR(32) NOT NULL DEFAULT 'MANUAL'");
		} catch (Exception e) {
			log.warn("disruption_source column patch skipped: {}", e.getMessage());
		}
		try {
			jdbcTemplate.execute("ALTER TABLE disruptions MODIFY COLUMN `type` VARCHAR(32) NOT NULL");
		} catch (Exception e) {
			log.warn("disruptions.type column patch skipped: {}", e.getMessage());
		}
		try {
			jdbcTemplate.execute("ALTER TABLE disruptions MODIFY COLUMN severity VARCHAR(32) NOT NULL");
		} catch (Exception e) {
			log.warn("disruptions.severity column patch skipped: {}", e.getMessage());
		}
	}
}
