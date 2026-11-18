package com.chipin.chipin.controller.api;

import com.chipin.chipin.dto.AssetView;
import com.chipin.chipin.model.AssetCost;
import com.chipin.chipin.model.SharedAsset;
import com.chipin.chipin.service.SharedAssetService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// JSON API for Feature 5 (Shared Asset Co-Ownership). Same logic as the /assets page.
@RestController
@RequestMapping("/api/assets")
public class AssetApiController {

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    private final SharedAssetService assetService;

    public AssetApiController(SharedAssetService assetService) {
        this.assetService = assetService;
    }

    // contributions: member -> amount they paid, e.g. {"Afifa": 12000, "Mufliha": 8000}
    public record RegisterRequest(String name, LocalDate purchaseDate, int usefulLifeMonths,
                                  Map<String, BigDecimal> contributions, String createdByUserId) {}
    public record UseRequest(String userId) {}
    public record CostRequest(String description, BigDecimal amount, String paidByUserId) {}
    public record BuyoutRequest(String leavingUserId) {}
    public record BuyInRequest(String joiningUserId, BigDecimal sharePercent) {}

    @GetMapping
    public List<AssetView> listAssets() {
        return assetService.getGroupAssetViews(DEFAULT_GROUP_ID, DEMO_MEMBERS);
    }

    // One asset with its owners, value today and full history.
    @GetMapping("/{id}")
    public AssetView getAsset(@PathVariable Long id) {
        return assetService.getAssetView(id, DEMO_MEMBERS);
    }

    @PostMapping
    public SharedAsset registerAsset(@RequestBody RegisterRequest request) {
        if (request.contributions() == null) {
            throw new IllegalArgumentException("Enter what at least one member paid.");
        }
        return assetService.registerAsset(DEFAULT_GROUP_ID, request.name(), request.purchaseDate(),
                request.usefulLifeMonths(), request.contributions(), request.createdByUserId());
    }

    @PostMapping("/{id}/use")
    public Map<String, String> logUse(@PathVariable Long id, @RequestBody UseRequest request) {
        assetService.logUse(id, request.userId());
        return Map.of("status", "logged");
    }

    @PostMapping("/{id}/costs")
    public AssetCost addCost(@PathVariable Long id, @RequestBody CostRequest request) {
        return assetService.addCost(id, request.description(), request.amount(), request.paidByUserId());
    }

    @PostMapping("/{id}/buyout")
    public Map<String, BigDecimal> buyOut(@PathVariable Long id, @RequestBody BuyoutRequest request) {
        return Map.of("buyoutAmount", assetService.buyOut(id, request.leavingUserId()));
    }

    @PostMapping("/{id}/buyin")
    public Map<String, BigDecimal> buyIn(@PathVariable Long id, @RequestBody BuyInRequest request) {
        return Map.of("price", assetService.buyIn(id, request.joiningUserId(), request.sharePercent()));
    }
}
