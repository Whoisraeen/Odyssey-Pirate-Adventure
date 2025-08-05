package com.odyssey.game;

import com.odyssey.core.GameConfig;
import com.odyssey.graphics.Renderer;
import com.odyssey.input.InputManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Game state management for The Odyssey.
 */
public class GameState {
    private static final Logger logger = LoggerFactory.getLogger(GameState.class);
    
    private final GameConfig config;
    
    public GameState(GameConfig config) {
        this.config = config;
    }
    
    public void initialize() {
        logger.info("Initializing game state");
    }
    
    public void update(double deltaTime, InputManager input) {
        // Update game logic
    }
    
    public void render(Renderer renderer) {
        // Render game UI and overlays
    }
    
    public void cleanup() {
        logger.info("Cleaning up game state");
    }
}