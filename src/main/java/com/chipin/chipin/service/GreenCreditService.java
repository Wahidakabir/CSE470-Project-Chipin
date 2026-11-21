package com.chipin.chipin.service;

import com.chipin.chipin.model.CreditTransaction;
import com.chipin.chipin.repository.CreditTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Feature: Green Credits. The one place that adds or spends credits.
 * Quiz rewards and currency conversion both go through this service.
 */
@Service
public class GreenCreditService {

    private final CreditTransactionRepository repository;

    public GreenCreditService(CreditTransactionRepository repository) {
        this.repository = repository;
    }

    public BigDecimal balanceOf(String userId) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (CreditTransaction t : repository.findByUserIdOrderByIdDesc(userId)) {
            total = total.add(t.getAmount());
        }
        return total;
    }

    public List<CreditTransaction> historyOf(String userId) {
        return repository.findByUserIdOrderByIdDesc(userId);
    }

    @Transactional
    public CreditTransaction award(String userId, BigDecimal amount, String reason) {
        requireValid(userId, amount);
        return save(userId, amount.setScale(2, RoundingMode.HALF_UP), reason);
    }

    // Spending adds a negative row. It is refused if the member does not have enough credits.
    @Transactional
    public CreditTransaction spend(String userId, BigDecimal amount, String reason) {
        requireValid(userId, amount);
        BigDecimal cost = amount.setScale(2, RoundingMode.HALF_UP);
        if (balanceOf(userId).compareTo(cost) < 0) {
            throw new IllegalStateException("Not enough green credits.");
        }
        return save(userId, cost.negate(), reason);
    }

    private void requireValid(String userId, BigDecimal amount) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Choose a member.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Credit amount must be greater than 0.");
        }
    }

    private CreditTransaction save(String userId, BigDecimal signedAmount, String reason) {
        CreditTransaction t = new CreditTransaction();
        t.setUserId(userId);
        t.setAmount(signedAmount);
        t.setReason(reason);
        t.setCreatedAt(LocalDateTime.now());
        return repository.save(t);
    }
}