package com.guidewire.in.service;

import com.guidewire.in.entity.AppFinance;
import com.guidewire.in.entity.Payment;
import com.guidewire.in.entity.PaymentStatus;
import com.guidewire.in.entity.PaymentType;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.AppFinanceRepository;
import com.guidewire.in.repository.PaymentRepository;
import com.guidewire.in.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FinanceService {

	private final AppFinanceRepository appFinanceRepository;
	private final UserRepository userRepository;
	private final PaymentRepository paymentRepository;

	public FinanceService(AppFinanceRepository appFinanceRepository,
			UserRepository userRepository,
			PaymentRepository paymentRepository) {
		this.appFinanceRepository = appFinanceRepository;
		this.userRepository      = userRepository;
		this.paymentRepository   = paymentRepository;
	}

	@Transactional
	public AppFinance getOrCreate() {
		return appFinanceRepository.findById(AppFinance.SINGLETON_ID)
				.orElseGet(() -> appFinanceRepository.save(new AppFinance()));
	}

	@Transactional
	public void recordPolicyPurchase(User user, BigDecimal premium, String source, String externalRef) {
		AppFinance fin = getOrCreate();
		fin.setTotalRevenue(fin.getTotalRevenue().add(premium));
		appFinanceRepository.save(fin);

		Payment p = new Payment();
		p.setUser(user);
		p.setAmount(premium);
		p.setType(PaymentType.POLICY_PURCHASE);
		p.setStatus(PaymentStatus.COMPLETED);
		p.setExternalRef(externalRef != null ? externalRef : source);
		paymentRepository.save(p);
	}

	/**
	 * When a claim is approved / created, credit the worker wallet and track platform liability.
	 */
	@Transactional
	public void creditWalletForClaim(User worker, BigDecimal amount, String reasonRef) {
		User w = userRepository.findById(worker.getId()).orElseThrow();
		double bal = w.getWalletBalance() != null ? w.getWalletBalance() : 0.0;
		w.setWalletBalance(round2(bal + amount.doubleValue()));
		userRepository.save(w);

		AppFinance fin = getOrCreate();
		fin.setTotalClaimsPaid(fin.getTotalClaimsPaid().add(amount));
		appFinanceRepository.save(fin);

		Payment p = new Payment();
		p.setUser(w);
		p.setAmount(amount);
		p.setType(PaymentType.CLAIM);
		p.setStatus(PaymentStatus.COMPLETED);
		p.setExternalRef(reasonRef != null && reasonRef.length() > 256 ? reasonRef.substring(0, 256) : reasonRef);
		paymentRepository.save(p);
	}

	private static double round2(double v) {
		return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}
}
