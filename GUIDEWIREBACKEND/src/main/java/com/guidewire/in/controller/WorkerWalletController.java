package com.guidewire.in.controller;

import com.guidewire.in.dto.BuyPolicyRequest;
import com.guidewire.in.dto.WalletAmountRequest;
import com.guidewire.in.entity.Policy;
import com.guidewire.in.entity.Role;
import com.guidewire.in.entity.User;
import com.guidewire.in.repository.PolicyRepository;
import com.guidewire.in.repository.UserRepository;
import com.guidewire.in.security.JwtFilter;
import com.guidewire.in.service.FinanceService;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Map;

@RestController
@RequestMapping("/worker")
public class WorkerWalletController {

	private final UserRepository userRepository;
	private final PolicyRepository policyRepository;
	private final FinanceService financeService;

	public WorkerWalletController(UserRepository userRepository,
			PolicyRepository policyRepository,
			FinanceService financeService) {
		this.userRepository   = userRepository;
		this.policyRepository = policyRepository;
		this.financeService   = financeService;
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
		if (w == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		w = userRepository.findById(w.getId()).orElseThrow();
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("walletBalance", w.getWalletBalance() != null ? w.getWalletBalance() : 0.0);
		body.put("activePolicyId", w.getActivePolicyId());
		return ResponseEntity.ok(body);
	}

	@Transactional
	@PostMapping("/add-money")
	public ResponseEntity<?> addMoney(HttpServletRequest req, @RequestBody WalletAmountRequest body) {
		User w = requireWorker(req);
		if (w == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		if (body.getAmount() == null || body.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			return ResponseEntity.badRequest().body(Map.of("error", "Positive amount required"));
		}
		w = userRepository.findById(w.getId()).orElseThrow();
		double bal = w.getWalletBalance() != null ? w.getWalletBalance() : 0.0;
		w.setWalletBalance(round2(bal + body.getAmount().doubleValue()));
		userRepository.save(w);
		return ResponseEntity.ok(Map.of("walletBalance", w.getWalletBalance()));
	}

	@Transactional
	@PostMapping("/withdraw")
	public ResponseEntity<?> withdraw(HttpServletRequest req, @RequestBody WalletAmountRequest body) {
		User w = requireWorker(req);
		if (w == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		if (body.getAmount() == null || body.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			return ResponseEntity.badRequest().body(Map.of("error", "Positive amount required"));
		}
		w = userRepository.findById(w.getId()).orElseThrow();
		double bal = w.getWalletBalance() != null ? w.getWalletBalance() : 0.0;
		if (bal < body.getAmount().doubleValue()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Insufficient wallet balance"));
		}
		w.setWalletBalance(round2(bal - body.getAmount().doubleValue()));
		userRepository.save(w);
		return ResponseEntity.ok(Map.of("walletBalance", w.getWalletBalance()));
	}

	/**
	 * POST /worker/buy-policy/{policyId}
	 * Body: { "source": "WALLET" | "RAZORPAY", "razorpayPaymentId": "..." }
	 */
	@Transactional
	@PostMapping("/buy-policy/{policyId}")
	public ResponseEntity<?> buyPolicy(HttpServletRequest req, @PathVariable Long policyId,
			@RequestBody(required = false) BuyPolicyRequest body) {
		User w = requireWorker(req);
		if (w == null) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();

		Policy policy = policyRepository.findById(policyId).orElse(null);
		if (policy == null || !policy.isActive()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Policy not found or inactive"));
		}

		String source = body != null && body.getSource() != null
				? body.getSource().trim().toUpperCase()
				: "RAZORPAY";
		BigDecimal premium = policy.getPremium();

		w = userRepository.findById(w.getId()).orElseThrow();

		if ("WALLET".equals(source)) {
			double bal = w.getWalletBalance() != null ? w.getWalletBalance() : 0.0;
			if (bal < premium.doubleValue()) {
				return ResponseEntity.badRequest().body(Map.of("error", "Insufficient wallet balance"));
			}
			w.setWalletBalance(round2(bal - premium.doubleValue()));
		} else if ("RAZORPAY".equals(source)) {
			// Dummy: trust client after Razorpay checkout success
		} else {
			return ResponseEntity.badRequest().body(Map.of("error", "source must be WALLET or RAZORPAY"));
		}

		w.setActivePolicyId(policy.getId());
		userRepository.save(w);

		financeService.recordPolicyPurchase(w, premium, source,
				body != null ? body.getRazorpayPaymentId() : null);

		Map<String, Object> res = new LinkedHashMap<>();
		res.put("message", "Policy purchased");
		res.put("activePolicyId", policy.getId());
		res.put("walletBalance", w.getWalletBalance());
		return ResponseEntity.ok(res);
	}
}
