package com.chipin.chipin.service;

// What happened when a member answered a question.
public record QuizResult(boolean correct, int credits) {}