package com.chipin.chipin.controller;

import com.chipin.chipin.model.DebtSettlement;
import com.chipin.chipin.model.RoundUpSaving;
import com.chipin.chipin.service.DebtSimplificationService;
import com.chipin.chipin.service.RoundUpService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

// Feature 1: Debt Simplification Engine with Graph Visualization
@Controller
@RequestMapping("/debts")
public class DebtSimplificationController {

    private final DebtSimplificationService debtService;
    private final RoundUpService roundUpService;

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    public DebtSimplificationController(DebtSimplificationService debtService,
                                        RoundUpService roundUpService) {
        this.debtService = debtService;
        this.roundUpService = roundUpService;
    }

    @GetMapping
    public String viewDebtsPage(@RequestParam(defaultValue = "Mufliha") String currentUser, Model model) {
        model.addAttribute("overview", debtService.getOverview(DEFAULT_GROUP_ID, DEMO_MEMBERS));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("members", DEMO_MEMBERS);
        model.addAttribute("undoablePaymentId", debtService.undoablePaymentId(DEFAULT_GROUP_ID));
        return "debts";
    }

    @PostMapping("/settle")
    public String recordPayment(@RequestParam String currentUser,
                                @RequestParam String fromUserId,
                                @RequestParam String toUserId,
                                @RequestParam BigDecimal amount,
                                RedirectAttributes redirectAttributes) {
        try {
            DebtSettlement payment = debtService.recordPayment(
                    DEFAULT_GROUP_ID, fromUserId, toUserId, amount, currentUser);
            String message = "Payment recorded: " + fromUserId + " paid " + toUserId + " ৳" + amount + ".";
            Optional<RoundUpSaving> saving = roundUpService.findForSettlement(payment.getId());
            if (saving.isPresent()) {
                message += " Round-up saved ৳" + saving.get().getTotalSaved()
                        + " to " + saving.get().getDestinationName() + ".";
            }
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/debts?currentUser=" + currentUser;
    }

    @PostMapping("/settle-all")
    public String settleAll(@RequestParam String currentUser, RedirectAttributes redirectAttributes) {
        try {
            List<DebtSettlement> payments = debtService.settleAll(DEFAULT_GROUP_ID, DEMO_MEMBERS, currentUser);
            BigDecimal saved = BigDecimal.ZERO;
            for (DebtSettlement payment : payments) {
                Optional<RoundUpSaving> saving = roundUpService.findForSettlement(payment.getId());
                if (saving.isPresent()) {
                    saved = saved.add(saving.get().getTotalSaved());
                }
            }
            String message = payments.size() + " payment(s) recorded. The group is all settled up!";
            if (saved.signum() > 0) {
                message += " Round-up saved ৳" + saved + " in total.";
            }
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/debts?currentUser=" + currentUser;
    }

    @PostMapping("/settlements/{id}/undo")
    public String undoPayment(@PathVariable Long id, @RequestParam String currentUser,
                              RedirectAttributes redirectAttributes) {
        try {
            DebtSettlement undone = debtService.undoPayment(DEFAULT_GROUP_ID, id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Undone: " + undone.getFromUserId() + " paying " + undone.getToUserId() + " ৳"
                            + undone.getAmount() + ". Any round-up from it was removed too.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/debts?currentUser=" + currentUser;
    }
}
