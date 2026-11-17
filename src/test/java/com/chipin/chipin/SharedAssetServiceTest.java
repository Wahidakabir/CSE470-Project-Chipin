package com.chipin.chipin;

import com.chipin.chipin.dto.AssetOwnerView;
import com.chipin.chipin.dto.AssetView;
import com.chipin.chipin.model.Expense;
import com.chipin.chipin.model.SharedAsset;
import com.chipin.chipin.repository.AssetCostRepository;
import com.chipin.chipin.repository.AssetEventRepository;
import com.chipin.chipin.repository.AssetShareRepository;
import com.chipin.chipin.repository.AssetUsageRepository;
import com.chipin.chipin.repository.ExpenseRepository;
import com.chipin.chipin.repository.SharedAssetRepository;
import com.chipin.chipin.service.SharedAssetService;
import com.chipin.chipin.support.InMemoryRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Feature 5 service tests. No database: repositories are in-memory stand-ins.
class SharedAssetServiceTest {

    private static final String GROUP = "group_01";
    private static final List<String> MEMBERS = List.of("Afifa", "Mufliha", "Musarrat", "Sayma");

    private final ExpenseRepository expenses = InMemoryRepository.create(ExpenseRepository.class);
    private final SharedAssetService service = new SharedAssetService(
            InMemoryRepository.create(SharedAssetRepository.class),
            InMemoryRepository.create(AssetShareRepository.class),
            InMemoryRepository.create(AssetUsageRepository.class),
            InMemoryRepository.create(AssetCostRepository.class),
            InMemoryRepository.create(AssetEventRepository.class),
            expenses);

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    // A 24000 washing machine bought 6 months ago with a 24-month life: worth 18000 today.
    // Afifa paid 12000 (50%), Mufliha 6000 (25%), Musarrat 6000 (25%).
    private SharedAsset washingMachine() {
        Map<String, BigDecimal> paid = new LinkedHashMap<>();
        paid.put("Afifa", bd("12000"));
        paid.put("Mufliha", bd("6000"));
        paid.put("Musarrat", bd("6000"));
        paid.put("Sayma", BigDecimal.ZERO);
        return service.registerAsset(GROUP, "Washing machine", LocalDate.now().minusMonths(6), 24, paid, "Mufliha");
    }

    private AssetView view(SharedAsset asset) {
        return service.getAssetView(asset.getId(), MEMBERS);
    }

    private BigDecimal shareOf(AssetView view, String userId) {
        for (AssetOwnerView owner : view.getOwners()) {
            if (owner.getUserId().equals(userId)) {
                return owner.getSharePercent().setScale(2, RoundingMode.HALF_UP);
            }
        }
        return null;
    }

    private Expense lastExpense() {
        List<Expense> ledger = expenses.findByGroupId(GROUP);
        return ledger.get(ledger.size() - 1);
    }

    @Test
    void registeringSetsPriceSharesAndValue() {
        AssetView view = view(washingMachine());

        assertEquals(bd("24000.00"), view.getAsset().getPurchasePrice()); // total of what was paid
        assertEquals(3, view.getOwners().size());                         // Sayma paid nothing
        assertEquals(bd("50.00"), shareOf(view, "Afifa"));
        assertEquals(bd("25.00"), shareOf(view, "Mufliha"));
        assertEquals(bd("18000.00"), view.getCurrentValue());
        assertEquals(75, view.getValueLeftPercent());
        assertEquals(List.of("Sayma"), view.getNonOwners());
        assertEquals(0, expenses.findByGroupId(GROUP).size()); // registering creates no debt
    }

    @Test
    void registrationNeedsANameAndAtLeastOnePayer() {
        Map<String, BigDecimal> nobody = new LinkedHashMap<>();
        nobody.put("Afifa", BigDecimal.ZERO);
        Map<String, BigDecimal> afifa = new LinkedHashMap<>();
        afifa.put("Afifa", bd("1000"));

        assertThrows(IllegalArgumentException.class, () ->
                service.registerAsset(GROUP, "Router", LocalDate.now(), 12, nobody, "Afifa"));
        assertThrows(IllegalArgumentException.class, () ->
                service.registerAsset(GROUP, " ", LocalDate.now(), 12, afifa, "Afifa"));
        assertThrows(IllegalArgumentException.class, () ->
                service.registerAsset(GROUP, "Router", LocalDate.now().plusDays(1), 12, afifa, "Afifa"));
    }

    @Test
    void costIsSplitByOwnershipWhenNobodyLoggedAUse() {
        SharedAsset asset = washingMachine();

        service.addCost(asset.getId(), "Service visit", bd("800"), "Afifa");

        Expense expense = lastExpense();
        assertEquals("Afifa", expense.getPaidByUserId());
        assertEquals("Asset Cost", expense.getCategory());
        assertEquals(400.0, expense.getSplits().get("Afifa"));
        assertEquals(200.0, expense.getSplits().get("Mufliha"));
        assertEquals(200.0, expense.getSplits().get("Musarrat"));
    }

    @Test
    void costIsSplitByLoggedUsesIncludingNonOwners() {
        SharedAsset asset = washingMachine();
        service.logUse(asset.getId(), "Mufliha");
        service.logUse(asset.getId(), "Mufliha");
        service.logUse(asset.getId(), "Mufliha");
        service.logUse(asset.getId(), "Sayma"); // not an owner, but she used it

        assertEquals(4, view(asset).getUsesSinceLastCost());
        service.addCost(asset.getId(), "Motor repair", bd("800"), "Afifa");

        Expense expense = lastExpense();
        assertEquals(600.0, expense.getSplits().get("Mufliha"));
        assertEquals(200.0, expense.getSplits().get("Sayma"));
        assertFalse(expense.getSplits().containsKey("Afifa")); // did not use it, pays nothing
    }

    @Test
    void buyoutPaysTheLeaverAndSharesMoveInProportion() {
        SharedAsset asset = washingMachine();

        BigDecimal buyout = service.buyOut(asset.getId(), "Musarrat");

        // 25% of 18000 = 4500; Afifa and Mufliha own 2:1, so they owe 3000 and 1500
        assertEquals(bd("4500.00"), buyout);
        AssetView view = view(asset);
        assertEquals(2, view.getOwners().size());
        assertEquals(bd("66.67"), shareOf(view, "Afifa"));
        assertEquals(bd("33.33"), shareOf(view, "Mufliha"));

        Expense expense = lastExpense();
        assertEquals("Musarrat", expense.getPaidByUserId()); // the leaver is the one owed
        assertEquals(3000.0, expense.getSplits().get("Afifa"));
        assertEquals(1500.0, expense.getSplits().get("Mufliha"));
    }

    @Test
    void buyInChargesTheJoinerAndPaysExistingOwnersInProportion() {
        SharedAsset asset = washingMachine();

        BigDecimal price = service.buyIn(asset.getId(), "Sayma", bd("20"));

        // 20% of 18000 = 3600, paid 50/25/25 to the existing owners
        assertEquals(bd("3600.00"), price);
        AssetView view = view(asset);
        assertEquals(bd("40.00"), shareOf(view, "Afifa"));
        assertEquals(bd("20.00"), shareOf(view, "Mufliha"));
        assertEquals(bd("20.00"), shareOf(view, "Sayma"));

        List<Expense> ledger = expenses.findByGroupId(GROUP);
        assertEquals(3, ledger.size()); // one entry per existing owner
        double owedBySayma = 0;
        for (Expense e : ledger) {
            owedBySayma += e.getSplits().get("Sayma");
        }
        assertEquals(3600.0, owedBySayma);
    }

    @Test
    void lastOwnerCannotLeaveAndOwnersCannotJoinTwice() {
        Map<String, BigDecimal> paid = new LinkedHashMap<>();
        paid.put("Afifa", bd("5000"));
        SharedAsset router = service.registerAsset(GROUP, "Router", LocalDate.now(), 12, paid, "Afifa");

        assertThrows(IllegalStateException.class, () -> service.buyOut(router.getId(), "Afifa"));
        assertThrows(IllegalArgumentException.class, () -> service.buyIn(router.getId(), "Afifa", bd("10")));
        assertThrows(IllegalArgumentException.class, () -> service.buyOut(router.getId(), "Sayma"));
    }

    @Test
    void historyRecordsEveryChangeNewestFirst() {
        SharedAsset asset = washingMachine();
        service.addCost(asset.getId(), "Service visit", bd("800"), "Afifa");
        service.buyOut(asset.getId(), "Musarrat");
        service.buyIn(asset.getId(), "Sayma", bd("20"));

        AssetView view = view(asset);

        assertEquals(4, view.getEvents().size());
        assertEquals("BUY_IN", view.getEvents().get(0).getType());
        assertEquals("REGISTERED", view.getEvents().get(3).getType());
        assertEquals(bd("18000.00"), view.getEvents().get(0).getValueAtTime());
    }

    @Test
    void fullyDepreciatedAssetIsWorthNothingAndBuyoutCreatesNoDebt() {
        Map<String, BigDecimal> paid = new LinkedHashMap<>();
        paid.put("Afifa", bd("6000"));
        paid.put("Mufliha", bd("6000"));
        SharedAsset old = service.registerAsset(GROUP, "Old fan", LocalDate.now().minusMonths(30), 24, paid, "Afifa");

        assertEquals(bd("0.00"), view(old).getCurrentValue());
        assertEquals(bd("0.00"), service.buyOut(old.getId(), "Mufliha"));
        assertEquals(0, expenses.findByGroupId(GROUP).size());
    }
}
