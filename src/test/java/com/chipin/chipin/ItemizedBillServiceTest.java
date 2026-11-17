package com.chipin.chipin;

import com.chipin.chipin.dto.BillView;
import com.chipin.chipin.model.BillItem;
import com.chipin.chipin.model.BillShare;
import com.chipin.chipin.model.Expense;
import com.chipin.chipin.model.ItemizedBill;
import com.chipin.chipin.repository.BillItemRepository;
import com.chipin.chipin.repository.BillShareRepository;
import com.chipin.chipin.repository.ExpenseRepository;
import com.chipin.chipin.repository.ItemAssignmentRepository;
import com.chipin.chipin.repository.ItemizedBillRepository;
import com.chipin.chipin.service.ItemizedBillService;
import com.chipin.chipin.support.InMemoryRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Feature 2 service tests. No database: repositories are in-memory stand-ins.
class ItemizedBillServiceTest {

    private static final String GROUP = "group_01";

    private final ExpenseRepository expenses = InMemoryRepository.create(ExpenseRepository.class);
    private final ItemizedBillService service = new ItemizedBillService(
            InMemoryRepository.create(ItemizedBillRepository.class),
            InMemoryRepository.create(BillItemRepository.class),
            InMemoryRepository.create(ItemAssignmentRepository.class),
            InMemoryRepository.create(BillShareRepository.class),
            expenses);

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    // Dinner paid by Mufliha: tax 150, tip 100, discount 50 -> extras of 200.
    private ItemizedBill dinner() {
        ItemizedBill bill = service.createBill(GROUP, "Mufliha", "Dinner", null,
                bd("150"), null, bd("100"), bd("50"));
        service.addItem(bill.getId(), "Pizza", bd("900"), List.of("Afifa", "Mufliha", "Sayma"));
        service.addItem(bill.getId(), "Drink", bd("150"), List.of("Afifa"));
        service.addItem(bill.getId(), "Pasta", bd("450"), List.of("Mufliha"));
        return bill;
    }

    private BigDecimal totalFor(BillView view, String userId) {
        for (BillShare share : view.getShares()) {
            if (share.getUserId().equals(userId)) {
                return share.getTotal();
            }
        }
        return null;
    }

    @Test
    void draftShowsALivePreviewOfTheSplit() {
        dinner();
        BillView view = service.getGroupBillViews(GROUP).get(0);

        assertTrue(view.isDraft());
        assertEquals(bd("1500.00"), view.getItemsSubtotal());
        assertEquals(bd("1700.00"), view.getGrandTotal());
        assertEquals(bd("510.00"), totalFor(view, "Afifa"));
        assertEquals(bd("850.00"), totalFor(view, "Mufliha"));
        assertEquals(bd("340.00"), totalFor(view, "Sayma"));
    }

    @Test
    void finalizingLocksTheBillAndPostsOneExpenseToTheLedger() {
        ItemizedBill bill = dinner();
        service.finalizeBill(bill.getId());

        BillView view = service.getGroupBillViews(GROUP).get(0);
        assertFalse(view.isDraft());
        assertEquals(3, view.getShares().size());

        List<Expense> ledger = expenses.findByGroupId(GROUP);
        assertEquals(1, ledger.size());
        Expense expense = ledger.get(0);
        assertEquals("Mufliha", expense.getPaidByUserId());
        assertEquals("APPROVED", expense.getStatus());
        assertEquals(1700.0, expense.getTotalAmount());
        assertEquals(510.0, expense.getSplits().get("Afifa"));
        assertEquals(340.0, expense.getSplits().get("Sayma"));
    }

    @Test
    void finalizedBillCannotBeChanged() {
        ItemizedBill bill = dinner();
        service.finalizeBill(bill.getId());

        assertThrows(IllegalStateException.class, () ->
                service.addItem(bill.getId(), "Dessert", bd("200"), List.of("Sayma")));
        assertThrows(IllegalStateException.class, () -> service.finalizeBill(bill.getId()));
    }

    @Test
    void itemNeedsAtLeastOneMemberAndAPositiveAmount() {
        ItemizedBill bill = service.createBill(GROUP, "Mufliha", "Lunch", null, null, null, null, null);

        assertThrows(IllegalArgumentException.class, () ->
                service.addItem(bill.getId(), "Rice", bd("100"), List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                service.addItem(bill.getId(), "Rice", bd("0"), List.of("Afifa")));
    }

    @Test
    void emptyBillCannotBeFinalized() {
        ItemizedBill bill = service.createBill(GROUP, "Mufliha", "Lunch", null, null, null, null, null);
        assertThrows(IllegalStateException.class, () -> service.finalizeBill(bill.getId()));
    }

    @Test
    void discountLargerThanTheWholeBillIsRejected() {
        ItemizedBill bill = service.createBill(GROUP, "Sayma", "Tea", null, null, null, null, bd("500"));
        service.addItem(bill.getId(), "Tea", bd("100"), List.of("Sayma"));

        assertThrows(IllegalStateException.class, () -> service.finalizeBill(bill.getId()));
    }

    @Test
    void removingAnItemUpdatesTheSplit() {
        ItemizedBill bill = service.createBill(GROUP, "Mufliha", "Snacks", null, null, null, null, null);
        service.addItem(bill.getId(), "Fuchka", bd("120"), List.of("Afifa", "Mufliha"));
        BillItem extra = service.addItem(bill.getId(), "Juice", bd("80"), List.of("Afifa"));

        service.removeItem(extra.getId());

        BillView view = service.getGroupBillViews(GROUP).get(0);
        assertEquals(1, view.getItems().size());
        assertEquals(bd("60.00"), totalFor(view, "Afifa"));
    }

    @Test
    void deletingADraftRemovesIt() {
        ItemizedBill bill = dinner();
        service.deleteDraft(bill.getId());
        assertEquals(0, service.getGroupBillViews(GROUP).size());
    }
}
