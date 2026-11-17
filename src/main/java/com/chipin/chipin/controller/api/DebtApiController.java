package com.chipin.chipin.controller.api;

import com.chipin.chipin.dto.DebtOverview;
import com.chipin.chipin.model.DebtSettlement;
import com.chipin.chipin.service.DebtSimplificationService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

// JSON API for Feature 1 (Debt Simplification). Same logic as the /debts page.
@RestController
@RequestMapping("/api/debts")
public class DebtApiController {

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    private final DebtSimplificationService debtService;

    public DebtApiController(DebtSimplificationService debtService) {
        this.debtService = debtService;
    }

    public record PaymentRequest(String fromUserId, String toUserId, BigDecimal amount, String recordedByUserId) {}
    public record SettleAllRequest(String recordedByUserId) {}

    // Balances, the "before" debts, the simplified payments and the payment history.
    @GetMapping
    public DebtOverview overview() {
        return debtService.getOverview(DEFAULT_GROUP_ID, DEMO_MEMBERS);
    }

    @PostMapping("/settle")
    public DebtSettlement recordPayment(@RequestBody PaymentRequest request) {
        return debtService.recordPayment(DEFAULT_GROUP_ID, request.fromUserId(), request.toUserId(),
                request.amount(), request.recordedByUserId());
    }

    @PostMapping("/settle-all")
    public List<DebtSettlement> settleAll(@RequestBody SettleAllRequest request) {
        return debtService.settleAll(DEFAULT_GROUP_ID, DEMO_MEMBERS, request.recordedByUserId());
    }

    // Undoes the most recent payment.
    @DeleteMapping("/settlements/{id}")
    public DebtSettlement undoPayment(@PathVariable Long id) {
        return debtService.undoPayment(DEFAULT_GROUP_ID, id);
    }
}
