package com.chipin.chipin.controller;

import com.chipin.chipin.service.EcoQuizService;
import com.chipin.chipin.service.QuizResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

// Feature: Eco Quiz and Green Credits
@Controller
@RequestMapping("/eco-quiz")
public class EcoQuizController {

    private static final List<String> DEMO_MEMBERS = Arrays.asList("Afifa", "Mufliha", "Musarrat", "Sayma");

    private final EcoQuizService quizService;

    public EcoQuizController(EcoQuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    public String viewQuizPage(@RequestParam(defaultValue = "Mufliha") String currentUser, Model model) {
        model.addAttribute("view", quizService.buildView(currentUser, LocalDateTime.now()));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("members", DEMO_MEMBERS);
        return "eco-quiz";
    }

    @PostMapping("/new-session")
    public String newSession(@RequestParam String currentUser, RedirectAttributes redirectAttributes) {
        try {
            quizService.startSession(currentUser, LocalDateTime.now());
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/eco-quiz?currentUser=" + currentUser;
    }

    @PostMapping("/attempts/{attemptId}/start")
    public String start(@PathVariable Long attemptId,
                        @RequestParam String currentUser,
                        RedirectAttributes redirectAttributes) {
        try {
            quizService.startQuestion(attemptId, currentUser, LocalDateTime.now());
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/eco-quiz?currentUser=" + currentUser;
    }

    @PostMapping("/attempts/{attemptId}/answer")
    public String answer(@PathVariable Long attemptId,
                         @RequestParam String currentUser,
                         @RequestParam int choice,
                         RedirectAttributes redirectAttributes) {
        try {
            QuizResult result = quizService.submitAnswer(attemptId, currentUser, choice, LocalDateTime.now());
            redirectAttributes.addFlashAttribute("successMessage", result.correct()
                    ? "Correct! You earned " + result.credits() + " green credits."
                    : "Not this time. No credits for this question.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/eco-quiz?currentUser=" + currentUser;
    }
}