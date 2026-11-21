package com.chipin.chipin.dto;

import java.math.BigDecimal;
import java.util.List;

// Everything the Eco Quiz page shows for one member.
// The page only reads these values, so it makes no decisions of its own.
public class EcoQuizView {

    private final BigDecimal balance;
    private final boolean hasSession;
    private final String sessionTitle;
    private final String progressText;
    private final String summaryText;          // empty until every question in the session is finished
    private final boolean canStartNewSession;
    private final String startBlockedReason;   // why a new session cannot start, or null
    private final List<Item> items;

    public EcoQuizView(BigDecimal balance, boolean hasSession, String sessionTitle, String progressText,
                       String summaryText, boolean canStartNewSession, String startBlockedReason,
                       List<Item> items) {
        this.balance = balance;
        this.hasSession = hasSession;
        this.sessionTitle = sessionTitle;
        this.progressText = progressText;
        this.summaryText = summaryText;
        this.canStartNewSession = canStartNewSession;
        this.startBlockedReason = startBlockedReason;
        this.items = items;
    }

    public BigDecimal getBalance() { return balance; }
    public boolean isHasSession() { return hasSession; }
    public String getSessionTitle() { return sessionTitle; }
    public String getProgressText() { return progressText; }
    public String getSummaryText() { return summaryText; }
    public boolean isCanStartNewSession() { return canStartNewSession; }
    public String getStartBlockedReason() { return startBlockedReason; }
    public List<Item> getItems() { return items; }

    // One question of the current session, as the page shows it.
    public static class Item {
        private final Long attemptId;
        private final String title;          // "Question 2" until the member starts it
        private final String rewardText;
        private final List<String> options;  // shuffled, and empty until the member starts it
        private final int remainingSeconds;
        private final boolean notStarted;
        private final boolean inProgress;
        private final boolean answered;
        private final boolean timedOut;
        private final String resultText;
        private final String explanation;

        public Item(Long attemptId, String title, String rewardText, List<String> options,
                    int remainingSeconds, boolean notStarted, boolean inProgress, boolean answered,
                    boolean timedOut, String resultText, String explanation) {
            this.attemptId = attemptId;
            this.title = title;
            this.rewardText = rewardText;
            this.options = options;
            this.remainingSeconds = remainingSeconds;
            this.notStarted = notStarted;
            this.inProgress = inProgress;
            this.answered = answered;
            this.timedOut = timedOut;
            this.resultText = resultText;
            this.explanation = explanation;
        }

        public Long getAttemptId() { return attemptId; }
        public String getTitle() { return title; }
        public String getRewardText() { return rewardText; }
        public List<String> getOptions() { return options; }
        public int getRemainingSeconds() { return remainingSeconds; }
        public boolean isNotStarted() { return notStarted; }
        public boolean isInProgress() { return inProgress; }
        public boolean isAnswered() { return answered; }
        public boolean isTimedOut() { return timedOut; }
        public String getResultText() { return resultText; }
        public String getExplanation() { return explanation; }
    }
}