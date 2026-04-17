package com.guidewire.in.controller;

import com.guidewire.in.dto.FinanceSummaryResponse;
import com.guidewire.in.entity.AppFinance;
import com.guidewire.in.security.JwtFilter;
import com.guidewire.in.service.FinanceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/admin/finance")
public class FinanceAdminController {

	private final FinanceService financeService;

	public FinanceAdminController(FinanceService financeService) {
		this.financeService = financeService;
	}

	@GetMapping
	public ResponseEntity<?> summary(HttpServletRequest req) {
		if (!"ADMIN".equals(req.getAttribute(JwtFilter.ATTR_ROLE))) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		AppFinance fin = financeService.getOrCreate();
		BigDecimal revenue = fin.getTotalRevenue();
		BigDecimal claims  = fin.getTotalClaimsPaid();
		BigDecimal profit  = revenue.subtract(claims);
		return ResponseEntity.ok(new FinanceSummaryResponse(revenue, claims, profit));
	}
}
