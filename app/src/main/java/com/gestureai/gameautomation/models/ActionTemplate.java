package com.gestureai.gameautomation.models;

public class ActionTemplate {
    public String strategyExplanation;
    public String decisionReasoning;
    public String gameContext;
    public String expectedOutcome;
    public String actionType;
    public float confidence;

    public ActionTemplate() {
        this.strategyExplanation = "";
        this.decisionReasoning = "";
        this.gameContext = "";
        this.expectedOutcome = "";
        this.actionType = "";
        this.confidence = 1.0f;
    }

    public ActionTemplate(String strategyExplanation, String decisionReasoning,
                          String gameContext, String expectedOutcome) {
        this.strategyExplanation = strategyExplanation;
        this.decisionReasoning = decisionReasoning;
        this.gameContext = gameContext;
        this.expectedOutcome = expectedOutcome;
        this.confidence = 1.0f;
    }
}