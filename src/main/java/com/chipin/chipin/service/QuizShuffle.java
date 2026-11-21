package com.chipin.chipin.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure helper for the Eco Quiz. Each attempt shows its answer choices in a different order,
 * so "the answer is B" means nothing to a friend. The same attempt always gets the same order,
 * so a page refresh does not reshuffle the choices.
 */
public final class QuizShuffle {

    private QuizShuffle() {}

    // order.get(k) is the original position of the choice shown in slot k.
    public static List<Integer> order(long seed, int optionCount) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < optionCount; i++) {
            order.add(i);
        }
        Collections.shuffle(order, new Random(seed));
        return order;
    }
}