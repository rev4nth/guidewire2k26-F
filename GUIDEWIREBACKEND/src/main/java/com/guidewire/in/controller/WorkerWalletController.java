package com.guidewire.in.controller;

import com.guidewire.in.dto.BuyPolicyRequest;
import com.guidewire.in.dto.WalletAmountRequest;
import com.guidewire.in.dto.WorkerPolicyResponse;
import com.guidewire.in.entity.Policy;
import com.guidewire.in.entity.Role;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.PolicyRepository;
import com.guidewire.in.repository.UserRepository;
import com.guidewire.in.security.JwtFilter;
import com.guidewire.in.service.FinanceService;
import com.guidewire.in.service.WorkerPolicyPricingService;
import com.guidewire.in.web.ApiResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/worker")
public class WorkerWalletController {

	private final UserRepository userRepository;
	private final PolicyRepository policyRepository;
	private final FinanceService financeService;
	private final WorkerPolicyPricingService workerPolicyPricingService;

	@Value("${app.demo.mode:true}")
	private boolean demoMode;

	public WorkerWalletController(UserRepository userRepository,
			PolicyRepository policyRepository,
			FinanceService financeService,
			WorkerPolicyPricingService workerPolicyPricingService) {
		this.userRepository   = userRepository;
		this.policyRepository = policyRepository;
		this.financeService   = financeService;
		this.workerPolicyPricingService = workerPolicyPricingService;
	}

	private Long uid(HttpServletRequest req) {
		Object a = req.getAttribute(JwtFilter.ATTR_USER_ID);
		return a == null ? null : Long.valueOf(a.toString());
	}

	private User requireWorker(HttpServletRequest req) {
		Long id = uid(req);
		if (id == null) return null;
		User u = userRepository.findById(id).orElse(null);
		if (u == null || u.getRole() != Role.WORKER) return null;
		return u;
	}

	private static double round2(double v) {
		return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

	@GetMapping("/wallet")
	public ResponseEntity<?> wallet(HttpServletRequest req) {
		User w = requireWorker(req);
		if (w == null) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
		w = userRepository.findById(w.getId()).orElseThrow();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("walletBalance", w.getWalletBalance() != null ? w.getWalletBalance() : 0.0);
		body.put("activePolicyId", w.getActivePolicyId());
		return ApiResponseBuilder.ok("Wallet loaded", body);
	}

	@Transactional
	@PostMapping("/add-money")
	public ResponseEntity<?> addMoney(HttpServletRequest req, @RequestBody WalletAmountRequest body) {
		User w = requireWorker(req);
		if (w == null) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
		if (body.getAmount() == null || body.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "Positive amount required");
		}
		w = userRepository.findById(w.getId()).orElseThrow();
		double bal = w.getWalletBalance() != null ? w.getWalletBalance() : 0.0;
		w.setWalletBalance(round2(bal + body.getAmount().doubleValue()));
		userRepository.save(w);
		return ApiResponseBuilder.ok("Balance updated", Map.of("walletBalance", w.getWalletBalance()));
	}

	@Transactional
	@PostMapping("/withdraw")
	public ResponseEntity<?> withdraw(HttpServletRequest req, @RequestBody WalletAmountRequest body) {
		User w = requireWorker(req);
		if (w == null) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
		if (body.getAmount() == null || body.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "Positive amount required");
		}
		w = userRepository.findById(w.getId()).orElseThrow();
		double bal = w.getWalletBalance() != null ? w.getWalletBalance() : 0.0;
		if (bal < body.getAmount().doubleValue()) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "Insufficient wallet balance");
		}
		w.setWalletBalance(round2(bal - body.getAmount().doubleValue()));
		userRepository.save(w);
		return ApiResponseBuilder.ok("Balance updated", Map.of("walletBalance", w.getWalletBalance()));
	}

	/**
	 * Catalog for the logged-in worker: premiums include +30% when they have no active plan and a recent HIGH disruption.
	 */
	@GetMapping("/policies")
	@Transactional(readOnly = true)
	public ResponseEntity<?> workerPolicies(HttpServletRequest req) {
		User w = requireWorker(req);
		if (w == null) {
			return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");
		}
		w = userRepository.findById(w.getId()).orElseThrow();
		User worker = w;
		List<WorkerPolicyResponse> list = policyRepository.findAllByActiveTrueOrderByIdAsc().stream()
				.map(p -> {
					BigDecimal base = p.getPremium();
					BigDecimal eff = workerPolicyPricingService.effectivePremium(p, worker);
					boolean applied = eff.compareTo(base) > 0;
					return new WorkerPolicyResponse(
							p.getId(),
							p.getName(),
							eff,
							base,
							p.getCoverage(),
							p.isActive(),
							applied,
							applied ? WorkerPolicyPricingService.SURCHARGE_PERCENT : null);
				})
				.collect(Collectors.toList());
		return ApiResponseBuilder.ok("Policies loaded", list);
	}

	/**
	 * POST /worker/buy-policy/{policyId}
	 * Body: { "source": "WALLET" | "SIMULATE" | "RAZORPAY", "razorpayPaymentId": "..." }
	 * Demo mode: always activates policy and returns success (SIMULATE/RAZORPAY skip wallet; WALLET deducts when possible).
	 */
	@Transactional
	@PostMapping("/buy-policy/{policyId}")
	public ResponseEntity<?> buyPolicy(HttpServletRequest req, @PathVariable Long policyId,
			@RequestBody(required = false) BuyPolicyRequest body) {
		User w = requireWorker(req);
		if (w == null) return ApiResponseBuilder.fail(HttpStatus.FORBIDDEN, "Forbidden");

		Policy policy = policyRepository.findById(policyId).orElse(null);
		if (policy == null || !policy.isActive()) {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "Policy not found or inactive");
		}

		String source = body != null && body.getSource() != null
				? body.getSource().trim().toUpperCase()
				: "SIMULATE";

		w = userRepository.findById(w.getId()).orElseThrow();
		BigDecimal premium = workerPolicyPricingService.effectivePremium(policy, w);

		if (demoMode) {
			if ("WALLET".equals(source)) {
				double bal = w.getWalletBalance() != null ? w.getWalletBalance() : 0.0;
				if (bal >= premium.doubleValue()) {
					w.setWalletBalance(round2(bal - premium.doubleValue()));
				}
			}
			w.setActivePolicyId(policy.getId());
			userRepository.save(w);
			String ref = body != null && body.getRazorpayPaymentId() != null && !body.getRazorpayPaymentId().isBlank()
					? body.getRazorpayPaymentId()
					: "demo-" + System.currentTimeMillis();
			financeService.recordPolicyPurchase(w, premium, source, ref);
			Map<String, Object> res = new LinkedHashMap<>();
			res.put("activePolicyId", policy.getId());
			res.put("walletBalance", w.getWalletBalance());
			return ApiResponseBuilder.ok("Policy activated", res);
		}

		if ("WALLET".equals(source)) {
			double bal = w.getWalletBalance() != null ? w.getWalletBalance() : 0.0;
			if (bal < premium.doubleValue()) {
				return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "Insufficient wallet balance");
			}
			w.setWalletBalance(round2(bal - premium.doubleValue()));
		} else if ("RAZORPAY".equals(source) || "SIMULATE".equals(source)) {
			// Simulated external payment
		} else {
			return ApiResponseBuilder.fail(HttpStatus.BAD_REQUEST, "source must be WALLET, SIMULATE, or RAZORPAY");
		}

		w.setActivePolicyId(policy.getId());
		userRepository.save(w);

		financeService.recordPolicyPurchase(w, premium, source,
				body != null ? body.getRazorpayPaymentId() : null);

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("activePolicyId", policy.getId());
		res.put("walletBalance", w.getWalletBalance());
		return ApiResponseBuilder.ok("Policy purchased", res);
	}
}
