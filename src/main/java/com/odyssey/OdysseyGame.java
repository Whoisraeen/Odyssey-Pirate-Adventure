package com.odyssey;

import com.odyssey.core.GameEngine;
import com.odyssey.core.GameState;
import com.odyssey.core.GameConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Odyssey - Revolutionary Voxel Maritime Adventure Game
 * 
 * Main entry point for The Odyssey game. This class initializes the game engine
 * and manages the primary game lifecycle.
 * 
 * Inspired by the vision of creating a maritime adventure that surpasses
 * traditional voxel games through innovative ocean mechanics, dynamic weather,
 * and immersive ship-building systems.
 * 
 * @author The Odyssey Development Team
 * @version 1.0.0-ALPHA
 */
public class OdysseyGame {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OdysseyGame.class);
    
    private GameEngine gameEngine;
    private GameConfig gameConfig;
    private boolean running = false;
    
    /**
     * Main entry point for The Odyssey game.
     * 
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        LOGGER.info("=== The Odyssey - Starting Maritime Adventure ===");
        LOGGER.info("Navigate the Boundless Azure - Build Your Fleet, Command Your Destiny, Shape the Seven Seas");
        
        try {
            OdysseyGame game = new OdysseyGame();
            game.initialize(args);
            game.run();
        } catch (Exception e) {
            LOGGER.error("Fatal error occurred during game execution", e);
            System.exit(1);
        }
    }
    
    /**
     * Initialize the game with command line arguments and configuration.
     * 
     * @param args Command line arguments
     */
    private void initialize(String[] args) {
        LOGGER.info("Initializing The Odyssey game engine...");
        
        // Load game configuration
        gameConfig = GameConfig.getInstance();
        LOGGER.info("Game configuration loaded: {}x{} resolution, {} mode", 
                   gameConfig.getWindowWidth(), 
                   gameConfig.getWindowHeight(),
                   gameConfig.isFullscreen() ? "fullscreen" : "windowed");
        
        // Initialize game engine
        gameEngine = new GameEngine(gameConfig);
        gameEngine.initialize();
        
        LOGGER.info("Game engine initialized successfully");
    }
    
    /**
     * Main game loop - handles the core game execution cycle.
     */
    private void run() {
        LOGGER.info("Starting main game loop...");
        running = true;
        
        // Initialize game timing
        long lastTime = System.nanoTime();
        double targetFPS = gameConfig.getTargetFPS();
        double nsPerFrame = 1_000_000_000.0 / targetFPS;
        double delta = 0;
        
        int frames = 0;
        long timer = System.currentTimeMillis();
        
        try {
            while (running && !gameEngine.shouldClose()) {
                long currentTime = System.nanoTime();
                delta += (currentTime - lastTime) / nsPerFrame;
                lastTime = currentTime;
                
                // Process input
                gameEngine.processInput();
                
                // Update game logic at fixed timestep
                while (delta >= 1) {
                    update();
                    delta--;
                }
                
                // Render the game
                render();
                frames++;
                
                // FPS counter and performance monitoring
                if (System.currentTimeMillis() - timer >= 1000) {
                    if (gameConfig.isDebugMode()) {
                        LOGGER.debug("FPS: {}, Memory: {} MB", 
                                   frames, 
                                   (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);
                    }
                    frames = 0;
                    timer += 1000;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error in main game loop", e);
        } finally {
            cleanup();
        }
    }
    
    /**
     * Update game logic - called at fixed timestep.
     */
    private void update() {
        gameEngine.update();
        
        // Check for game state changes
        if (gameEngine.getGameState() == GameState.EXITING) {
            running = false;
        }
    }
    
    /**
     * Render the game - called as fast as possible.
     */
    private void render() {
        gameEngine.render();
    }
    
    /**
     * Clean up resources and shutdown the game gracefully.
     */
    private void cleanup() {
        LOGGER.info("Shutting down The Odyssey...");
        
        if (gameEngine != null) {
            gameEngine.cleanup();
        }
        
        LOGGER.info("The Odyssey has been shut down successfully. Fair winds and following seas!");
    }
}