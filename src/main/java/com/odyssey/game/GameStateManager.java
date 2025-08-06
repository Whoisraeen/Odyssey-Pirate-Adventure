package com.odyssey.game;

import com.odyssey.core.Engine;
import com.odyssey.graphics.Renderer;
import com.odyssey.input.InputAbstractionLayer;
import com.odyssey.input.InputAction;
import com.odyssey.ui.UIRenderer;
import org.joml.Vector4f;
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
    private final Engine engine;
    private final UIRenderer uiRenderer;
    private final InputAbstractionLayer inputLayer;
    
    public enum StateType {
        MENU, LOADING, PLAYING, PAUSED, INVENTORY, MAP, SETTINGS
    }
    
    public GameStateManager(Engine engine) {
        this.engine = engine;
        this.uiRenderer = new UIRenderer();
        this.inputLayer = new InputAbstractionLayer(engine.getInputManager(), "keybindings.cfg");
    }
    
    public void initialize() {
        logger.info("Initializing game state manager");
        
        // Initialize UI renderer
        var window = engine.getWindow();
        uiRenderer.initialize(window.getWidth(), window.getHeight());
        inputLayer.initialize();
        
        // Skip main menu for development - start directly in playing state
        pushState(new PlayingState(this));
        logger.info("Game started in PLAYING state (main menu temporarily disabled for development)");
    }
    
    public void update(double deltaTime) {
        // Update input layer
        inputLayer.update();
        
        if (currentState != null) {
            currentState.update(deltaTime);
        }
    }
    
    public void render(Renderer renderer) {
        if (currentState != null) {
            // Begin UI rendering
            uiRenderer.beginFrame();
            
            // Render current state
            currentState.render(renderer);
            
            // End UI rendering
            uiRenderer.endFrame();
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
        
        uiRenderer.cleanup();
    }
    
    // Getters
    public Engine getEngine() { return engine; }
    public UIRenderer getUIRenderer() { return uiRenderer; }
    public InputAbstractionLayer getInputLayer() { return inputLayer; }
    
    public GameState getCurrentState() {
        return currentState;
    }
    
    public boolean hasStates() {
        return currentState != null;
    }
    
    // Game state implementations
    public static abstract class GameState {
        protected GameStateManager stateManager;
        
        public GameState(GameStateManager stateManager) {
            this.stateManager = stateManager;
        }
        
        public abstract void initialize();
        public abstract void update(double deltaTime);
        public abstract void render(Renderer renderer);
        public abstract void cleanup();
    }
    
    public static class MainMenuState extends GameState {
        private static final Logger logger = LoggerFactory.getLogger(MainMenuState.class);
        private int selectedOption = 0;
        private final String[] menuOptions = {"Start Game", "Settings", "Exit"};
        
        public MainMenuState(GameStateManager stateManager) {
            super(stateManager);
        }
        
        @Override
        public void initialize() {
            logger.debug("Initialized main menu state");
            // Release mouse capture for menu navigation
            stateManager.getEngine().getInputManager().setMouseCaptured(false);
        }
        
        @Override
        public void update(double deltaTime) {
            var inputLayer = stateManager.getInputLayer();
            
            // Navigate menu with keyboard/gamepad
            if (inputLayer.wasActionJustPressed(InputAction.MOVE_UP) || 
                inputLayer.wasActionJustPressed(InputAction.LOOK_VERTICAL) && inputLayer.getActionValue(InputAction.LOOK_VERTICAL) < -0.5f) {
                logger.debug("Menu navigation UP detected");
                selectedOption = Math.max(0, selectedOption - 1);
            }
            if (inputLayer.wasActionJustPressed(InputAction.MOVE_DOWN) || 
                inputLayer.wasActionJustPressed(InputAction.LOOK_VERTICAL) && inputLayer.getActionValue(InputAction.LOOK_VERTICAL) > 0.5f) {
                logger.debug("Menu navigation DOWN detected");
                selectedOption = Math.min(menuOptions.length - 1, selectedOption + 1);
            }
            
            // Select option
            if (inputLayer.wasActionJustPressed(InputAction.CONFIRM) || 
                inputLayer.wasActionJustPressed(InputAction.JUMP)) {
                logger.debug("Menu CONFIRM detected for option: {}", selectedOption);
                handleMenuSelection();
            }
            
            // Handle ESC key and controller menu button - exit game from main menu
            if (inputLayer.wasActionJustPressed(InputAction.OPEN_MENU) ||
                inputLayer.wasActionJustPressed(InputAction.CANCEL)) {
                logger.info("ESC/Menu button pressed in MAIN MENU - exiting game (this is correct behavior)");
                logger.info("To test pause menu: Start the game first, then press ESC during gameplay");
                stateManager.getEngine().stop();
            }
        }
        
        private void handleMenuSelection() {
            switch (selectedOption) {
                case 0: // Start Game
                    stateManager.changeState(new PlayingState(stateManager));
                    break;
                case 1: // Settings
                    // TODO: Implement settings menu
                    logger.info("Settings menu not yet implemented");
                    break;
                case 2: // Exit
                    stateManager.getEngine().stop();
                    break;
            }
        }
        
        @Override
        public void render(Renderer renderer) {
            logger.info("MainMenuState.render() called");
            var uiRenderer = stateManager.getUIRenderer();
            var window = stateManager.getEngine().getWindow();
            
            logger.info("Rendering main menu for window size: {}x{}", window.getWidth(), window.getHeight());
            
            // Draw background (dark semi-transparent)
            uiRenderer.drawRectangle(0, 0, window.getWidth(), window.getHeight(), 
                new Vector4f(0.0f, 0.1f, 0.2f, 0.8f));
            
            // Add some bright test rectangles to ensure UI is visible
            uiRenderer.drawRectangle(100, 100, 200, 50, new Vector4f(1.0f, 0.0f, 0.0f, 1.0f)); // Red test
            uiRenderer.drawRectangle(100, 200, 200, 50, new Vector4f(0.0f, 1.0f, 0.0f, 1.0f)); // Green test
            uiRenderer.drawRectangle(100, 300, 200, 50, new Vector4f(0.0f, 0.0f, 1.0f, 1.0f)); // Blue test
            
            // Draw title
            uiRenderer.drawText("THE ODYSSEY", window.getWidth() / 2f - 200, 100, 48, 
                new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
            
            // Draw subtitle
            uiRenderer.drawText("Navigate the Boundless Azure", window.getWidth() / 2f - 150, 160, 24, 
                new Vector4f(0.8f, 0.9f, 1.0f, 1.0f));
            
            // Draw menu options
            float startY = 300;
            for (int i = 0; i < menuOptions.length; i++) {
                Vector4f color = (i == selectedOption) ? 
                    new Vector4f(1.0f, 1.0f, 0.0f, 1.0f) : // Yellow for selected
                    new Vector4f(0.9f, 0.9f, 0.9f, 1.0f);   // Light gray for others
                
                logger.info("Drawing menu option {}: '{}' at y={}", i, menuOptions[i], startY + i * 60);
                uiRenderer.drawText(menuOptions[i], window.getWidth() / 2f - 100, startY + i * 60, 32, color);
            }
            
            // Draw controls hint
            uiRenderer.drawText("Use WASD/Arrow Keys or Gamepad to navigate, Enter/A to select", 
                50, window.getHeight() - 50, 16, new Vector4f(0.7f, 0.7f, 0.7f, 1.0f));
            
            logger.info("MainMenuState.render() completed - added {} UI elements", 1 + 1 + menuOptions.length + 1);
        }
        
        @Override
        public void cleanup() {
            logger.debug("Cleaned up main menu state");
        }
    }
    
    public static class PlayingState extends GameState {
        private static final Logger logger = LoggerFactory.getLogger(PlayingState.class);
        
        public PlayingState(GameStateManager stateManager) {
            super(stateManager);
        }
        
        @Override
        public void initialize() {
            logger.debug("Initialized playing state");
        }
        
        @Override
        public void update(double deltaTime) {
            var inputLayer = stateManager.getInputLayer();
            
            // Check for pause/menu input
            if (inputLayer.wasActionJustPressed(InputAction.OPEN_MENU)) {
                logger.info("ESC/Menu button pressed in PLAYING STATE - opening pause menu");
                stateManager.getEngine().getInputManager().setMouseCaptured(false);
                stateManager.pushState(new PauseState(stateManager));
            }
        }
        
        @Override
        public void render(Renderer renderer) {
            // Game UI rendering (minimal for now)
            var uiRenderer = stateManager.getUIRenderer();
            
            // Draw crosshair if mouse is captured
            if (stateManager.getEngine().getInputManager().isMouseCaptured()) {
                var window = stateManager.getEngine().getWindow();
                float centerX = window.getWidth() / 2f;
                float centerY = window.getHeight() / 2f;
                
                // Simple crosshair
                uiRenderer.drawRectangle(centerX - 10, centerY - 1, 20, 2, 
                    new Vector4f(1.0f, 1.0f, 1.0f, 0.8f));
                uiRenderer.drawRectangle(centerX - 1, centerY - 10, 2, 20, 
                    new Vector4f(1.0f, 1.0f, 1.0f, 0.8f));
            }
        }
        
        @Override
        public void cleanup() {
            logger.debug("Cleaned up playing state");
        }
    }
    
    public static class PauseState extends GameState {
        private static final Logger logger = LoggerFactory.getLogger(PauseState.class);
        private int selectedOption = 0;
        private final String[] menuOptions = {"Resume", "Settings", "Main Menu", "Exit Game"};
        
        public PauseState(GameStateManager stateManager) {
            super(stateManager);
        }
        
        @Override
        public void initialize() {
            logger.debug("Initialized pause state");
            // Release mouse capture for menu navigation
            stateManager.getEngine().getInputManager().setMouseCaptured(false);
        }
        
        @Override
        public void update(double deltaTime) {
            var inputLayer = stateManager.getInputLayer();
            
            // Navigate menu
            if (inputLayer.wasActionJustPressed(InputAction.MOVE_UP) || 
                inputLayer.wasActionJustPressed(InputAction.LOOK_VERTICAL) && inputLayer.getActionValue(InputAction.LOOK_VERTICAL) < -0.5f) {
                selectedOption = Math.max(0, selectedOption - 1);
            }
            if (inputLayer.wasActionJustPressed(InputAction.MOVE_DOWN) || 
                inputLayer.wasActionJustPressed(InputAction.LOOK_VERTICAL) && inputLayer.getActionValue(InputAction.LOOK_VERTICAL) > 0.5f) {
                selectedOption = Math.min(menuOptions.length - 1, selectedOption + 1);
            }
            
            // Select option or resume with menu button
            if (inputLayer.wasActionJustPressed(InputAction.CONFIRM) || 
                inputLayer.wasActionJustPressed(InputAction.JUMP)) {
                handleMenuSelection();
            } else if (inputLayer.wasActionJustPressed(InputAction.OPEN_MENU) ||
                       inputLayer.wasActionJustPressed(InputAction.CANCEL)) {
                // Handle ESC key and controller menu button - resume game
                logger.info("ESC/Menu button pressed in PAUSE MENU - resuming game");
                stateManager.getEngine().getInputManager().setMouseCaptured(true);
                stateManager.popState();
            }
        }
        
        private void handleMenuSelection() {
            switch (selectedOption) {
                case 0: // Resume
                    stateManager.popState();
                    break;
                case 1: // Settings
                    // TODO: Implement settings menu
                    logger.info("Settings menu not yet implemented");
                    break;
                case 2: // Main Menu
                    stateManager.changeState(new MainMenuState(stateManager));
                    break;
                case 3: // Exit Game
                    stateManager.getEngine().stop();
                    break;
            }
        }
        
        @Override
        public void render(Renderer renderer) {
            var uiRenderer = stateManager.getUIRenderer();
            var window = stateManager.getEngine().getWindow();
            
            // Draw semi-transparent overlay
            uiRenderer.drawRectangle(0, 0, window.getWidth(), window.getHeight(), 
                new Vector4f(0.0f, 0.0f, 0.0f, 0.5f));
            
            // Draw pause menu background
            float menuWidth = 400;
            float menuHeight = 350;
            float menuX = (window.getWidth() - menuWidth) / 2f;
            float menuY = (window.getHeight() - menuHeight) / 2f;
            
            uiRenderer.drawRectangle(menuX, menuY, menuWidth, menuHeight, 
                new Vector4f(0.2f, 0.3f, 0.5f, 0.9f));
            
            // Draw title
            uiRenderer.drawText("PAUSED", menuX + 150, menuY + 30, 36, 
                new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
            
            // Draw menu options
            float startY = menuY + 100;
            for (int i = 0; i < menuOptions.length; i++) {
                Vector4f color = (i == selectedOption) ? 
                    new Vector4f(1.0f, 1.0f, 0.0f, 1.0f) : // Yellow for selected
                    new Vector4f(0.9f, 0.9f, 0.9f, 1.0f);   // Light gray for others
                
                uiRenderer.drawText(menuOptions[i], menuX + 50, startY + i * 50, 24, color);
            }
            
            // Draw controls hint
            uiRenderer.drawText("ESC/Menu Button to Resume", menuX + 50, menuY + menuHeight - 40, 16, 
                new Vector4f(0.7f, 0.7f, 0.7f, 1.0f));
        }
        
        @Override
        public void cleanup() {
            logger.debug("Cleaned up pause state");
        }
    }
}