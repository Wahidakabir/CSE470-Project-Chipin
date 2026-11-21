package com.chipin.chipin;

import com.chipin.chipin.service.QuizScoring;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizScoringTest {

    // 20 seconds to answer, reward 2, so the fast bonus applies up to 10 seconds.
    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 20, 10, 0, 0);

    private int score(boolean correct, int secondsAfterStart) {
        return QuizScoring.creditsFor(correct, START, START.plusSeconds(secondsAfterStart), 10, 2);
    }

    @Test
    void wrongAnswerEarnsNothing() {
        assertEquals(0, score(false, 3));
    }

    @Test
    void fastCorrectAnswerGetsTheBonus() {
        assertEquals(3, score(true, 3));
        assertEquals(3, score(true, 5)); // exactly half the time
    }

    @Test
    void slowerCorrectAnswerGetsTheBaseReward() {
        assertEquals(2, score(true, 6));
        assertEquals(2, score(true, 10)); // exactly at the limit
    }

    @Test
    void answerAfterTheLimitPlusGraceEarnsNothing() {
        assertEquals(2, score(true, 12)); // inside the 2 second grace
        assertEquals(0, score(true, 13));
    }

    @Test
    void inTimeChecksTheLimitPlusGrace() {
        assertTrue(QuizScoring.isInTime(START, START.plusSeconds(12), 10));
        assertFalse(QuizScoring.isInTime(START, START.plusSeconds(13), 10));
    }
}