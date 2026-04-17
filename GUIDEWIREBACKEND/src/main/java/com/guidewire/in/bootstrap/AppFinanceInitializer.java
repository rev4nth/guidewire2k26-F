package com.guidewire.in.bootstrap;

import com.guidewire.in.entity.AppFinance;
import com.guidewire.in.repository.AppFinanceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AppFinanceInitializer implements CommandLineRunner {

	private final AppFinanceRepository appFinanceRepository;

	public AppFinanceInitializer(AppFinanceRepository appFinanceRepository) {
		this.appFinanceRepository = appFinanceRepository;
	}

	@Override
	public void run(String... args) {
		if (appFinanceRepository.findById(AppFinance.SINGLETON_ID).isEmpty()) {
			appFinanceRepository.save(new AppFinance());
		}
	}
}
