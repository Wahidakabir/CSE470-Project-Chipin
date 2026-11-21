package com.chipin.chipin.service;

import com.chipin.chipin.dto.EcoQuizView;
import com.chipin.chipin.model.QuizAttempt;
import com.chipin.chipin.model.QuizQuestion;
import com.chipin.chipin.repository.QuizAttemptRepository;
import com.chipin.chipin.repository.QuizQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Feature: Eco Quiz and Green Credits.
 *
 * A member starts a session of 5 questions picked from the bank. A question the member has
 * been given before is never picked again. Pressing Start on a question starts a timer kept
 * on the server, and a correct answer in time earns green credits through GreenCreditService.
 * A member can play a limited number of sessions a day.
 */
@Service
public class EcoQuizService {

    public static final int QUESTIONS_PER_SESSION = 5;
    public static final int MAX_SESSIONS_PER_DAY = 2;
    public static final int SECONDS_PER_QUESTION = 15;

    private enum State { ASSIGNED, RUNNING, DONE, TIMED_OUT }

    private final QuizQuestionRepository questionRepository;
    private final QuizAttemptRepository attemptRepository;
    private final GreenCreditService creditService;

    public EcoQuizService(QuizQuestionRepository questionRepository,
                          QuizAttemptRepository attemptRepository,
                          GreenCreditService creditService) {
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.creditService = creditService;
    }

    // ---------- Reading ----------

    public EcoQuizView buildView(String userId, LocalDateTime now) {
        Map<Long, QuizQuestion> questions = questionsById();
        List<QuizAttempt> mine = attemptRepository.findByUserId(userId);
        int sessionNo = latestSession(mine);

        List<QuizAttempt> current = new ArrayList<>();
        for (QuizAttempt a : mine) {
            if (a.getSessionNo() == sessionNo) {
                current.add(a);
            }
        }
        current.sort(Comparator.comparing(QuizAttempt::getId));

        List<EcoQuizView.Item> items = new ArrayList<>();
        int finished = 0;
        int correctCount = 0;
        int creditsEarned = 0;
        for (int i = 0; i < current.size(); i++) {
            QuizAttempt a = current.get(i);
            QuizQuestion q = questions.get(a.getQuestionId());
            State state = stateOf(a, q, now);
            if (state == State.DONE || state == State.TIMED_OUT) {
                finished++;
            }
            if (state == State.DONE && a.isCorrect()) {
                correctCount++;
            }
            creditsEarned += a.getCreditsEarned();
            items.add(buildItem(a, q, i + 1, state, now));
        }

        boolean hasSession = !current.isEmpty();
        boolean complete = hasSession && finished == current.size();
        String summary = complete
                ? "Session complete: " + correctCount + " of " + current.size() + " correct, "
                    + creditsEarned + " green credits earned."
                : null;

        int unseen = questions.size() - mine.size();
        String reason = null;
        if (hasSession && !complete) {
            reason = "Finish every question in this session first.";
        } else if (sessionsToday(mine, now) >= MAX_SESSIONS_PER_DAY) {
            reason = "You have played " + MAX_SESSIONS_PER_DAY + " sessions today. Come back tomorrow.";
        } else if (unseen <= 0) {
            reason = "You have answered every question in the bank. New questions appear when they are added.";
        }

        return new EcoQuizView(creditService.balanceOf(userId), hasSession,
                "Session " + sessionNo,
                finished + " of " + current.size() + " questions finished",
                summary, reason == null, reason, items);
    }

    private EcoQuizView.Item buildItem(QuizAttempt a, QuizQuestion q, int number, State state, LocalDateTime now) {
        boolean reveal = state != State.ASSIGNED;
        String title = reveal ? q.getQuestionText() : "Question " + number;
        List<String> options = state == State.RUNNING ? shuffledOptions(a, q) : List.of();

        int remaining = 0;
        if (state == State.RUNNING) {
            long limitMs = q.getSecondsToAnswer() * 1000L;
            long elapsedMs = Duration.between(a.getStartedAt(), now).toMillis();
            remaining = (int) Math.ceil((limitMs - elapsedMs) / 1000.0);
        }

        String rewardText = q.getRewardCredits() + " credits (+" + QuizScoring.SPEED_BONUS
                + " bonus if answered within " + (q.getSecondsToAnswer() / 2) + " seconds). "
                + "You get " + q.getSecondsToAnswer() + " seconds once you press Start.";

        String resultText = null;
        String explanation = null;
        String rightAnswer = q.getOptions().get(q.getCorrectIndex());
        if (state == State.DONE) {
            resultText = a.isCorrect()
                    ? "Correct. You earned " + a.getCreditsEarned() + " green credits."
                    : "Not correct. The right answer was: " + rightAnswer + ".";
            explanation = q.getExplanation();
        } else if (state == State.TIMED_OUT) {
            resultText = "Time ran out. No credits. The right answer was: " + rightAnswer + ".";
            explanation = q.getExplanation();
        }

        return new EcoQuizView.Item(a.getId(), title, rewardText, options, remaining,
                state == State.ASSIGNED, state == State.RUNNING, state == State.DONE,
                state == State.TIMED_OUT, resultText, explanation);
    }

    // ---------- Writing ----------

    // Gives the member the next session: up to 5 questions they have never been given.
    @Transactional
    public List<QuizAttempt> startSession(String userId, LocalDateTime now) {
        return startSession(userId, now, new Random());
    }

    @Transactional
    public List<QuizAttempt> startSession(String userId, LocalDateTime now, Random random) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Choose a member.");
        }
        Map<Long, QuizQuestion> questions = questionsById();
        List<QuizAttempt> mine = attemptRepository.findByUserId(userId);
        int latest = latestSession(mine);

        for (QuizAttempt a : mine) {
            if (a.getSessionNo() == latest && stateOf(a, questions.get(a.getQuestionId()), now) != State.DONE
                    && stateOf(a, questions.get(a.getQuestionId()), now) != State.TIMED_OUT) {
                throw new IllegalStateException("Finish every question in your current session first.");
            }
        }
        if (sessionsToday(mine, now) >= MAX_SESSIONS_PER_DAY) {
            throw new IllegalStateException("You have played " + MAX_SESSIONS_PER_DAY
                    + " sessions today. Come back tomorrow.");
        }

        Set<Long> seen = new HashSet<>();
        for (QuizAttempt a : mine) {
            seen.add(a.getQuestionId());
        }
        List<QuizQuestion> unseen = new ArrayList<>();
        for (QuizQuestion q : questions.values()) {
            if (!seen.contains(q.getId())) {
                unseen.add(q);
            }
        }
        if (unseen.isEmpty()) {
            throw new IllegalStateException("You have answered every question in the bank. "
                    + "New questions appear when they are added.");
        }

        unseen.sort(Comparator.comparing(QuizQuestion::getId)); // same start, so a seeded Random gives the same picks
        Collections.shuffle(unseen, random);
        int count = Math.min(QUESTIONS_PER_SESSION, unseen.size());

        List<QuizAttempt> created = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            QuizAttempt attempt = new QuizAttempt();
            attempt.setQuestionId(unseen.get(i).getId());
            attempt.setUserId(userId);
            attempt.setSessionNo(latest + 1);
            attempt.setAssignedAt(now);
            created.add(attemptRepository.save(attempt));
        }
        return created;
    }

    // Starts the timer on one question of the member's session. It can be started only once.
    @Transactional
    public QuizAttempt startQuestion(Long attemptId, String userId, LocalDateTime now) {
        QuizAttempt attempt = ownedAttempt(attemptId, userId);
        if (attempt.getStartedAt() != null) {
            throw new IllegalStateException("You have already started this question.");
        }
        attempt.setStartedAt(now);
        return attemptRepository.save(attempt);
    }

    @Transactional
    public QuizResult submitAnswer(Long attemptId, String userId, int choice, LocalDateTime now) {
        QuizAttempt attempt = ownedAttempt(attemptId, userId);
        QuizQuestion q = questionRepository.findById(attempt.getQuestionId())
                .orElseThrow(() -> new IllegalArgumentException("Question not found."));
        if (attempt.getStartedAt() == null) {
            throw new IllegalStateException("Press Start before answering.");
        }
        if (attempt.getAnsweredAt() != null) {
            throw new IllegalStateException("You have already answered this question.");
        }
        if (!QuizScoring.isInTime(attempt.getStartedAt(), now, q.getSecondsToAnswer())) {
            throw new IllegalStateException("Time is up. No credits for this question.");
        }
        if (choice < 0 || choice >= q.getOptionCount()) {
            throw new IllegalArgumentException("Choose one of the listed answers.");
        }

        // The member picked a slot in their shuffled list; turn it back into the original position.
        int original = QuizShuffle.order(attempt.getId(), q.getOptionCount()).get(choice);
        boolean correct = original == q.getCorrectIndex();
        int credits = QuizScoring.creditsFor(correct, attempt.getStartedAt(), now,
                q.getSecondsToAnswer(), q.getRewardCredits());

        attempt.setChoice(choice);
        attempt.setCorrect(correct);
        attempt.setCreditsEarned(credits);
        attempt.setAnsweredAt(now);
        attemptRepository.save(attempt);

        if (credits > 0) {
            creditService.award(userId, BigDecimal.valueOf(credits), "QUIZ_CORRECT");
        }
        return new QuizResult(correct, credits);
    }

    @Transactional
    public QuizQuestion addQuestion(String text, List<String> options, int correctIndex, String explanation,
                                    int reward, int secondsToAnswer) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Write the question.");
        }
        if (options == null || options.size() < 2 || options.size() > 4
                || options.stream().anyMatch(o -> o == null || o.isBlank() || o.contains("|"))) {
            throw new IllegalArgumentException("Give 2 to 4 answer choices.");
        }
        if (correctIndex < 0 || correctIndex >= options.size()) {
            throw new IllegalArgumentException("The correct answer must be one of the choices.");
        }
        if (reward <= 0) {
            throw new IllegalArgumentException("Reward must be at least 1 credit.");
        }
        if (secondsToAnswer < 5 || secondsToAnswer > 120) {
            throw new IllegalArgumentException("The time to answer must be between 5 and 120 seconds.");
        }

        QuizQuestion q = new QuizQuestion();
        q.setQuestionText(text.trim());
        q.setOptionsText(String.join("|", options));
        q.setCorrectIndex(correctIndex);
        q.setExplanation(explanation);
        q.setRewardCredits(reward);
        q.setSecondsToAnswer(secondsToAnswer);
        return questionRepository.save(q);
    }

    // Adds every question from EcoQuestionBank that is not in the database yet. Safe to run on every start.
    @Transactional
    public int seedQuestionBank() {
        Set<String> existing = new HashSet<>();
        for (QuizQuestion q : questionRepository.findAll()) {
            existing.add(q.getQuestionText());
        }
        int added = 0;
        for (EcoQuestionBank.Entry e : EcoQuestionBank.all()) {
            if (existing.contains(e.text())) {
                continue;
            }
            addQuestion(e.text(), e.options(), 0, e.explanation(), e.reward(), SECONDS_PER_QUESTION);
            added++;
        }
        return added;
    }

    // ---------- Helpers ----------

    private QuizAttempt ownedAttempt(Long attemptId, String userId) {
        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found."));
        if (userId == null || !userId.equals(attempt.getUserId())) {
            throw new IllegalStateException("This question belongs to another member.");
        }
        return attempt;
    }

    private Map<Long, QuizQuestion> questionsById() {
        Map<Long, QuizQuestion> map = new HashMap<>();
        for (QuizQuestion q : questionRepository.findAll()) {
            map.put(q.getId(), q);
        }
        return map;
    }

    private State stateOf(QuizAttempt a, QuizQuestion q, LocalDateTime now) {
        if (a.getAnsweredAt() != null) {
            return State.DONE;
        }
        if (a.getStartedAt() == null) {
            return State.ASSIGNED;
        }
        long elapsedMs = Duration.between(a.getStartedAt(), now).toMillis();
        return elapsedMs <= q.getSecondsToAnswer() * 1000L ? State.RUNNING : State.TIMED_OUT;
    }

    private int latestSession(List<QuizAttempt> attempts) {
        int latest = 0;
        for (QuizAttempt a : attempts) {
            latest = Math.max(latest, a.getSessionNo());
        }
        return latest;
    }

    // How many different sessions were given to the member on the same calendar day as "now".
    private int sessionsToday(List<QuizAttempt> attempts, LocalDateTime now) {
        Set<Integer> today = new HashSet<>();
        for (QuizAttempt a : attempts) {
            if (a.getAssignedAt().toLocalDate().equals(now.toLocalDate())) {
                today.add(a.getSessionNo());
            }
        }
        return today.size();
    }

    private List<String> shuffledOptions(QuizAttempt a, QuizQuestion q) {
        List<String> original = q.getOptions();
        List<String> shuffled = new ArrayList<>();
        for (int position : QuizShuffle.order(a.getId(), original.size())) {
            shuffled.add(original.get(position));
        }
        return shuffled;
    }
}