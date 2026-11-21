package com.chipin.chipin;

import com.chipin.chipin.repository.CreditTransactionRepository;
import com.chipin.chipin.service.GreenCreditService;
import com.chipin.chipin.support.InMemoryRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

// Green credit ledger tests. No database: the repository is an in-memory stand-in.
class GreenCreditServiceTest {

    private final GreenCreditService service =
            new GreenCreditService(InMemoryRepository.create(CreditTransactionRepository.class));

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    @Test
    void balanceIsTheSumOfEarnedMinusSpent() {
        service.award("Afifa", bd("10"), "QUIZ_CORRECT");
        service.award("Afifa", bd("5"), "QUIZ_CORRECT");
        service.spend("Afifa", bd("4"), "CONVERSION");

        assertEquals(bd("11.00"), service.balanceOf("Afifa"));
    }

    @Test
    void membersHaveSeparateBalances() {
        service.award("Afifa", bd("10"), "QUIZ_CORRECT");
        assertEquals(bd("0.00"), service.balanceOf("Sayma"));
    }

    @Test
    void cannotSpendMoreThanTheBalance() {
        service.award("Afifa", bd("10"), "QUIZ_CORRECT");

        assertThrows(IllegalStateException.class, () -> service.spend("Afifa", bd("10.01"), "CONVERSION"));
        assertEquals(bd("10.00"), service.balanceOf("Afifa")); // unchanged
        assertDoesNotThrow(() -> service.spend("Afifa", bd("10.00"), "CONVERSION")); // exact balance is fine
    }

    @Test
    void rejectsZeroNegativeAndMissingInput() {
        assertThrows(IllegalArgumentException.class, () -> service.award("Afifa", bd("0"), "X"));
        assertThrows(IllegalArgumentException.class, () -> service.award("Afifa", bd("-5"), "X"));
        assertThrows(IllegalArgumentException.class, () -> service.award(" ", bd("5"), "X"));
        assertThrows(IllegalArgumentException.class, () -> service.spend("Afifa", null, "X"));
    }
}