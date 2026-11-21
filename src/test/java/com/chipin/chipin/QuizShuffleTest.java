package com.chipin.chipin;

import com.chipin.chipin.service.QuizShuffle;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuizShuffleTest {

    @Test
    void sameSeedGivesTheSameOrder() {
        assertEquals(QuizShuffle.order(42, 4), QuizShuffle.order(42, 4));
    }

    @Test
    void everyOriginalPositionAppearsExactlyOnce() {
        List<Integer> order = QuizShuffle.order(7, 4);
        assertEquals(4, new HashSet<>(order).size());
        assertTrue(order.containsAll(List.of(0, 1, 2, 3)));
    }

    @Test
    void differentSeedsGiveDifferentOrdersSomewhere() {
        boolean differs = false;
        for (long seed = 1; seed <= 20; seed++) {
            if (!QuizShuffle.order(seed, 4).equals(QuizShuffle.order(seed + 1, 4))) {
                differs = true;
            }
        }
        assertTrue(differs);
        assertNotEquals(List.of(), QuizShuffle.order(1, 3));
    }
}