package com.chipin.chipin;

import com.chipin.chipin.dto.DebtEdge;
import com.chipin.chipin.dto.DebtOverview;
import com.chipin.chipin.dto.MemberBalance;
import com.chipin.chipin.model.DebtSettlement;
import com.chipin.chipin.model.Expense;
import com.chipin.chipin.repository.DebtSettlementRepository;
import com.chipin.chipin.repository.ExpenseRepository;
import com.chipin.chipin.repository.RoundUpSavingRepository;
import com.chipin.chipin.repository.RoundUpSettingsRepository;
import com.chipin.chipin.service.DebtSimplificationService;
import com.chipin.chipin.service.RoundUpService;
import com.chipin.chipin.support.InMemoryRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Feature 1 service tests. No database: repositories are in-memory stand-ins.
class DebtSimplificationServiceTest {

    private static final String GROUP = "group_01";
    private static final List<String> MEMBERS = List.of("Afifa", "Mufliha", "Musarrat", "Sayma");

    private final ExpenseRepository expenses = InMemoryRepository.create(ExpenseRepository.class);
    private final RoundUpService roundUp = new RoundUpService(
            InMemoryRepository.create(RoundUpSettingsRepository.class),
            InMemoryRepository.create(RoundUpSavingRepository.class));
    private final DebtSimplificationService service = new DebtSimplificationService(
            expenses, InMemoryRepository.create(DebtSettlementRepository.class), roundUp);

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    // Adds a ledger entry: each "member, amount" pair owes the payer that amount.
    private void expense(String payer, String category, String status, Object... owed) {
        Map<String, Double> splits = new LinkedHashMap<>();
        for (int i = 0; i < owed.length; i += 2) {
            splits.put((String) owed[i], (Double) owed[i + 1]);
        }
        Expense e = new Expense();
        e.setGroupId(GROUP);
        e.setPaidByUserId(payer);
        e.setCategory(category);
        e.setStatus(status);
        e.setSplits(splits);
        e.setCreatedAt(LocalDateTime.now());
        expenses.save(e);
    }

    private BigDecimal balanceOf(DebtOverview overview, String userId) {
        for (MemberBalance b : overview.getBalances()) {
            if (b.getUserId().equals(userId)) {
                return b.getBalance();
            }
        }
        return null;
    }

    @Test
    void emptyLedgerMeansEveryoneIsSettled() {
        DebtOverview overview = service.getOverview(GROUP, MEMBERS);

        assertTrue(overview.isSettledUp());
        assertEquals(4, overview.getBalances().size()); // members still listed, at zero
        assertEquals(0, overview.getRawCount());
    }

    @Test
    void chainOfDebtsIsSimplifiedToOnePayment() {
        // Afifa owes Mufliha 100, Mufliha owes Sayma 100 -> Afifa pays Sayma directly
        expense("Mufliha", "Itemized Bill", "APPROVED", "Afifa", 100.0);
        expense("Sayma", "Itemized Bill", "APPROVED", "Mufliha", 100.0);

        DebtOverview overview = service.getOverview(GROUP, MEMBERS);

        assertEquals(2, overview.getRawCount());
        assertEquals(1, overview.getSimplifiedCount());
        assertEquals(1, overview.getPaymentsSaved());
        DebtEdge payment = overview.getSimplifiedPayments().get(0);
        assertEquals("Afifa", payment.getFromUserId());
        assertEquals("Sayma", payment.getToUserId());
        assertEquals(bd("100.00"), payment.getAmount());
    }

    @Test
    void payerDoesNotOweTheirOwnShare() {
        expense("Mufliha", "Recurring Bill", "APPROVED", "Mufliha", 250.0, "Afifa", 250.0);

        DebtOverview overview = service.getOverview(GROUP, MEMBERS);

        assertEquals(bd("250.00"), balanceOf(overview, "Mufliha"));
        assertEquals(bd("-250.00"), balanceOf(overview, "Afifa"));
    }

    @Test
    void emergencyAndUnapprovedExpensesAreNotDebts() {
        expense("Musarrat", "Emergency Expense", "APPROVED", "Afifa", 900.0);
        expense("Musarrat", "Approved Expense", "PENDING", "Afifa", 900.0);

        assertTrue(service.getOverview(GROUP, MEMBERS).isSettledUp());
    }

    @Test
    void paymentCannotBeLargerThanWhatIsOwed() {
        expense("Mufliha", "Itemized Bill", "APPROVED", "Afifa", 100.0);

        assertThrows(IllegalStateException.class, () ->
                service.recordPayment(GROUP, "Afifa", "Mufliha", bd("150"), "Afifa"));
        assertThrows(IllegalStateException.class, () ->
                service.recordPayment(GROUP, "Mufliha", "Afifa", bd("10"), "Afifa")); // wrong direction
        assertThrows(IllegalArgumentException.class, () ->
                service.recordPayment(GROUP, "Afifa", "Afifa", bd("10"), "Afifa"));
    }

    @Test
    void partPaymentReducesWhatIsStillOwed() {
        expense("Mufliha", "Itemized Bill", "APPROVED", "Afifa", 100.0);

        service.recordPayment(GROUP, "Afifa", "Mufliha", bd("40"), "Afifa");

        DebtOverview overview = service.getOverview(GROUP, MEMBERS);
        assertEquals(bd("60.00"), overview.getSimplifiedPayments().get(0).getAmount());
    }

    @Test
    void settleAllClearsEveryBalanceAndTheGraphs() {
        expense("Mufliha", "Itemized Bill", "APPROVED", "Afifa", 510.0, "Sayma", 340.0);
        expense("Afifa", "Recurring Bill", "APPROVED", "Musarrat", 250.0);

        List<DebtSettlement> recorded = service.settleAll(GROUP, MEMBERS, "Mufliha");

        DebtOverview overview = service.getOverview(GROUP, MEMBERS);
        assertTrue(recorded.size() > 0);
        assertTrue(overview.isSettledUp());
        assertEquals(0, overview.getRawCount()); // settled expenses drop off the "before" graph
        assertEquals(recorded.size(), overview.getSettlements().size()); // history is kept
    }

    @Test
    void payingADebtRoundsItUpIntoSavings() {
        expense("Mufliha", "Itemized Bill", "APPROVED", "Afifa", 237.0);

        DebtSettlement payment = service.recordPayment(GROUP, "Afifa", "Mufliha", bd("237"), "Afifa");

        // default rule: round up to the next 10, so 237 saves 3
        assertEquals(bd("3.00"), roundUp.findForSettlement(payment.getId()).get().getTotalSaved());
    }

    @Test
    void undoRestoresTheDebtAndRemovesTheRoundUp() {
        expense("Mufliha", "Itemized Bill", "APPROVED", "Afifa", 237.0);
        DebtSettlement payment = service.recordPayment(GROUP, "Afifa", "Mufliha", bd("237"), "Afifa");

        service.undoPayment(GROUP, payment.getId());

        DebtOverview overview = service.getOverview(GROUP, MEMBERS);
        assertEquals(bd("237.00"), overview.getSimplifiedPayments().get(0).getAmount());
        assertEquals(0, overview.getSettlements().size());
        assertFalse(roundUp.findForSettlement(payment.getId()).isPresent());
    }

    @Test
    void onlyTheMostRecentPaymentCanBeUndone() {
        expense("Mufliha", "Itemized Bill", "APPROVED", "Afifa", 100.0);
        DebtSettlement first = service.recordPayment(GROUP, "Afifa", "Mufliha", bd("30"), "Afifa");
        DebtSettlement second = service.recordPayment(GROUP, "Afifa", "Mufliha", bd("30"), "Afifa");

        assertEquals(second.getId(), service.undoablePaymentId(GROUP));
        assertThrows(IllegalStateException.class, () -> service.undoPayment(GROUP, first.getId()));
    }
}
