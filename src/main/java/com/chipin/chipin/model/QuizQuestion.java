package com.chipin.chipin.model;

import jakarta.persistence.*;
import java.util.Arrays;
import java.util.List;

// One eco question in the question bank (Eco Quiz feature).
// The correct answer is stored by its position in optionsText.
@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String questionText;

    // Answer choices joined with "|", e.g. "Recycling bin|Food waste|Landfill"
    @Column(length = 1000)
    private String optionsText;

    private int correctIndex;      // 0 = first option
    @Column(length = 500)
    private String explanation;    // shown after the member has answered
    private int rewardCredits;
    private int secondsToAnswer;   // the per-question timer

    public QuizQuestion() {}

    public List<String> getOptions() { return Arrays.asList(optionsText.split("\\|")); }
    public int getOptionCount() { return getOptions().size(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getOptionsText() { return optionsText; }
    public void setOptionsText(String optionsText) { this.optionsText = optionsText; }
    public int getCorrectIndex() { return correctIndex; }
    public void setCorrectIndex(int correctIndex) { this.correctIndex = correctIndex; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public int getRewardCredits() { return rewardCredits; }
    public void setRewardCredits(int rewardCredits) { this.rewardCredits = rewardCredits; }
    public int getSecondsToAnswer() { return secondsToAnswer; }
    public void setSecondsToAnswer(int secondsToAnswer) { this.secondsToAnswer = secondsToAnswer; }
}