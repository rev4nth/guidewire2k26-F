package com.guidewire.in.bootstrap;

import com.guidewire.in.entity.Policy;
import com.guidewire.in.entity.Role;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.PolicyRepository;
import com.guidewire.in.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DefaultAdminInitializer implements CommandLineRunner {

	private static final String ADMIN_EMAIL = "admin@safeflex.com";
	private static final String ADMIN_PASSWORD = "admin123";
	private static final String GOVT_EMAIL = "govt@safeflex.com";
	private static final String GOVT_PASSWORD = "govt123";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final PolicyRepository policyRepository;

	public DefaultAdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
			PolicyRepository policyRepository) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.policyRepository = policyRepository;
	}

	@Override
	public void run(String... args) {
		if (userRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
			User admin = new User();
			admin.setName("Admin");
			admin.setEmail(ADMIN_EMAIL);
			admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
			admin.setRole(Role.ADMIN);
			admin.setLocation("HQ");
			userRepository.save(admin);
		}
		if (userRepository.findByEmail(GOVT_EMAIL).isEmpty()) {
			User govt = new User();
			govt.setName("Government");
			govt.setEmail(GOVT_EMAIL);
			govt.setPassword(passwordEncoder.encode(GOVT_PASSWORD));
			govt.setRole(Role.GOVT);
			govt.setLocation("India");
			userRepository.save(govt);
		}
		seedPolicy("BASIC",   BigDecimal.valueOf(99),  BigDecimal.valueOf(500));
		seedPolicy("PRO",     BigDecimal.valueOf(199), BigDecimal.valueOf(1500));
		seedPolicy("PREMIUM", BigDecimal.valueOf(349), BigDecimal.valueOf(3000));
	}

	private void seedPolicy(String name, BigDecimal premium, BigDecimal coverage) {
		if (policyRepository.findByNameIgnoreCase(name).isEmpty()) {
			Policy p = new Policy();
			p.setName(name);
			p.setPremium(premium);
			p.setCoverage(coverage);
			p.setActive(true);
			policyRepository.save(p);
		}
	}
}
