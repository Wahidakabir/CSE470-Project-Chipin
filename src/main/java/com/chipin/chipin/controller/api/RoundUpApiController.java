package com.chipin.chipin.controller.api;

import com.chipin.chipin.dto.RoundUpOverview;
import com.chipin.chipin.model.RoundUpSettings;
import com.chipin.chipin.service.RoundUpService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

// JSON API for Feature 3 (Round-Up with Boosters). Same logic as the /roundup page.
@RestController
@RequestMapping("/api/roundup")
public class RoundUpApiController {

    private static final String DEFAULT_GROUP_ID = "group_01";

    private final RoundUpService roundUpService;

    public RoundUpApiController(RoundUpService roundUpService) {
        this.roundUpService = roundUpService;
    }

    public record SettingsRequest(boolean enabled, BigDecimal roundTo, boolean weekendDoubleEnabled,
                                  boolean fixedExtraEnabled, BigDecimal fixedExtraAmount,
                                  String destinationType, String destinationName) {}

    // One member's rules, totals, goals and round-up history.
    @GetMapping("/{userId}")
    public RoundUpOverview overview(@PathVariable String userId) {
        return roundUpService.getOverview(DEFAULT_GROUP_ID, userId);
    }

    @PutMapping("/{userId}/settings")
    public RoundUpSettings saveSettings(@PathVariable String userId, @RequestBody SettingsRequest request) {
        return roundUpService.saveSettings(DEFAULT_GROUP_ID, userId, request.enabled(), request.roundTo(),
                request.weekendDoubleEnabled(), request.fixedExtraEnabled(), request.fixedExtraAmount(),
                request.destinationType(), request.destinationName());
    }
}
