package com.odyssey.game;

import com.odyssey.core.Engine;
import com.odyssey.world.save.SaveManager;
import com.odyssey.graphics.Renderer;
import com.odyssey.input.InputAbstractionLayer;
import com.odyssey.input.InputAction;
import com.odyssey.ui.UIRenderer;
// import com.odyssey.ui.TextNode; // Disabled to fix compilation
import com.odyssey.ui.MainMenuSystem;
import com.odyssey.events.EventBus;
import com.odyssey.events.EventBus.Subscribe;
import com.odyssey.events.MenuEvent;
import com.odyssey.world.save.SaveData;
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
    private final MainMenuSystem menuSystem;
    private final EventBus eventBus;
    
    public enum StateType {
        MENU, LOADING, PLAYING, PAUSED, INVENTORY, MAP, SETTINGS
    }
    
    public GameStateManager(Engine engine) {
        this.engine = engine;
        this.uiRenderer = new UIRenderer();
        this.inputLayer = new InputAbstractionLayer(engine.getInputManager(), "keybindings.cfg");
        this.menuSystem = MainMenuSystem.getInstance();
        this.eventBus = EventBus.getInstance();
        
        // Register event listeners for menu system integration
        registerMenuEventListeners();
    }
    
    public void initialize() {
        logger.info("Initializing game state manager");
        
        // Initialize UI renderer
        var window = engine.getWindow();
        uiRenderer.initialize(window.getWidth(), window.getHeight());
        inputLayer.initialize();
        
        // Start with main menu to test TextNode implementation
        pushState(new MainMenuState(this));
        logger.info("Game started in MAIN MENU state for TextNode testing");
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
    
    public MainMenuSystem getMenuSystem() {
        return menuSystem;
    }
    
    /**
     * Registers event listeners for menu system integration
     */
    private void registerMenuEventListeners() {
        eventBus.register(this);
    }
    
    @Subscribe
    public void handleGameStarted(MenuEvent.GameStarted event) {
        logger.info("Starting game with type: {}", event.getStartType());
        changeState(new PlayingState(this));
    }
    
    @Subscribe
    public void handleGameSaved(MenuEvent.GameSaved event) {
        logger.info("Game saved to slot: {}", event.getSaveSlot());
        
        try {
            // Get the save manager from the engine
            SaveManager saveManager = engine.getSaveManager();
            if (saveManager == null) {
                logger.error("SaveManager not available for saving game");
                return;
            }
            
            // Create save data from current game state
            SaveData saveData = createSaveData();
            
            // Save the game to the specified slot
            saveManager.saveGame(event.getSaveSlot(), saveData);
            
            logger.info("Game successfully saved to slot: {}", event.getSaveSlot());
            
        } catch (Exception e) {
            logger.error("Failed to save game to slot {}: {}", event.getSaveSlot(), e.getMessage(), e);
        }
    }
    
    @Subscribe
    public void handleGameLoaded(MenuEvent.GameLoaded event) {
        logger.info("Game loaded from slot: {}", event.getSaveSlot());
        
        try {
            // Get the save manager from the engine
            SaveManager saveManager = engine.getSaveManager();
            if (saveManager == null) {
                logger.error("SaveManager not available for loading game");
                return;
            }
            
            // Load the game from the specified slot
            SaveData saveData = saveManager.loadGame(event.getSaveSlot());
            if (saveData == null) {
                logger.error("Failed to load save data from slot: {}", event.getSaveSlot());
                return;
            }
            
            // Apply the loaded save data to the game state
            applySaveData(saveData);
            
            // Change to playing state
            changeState(new PlayingState(this));
            
            logger.info("Game successfully loaded from slot: {}", event.getSaveSlot());
            
        } catch (Exception e) {
            logger.error("Failed to load game from slot {}: {}", event.getSaveSlot(), e.getMessage(), e);
        }
    }
    
    public GameState getCurrentState() {
        return currentState;
    }
    
    public boolean hasStates() {
        return currentState != null;
    }
    
    /**
     * Creates save data from the current game state.
     */
    private SaveData createSaveData() {
        SaveData saveData = new SaveData();
        
        // Save basic game information
        saveData.setTimestamp(System.currentTimeMillis());
        saveData.setGameVersion(engine.getVersion());
        
        // Save player data
        if (engine.getPlayerManager() != null) {
            saveData.setPlayerData(engine.getPlayerManager().getPlayerData());
        }
        
        // Save world data
        if (engine.getWorldManager() != null) {
            saveData.setWorldData(engine.getWorldManager().getWorldData());
        }
        
        // Save ship data
        if (engine.getShipManager() != null) {
            saveData.setShipData(engine.getShipManager().getShipData());
        }
        
        // Save inventory data
        if (engine.getInventoryManager() != null) {
            saveData.setInventoryData(engine.getInventoryManager().getInventoryData());
        }
        
        // Save quest data
        if (engine.getQuestManager() != null) {
            saveData.setQuestData(engine.getQuestManager().getQuestData());
        }
        
        // Save game settings
        if (engine.getGameConfig() != null) {
            saveData.setGameSettings(engine.getGameConfig().getGameSettings());
        }
        
        logger.debug("Created save data with timestamp: {}", saveData.getTimestamp());
        return saveData;
    }
    
    /**
     * Applies loaded save data to the current game state.
     */
    private void applySaveData(SaveData saveData) {
        logger.debug("Applying save data with timestamp: {}", saveData.getTimestamp());
        
        // Apply player data
        if (saveData.getPlayerData() != null && engine.getPlayerManager() != null) {
            engine.getPlayerManager().setPlayerData(saveData.getPlayerData());
        }
        
        // Apply world data
        if (saveData.getWorldData() != null && engine.getWorldManager() != null) {
            engine.getWorldManager().setWorldData(saveData.getWorldData());
        }
        
        // Apply ship data
        if (saveData.getShipData() != null && engine.getShipManager() != null) {
            engine.getShipManager().setShipData(saveData.getShipData());
        }
        
        // Apply inventory data
        if (saveData.getInventoryData() != null && engine.getInventoryManager() != null) {
            engine.getInventoryManager().setInventoryData(saveData.getInventoryData());
        }
        
        // Apply quest data
        if (saveData.getQuestData() != null && engine.getQuestManager() != null) {
            engine.getQuestManager().setQuestData(saveData.getQuestData());
        }
        
        // Apply game settings
        if (saveData.getGameSettings() != null && engine.getConfig() != null) {
            engine.getConfig().applyGameSettings(saveData.getGameSettings());
        }
        
        logger.debug("Successfully applied save data");
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
        // TextNodes disabled to fix compilation
        // private TextNode subtitleTextNode;
        // private TextNode testTextNode;
        private double animationTime = 0.0;
        private double startupTime = 0.0;
        private static final double STARTUP_DELAY = 1.0; // 1 second delay before allowing exit
        
        public MainMenuState(GameStateManager stateManager) {
            super(stateManager);
        }
        
        @Override
        public void initialize() {
            logger.debug("Initialized main menu state");
            // Release mouse capture for menu navigation
            stateManager.getEngine().getInputManager().setMouseCaptured(false);
            
            // TextNodes disabled for compilation - using rectangle-based UI instead
            
            // Open the main menu
            stateManager.getMenuSystem().openMenu("main");
        }
        
        @Override
        public void update(double deltaTime) {
            var inputLayer = stateManager.getInputLayer();
            var menuSystem = stateManager.getMenuSystem();
            
            // Update timing
            animationTime += deltaTime;
            startupTime += deltaTime;
            
            // TextNode animation disabled
            
            // Update menu system
            menuSystem.update();
            
            // Handle input for menu navigation
            if (inputLayer.wasActionJustPressed(InputAction.MOVE_UP)) {
                menuSystem.handleInput("up");
            }
            if (inputLayer.wasActionJustPressed(InputAction.MOVE_DOWN)) {
                menuSystem.handleInput("down");
            }
            if (inputLayer.wasActionJustPressed(InputAction.CONFIRM) || inputLayer.wasActionJustPressed(InputAction.JUMP)) {
                menuSystem.handleInput("select");
            }
            
            // Handle ESC key and controller menu button - exit game from main menu
            // Only allow exit after startup delay to prevent accidental immediate exits
            if (inputLayer.wasActionJustPressed(InputAction.OPEN_MENU) || inputLayer.wasActionJustPressed(InputAction.CANCEL)) {
                if (startupTime <= STARTUP_DELAY) {
                    logger.debug("Exit input detected during startup delay ({:.2f}s < {:.2f}s) - ignoring", 
                        startupTime, STARTUP_DELAY);
                } else {
                    logger.info("Exit requested from main menu - shutting down game");
                    stateManager.getEngine().stop();
                }
            }
        }
        
        @Override
        public void render(Renderer renderer) {
            var uiRenderer = stateManager.getUIRenderer();
            var window = stateManager.getEngine().getWindow();
            var menuSystem = stateManager.getMenuSystem();
            
            // DEBUG: Bright red test rectangle to verify rendering
            uiRenderer.drawRectangle(100, 100, 200, 100, 
                new Vector4f(1.0f, 0.0f, 0.0f, 1.0f));
            
            // Draw background (dark semi-transparent)
            uiRenderer.drawRectangle(0, 0, window.getWidth(), window.getHeight(), 
                new Vector4f(0.0f, 0.1f, 0.2f, 0.8f));
            
            // Draw title
            uiRenderer.drawText("THE ODYSSEY", window.getWidth() / 2f - 200, 100, 48, 
                new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
            
            // Draw subtitle using rectangle-based text
            uiRenderer.drawText("Pirate Adventure Awaits", window.getWidth() / 2f - 150, 150, 24, 
                new Vector4f(0.8f, 0.9f, 1.0f, 1.0f));
            
            // Draw animated test text with color animation
            float r = (float) (0.5f + 0.5f * Math.sin(animationTime * 2.0));
            float g = (float) (0.5f + 0.5f * Math.sin(animationTime * 2.0 + Math.PI / 3));
            float b = (float) (0.5f + 0.5f * Math.sin(animationTime * 2.0 + 2 * Math.PI / 3));
            uiRenderer.drawText("Animated Text Test", window.getWidth() / 2f - 100, 200, 20, 
                new Vector4f(r, g, b, 1.0f));
            
            // Render the current menu
            if (menuSystem.isMenuOpen()) {
                var currentMenu = menuSystem.getCurrentMenu();
                if (currentMenu != null) {
                    // Draw menu background
                    uiRenderer.drawRectangle(window.getWidth() / 2f - 200, 250, 400, 300,
                        new Vector4f(0.1f, 0.1f, 0.1f, 0.9f));
                    
                    // Draw menu title
                    uiRenderer.drawText(currentMenu.getTitle(), window.getWidth() / 2f - 100, 270, 24,
                        new Vector4f(1.0f, 0.8f, 0.2f, 1.0f));
                    
                    // Draw menu items
                    float yOffset = 320;
                    for (var item : currentMenu.getItems()) {
                        if (item.isVisible()) {
                            Vector4f color = item.isEnabled() ? 
                                new Vector4f(0.9f, 0.9f, 0.9f, 1.0f) : 
                                new Vector4f(0.5f, 0.5f, 0.5f, 1.0f);
                            
                            uiRenderer.drawText(item.getTitle(), window.getWidth() / 2f - 180, yOffset, 18, color);
                            yOffset += 30;
                        }
                    }
                }
            }
            
            // Draw controls hint
            uiRenderer.drawText("Use WASD/Arrow Keys or Gamepad to navigate, Enter/A to select", 
                50, window.getHeight() - 50, 16, new Vector4f(0.7f, 0.7f, 0.7f, 1.0f));
        }
        
        private void renderMenu(UIRenderer uiRenderer, MainMenuSystem.Menu menu) {
            if (menu == null) return;
            
            var window = stateManager.getEngine().getWindow();
            float startY = 300;
            
            var items = menu.getItems();
            for (int i = 0; i < items.size(); i++) {
                var item = items.get(i);
                if (!item.isVisible()) continue;
                
                Vector4f color = item.isEnabled() ? 
                    new Vector4f(0.9f, 0.9f, 0.9f, 1.0f) : // Light gray for enabled
                    new Vector4f(0.5f, 0.5f, 0.5f, 1.0f);   // Dark gray for disabled
                
                uiRenderer.drawText(item.getTitle(), window.getWidth() / 2f - 100, startY + i * 60, 32, color);
            }
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
        
        public PauseState(GameStateManager stateManager) {
            super(stateManager);
        }
        
        @Override
        public void initialize() {
            logger.debug("Initialized pause state");
            // Release mouse capture for menu navigation
            stateManager.getEngine().getInputManager().setMouseCaptured(false);
            
            // Open the in-game pause menu
            stateManager.getMenuSystem().openMenu("ingame");
        }
        
        @Override
        public void update(double deltaTime) {
            var inputLayer = stateManager.getInputLayer();
            var menuSystem = stateManager.getMenuSystem();
            
            // Update menu system
            menuSystem.update();
            
            // Handle input for menu navigation
            if (inputLayer.wasActionJustPressed(InputAction.MOVE_UP)) {
                menuSystem.handleInput("up");
            }
            if (inputLayer.wasActionJustPressed(InputAction.MOVE_DOWN)) {
                menuSystem.handleInput("down");
            }
            if (inputLayer.wasActionJustPressed(InputAction.CONFIRM) || inputLayer.wasActionJustPressed(InputAction.JUMP)) {
                menuSystem.handleInput("select");
            }
            
            // Handle ESC key and controller menu button - resume game
            if (inputLayer.wasActionJustPressed(InputAction.OPEN_MENU) ||
                inputLayer.wasActionJustPressed(InputAction.CANCEL)) {
                logger.info("ESC/Menu button pressed in PAUSE MENU - resuming game");
                stateManager.getEngine().getInputManager().setMouseCaptured(true);
                stateManager.popState();
            }
        }
        
        @Override
        public void render(Renderer renderer) {
            var uiRenderer = stateManager.getUIRenderer();
            var window = stateManager.getEngine().getWindow();
            var menuSystem = stateManager.getMenuSystem();
            
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
            
            // Render menu system if a menu is open
            if (menuSystem.isMenuOpen()) {
                renderPauseMenu(uiRenderer, menuSystem.getCurrentMenu(), menuX, menuY);
            }
            
            // Draw controls hint
            uiRenderer.drawText("ESC/Menu Button to Resume", menuX + 50, menuY + menuHeight - 40, 16, 
                new Vector4f(0.7f, 0.7f, 0.7f, 1.0f));
        }
        
        private void renderPauseMenu(UIRenderer uiRenderer, MainMenuSystem.Menu menu, float menuX, float menuY) {
            if (menu == null) return;
            
            float startY = menuY + 100;
            var items = menu.getItems();
            
            for (int i = 0; i < items.size(); i++) {
                var item = items.get(i);
                if (!item.isVisible()) continue;
                
                Vector4f color = item.isEnabled() ? 
                    new Vector4f(0.9f, 0.9f, 0.9f, 1.0f) : // Light gray for enabled
                    new Vector4f(0.5f, 0.5f, 0.5f, 1.0f);   // Dark gray for disabled
                
                uiRenderer.drawText(item.getTitle(), menuX + 50, startY + i * 50, 24, color);
            }
        }
        
        @Override
        public void cleanup() {
            logger.debug("Cleaned up pause state");
        }
    }
}