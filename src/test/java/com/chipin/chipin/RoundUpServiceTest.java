package com.chipin.chipin;

import com.chipin.chipin.dto.GoalTotal;
import com.chipin.chipin.dto.RoundUpOverview;
import com.chipin.chipin.model.DebtSettlement;
import com.chipin.chipin.model.RoundUpSaving;
import com.chipin.chipin.model.RoundUpSettings;
import com.chipin.chipin.repository.RoundUpSavingRepository;
import com.chipin.chipin.repository.RoundUpSettingsRepository;
import com.chipin.chipin.service.RoundUpService;
import com.chipin.chipin.support.InMemoryRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// Feature 3 service tests. No database: repositories are in-memory stand-ins.
class RoundUpServiceTest {

    private static final String GROUP = "group_01";
    // Fixed dates so the weekend rule does not depend on the day the tests run.
    private static final LocalDateTime A_SUNDAY = LocalDateTime.of(2026, 11, 22, 12, 0);
    private static final LocalDateTime A_FRIDAY = LocalDateTime.of(2026, 11, 20, 12, 0);

    private final RoundUpService service = new RoundUpService(
            InMemoryRepository.create(RoundUpSettingsRepository.class),
            InMemoryRepository.create(RoundUpSavingRepository.class));
    private long nextSettlementId = 1;

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    private DebtSettlement payment(String from, String amount, LocalDateTime when) {
        DebtSettlement s = new DebtSettlement();
        s.setId(nextSettlementId++);
        s.setGroupId(GROUP);
        s.setFromUserId(from);
        s.setToUserId("Mufliha");
        s.setAmount(bd(amount));
        s.setSettledAt(when);
        return s;
    }

    @Test
    void memberWhoNeverChangedRulesGetsTheDefaults() {
        RoundUpSettings settings = service.getSettings(GROUP, "Afifa");

        assertTrue(settings.isEnabled());
        assertEquals(bd("10.00"), settings.getRoundTo());
        assertEquals(RoundUpService.PERSONAL, settings.getDestinationType());
    }

    @Test
    void paymentIsRoundedUpWithThePayersRules() {
        service.saveSettings(GROUP, "Sayma", true, bd("50"), false, false, null,
                RoundUpService.GROUP, "Cox's Bazar trip");

        Optional<RoundUpSaving> saving = service.applyToSettlement(payment("Sayma", "237", A_SUNDAY));

        assertEquals(bd("13.00"), saving.get().getTotalSaved());
        assertEquals("Cox's Bazar trip", saving.get().getDestinationName());
    }

    @Test
    void weekendDoubleAndFixedExtraStack() {
        service.saveSettings(GROUP, "Sayma", true, bd("10"), true, true, bd("5"),
                RoundUpService.PERSONAL, "Laptop");

        RoundUpSaving weekday = service.applyToSettlement(payment("Sayma", "237", A_SUNDAY)).get();
        RoundUpSaving weekend = service.applyToSettlement(payment("Sayma", "237", A_FRIDAY)).get();

        assertEquals(bd("8.00"), weekday.getTotalSaved());   // 3 round-up + 5 fixed
        assertEquals(bd("11.00"), weekend.getTotalSaved());  // 3 doubled to 6, + 5 fixed
    }

    @Test
    void switchedOffSavesNothing() {
        service.saveSettings(GROUP, "Musarrat", false, bd("10"), false, false, null,
                RoundUpService.PERSONAL, "My savings");

        assertFalse(service.applyToSettlement(payment("Musarrat", "237", A_SUNDAY)).isPresent());
    }

    @Test
    void exactMultipleWithNoBoostersSavesNothing() {
        assertFalse(service.applyToSettlement(payment("Afifa", "240", A_SUNDAY)).isPresent());
    }

    @Test
    void invalidRulesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.saveSettings(GROUP, "Afifa", true,
                bd("7"), false, false, null, RoundUpService.PERSONAL, "My savings")); // not 10, 50 or 100
        assertThrows(IllegalArgumentException.class, () -> service.saveSettings(GROUP, "Afifa", true,
                bd("10"), false, true, null, RoundUpService.PERSONAL, "My savings")); // fixed extra with no amount
        assertThrows(IllegalArgumentException.class, () -> service.saveSettings(GROUP, "Afifa", true,
                bd("10"), false, false, null, RoundUpService.PERSONAL, " ")); // no goal name
        assertThrows(IllegalArgumentException.class, () -> service.saveSettings(GROUP, "Afifa", true,
                bd("10"), false, false, null, "SOMEWHERE", "My savings")); // unknown destination
    }

    @Test
    void savingRulesTwiceUpdatesTheSameRow() {
        service.saveSettings(GROUP, "Afifa", true, bd("50"), false, false, null, RoundUpService.PERSONAL, "A");
        RoundUpSettings again = service.saveSettings(GROUP, "Afifa", true, bd("100"), false, false, null,
                RoundUpService.PERSONAL, "B");

        assertEquals(bd("100.00"), service.getSettings(GROUP, "Afifa").getRoundTo());
        assertEquals(again.getId(), service.getSettings(GROUP, "Afifa").getId());
    }

    @Test
    void overviewAddsUpTotalsPerGoal() {
        service.saveSettings(GROUP, "Sayma", true, bd("10"), false, false, null,
                RoundUpService.GROUP, "Cox's Bazar trip");
        service.saveSettings(GROUP, "Afifa", true, bd("10"), false, false, null,
                RoundUpService.GROUP, "Cox's Bazar trip");
        service.applyToSettlement(payment("Sayma", "237", A_SUNDAY)); // saves 3
        service.applyToSettlement(payment("Afifa", "76", A_SUNDAY));  // saves 4
        service.applyToSettlement(payment("Mufliha", "55", A_SUNDAY)); // default personal goal, saves 5

        RoundUpOverview sayma = service.getOverview(GROUP, "Sayma");
        RoundUpOverview mufliha = service.getOverview(GROUP, "Mufliha");

        assertEquals(bd("3.00"), sayma.getMySavedTotal());
        assertEquals(bd("7.00"), sayma.getGroupGoalTotal()); // both members' group savings
        assertEquals(1, sayma.getGoals().size());            // Sayma has no personal goal
        GoalTotal trip = sayma.getGoals().get(0);
        assertEquals("Cox's Bazar trip", trip.getName());
        assertEquals(bd("7.00"), trip.getTotal());
        assertEquals(2, trip.getPayments());

        assertEquals(bd("5.00"), mufliha.getMyPersonalTotal());
        assertEquals(2, mufliha.getGoals().size()); // her personal goal plus the group goal
    }

    @Test
    void removingASavingForAnUndonePayment() {
        DebtSettlement p = payment("Afifa", "237", A_SUNDAY);
        service.applyToSettlement(p);

        service.removeForSettlement(p.getId());

        assertFalse(service.findForSettlement(p.getId()).isPresent());
        assertEquals(bd("0.00"), service.getOverview(GROUP, "Afifa").getMySavedTotal());
    }
}
