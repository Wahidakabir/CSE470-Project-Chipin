package com.chipin.chipin.controller.api;

import com.chipin.chipin.dto.BillView;
import com.chipin.chipin.model.BillItem;
import com.chipin.chipin.model.ItemizedBill;
import com.chipin.chipin.service.ItemizedBillService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// JSON API for Feature 2 (Itemized Receipt Splitting). Same logic as the /bills page.
@RestController
@RequestMapping("/api/bills")
public class BillApiController {

    private static final String DEFAULT_GROUP_ID = "group_01";

    private final ItemizedBillService billService;

    public BillApiController(ItemizedBillService billService) {
        this.billService = billService;
    }

    // What the client sends, as JSON.
    public record CreateBillRequest(String title, String paidByUserId, LocalDate billDate,
                                    BigDecimal tax, BigDecimal serviceCharge, BigDecimal tip, BigDecimal discount) {}
    public record AddItemRequest(String name, BigDecimal amount, List<String> assignees) {}
    public record ExtrasRequest(BigDecimal tax, BigDecimal serviceCharge, BigDecimal tip, BigDecimal discount) {}

    @GetMapping
    public List<BillView> listBills() {
        return billService.getGroupBillViews(DEFAULT_GROUP_ID);
    }

    @PostMapping
    public ItemizedBill createBill(@RequestBody CreateBillRequest request) {
        return billService.createBill(DEFAULT_GROUP_ID, request.paidByUserId(), request.title(),
                request.billDate(), request.tax(), request.serviceCharge(), request.tip(), request.discount());
    }

    @PostMapping("/{id}/items")
    public BillItem addItem(@PathVariable Long id, @RequestBody AddItemRequest request) {
        return billService.addItem(id, request.name(), request.amount(), request.assignees());
    }

    @DeleteMapping("/items/{itemId}")
    public Map<String, String> removeItem(@PathVariable Long itemId) {
        billService.removeItem(itemId);
        return Map.of("status", "removed");
    }

    @PutMapping("/{id}/extras")
    public Map<String, String> updateExtras(@PathVariable Long id, @RequestBody ExtrasRequest request) {
        billService.updateExtras(id, request.tax(), request.serviceCharge(), request.tip(), request.discount());
        return Map.of("status", "updated");
    }

    @PostMapping("/{id}/finalize")
    public ItemizedBill finalizeBill(@PathVariable Long id) {
        return billService.finalizeBill(id);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteDraft(@PathVariable Long id) {
        billService.deleteDraft(id);
        return Map.of("status", "deleted");
    }
}
