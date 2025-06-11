package com.gestureai.gameautomation.ai;

import com.gestureai.gameautomation.GameAction;

public class Experience {
    public GameStrategyAgent.UniversalGameState state;
    public GameAction action;
    public float reward;
    public GameStrategyAgent.UniversalGameState nextState;
    public boolean gameOver;

    public Experience(GameStrategyAgent.UniversalGameState state, GameAction action, 
                     float reward, GameStrategyAgent.UniversalGameState nextState, boolean gameOver) {
        this.state = state;
        this.action = action;
        this.reward = reward;
        this.nextState = nextState;
        this.gameOver = gameOver;
    }
}