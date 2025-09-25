package com.odyssey.ui;

import com.odyssey.core.GameEngine;
import com.odyssey.core.GameState;
import com.odyssey.rendering.TextRenderer;
import com.odyssey.util.Logger;

/**
 * Main menu UI for The Odyssey.
 * Handles the display and interaction of the main menu screen.
 */
public class MainMenu {
    
    private static final Logger LOGGER = Logger.getLogger(MainMenu.class);
    
    private TextRenderer textRenderer;
    private GameEngine gameEngine;
    private int selectedOption = 0;
    private String[] menuOptions = {
        "Start New Adventure",
        "Load Game",
        "Multiplayer",
        "Settings",
        "Exit"
    };
    
    private boolean initialized = false;
    
    /**
     * Constructor for MainMenu.
     * 
     * @param gameEngine The game engine instance for state transitions
     */
    public MainMenu(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }
    
    /**
     * Default constructor for backward compatibility.
     */
    public MainMenu() {
        this.gameEngine = null;
    }
    
    /**
     * Set the game engine reference.
     * 
     * @param gameEngine The game engine instance
     */
    public void setGameEngine(GameEngine gameEngine) {
        this.gameEngine = gameEngine;
    }
    
    /**
     * Initialize the main menu.
     */
    public void initialize() {
        if (initialized) {
            LOGGER.warn("MainMenu already initialized");
            return;
        }
        
        LOGGER.info("Initializing MainMenu...");
        
        textRenderer = new TextRenderer();
        textRenderer.initialize();
        
        initialized = true;
        LOGGER.info("MainMenu initialized successfully");
    }
    
    /**
     * Render the main menu UI.
     * 
     * @param width  The screen width
     * @param height The screen height
     */
    public void render(int width, int height) {
        LOGGER.info("MainMenu.render() called with dimensions: " + width + "x" + height);
        
        if (textRenderer == null) {
            LOGGER.warn("TextRenderer is null in MainMenu.render()");
            return;
        }
        
        try {
            // Render main menu title
            textRenderer.renderText("THE ODYSSEY", 
                                  width / 2 - 120, height / 2 + 100, 
                                  2.0f, new float[]{1.0f, 1.0f, 1.0f}, 
                                  width, height);
            
            // Render menu options
            textRenderer.renderText("New Game", 
                                  width / 2 - 60, height / 2 + 20, 
                                  1.0f, new float[]{0.8f, 0.8f, 0.8f}, 
                                  width, height);
            
            textRenderer.renderText("Load Game", 
                                  width / 2 - 60, height / 2 - 20, 
                                  1.0f, new float[]{0.8f, 0.8f, 0.8f}, 
                                  width, height);
            
            textRenderer.renderText("Settings", 
                                  width / 2 - 60, height / 2 - 60, 
                                  1.0f, new float[]{0.8f, 0.8f, 0.8f}, 
                                  width, height);
            
            textRenderer.renderText("Exit", 
                                  width / 2 - 60, height / 2 - 100, 
                                  1.0f, new float[]{0.8f, 0.8f, 0.8f}, 
                                  width, height);
            
            LOGGER.info("MainMenu text rendering completed successfully");
            
        } catch (Exception e) {
            LOGGER.error("Error rendering main menu: " + e.getMessage());
        }
    }
    
    /**
     * Handle input for menu navigation.
     */
    public void handleInput(MenuInput input) {
        switch (input) {
            case UP:
                selectedOption = (selectedOption - 1 + menuOptions.length) % menuOptions.length;
                LOGGER.debug("Menu selection: {}", menuOptions[selectedOption]);
                break;
            case DOWN:
                selectedOption = (selectedOption + 1) % menuOptions.length;
                LOGGER.debug("Menu selection: {}", menuOptions[selectedOption]);
                break;
            case SELECT:
                LOGGER.info("Menu option selected: {}", menuOptions[selectedOption]);
                handleMenuSelection();
                break;
        }
    }
    
    /**
     * Handle menu selection.
     */
    private void handleMenuSelection() {
        if (gameEngine == null) {
            LOGGER.warn("GameEngine reference is null, cannot handle menu selection");
            return;
        }
        
        switch (selectedOption) {
            case 0: // Start New Adventure
                LOGGER.info("Starting new adventure...");
                gameEngine.setState(GameState.LOADING);
                break;
            case 1: // Load Game
                LOGGER.info("Loading game...");
                gameEngine.setState(GameState.LOAD_GAME);
                break;
            case 2: // Multiplayer
                LOGGER.info("Opening multiplayer...");
                gameEngine.setState(GameState.MULTIPLAYER_LOBBY);
                break;
            case 3: // Settings
                LOGGER.info("Opening settings...");
                gameEngine.setState(GameState.SETTINGS);
                break;
            case 4: // Exit
                LOGGER.info("Exiting game...");
                gameEngine.setState(GameState.EXITING);
                break;
        }
    }
    
    /**
     * Get the currently selected menu option.
     */
    public int getSelectedOption() {
        return selectedOption;
    }
    
    /**
     * Get the menu option text for the selected index.
     */
    public String getSelectedOptionText() {
        return menuOptions[selectedOption];
    }
    
    /**
     * Clean up resources.
     */
    public void cleanup() {
        if (textRenderer != null) {
            textRenderer.cleanup();
        }
        
        initialized = false;
        LOGGER.info("MainMenu cleaned up");
    }
    
    /**
     * Input types for menu navigation.
     */
    public enum MenuInput {
        UP,
        DOWN,
        SELECT
    }
}