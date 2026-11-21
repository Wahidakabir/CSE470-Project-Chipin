package com.chipin.chipin.service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Pure scoring rules for the Eco Quiz. No database or Spring code, so it is easy to unit test.
 * A member gets secondsToAnswer seconds after pressing Start. A correct answer earns the reward,
 * plus a flat bonus if it came in the first half of the time. Wrong or late answers earn nothing.
 */
public final class QuizScoring {

    public static final int SPEED_BONUS = 1;
    // A small allowance for the time a request takes to reach the server.
    public static final int GRACE_SECONDS = 2;

    private QuizScoring() {}

    public static boolean isInTime(LocalDateTime startedAt, LocalDateTime now, int secondsToAnswer) {
        long elapsedMs = Duration.between(startedAt, now).toMillis();
        return elapsedMs <= (secondsToAnswer + GRACE_SECONDS) * 1000L;
    }

    public static int creditsFor(boolean correct, LocalDateTime startedAt, LocalDateTime answeredAt,
                                 int secondsToAnswer, int reward) {
        if (!correct || !isInTime(startedAt, answeredAt, secondsToAnswer)) {
            return 0;
        }
        long elapsedMs = Duration.between(startedAt, answeredAt).toMillis();
        boolean fast = elapsedMs * 2 <= secondsToAnswer * 1000L;
        return fast ? reward + SPEED_BONUS : reward;
    }
}