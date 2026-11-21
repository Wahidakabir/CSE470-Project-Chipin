package com.chipin.chipin;

import com.chipin.chipin.dto.EcoQuizView;
import com.chipin.chipin.model.QuizAttempt;
import com.chipin.chipin.repository.CreditTransactionRepository;
import com.chipin.chipin.repository.QuizAttemptRepository;
import com.chipin.chipin.repository.QuizQuestionRepository;
import com.chipin.chipin.service.EcoQuestionBank;
import com.chipin.chipin.service.EcoQuizService;
import com.chipin.chipin.service.GreenCreditService;
import com.chipin.chipin.service.QuizResult;
import com.chipin.chipin.service.QuizShuffle;
import com.chipin.chipin.support.InMemoryRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

// Eco Quiz service tests. No database: repositories are in-memory stand-ins.
class EcoQuizServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 21, 9, 0, 0);

    private final QuizQuestionRepository questions = InMemoryRepository.create(QuizQuestionRepository.class);
    private final GreenCreditService credits =
            new GreenCreditService(InMemoryRepository.create(CreditTransactionRepository.class));
    private final EcoQuizService service = new EcoQuizService(
            questions, InMemoryRepository.create(QuizAttemptRepository.class), credits);

    // Adds n questions. The correct answer is the first option (index 0). Reward 2, 20 seconds.
    private void addQuestions(int n) {
        for (int i = 1; i <= n; i++) {
            service.addQuestion("Question text " + i + "?", List.of("Right", "Wrong A", "Wrong B"),
                    0, "Because.", 2, 20);
        }
    }

    // Starts and answers every question of a session.
    private void playAll(List<QuizAttempt> attempts, String user, LocalDateTime when) {
        for (QuizAttempt a : attempts) {
            service.startQuestion(a.getId(), user, when);
            service.submitAnswer(a.getId(), user, 0, when.plusSeconds(5));
        }
    }

    // The slot a member must pick to choose the right answer (original position 0).
    private int rightSlot(QuizAttempt attempt) {
        return QuizShuffle.order(attempt.getId(), 3).indexOf(0);
    }

    // ---------- sessions ----------

    @Test
    void aSessionHasFiveQuestions() {
        addQuestions(12);

        List<QuizAttempt> session = service.startSession("Afifa", START, new Random(1));

        assertEquals(5, session.size());
        assertEquals(1, session.get(0).getSessionNo());
    }

    @Test
    void aMemberNeverSeesTheSameQuestionTwo() {
        addQuestions(12);
        Set<Long> seen = new HashSet<>();
        int given = 0;

        List<QuizAttempt> first = service.startSession("Afifa", START, new Random(1));
        playAll(first, "Afifa", START);
        List<QuizAttempt> second = service.startSession("Afifa", START.plusHours(1), new Random(2));
        playAll(second, "Afifa", START.plusHours(1));
        List<QuizAttempt> third = service.startSession("Afifa", START.plusDays(1), new Random(3));
        playAll(third, "Afifa", START.plusDays(1));

        for (List<QuizAttempt> session : List.of(first, second, third)) {
            for (QuizAttempt a : session) {
                seen.add(a.getQuestionId());
                given++;
            }
        }
        assertEquals(2, third.size());   // only 2 were left in the bank
        assertEquals(12, given);
        assertEquals(12, seen.size());   // 12 different questions, none repeated

        // The bank is used up for Afifa.
        assertThrows(IllegalStateException.class, () -> service.startSession("Afifa", START.plusDays(2)));
    }

    @Test
    void membersAreGivenQuestionsIndependently() {
        addQuestions(5);
        service.startSession("Afifa", START, new Random(1));

        assertEquals(5, service.startSession("Sayma", START, new Random(1)).size());
    }

    @Test
    void cannotStartANewSessionWhileOneIsUnfinished() {
        addQuestions(12);
        service.startSession("Afifa", START, new Random(1));

        assertThrows(IllegalStateException.class, () -> service.startSession("Afifa", START.plusMinutes(5)));
    }

    @Test
    void dailySessionLimitIsEnforcedAndResetsTomorrow() {
        addQuestions(20);
        playAll(service.startSession("Afifa", START, new Random(1)), "Afifa", START);
        playAll(service.startSession("Afifa", START.plusHours(1), new Random(2)), "Afifa", START.plusHours(1));

        assertThrows(IllegalStateException.class, () -> service.startSession("Afifa", START.plusHours(2)));
        assertEquals(5, service.startSession("Afifa", START.plusDays(1), new Random(3)).size());
    }

    // ---------- answering and the timer ----------

    @Test
    void fastCorrectAnswerEarnsTheBonus() {
        addQuestions(5);
        QuizAttempt a = service.startSession("Afifa", START, new Random(1)).get(0);
        service.startQuestion(a.getId(), "Afifa", START);

        QuizResult result = service.submitAnswer(a.getId(), "Afifa", rightSlot(a), START.plusSeconds(5));

        assertTrue(result.correct());
        assertEquals(3, result.credits());
        assertEquals(new BigDecimal("3.00"), credits.balanceOf("Afifa"));
    }

    @Test
    void slowerCorrectAnswerEarnsTheBaseReward() {
        addQuestions(5);
        QuizAttempt a = service.startSession("Afifa", START, new Random(1)).get(0);
        service.startQuestion(a.getId(), "Afifa", START);

        assertEquals(2, service.submitAnswer(a.getId(), "Afifa", rightSlot(a), START.plusSeconds(15)).credits());
    }

    @Test
    void wrongAnswerEarnsNothing() {
        addQuestions(5);
        QuizAttempt a = service.startSession("Afifa", START, new Random(1)).get(0);
        service.startQuestion(a.getId(), "Afifa", START);
        int wrongSlot = (rightSlot(a) + 1) % 3;

        QuizResult result = service.submitAnswer(a.getId(), "Afifa", wrongSlot, START.plusSeconds(5));

        assertFalse(result.correct());
        assertEquals(0, result.credits());
        assertEquals(new BigDecimal("0.00"), credits.balanceOf("Afifa"));
    }

    @Test
    void answerAfterTheTimerIsRejected() {
        addQuestions(5);
        QuizAttempt a = service.startSession("Afifa", START, new Random(1)).get(0);
        service.startQuestion(a.getId(), "Afifa", START);

        assertThrows(IllegalStateException.class,
                () -> service.submitAnswer(a.getId(), "Afifa", rightSlot(a), START.plusSeconds(30)));
        assertEquals(new BigDecimal("0.00"), credits.balanceOf("Afifa"));
    }

    @Test
    void mustPressStartBeforeAnsweringAndOnlyOnce() {
        addQuestions(5);
        QuizAttempt a = service.startSession("Afifa", START, new Random(1)).get(0);

        assertThrows(IllegalStateException.class, () -> service.submitAnswer(a.getId(), "Afifa", 0, START));
        service.startQuestion(a.getId(), "Afifa", START);
        assertThrows(IllegalStateException.class, () -> service.startQuestion(a.getId(), "Afifa", START.plusSeconds(40)));
    }

    @Test
    void aMemberCanAnswerOnlyOnce() {
        addQuestions(5);
        QuizAttempt a = service.startSession("Afifa", START, new Random(1)).get(0);
        service.startQuestion(a.getId(), "Afifa", START);
        service.submitAnswer(a.getId(), "Afifa", rightSlot(a), START.plusSeconds(5));

        assertThrows(IllegalStateException.class,
                () -> service.submitAnswer(a.getId(), "Afifa", rightSlot(a), START.plusSeconds(6)));
        assertEquals(new BigDecimal("3.00"), credits.balanceOf("Afifa")); // no double reward
    }

    @Test
    void aMemberCannotUseSomeoneElsesQuestion() {
        addQuestions(5);
        QuizAttempt a = service.startSession("Afifa", START, new Random(1)).get(0);

        assertThrows(IllegalStateException.class, () -> service.startQuestion(a.getId(), "Sayma", START));
    }

    @Test
    void choiceMustBeOneOfTheOptions() {
        addQuestions(5);
        QuizAttempt a = service.startSession("Afifa", START, new Random(1)).get(0);
        service.startQuestion(a.getId(), "Afifa", START);

        assertThrows(IllegalArgumentException.class, () -> service.submitAnswer(a.getId(), "Afifa", 3, START.plusSeconds(5)));
        assertThrows(IllegalArgumentException.class, () -> service.submitAnswer(a.getId(), "Afifa", -1, START.plusSeconds(5)));
    }

    // ---------- the page ----------

    @Test
    void viewHidesQuestionsUntilStartedThenShowsTheTimerAndTheSummary() {
        addQuestions(12);
        List<QuizAttempt> session = service.startSession("Afifa", START, new Random(1));

        EcoQuizView before = service.buildView("Afifa", START);
        assertEquals(5, before.getItems().size());
        assertTrue(before.getItems().get(0).isNotStarted());
        assertEquals("Question 1", before.getItems().get(0).getTitle());
        assertTrue(before.getItems().get(0).getOptions().isEmpty());
        assertFalse(before.isCanStartNewSession());

        QuizAttempt first = session.get(0);
        service.startQuestion(first.getId(), "Afifa", START);
        EcoQuizView.Item running = service.buildView("Afifa", START.plusSeconds(5)).getItems().get(0);
        assertTrue(running.isInProgress());
        assertEquals(3, running.getOptions().size());
        assertEquals(15, running.getRemainingSeconds());

        EcoQuizView.Item expired = service.buildView("Afifa", START.plusSeconds(30)).getItems().get(0);
        assertTrue(expired.isTimedOut());
        assertTrue(expired.getResultText().contains("Time ran out"));

        for (int i = 1; i < session.size(); i++) {
            service.startQuestion(session.get(i).getId(), "Afifa", START);
            service.submitAnswer(session.get(i).getId(), "Afifa", rightSlot(session.get(i)), START.plusSeconds(5));
        }
        EcoQuizView done = service.buildView("Afifa", START.plusSeconds(30));
        assertTrue(done.getSummaryText().contains("4 of 5 correct"));
        assertTrue(done.isCanStartNewSession());
    }

    @Test
    void shuffledOptionsKeepTheSameOrderOnRefresh() {
        addQuestions(5);
        QuizAttempt a = service.startSession("Afifa", START, new Random(1)).get(0);
        service.startQuestion(a.getId(), "Afifa", START);

        List<String> one = service.buildView("Afifa", START.plusSeconds(2)).getItems().get(0).getOptions();
        List<String> two = service.buildView("Afifa", START.plusSeconds(4)).getItems().get(0).getOptions();

        assertEquals(one, two);
    }

    // ---------- question bank ----------

    @Test
    void badQuestionsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.addQuestion(" ", List.of("A", "B"), 0, null, 2, 20));
        assertThrows(IllegalArgumentException.class, () -> service.addQuestion("Q?", List.of("A"), 0, null, 2, 20));
        assertThrows(IllegalArgumentException.class, () -> service.addQuestion("Q?", List.of("A", "B"), 5, null, 2, 20));
        assertThrows(IllegalArgumentException.class, () -> service.addQuestion("Q?", List.of("A", "B"), 0, null, 0, 20));
        assertThrows(IllegalArgumentException.class, () -> service.addQuestion("Q?", List.of("A", "B"), 0, null, 2, 1));
    }

    @Test
    void seedingTheBankAddsEveryQuestionOnceAndCanBeRepeated() {
        int bankSize = EcoQuestionBank.all().size();

        assertEquals(bankSize, service.seedQuestionBank());
        assertEquals(0, service.seedQuestionBank()); // nothing is added twice
        assertEquals(bankSize, questions.findAll().size());
        assertTrue(bankSize >= 30);
    }

    @Test
    void everyBankQuestionIsValid() {
        for (EcoQuestionBank.Entry e : EcoQuestionBank.all()) {
            assertDoesNotThrow(() -> service.addQuestion(e.text(), e.options(), 0, e.explanation(), e.reward(), 20), e.text());
        }
    }
}