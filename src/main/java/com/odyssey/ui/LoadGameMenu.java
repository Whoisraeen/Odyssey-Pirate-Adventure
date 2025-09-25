package com.odyssey.ui;

import com.odyssey.core.GameEngine;
import com.odyssey.core.GameState;
import com.odyssey.save.SaveManager.SaveInfo;
import com.odyssey.rendering.TextRenderer;
import com.odyssey.save.SaveManager;
import com.odyssey.input.GameAction;
import com.odyssey.input.InputManager;
import com.odyssey.util.Logger;

import java.util.List;
import java.util.ArrayList;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.*;

/**
 * Load Game menu UI for The Odyssey.
 * Handles the display and interaction of the load game screen.
 * 
 * @author Odyssey Development Team
 * @version 1.0
 * @since Java 25
 */
public class LoadGameMenu {
    
    private static final Logger LOGGER = Logger.getLogger(LoadGameMenu.class);
    
    private TextRenderer textRenderer;
    private SaveManager saveManager;
    private GameEngine gameEngine;
    private int selectedSaveIndex = 0;
    private List<SaveInfo> availableSaves;
    private boolean initialized = false;
    private boolean visible = false;
    private boolean isLoading = false;
    private String loadingMessage = "";
    private String errorMessage = "";
    private long loadingStartTime = 0;
    
    // UI constants
    private static final int MAX_VISIBLE_SAVES = 8;
    private static final int SAVE_ITEM_HEIGHT = 40;
    private static final float TITLE_SCALE = 2.0f;
    private static final float SAVE_NAME_SCALE = 1.2f;
    private static final float SAVE_INFO_SCALE = 0.8f;
    
    // Date formatting
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT);
    
    // Overlay rendering constants
    private static final float OVERLAY_ALPHA = 0.7f;
    private static final float PANEL_ALPHA = 0.9f;
    private static final float ERROR_PANEL_ALPHA = 0.95f;
    
    /**
     * Constructor for LoadGameMenu.
     * 
     * @param saveManager The save manager instance
     * @param gameEngine The game engine instance
     */
    public LoadGameMenu(SaveManager saveManager, GameEngine gameEngine) {
        this.saveManager = saveManager;
        this.gameEngine = gameEngine;
        this.availableSaves = new ArrayList<>();
    }
    
    /**
     * Initialize the load game menu.
     */
    public void initialize() {
        if (initialized) {
            LOGGER.warn("LoadGameMenu already initialized");
            return;
        }
        
        LOGGER.info("Initializing LoadGameMenu...");
        
        textRenderer = new TextRenderer();
        textRenderer.initialize();
        
        refreshSaveList();
        
        initialized = true;
        LOGGER.info("LoadGameMenu initialized successfully");
    }
    
    /**
     * Refresh the list of available saves.
     */
    public void refreshSaveList() {
        if (saveManager != null) {
            saveManager.refreshSaveList();
            availableSaves = saveManager.getAvailableSaves();
            
            // Reset selection if it's out of bounds
            if (selectedSaveIndex >= availableSaves.size()) {
                selectedSaveIndex = Math.max(0, availableSaves.size() - 1);
            }
            
            LOGGER.debug("Refreshed save list: {} saves found", availableSaves.size());
        }
    }
    
    /**
     * Show the load game menu.
     */
    public void show() {
        visible = true;
        refreshSaveList();
        LOGGER.debug("Load game menu shown");
    }
    
    /**
     * Updates the load game menu state.
     * 
     * @param deltaTime Time elapsed since last update in seconds
     */
    public void update(double deltaTime) {
        if (!visible || !initialized) {
            return;
        }
        
        // Clear error message after 5 seconds
        if (!errorMessage.isEmpty() && System.currentTimeMillis() - loadingStartTime > 5000) {
            errorMessage = "";
        }
        
        // Don't process input while loading or showing error
        if (isLoading || !errorMessage.isEmpty()) {
            return;
        }
        
        // Process input if gameEngine is available
        if (gameEngine != null && gameEngine.getInputManager() != null) {
            var inputManager = gameEngine.getInputManager();
            
            // Handle navigation input
            if (inputManager.isActionPressed(com.odyssey.input.GameAction.MOVE_FORWARD) || 
                inputManager.isActionPressed(com.odyssey.input.GameAction.TURN_LEFT)) {
                // Navigate up in the save list
                if (selectedSaveIndex > 0) {
                    selectedSaveIndex--;
                }
            }
            
            if (inputManager.isActionPressed(com.odyssey.input.GameAction.MOVE_BACKWARD) || 
                inputManager.isActionPressed(com.odyssey.input.GameAction.TURN_RIGHT)) {
                // Navigate down in the save list
                if (selectedSaveIndex < availableSaves.size() - 1) {
                    selectedSaveIndex++;
                }
            }
            
            // Handle selection input
            if (inputManager.isActionPressed(com.odyssey.input.GameAction.INTERACT) || 
                inputManager.isActionPressed(com.odyssey.input.GameAction.PRIMARY_ACTION)) {
                handleInput(MenuInput.SELECT);
            }
            
            // Handle cancel input
            if (inputManager.isActionPressed(com.odyssey.input.GameAction.MENU)) {
                handleInput(MenuInput.CANCEL);
            }
        }
    }
    
    /**
     * Render the load game menu.
     */
    public void hide() {
        visible = false;
        LOGGER.debug("Load game menu hidden");
    }
    
    /**
     * Check if the menu is visible.
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Render the load game menu with proper overlay rendering.
     * 
     * @param width  The screen width
     * @param height The screen height
     */
    public void render(int width, int height) {
        if (!visible || !initialized) {
            return;
        }
        
        LOGGER.debug("LoadGameMenu.render() called with dimensions: {}x{}", width, height);
        
        if (textRenderer == null) {
            LOGGER.warn("TextRenderer is null in LoadGameMenu.render()");
            return;
        }
        
        try {
            // Set up OpenGL state for overlay rendering
            setupOverlayRendering();
            
            // Render semi-transparent background overlay
            renderBackgroundOverlay(width, height);
            
            // Render main menu panel
            renderMenuPanel(width, height);
            
            // Render content based on current state
            if (isLoading) {
                renderLoadingDialog(width, height);
            } else if (!errorMessage.isEmpty()) {
                renderErrorDialog(width, height);
            } else {
                renderSaveList(width, height);
                renderInstructions(width, height);
            }
            
            // Restore OpenGL state
            restoreRenderingState();
            
            LOGGER.debug("LoadGameMenu text rendering completed successfully");
            
        } catch (Exception e) {
            LOGGER.error("Error rendering load game menu: {}", e.getMessage());
        }
    }
    
    /**
     * Format save information for display with proper date formatting.
     */
    private String formatSaveInfo(SaveInfo save) {
        // Format the last modified date properly
        LocalDateTime lastModified = save.getLastModified();
        String formattedDate = lastModified.format(DATE_FORMATTER);
        
        // Format playtime
        long hours = save.getPlaytimeSeconds() / 3600;
        long minutes = (save.getPlaytimeSeconds() % 3600) / 60;
        
        return String.format("%s - %dh %dm", formattedDate, hours, minutes);
    }
    
    /**
     * Set up OpenGL state for overlay rendering.
     */
    private void setupOverlayRendering() {
        // Enable blending for transparency
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Set up orthographic projection for 2D rendering
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, 1920, 1080, 0, -1, 1); // TODO: Use actual window dimensions
        
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        
        // Disable depth testing for UI elements
        glDisable(GL_DEPTH_TEST);
    }
    
    /**
     * Restore OpenGL state after overlay rendering.
     */
    private void restoreRenderingState() {
        // Restore matrices
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        
        // Re-enable depth testing
        glEnable(GL_DEPTH_TEST);
        
        // Disable blending
        glDisable(GL_BLEND);
    }
    
    /**
     * Render semi-transparent background overlay.
     */
    private void renderBackgroundOverlay(int width, int height) {
        glColor4f(0.0f, 0.0f, 0.0f, OVERLAY_ALPHA);
        glBegin(GL_QUADS);
        glVertex2f(0, 0);
        glVertex2f(width, 0);
        glVertex2f(width, height);
        glVertex2f(0, height);
        glEnd();
    }
    
    /**
     * Render the main menu panel background.
     */
    private void renderMenuPanel(int width, int height) {
        float panelWidth = width * 0.6f;
        float panelHeight = height * 0.8f;
        float panelX = (width - panelWidth) / 2.0f;
        float panelY = (height - panelHeight) / 2.0f;
        
        // Panel background with slight transparency
        glColor4f(0.1f, 0.15f, 0.2f, PANEL_ALPHA);
        glBegin(GL_QUADS);
        glVertex2f(panelX, panelY);
        glVertex2f(panelX + panelWidth, panelY);
        glVertex2f(panelX + panelWidth, panelY + panelHeight);
        glVertex2f(panelX, panelY + panelHeight);
        glEnd();
        
        // Panel border
        glColor4f(0.3f, 0.4f, 0.5f, 1.0f);
        glLineWidth(2.0f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(panelX, panelY);
        glVertex2f(panelX + panelWidth, panelY);
        glVertex2f(panelX + panelWidth, panelY + panelHeight);
        glVertex2f(panelX, panelY + panelHeight);
        glEnd();
    }
    
    /**
     * Render the save list with proper formatting.
     */
    private void renderSaveList(int width, int height) {
        // Render title
        textRenderer.renderText("LOAD GAME", 
                              width / 2 - 100, height / 2 - 300, 
                              TITLE_SCALE, new float[]{1.0f, 1.0f, 1.0f}, 
                              width, height);
        
        // Check if there are any saves
        if (availableSaves.isEmpty()) {
            textRenderer.renderText("No save games found", 
                                  width / 2 - 120, height / 2, 
                                  SAVE_NAME_SCALE, new float[]{0.7f, 0.7f, 0.7f}, 
                                  width, height);
            
            textRenderer.renderText("Press ESC to return to main menu", 
                                  width / 2 - 150, height / 2 + 40, 
                                  SAVE_INFO_SCALE, new float[]{0.5f, 0.5f, 0.5f}, 
                                  width, height);
            return;
        }
        
        // Calculate visible save range
        int startIndex = Math.max(0, selectedSaveIndex - MAX_VISIBLE_SAVES / 2);
        int endIndex = Math.min(availableSaves.size(), startIndex + MAX_VISIBLE_SAVES);
        
        // Adjust start index if we're near the end
        if (endIndex - startIndex < MAX_VISIBLE_SAVES && availableSaves.size() > MAX_VISIBLE_SAVES) {
            startIndex = Math.max(0, endIndex - MAX_VISIBLE_SAVES);
        }
        
        // Render save list
        int yOffset = height / 2 - 100;
        for (int i = startIndex; i < endIndex; i++) {
            SaveInfo save = availableSaves.get(i);
            boolean isSelected = (i == selectedSaveIndex);
            
            // Calculate position
            int itemY = yOffset + (i - startIndex) * SAVE_ITEM_HEIGHT;
            
            // Render selection highlight background
            if (isSelected) {
                glColor4f(0.2f, 0.3f, 0.4f, 0.6f);
                glBegin(GL_QUADS);
                glVertex2f(width / 2 - 220, itemY - 5);
                glVertex2f(width / 2 + 220, itemY - 5);
                glVertex2f(width / 2 + 220, itemY + 35);
                glVertex2f(width / 2 - 220, itemY + 35);
                glEnd();
                
                textRenderer.renderText("> ", 
                                      width / 2 - 200, itemY, 
                                      SAVE_NAME_SCALE, new float[]{1.0f, 1.0f, 0.0f}, 
                                      width, height);
            }
            
            // Render save name
            float[] nameColor = isSelected ? new float[]{1.0f, 1.0f, 1.0f} : new float[]{0.8f, 0.8f, 0.8f};
            textRenderer.renderText(save.getName(), 
                                  width / 2 - 180, itemY, 
                                  SAVE_NAME_SCALE, nameColor, 
                                  width, height);
            
            // Render save info (date and playtime)
            String saveInfo = formatSaveInfo(save);
            float[] infoColor = isSelected ? new float[]{0.9f, 0.9f, 0.9f} : new float[]{0.6f, 0.6f, 0.6f};
            textRenderer.renderText(saveInfo, 
                                  width / 2 - 180, itemY + 20, 
                                  SAVE_INFO_SCALE, infoColor, 
                                  width, height);
        }
        
        // Show scroll indicators if needed
        if (availableSaves.size() > MAX_VISIBLE_SAVES) {
            if (startIndex > 0) {
                textRenderer.renderText("^ More saves above", 
                                      width / 2 - 80, height / 2 - 200, 
                                      SAVE_INFO_SCALE, new float[]{0.5f, 0.5f, 0.5f}, 
                                      width, height);
            }
            if (endIndex < availableSaves.size()) {
                textRenderer.renderText("v More saves below", 
                                      width / 2 - 80, height / 2 + 200, 
                                      SAVE_INFO_SCALE, new float[]{0.5f, 0.5f, 0.5f}, 
                                      width, height);
            }
        }
    }
    
    /**
     * Render instruction text.
     */
    private void renderInstructions(int width, int height) {
        textRenderer.renderText("Use W/S to navigate, ENTER to load, ESC to cancel", 
                              width / 2 - 200, height / 2 + 250, 
                              SAVE_INFO_SCALE, new float[]{0.5f, 0.5f, 0.5f}, 
                              width, height);
    }
    
    /**
     * Render loading dialog with animated progress indicator.
     */
    private void renderLoadingDialog(int width, int height) {
        // Render loading panel background
        float panelWidth = width * 0.4f;
        float panelHeight = height * 0.2f;
        float panelX = (width - panelWidth) / 2.0f;
        float panelY = (height - panelHeight) / 2.0f;
        
        glColor4f(0.05f, 0.1f, 0.15f, PANEL_ALPHA);
        glBegin(GL_QUADS);
        glVertex2f(panelX, panelY);
        glVertex2f(panelX + panelWidth, panelY);
        glVertex2f(panelX + panelWidth, panelY + panelHeight);
        glVertex2f(panelX, panelY + panelHeight);
        glEnd();
        
        // Panel border
        glColor4f(0.0f, 0.8f, 0.0f, 1.0f);
        glLineWidth(2.0f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(panelX, panelY);
        glVertex2f(panelX + panelWidth, panelY);
        glVertex2f(panelX + panelWidth, panelY + panelHeight);
        glVertex2f(panelX, panelY + panelHeight);
        glEnd();
        
        // Loading message
        textRenderer.renderText(loadingMessage, 
                              width / 2 - 100, height / 2 - 20, 
                              SAVE_NAME_SCALE, new float[]{0.0f, 1.0f, 0.0f}, 
                              width, height);
        
        // Loading animation (animated dots)
        long elapsed = System.currentTimeMillis() - loadingStartTime;
        int dots = (int) ((elapsed / 500) % 4); // Change every 500ms
        String animation = ".".repeat(Math.max(1, dots));
        textRenderer.renderText(animation, 
                              width / 2 + 100, height / 2 - 20, 
                              SAVE_NAME_SCALE, new float[]{0.0f, 1.0f, 0.0f}, 
                              width, height);
        
        // Progress bar
        float barWidth = panelWidth * 0.8f;
        float barHeight = 8.0f;
        float barX = panelX + (panelWidth - barWidth) / 2.0f;
        float barY = panelY + panelHeight * 0.7f;
        
        // Progress bar background
        glColor4f(0.2f, 0.2f, 0.2f, 0.8f);
        glBegin(GL_QUADS);
        glVertex2f(barX, barY);
        glVertex2f(barX + barWidth, barY);
        glVertex2f(barX + barWidth, barY + barHeight);
        glVertex2f(barX, barY + barHeight);
        glEnd();
        
        // Animated progress
        float progress = (float) ((elapsed / 100) % (int) barWidth) / barWidth;
        float progressWidth = barWidth * progress;
        
        glColor4f(0.0f, 0.8f, 0.0f, 0.9f);
        glBegin(GL_QUADS);
        glVertex2f(barX, barY);
        glVertex2f(barX + progressWidth, barY);
        glVertex2f(barX + progressWidth, barY + barHeight);
        glVertex2f(barX, barY + barHeight);
        glEnd();
    }
    
    /**
     * Render error dialog with enhanced visual feedback.
     */
    private void renderErrorDialog(int width, int height) {
        // Render error panel background
        float panelWidth = width * 0.5f;
        float panelHeight = height * 0.3f;
        float panelX = (width - panelWidth) / 2.0f;
        float panelY = (height - panelHeight) / 2.0f;
        
        glColor4f(0.2f, 0.05f, 0.05f, ERROR_PANEL_ALPHA);
        glBegin(GL_QUADS);
        glVertex2f(panelX, panelY);
        glVertex2f(panelX + panelWidth, panelY);
        glVertex2f(panelX + panelWidth, panelY + panelHeight);
        glVertex2f(panelX, panelY + panelHeight);
        glEnd();
        
        // Panel border (red)
        glColor4f(1.0f, 0.0f, 0.0f, 1.0f);
        glLineWidth(3.0f);
        glBegin(GL_LINE_LOOP);
        glVertex2f(panelX, panelY);
        glVertex2f(panelX + panelWidth, panelY);
        glVertex2f(panelX + panelWidth, panelY + panelHeight);
        glVertex2f(panelX, panelY + panelHeight);
        glEnd();
        
        // Error title
        textRenderer.renderText("ERROR", 
                              width / 2 - 40, height / 2 - 60, 
                              TITLE_SCALE, new float[]{1.0f, 0.2f, 0.2f}, 
                              width, height);
        
        // Error message (word-wrapped if too long)
        String[] words = errorMessage.split(" ");
        StringBuilder line = new StringBuilder();
        int lineY = height / 2 - 20;
        
        for (String word : words) {
            if (line.length() + word.length() > 50) { // Approximate character limit per line
                textRenderer.renderText(line.toString(), 
                                      width / 2 - 200, lineY, 
                                      SAVE_NAME_SCALE, new float[]{1.0f, 0.8f, 0.8f}, 
                                      width, height);
                line = new StringBuilder(word + " ");
                lineY += 25;
            } else {
                line.append(word).append(" ");
            }
        }
        
        // Render remaining text
        if (line.length() > 0) {
            textRenderer.renderText(line.toString(), 
                                  width / 2 - 200, lineY, 
                                  SAVE_NAME_SCALE, new float[]{1.0f, 0.8f, 0.8f}, 
                                  width, height);
        }
        
        // Instructions
        textRenderer.renderText("Press ESC to continue", 
                              width / 2 - 80, height / 2 + 80, 
                              SAVE_INFO_SCALE, new float[]{0.8f, 0.8f, 0.8f}, 
                              width, height);
    }
    
    /**
     * Handle input for menu navigation.
     */
    public void handleInput(MenuInput input) {
        if (!visible || availableSaves.isEmpty()) {
            return;
        }
        
        switch (input) {
            case UP:
                selectedSaveIndex = Math.max(0, selectedSaveIndex - 1);
                LOGGER.debug("Save selection: {} ({})", selectedSaveIndex, 
                           selectedSaveIndex < availableSaves.size() ? availableSaves.get(selectedSaveIndex).getName() : "none");
                break;
            case DOWN:
                selectedSaveIndex = Math.min(availableSaves.size() - 1, selectedSaveIndex + 1);
                LOGGER.debug("Save selection: {} ({})", selectedSaveIndex, 
                           selectedSaveIndex < availableSaves.size() ? availableSaves.get(selectedSaveIndex).getName() : "none");
                break;
            case SELECT:
                if (selectedSaveIndex >= 0 && selectedSaveIndex < availableSaves.size()) {
                    SaveInfo selectedSave = availableSaves.get(selectedSaveIndex);
                    LOGGER.info("Save selected for loading: {}", selectedSave.getName());
                    handleSaveSelection(selectedSave);
                }
                break;
            case CANCEL:
                LOGGER.info("Load game menu cancelled");
                hide();
                // Return to main menu
                if (gameEngine != null) {
                    gameEngine.setState(GameState.MAIN_MENU);
                }
                break;
        }
    }
    
    /**
     * Handles save selection and loading with comprehensive error handling.
     */
    private void handleSaveSelection(SaveInfo save) {
        if (save == null || isLoading) {
            return;
        }
        
        // Start loading process
        isLoading = true;
        loadingMessage = "Loading " + save.getName() + "...";
        loadingStartTime = System.currentTimeMillis();
        errorMessage = "";
        
        LOGGER.info("Loading save: {}", save.getName());
        
        try {
            // Validate save file before loading
            if (!saveManager.validateSaveFile(save.getName())) {
                throw new RuntimeException("Save file is corrupted or invalid");
            }
            
            // Update loading message
            loadingMessage = "Validating save data...";
            
            // Check if save file exists using SaveManager's method
            Path saveFilePath = saveManager.getSaveFilePath(save.getName());
            if (!java.nio.file.Files.exists(saveFilePath)) {
                throw new RuntimeException("Save file not found");
            }
            
            // Check file permissions
            if (!java.nio.file.Files.isReadable(saveFilePath)) {
                throw new RuntimeException("Cannot read save file - check permissions");
            }
            
            // Simulate loading time for visual feedback
            Thread.sleep(500);
            
            loadingMessage = "Initializing world...";
            Thread.sleep(300);
            
            // Attempt to load the game
            if (gameEngine != null) {
                gameEngine.loadGameFromSave(save.getName());
                
                // Loading successful
                isLoading = false;
                loadingMessage = "";
                
                // Hide the menu and transition to game
                hide();
                
                LOGGER.info("Successfully loaded save: {}", save.getName());
                
            } else {
                throw new RuntimeException("Game engine unavailable");
            }
            
        } catch (SecurityException e) {
            // Loading failed
            isLoading = false;
            loadingMessage = "";
            errorMessage = "Access denied - check file permissions: " + e.getMessage();
            loadingStartTime = System.currentTimeMillis();
            
            LOGGER.error("Security error accessing save file: {}", save.getName(), e);
        } catch (Exception e) {
            // Loading failed
            isLoading = false;
            loadingMessage = "";
            errorMessage = "Failed to load " + save.getName() + ": " + e.getMessage();
            loadingStartTime = System.currentTimeMillis();
            
            LOGGER.error("Error loading save: {}", save.getName(), e);
        }
    }
    
    /**
     * Shows an error message and returns to the main menu.
     * 
     * @param errorMessage The error message to display
     */
    private void showErrorAndReturnToMenu(String errorMessage) {
        // TODO: Show error dialog to user
        LOGGER.error("Load game error: {}", errorMessage);
        
        // Return to main menu
        if (gameEngine != null) {
            gameEngine.setState(GameState.MAIN_MENU);
        }
    }
    
    /**
     * Get the currently selected save.
     */
    public SaveInfo getSelectedSave() {
        if (selectedSaveIndex >= 0 && selectedSaveIndex < availableSaves.size()) {
            return availableSaves.get(selectedSaveIndex);
        }
        return null;
    }
    
    /**
     * Get the currently selected save index.
     */
    public int getSelectedSaveIndex() {
        return selectedSaveIndex;
    }
    
    /**
     * Get the list of available saves.
     */
    public List<SaveInfo> getAvailableSaves() {
        return new ArrayList<>(availableSaves);
    }
    
    /**
     * Clean up resources.
     */
    public void cleanup() {
        if (textRenderer != null) {
            textRenderer.cleanup();
        }
        
        initialized = false;
        visible = false;
        LOGGER.info("LoadGameMenu cleaned up");
    }
    
    /**
     * Input types for menu navigation.
     */
    public enum MenuInput {
        UP,
        DOWN,
        SELECT,
        CANCEL
    }
}