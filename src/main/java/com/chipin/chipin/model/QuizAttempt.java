package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

// One question given to one member inside a session.
// It is created when the session is assigned (startedAt is empty), filled in when the member
// presses Start, and finished when they answer. The unique constraint means a member can
// be given each question only once, so a question is never repeated for them.
@Entity
@Table(name = "quiz_attempts",
        uniqueConstraints = @UniqueConstraint(name = "uk_attempt_question_user", columnNames = {"question_id", "user_id"}))
public class QuizAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long questionId;
    private String userId;
    private int sessionNo;
    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;   // empty until the member presses Start
    private LocalDateTime answeredAt;  // empty until the member answers
    private int choice = -1;           // position in the shuffled list the member saw
    private boolean correct;
    private int creditsEarned;

    public QuizAttempt() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getQuestionId() { return questionId; }
    public void setQuestionId(Long questionId) { this.questionId = questionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public int getSessionNo() { return sessionNo; }
    public void setSessionNo(int sessionNo) { this.sessionNo = sessionNo; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
    public int getChoice() { return choice; }
    public void setChoice(int choice) { this.choice = choice; }
    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }
    public int getCreditsEarned() { return creditsEarned; }
    public void setCreditsEarned(int creditsEarned) { this.creditsEarned = creditsEarned; }
}