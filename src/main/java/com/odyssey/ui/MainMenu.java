package com.odyssey.ui;

import com.odyssey.rendering.TextRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main menu UI for The Odyssey.
 * Handles the display and interaction of the main menu screen.
 */
public class MainMenu {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MainMenu.class);
    
    private TextRenderer textRenderer;
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
     * Render the main menu.
     */
    public void render(int windowWidth, int windowHeight) {
        if (!initialized) return;
        
        // Render game title
        float[] titleColor = {1.0f, 1.0f, 1.0f}; // White
        float titleX = windowWidth / 2.0f - 200.0f; // Center horizontally
        float titleY = windowHeight / 2.0f + 150.0f; // Upper portion
        textRenderer.renderText("THE ODYSSEY", titleX, titleY, 2.0f, titleColor, windowWidth, windowHeight);
        
        // Render subtitle
        float[] subtitleColor = {0.8f, 0.8f, 0.8f}; // Light gray
        float subtitleX = windowWidth / 2.0f - 180.0f;
        float subtitleY = titleY - 50.0f;
        textRenderer.renderText("Pirate Adventure", subtitleX, subtitleY, 1.0f, subtitleColor, windowWidth, windowHeight);
        
        // Render menu options
        float menuStartY = windowHeight / 2.0f - 50.0f;
        float menuSpacing = 40.0f;
        
        for (int i = 0; i < menuOptions.length; i++) {
            float[] optionColor;
            float scale;
            
            if (i == selectedOption) {
                // Highlight selected option
                optionColor = new float[]{1.0f, 0.8f, 0.2f}; // Golden yellow
                scale = 1.2f;
            } else {
                optionColor = new float[]{0.9f, 0.9f, 0.9f}; // Light gray
                scale = 1.0f;
            }
            
            float optionX = windowWidth / 2.0f - 120.0f;
            float optionY = menuStartY - (i * menuSpacing);
            
            textRenderer.renderText(menuOptions[i], optionX, optionY, scale, optionColor, windowWidth, windowHeight);
        }
        
        // Render instructions
        float[] instructionColor = {0.6f, 0.6f, 0.6f}; // Dark gray
        float instructionX = windowWidth / 2.0f - 150.0f;
        float instructionY = 100.0f;
        textRenderer.renderText("Use ARROW KEYS to navigate, ENTER to select", instructionX, instructionY, 0.7f, instructionColor, windowWidth, windowHeight);
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
        switch (selectedOption) {
            case 0: // Start New Adventure
                LOGGER.info("Starting new adventure...");
                // TODO: Transition to game state
                break;
            case 1: // Load Game
                LOGGER.info("Loading game...");
                // TODO: Show load game screen
                break;
            case 2: // Multiplayer
                LOGGER.info("Opening multiplayer...");
                // TODO: Show multiplayer lobby
                break;
            case 3: // Settings
                LOGGER.info("Opening settings...");
                // TODO: Show settings screen
                break;
            case 4: // Exit
                LOGGER.info("Exiting game...");
                // TODO: Exit game
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