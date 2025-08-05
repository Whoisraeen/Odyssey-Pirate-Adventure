package com.odyssey.game;

import com.odyssey.graphics.Renderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Game state management system for The Odyssey.
 * Handles different game states like menu, playing, paused, etc.
 */
public class GameStateManager {
    private static final Logger logger = LoggerFactory.getLogger(GameStateManager.class);
    
    private final Stack<GameState> stateStack = new Stack<>();
    private GameState currentState;
    
    public enum StateType {
        MENU, LOADING, PLAYING, PAUSED, INVENTORY, MAP, SETTINGS
    }
    
    public void initialize() {
        logger.info("Initializing game state manager");
        
        // Start with playing state for now
        // In a full implementation, this would start with a menu state
        pushState(new PlayingState());
    }
    
    public void update(double deltaTime) {
        if (currentState != null) {
            currentState.update(deltaTime);
        }
    }
    
    public void render(Renderer renderer) {
        if (currentState != null) {
            currentState.render(renderer);
        }
    }
    
    public void pushState(GameState state) {
        if (currentState != null) {
            stateStack.push(currentState);
        }
        currentState = state;
        currentState.initialize();
        logger.debug("Pushed game state: {}", state.getClass().getSimpleName());
    }
    
    public void popState() {
        if (currentState != null) {
            currentState.cleanup();
        }
        
        if (!stateStack.isEmpty()) {
            currentState = stateStack.pop();
            logger.debug("Popped to game state: {}", currentState.getClass().getSimpleName());
        } else {
            currentState = null;
            logger.debug("No more game states in stack");
        }
    }
    
    public void changeState(GameState newState) {
        if (currentState != null) {
            currentState.cleanup();
        }
        
        stateStack.clear();
        currentState = newState;
        currentState.initialize();
        logger.debug("Changed to game state: {}", newState.getClass().getSimpleName());
    }
    
    public void cleanup() {
        logger.info("Cleaning up game state manager");
        
        if (currentState != null) {
            currentState.cleanup();
        }
        
        while (!stateStack.isEmpty()) {
            stateStack.pop().cleanup();
        }
    }
    
    public GameState getCurrentState() {
        return currentState;
    }
    
    public boolean hasStates() {
        return currentState != null;
    }
    
    // Basic game state implementations
    public static abstract class GameState {
        public abstract void initialize();
        public abstract void update(double deltaTime);
        public abstract void render(Renderer renderer);
        public abstract void cleanup();
    }
    
    public static class PlayingState extends GameState {
        private static final Logger logger = LoggerFactory.getLogger(PlayingState.class);
        
        @Override
        public void initialize() {
            logger.debug("Initialized playing state");
        }
        
        @Override
        public void update(double deltaTime) {
            // Update game logic
        }
        
        @Override
        public void render(Renderer renderer) {
            // Render game UI
        }
        
        @Override
        public void cleanup() {
            logger.debug("Cleaned up playing state");
        }
    }
    
    public static class MenuState extends GameState {
        private static final Logger logger = LoggerFactory.getLogger(MenuState.class);
        
        @Override
        public void initialize() {
            logger.debug("Initialized menu state");
        }
        
        @Override
        public void update(double deltaTime) {
            // Update menu logic
        }
        
        @Override
        public void render(Renderer renderer) {
            // Render menu UI
        }
        
        @Override
        public void cleanup() {
            logger.debug("Cleaned up menu state");
        }
    }
}