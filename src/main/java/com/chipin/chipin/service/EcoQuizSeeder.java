package com.chipin.chipin.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// Runs when the app starts and adds any question from the question bank that is missing.
@Component
public class EcoQuizSeeder implements ApplicationRunner {

    private final EcoQuizService quizService;

    public EcoQuizSeeder(EcoQuizService quizService) {
        this.quizService = quizService;
    }

    @Override
    public void run(ApplicationArguments args) {
        quizService.seedQuestionBank();
    }
}