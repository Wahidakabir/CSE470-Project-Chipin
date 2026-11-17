package com.chipin.chipin.controller;

import com.chipin.chipin.service.SharedAssetService;
import org.springframework.context.ApplicationContext;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Feature 5: Shared Asset Co-Ownership with Usage-Based Costs and Buy-In/Buyout
@Controller
@RequestMapping("/assets")
public class SharedAssetController {

    private final SharedAssetService assetService;

    private static final String DEFAULT_GROUP_ID = "group_01";
    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    // Pages of two teammates' features that connect to shared assets:
    //   Feature 11 Group Budget Management (/budgets): asset running costs are saved as expenses, which a budget adds up.
    //   Feature 15 Collaborative Savings   (/savings):  a buy-in can be paid from a goal.
    // The buttons open these pages when the teammate's controller is part of this project.
    private static final String BUDGET_PATH = "/budgets";
    private static final String BUDGET_CONTROLLER_BEAN = "budgetController";
    private static final String SAVINGS_GOALS_PATH = "/savings";
    private static final String SAVINGS_GOALS_CONTROLLER_BEAN = "savingsGoalController";

    private final ApplicationContext applicationContext;

    public SharedAssetController(SharedAssetService assetService, ApplicationContext applicationContext) {
        this.assetService = assetService;
        this.applicationContext = applicationContext;
    }

    private void addPageBasics(Model model, String currentUser) {
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("members", DEMO_MEMBERS);
    }

    // The "Group budget" and "Savings goals" buttons come here. If the teammate's page is part of
    // this project we send the person there; if not, they stay on the assets page with a short message.
    @GetMapping("/open/budget")
    public String openBudget(@RequestParam(defaultValue = "Mufliha") String currentUser,
                             RedirectAttributes redirectAttributes) {
        return openTeammatePage(BUDGET_PATH, BUDGET_CONTROLLER_BEAN, "Group budget (feature 11)",
                currentUser, redirectAttributes);
    }

    @GetMapping("/open/savings-goals")
    public String openSavingsGoals(@RequestParam(defaultValue = "Mufliha") String currentUser,
                                   RedirectAttributes redirectAttributes) {
        return openTeammatePage(SAVINGS_GOALS_PATH, SAVINGS_GOALS_CONTROLLER_BEAN, "Savings goals (feature 15)",
                currentUser, redirectAttributes);
    }

    private String openTeammatePage(String path, String controllerBean, String featureName,
                                    String currentUser, RedirectAttributes redirectAttributes) {
        if (applicationContext.containsBean(controllerBean)) {
            return "redirect:" + path + "?currentUser=" + currentUser;
        }
        redirectAttributes.addFlashAttribute("errorMessage",
                featureName + " is not added to this project yet. This button opens it once that feature is merged.");
        return "redirect:/assets?currentUser=" + currentUser;
    }

    @GetMapping
    public String viewAssetsPage(@RequestParam(defaultValue = "Mufliha") String currentUser, Model model) {
        model.addAttribute("assets", assetService.getGroupAssetViews(DEFAULT_GROUP_ID, DEMO_MEMBERS));
        addPageBasics(model, currentUser);
        model.addAttribute("today", LocalDate.now());
        return "assets";
    }

    // Every share change and cost for one asset, with the value at that time.
    @GetMapping("/{id}/history")
    public String viewAssetHistory(@PathVariable Long id,
                                   @RequestParam(defaultValue = "Mufliha") String currentUser,
                                   Model model, RedirectAttributes redirectAttributes) {
        try {
            model.addAttribute("v", assetService.getAssetView(id, DEMO_MEMBERS));
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/assets?currentUser=" + currentUser;
        }
        addPageBasics(model, currentUser);
        return "asset-history";
    }

    // The form sends one "paid" amount per member, in the same order as DEMO_MEMBERS.
    @PostMapping("/create")
    public String registerAsset(@RequestParam String currentUser,
                                @RequestParam String name,
                                @RequestParam(required = false)
                                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate purchaseDate,
                                @RequestParam int usefulLifeMonths,
                                @RequestParam List<String> paid,
                                RedirectAttributes redirectAttributes) {
        try {
            Map<String, BigDecimal> contributions = new LinkedHashMap<>();
            for (int i = 0; i < DEMO_MEMBERS.size() && i < paid.size(); i++) {
                String text = paid.get(i) == null ? "" : paid.get(i).trim();
                contributions.put(DEMO_MEMBERS.get(i), text.isEmpty() ? BigDecimal.ZERO : new BigDecimal(text));
            }
            assetService.registerAsset(DEFAULT_GROUP_ID, name, purchaseDate, usefulLifeMonths,
                    contributions, currentUser);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Asset registered. Ownership shares follow what each member paid.");
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Enter amounts as numbers, e.g. 6000.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assets?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/use")
    public String logUse(@PathVariable Long id, @RequestParam String currentUser,
                         RedirectAttributes redirectAttributes) {
        try {
            assetService.logUse(id, currentUser);
            redirectAttributes.addFlashAttribute("successMessage", "Use logged for " + currentUser + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assets?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/costs")
    public String addCost(@PathVariable Long id,
                          @RequestParam String currentUser,
                          @RequestParam String description,
                          @RequestParam BigDecimal amount,
                          @RequestParam String paidByUserId,
                          RedirectAttributes redirectAttributes) {
        try {
            assetService.addCost(id, description, amount, paidByUserId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Cost added and split. What each member owes is now on the Debts page.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assets?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/buyout")
    public String buyOut(@PathVariable Long id,
                         @RequestParam String currentUser,
                         @RequestParam String leavingUserId,
                         RedirectAttributes redirectAttributes) {
        try {
            BigDecimal buyout = assetService.buyOut(id, leavingUserId);
            redirectAttributes.addFlashAttribute("successMessage",
                    leavingUserId + " was bought out for ৳" + buyout + ". The remaining owners now owe them that on the Debts page.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assets?currentUser=" + currentUser;
    }

    @PostMapping("/{id}/buyin")
    public String buyIn(@PathVariable Long id,
                        @RequestParam String currentUser,
                        @RequestParam String joiningUserId,
                        @RequestParam BigDecimal sharePercent,
                        RedirectAttributes redirectAttributes) {
        try {
            BigDecimal price = assetService.buyIn(id, joiningUserId, sharePercent);
            redirectAttributes.addFlashAttribute("successMessage",
                    joiningUserId + " joined for ৳" + price + ". They now owe the existing owners that on the Debts page.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/assets?currentUser=" + currentUser;
    }
}
