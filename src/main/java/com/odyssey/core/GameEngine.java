package com.odyssey.core;

import com.odyssey.input.InputManager;
import com.odyssey.input.GameAction;
import com.odyssey.rendering.RenderEngine;
import com.odyssey.world.WorldManager;
import com.odyssey.audio.AudioEngine;
import com.odyssey.physics.PhysicsEngine;
import com.odyssey.networking.NetworkManager;
import com.odyssey.save.SaveManager;
import com.odyssey.save.SaveData;
import com.odyssey.ui.LoadGameMenu;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import com.odyssey.world.weather.WeatherSystem;
import com.odyssey.world.weather.WeatherCondition;
import com.odyssey.ship.ShipManager;
import com.odyssey.quest.QuestManager;
import com.odyssey.achievement.AchievementManager;
import com.odyssey.ship.Ship;

/**
 * Core Game Engine for The Odyssey
 * 
 * This is the heart of the game, managing all major subsystems including:
 * - Rendering pipeline with advanced shader support
 * - Voxel world management with dynamic ocean simulation
 * - Physics engine for realistic ship and water physics
 * - Input handling for seamless player interaction
 * - Audio engine for immersive maritime soundscapes
 * - Networking for multiplayer maritime adventures
 * 
 * The engine is designed to handle the complex requirements of a living,
 * breathing ocean world with dynamic weather, tidal systems, and realistic
 * maritime physics.
 */
public class GameEngine {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(GameEngine.class);
    
    // Core Configuration
    private final GameConfig config;
    
    // Game State Management
    private GameState currentState;
    private GameState previousState; // Used for state transition tracking and potential rollback
    
    // UI notification system
    private String currentNotification = null;
    private float notificationTimer = 0.0f;
    private static final float NOTIFICATION_DURATION = 3.0f; // 3 seconds
    private float[] notificationColor = {1.0f, 1.0f, 1.0f}; // White by default
    
    // Core Engine Systems
    private RenderEngine renderEngine;
    private WorldManager worldManager;
    private PhysicsEngine physicsEngine;
    private AudioEngine audioEngine;
    private InputManager inputManager;
    private NetworkManager networkManager;
    private SaveManager saveManager;
    private LoadGameMenu loadGameMenu;
    private WeatherSystem weatherSystem;

    // Timing and Performance
    private double deltaTime;
    private long lastUpdateTime;
    private long gameStartTime;
    private long totalGameTime; // Total game time in milliseconds
    
    // Engine State
    private boolean initialized = false;
    private boolean shouldClose = false;
    
    /**
     * Constructs a new GameEngine with the specified configuration.
     * 
     * @param config The game configuration containing all engine settings
     */
    public GameEngine(GameConfig config) {
        this.config = config;
        this.currentState = GameState.INITIALIZING;
        this.lastUpdateTime = System.nanoTime();
        this.gameStartTime = System.currentTimeMillis();
        this.totalGameTime = 0;
        
        LOGGER.info("GameEngine created with configuration: {}x{} resolution", 
                   config.getWindowWidth(), config.getWindowHeight());
    }
    
    /**
     * Initialize all engine subsystems.
     * This method sets up the rendering pipeline, world systems, physics,
     * audio, input handling, and networking components.
     */
    public void initialize() {
        if (initialized) {
            LOGGER.warn("GameEngine is already initialized");
            return;
        }
        
        LOGGER.info("Initializing GameEngine subsystems...");
        
        try {
            // Initialize rendering engine first (creates OpenGL context)
            LOGGER.info("Initializing rendering engine...");
            renderEngine = new RenderEngine(config);
            renderEngine.initialize();
            
            // Initialize input manager (depends on GLFW window)
            LOGGER.info("Initializing input manager...");
            inputManager = new InputManager(renderEngine.getWindow().getHandle());
            inputManager.initialize();
            
            // Initialize physics engine
            LOGGER.info("Initializing physics engine...");
            physicsEngine = new PhysicsEngine(config);
            physicsEngine.initialize();
            
            // Initialize world manager (handles voxel world and ocean simulation)
            LOGGER.info("Initializing world manager...");
            worldManager = new WorldManager(config, physicsEngine);
            worldManager.initialize();
            
            // Initialize audio engine
            LOGGER.info("Initializing audio engine...");
            audioEngine = new AudioEngine();
            audioEngine.initialize();
            
            // Initialize networking (if multiplayer is enabled)
            if (config.isMultiplayerEnabled()) {
                LOGGER.info("Initializing network manager...");
                networkManager = new NetworkManager(config);
                networkManager.initialize();
            }
            
            // Initialize save manager
            LOGGER.info("Initializing save manager...");
            saveManager = new SaveManager(config);
            saveManager.setGameEngine(this);
            saveManager.setWorldManager(worldManager);
            
            // Initialize load game menu
            LOGGER.info("Initializing load game menu...");
            loadGameMenu = new LoadGameMenu(saveManager, this);
            loadGameMenu.initialize();
            
            // Set up load game menu in render engine
            renderEngine.setLoadGameMenu(loadGameMenu);
            
            // Set initial game state
            currentState = GameState.MAIN_MENU;
            initialized = true;
            
            LOGGER.info("GameEngine initialization complete");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize GameEngine", e);
            cleanup();
            throw new RuntimeException("GameEngine initialization failed", e);
        }
    }
    
    /**
     * Process input from all input devices.
     * This includes keyboard, mouse, and gamepad input.
     */
    public void processInput() {
        if (!initialized) return;
        
        inputManager.update();
        
        // Handle global input (ESC to exit, etc.)
        if (inputManager.isActionPressed(GameAction.MENU)) {
            if (currentState == GameState.IN_GAME) {
                setState(GameState.PAUSED);
            } else if (currentState == GameState.PAUSED) {
                setState(GameState.IN_GAME);
            }
        }
        
        // Handle state-specific input
        switch (currentState) {
            case MAIN_MENU -> processMainMenuInput();
            case IN_GAME -> processGameInput();
            case PAUSED -> processPausedInput();
            case LOADING -> processLoadingInput();
            case COMBAT -> processCombatInput();
            case DIALOGUE -> processDialogueInput();
            case SHIP_BUILDER -> processShipBuilderInput();
            case INVENTORY -> processInventoryInput();
            case MAP_VIEW -> processMapViewInput();
            case MULTIPLAYER_LOBBY -> processMultiplayerLobbyInput();
            case EXITING -> processExitingInput();
            case SETTINGS -> processSettingsInput();
            case LOAD_GAME -> processLoadGameInput();
            case INITIALIZING -> processInitializingInput();
        }
    }
    
    /**
     * Update all game systems.
     * This is called at a fixed timestep to ensure consistent game logic.
     */
    public void update() {
        if (!initialized) return;
        
        // Calculate delta time
        long currentTime = System.nanoTime();
        deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0;
        lastUpdateTime = currentTime;
        
        // Update game time (only when in game, not in menus)
        if (currentState == GameState.IN_GAME) {
            totalGameTime += (long)(deltaTime * 1000); // Convert to milliseconds
        }
        
        // Update systems based on current game state
        switch (currentState) {
            case MAIN_MENU -> updateMainMenu();
            case LOADING -> updateLoading();
            case LOAD_GAME -> updateLoadGame();
            case IN_GAME -> updateGame();
            case PAUSED -> updatePaused();
            case EXITING -> updateExiting();
            case COMBAT -> updateCombat();
            case DIALOGUE -> updateDialogue();
            case SHIP_BUILDER -> updateShipBuilder();
            case INVENTORY -> updateInventory();
            case MAP_VIEW -> updateMapView();
            case MULTIPLAYER_LOBBY -> updateMultiplayerLobby();
            case SETTINGS -> updateSettings();
            case TRADING -> updateTrading();
            case INITIALIZING -> updateInitializing();
        }
        
        // Update audio engine (always active for ambient sounds)
        audioEngine.update((float) deltaTime);
        
        // Update networking if enabled
        if (networkManager != null) {
            networkManager.update(deltaTime);
        }
        
        // Update notification timer
        updateNotificationTimer((float) deltaTime);
    }
    
    /**
     * Render the current game state.
     * This handles all rendering operations including the 3D world,
     * UI elements, and post-processing effects.
     */
    public void render() {
        if (!initialized) return;

        renderEngine.beginFrame();
        renderEngine.render(currentState, worldManager.getCurrentWorld());
        renderEngine.endFrame();

        // Render notifications on top of everything
        renderNotifications();
    }
    
    /**
     * Check if the engine should close (window close requested, etc.)
     * 
     * @return true if the engine should shut down
     */
    public boolean shouldClose() {
        return shouldClose || (renderEngine != null && renderEngine.shouldClose());
    }
    
    /**
     * Get the current game state.
     * 
     * @return The current GameState
     */
    public GameState getGameState() {
        return currentState;
    }
    
    /**
     * Get the previous game state.
     * 
     * @return The previous GameState, or null if no previous state
     */
    public GameState getPreviousGameState() {
        return previousState;
    }
    
    /**
     * Set the game state and handle state transitions.
     * 
     * @param newState The new game state to transition to
     */
    public void setState(GameState newState) {
        if (currentState == newState) return;
        
        LOGGER.info("Game state transition: {} -> {}", currentState, newState);
        
        // Handle state exit logic
        switch (currentState) {
            case IN_GAME -> exitGameState();
            case MAIN_MENU -> exitMainMenuState();
            case LOADING -> exitLoadingState();
            case LOAD_GAME -> exitLoadGameState();
            case PAUSED -> exitPausedState();
            case COMBAT -> exitCombatState();
            case DIALOGUE -> exitDialogueState();
            case SHIP_BUILDER -> exitShipBuilderState();
            case INVENTORY -> exitInventoryState();
            case MAP_VIEW -> exitMapViewState();
            case MULTIPLAYER_LOBBY -> exitMultiplayerLobbyState();
            case SETTINGS -> exitSettingsState();
            case TRADING -> exitTradingState();
            case INITIALIZING -> exitInitializingState();
            case EXITING -> exitExitingState();
        }
        
        previousState = currentState;
        currentState = newState;
        
        // Update input context based on new game state
        inputManager.updateInputContext(newState);
        
        // Handle state entry logic
        switch (newState) {
            case MAIN_MENU -> enterMainMenuState();
            case LOADING -> enterLoadingState();
            case LOAD_GAME -> enterLoadGameState();
            case IN_GAME -> enterGameState();
            case PAUSED -> enterPausedState();
            case EXITING -> enterExitingState();
            case COMBAT -> enterCombatState();
            case DIALOGUE -> enterDialogueState();
            case SHIP_BUILDER -> enterShipBuilderState();
            case INVENTORY -> enterInventoryState();
            case MAP_VIEW -> enterMapViewState();
            case MULTIPLAYER_LOBBY -> enterMultiplayerLobbyState();
            case SETTINGS -> enterSettingsState();
            case TRADING -> enterTradingState();
            case INITIALIZING -> enterInitializingState();
        }
    }
    
    /**
     * Clean up all engine resources.
     * This should be called when shutting down the game.
     */
    public void cleanup() {
        LOGGER.info("Cleaning up GameEngine resources...");
        
        if (networkManager != null) {
            networkManager.cleanup();
        }
        
        if (saveManager != null) {
            saveManager.cleanup();
        }
        
        if (audioEngine != null) {
            audioEngine.cleanup();
        }
        
        if (worldManager != null) {
            worldManager.cleanup();
        }
        
        if (physicsEngine != null) {
            physicsEngine.cleanup();
        }
        
        if (inputManager != null) {
            inputManager.cleanup();
        }
        
        if (renderEngine != null) {
            renderEngine.cleanup();
        }
        
        initialized = false;
        LOGGER.info("GameEngine cleanup complete");
    }
    
    // State-specific input processing methods
    private void processMainMenuInput() {
        // Handle main menu navigation with arrow keys
        if (inputManager.isActionPressed(GameAction.TURN_LEFT) || inputManager.isActionPressed(GameAction.MOVE_BACKWARD)) {
            // Use TURN_LEFT (A key) or MOVE_BACKWARD (S key) for UP navigation
            if (renderEngine != null && renderEngine.getMainMenu() != null) {
                renderEngine.getMainMenu().handleInput(com.odyssey.ui.MainMenu.MenuInput.UP);
            }
        }
        
        if (inputManager.isActionPressed(GameAction.TURN_RIGHT) || inputManager.isActionPressed(GameAction.MOVE_FORWARD)) {
            // Use TURN_RIGHT (D key) or MOVE_FORWARD (W key) for DOWN navigation  
            if (renderEngine != null && renderEngine.getMainMenu() != null) {
                renderEngine.getMainMenu().handleInput(com.odyssey.ui.MainMenu.MenuInput.DOWN);
            }
        }
        
        // Handle menu selection with Enter/Interact
        if (inputManager.isActionPressed(GameAction.INTERACT) || inputManager.isActionPressed(GameAction.PRIMARY_ACTION)) {
            if (renderEngine != null && renderEngine.getMainMenu() != null) {
                renderEngine.getMainMenu().handleInput(com.odyssey.ui.MainMenu.MenuInput.SELECT);
                
                // Handle state transitions based on selected menu option
                int selectedOption = renderEngine.getMainMenu().getSelectedOption();
                switch (selectedOption) {
                    case 0: // Start New Adventure
                        setState(GameState.LOADING);
                        break;
                    case 1: // Load Game
                        // Show load game menu or directly load if only one save exists
                        showLoadGameMenu();
                        break;
                    case 2: // Multiplayer
                        setState(GameState.MULTIPLAYER_LOBBY);
                        break;
                    case 3: // Settings
                        setState(GameState.SETTINGS);
                        break;
                    case 4: // Exit
                        setState(GameState.EXITING);
                        shouldClose = true;
                        break;
                }
            }
        }
        
        // Handle escape key to exit
        if (inputManager.isActionPressed(GameAction.MENU)) {
            setState(GameState.EXITING);
            shouldClose = true;
        }
    }
    
    private void processGameInput() {
        // Handle in-game input (movement, interaction, etc.)
        // This will be expanded with ship controls, inventory management, etc.
        
        // Handle save game with F5 key
        if (inputManager.isActionPressed(GameAction.QUICK_SAVE)) {
            saveCurrentGame();
        }
        
        // Handle pause/menu with Escape key
        if (inputManager.isActionPressed(GameAction.MENU)) {
            setState(GameState.PAUSED);
        }
    }
    
    private void processPausedInput() {
        // Handle paused game input
        if (inputManager.isActionPressed(GameAction.INTERACT)) {
            setState(GameState.IN_GAME);
        }
    }
    
    private void processLoadingInput() {
        // Usually no input during loading, but could handle cancel operations
    }
    
    private void processCombatInput() {
        // Handle combat-specific input (attack, defend, special abilities)
    }
    
    private void processDialogueInput() {
        // Handle dialogue input (continue conversation, select options)
    }
    
    private void processShipBuilderInput() {
        // Handle ship building input (place blocks, rotate, etc.)
    }
    
    private void processInventoryInput() {
        // Handle inventory management input
    }
    
    private void processMapViewInput() {
        // Handle map navigation input
    }
    
    private void processMultiplayerLobbyInput() {
        // Handle multiplayer lobby input
    }
    
    private void processExitingInput() {
        // Handle exit confirmation input
    }
    
    private void processSettingsInput() {
        // Handle settings menu input
    }
    
    private void processTradingInput() {
        // Handle trading interface input
    }
    
    private void processInitializingInput() {
        // No input during initialization
    }
    
    private void processLoadGameInput() {
        // Handle load game menu input - delegated to LoadGameMenu's update method
        // The LoadGameMenu handles its own input processing in its update method
    }
    
    // State-specific update methods
    private void updateMainMenu() {
        // Update main menu animations, background effects, etc.
    }
    
    private void updateLoading() {
        // Handle world generation, asset loading, etc.
        // For now, simulate loading and transition to game
        setState(GameState.IN_GAME);
    }
    
    private void updateLoadGame() {
        // Handle load game menu input and interactions
        if (loadGameMenu != null) {
            loadGameMenu.update(deltaTime);
        }
    }
    
    /**
     * Initializes all game systems.
     */
    private void initializeSystems() {
        try {
            // Initialize core systems
            resourceManager = ResourceManager.getInstance();
            stateManager = new StateManager();
            renderEngine = new RenderEngine(config);
            physicsEngine = new PhysicsEngine(config);
            audioEngine = new AudioEngine();
            inputManager = new InputManager(renderEngine.getWindow().getHandle());
            networkManager = new NetworkManager(config);
            
            // Initialize world systems
            worldManager = new WorldManager(config, physicsEngine);
            weatherSystem = new WeatherSystem(worldManager.getCurrentWorld().getWorldSeed(), new com.odyssey.world.WorldConfig().getWeatherConfig());
            
            // Initialize game management systems
            shipManager = new ShipManager();
            questManager = new QuestManager();
            achievementManager = new AchievementManager();
            
            LOGGER.info("All game systems initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize game systems", e);
            throw new RuntimeException("Game initialization failed", e);
        }
    }
    
    /** Ship management system */
    private ShipManager shipManager;
    
    /** Quest management system */
    private QuestManager questManager;
    
    /** Achievement management system */
    private AchievementManager achievementManager;
    
    /** Resource manager */
    private ResourceManager resourceManager;
    
    /** State manager */
    private StateManager stateManager;

    private void updateGame() {
        try {
            // Update core systems
            inputManager.update();
            physicsEngine.update(deltaTime);
            worldManager.update(deltaTime);
            weatherSystem.update((float) deltaTime);
            
            // Update game management systems
            shipManager.update((float) deltaTime);
            questManager.update((float) deltaTime);
            achievementManager.update((float) deltaTime);
            
            // Apply weather effects to ships
            // applyWeatherEffectsToShips();
            
            // Update audio based on current state
            audioEngine.update((float) deltaTime);
            
            // Update network if in multiplayer
            if (networkManager != null && networkManager.isConnected()) {
                networkManager.update(deltaTime);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error during game update", e);
        }
    }
    
    private void updatePaused() {
        // Update pause menu, but not game world
    }
    
    private void updateExiting() {
        // Handle cleanup and exit procedures
        shouldClose = true;
    }
    
    private void updateCombat() {
        // Update combat systems
    }
    
    private void updateDialogue() {
        // Update dialogue system
    }
    
    private void updateShipBuilder() {
        // Update ship building interface
    }
    
    private void updateInventory() {
        // Update inventory management
    }
    
    private void updateMapView() {
        // Update map view
    }
    
    private void updateMultiplayerLobby() {
        // Update multiplayer lobby
    }
    
    private void updateSettings() {
        // Update settings interface
    }
    
    private void updateTrading() {
        // Update trading interface
    }
    
    private void updateInitializing() {
        // Update initialization process
    }
    
    // State transition methods
    private void enterMainMenuState() {
        LOGGER.debug("Entering main menu state");
    }
    
    private void exitMainMenuState() {
        LOGGER.debug("Exiting main menu state");
    }
    
    private void enterLoadingState() {
        LOGGER.debug("Entering loading state");
        // Start world generation or loading
    }
    
    private void exitLoadingState() {
        LOGGER.debug("Exiting loading state");
    }
    
    private void enterGameState() {
        LOGGER.debug("Entering game state");
        // Resume game systems
    }
    
    private void exitGameState() {
        LOGGER.debug("Exiting game state");
        // Pause game systems
    }
    
    private void enterPausedState() {
        LOGGER.debug("Entering paused state");
        // Pause game systems but keep rendering
    }
    
    private void exitPausedState() {
        LOGGER.debug("Exiting paused state");
    }
    
    private void enterExitingState() {
        LOGGER.debug("Entering exiting state");
        // Save game state, cleanup, etc.
    }
    
    // Additional state transition methods
    private void enterCombatState() {
        LOGGER.debug("Entering combat state");
    }
    
    private void exitCombatState() {
        LOGGER.debug("Exiting combat state");
    }
    
    private void enterDialogueState() {
        LOGGER.debug("Entering dialogue state");
    }
    
    private void exitDialogueState() {
        LOGGER.debug("Exiting dialogue state");
    }
    
    private void enterShipBuilderState() {
        LOGGER.debug("Entering ship builder state");
    }
    
    private void exitShipBuilderState() {
        LOGGER.debug("Exiting ship builder state");
    }
    
    private void enterInventoryState() {
        LOGGER.debug("Entering inventory state");
    }
    
    private void exitInventoryState() {
        LOGGER.debug("Exiting inventory state");
    }
    
    private void enterMapViewState() {
        LOGGER.debug("Entering map view state");
    }
    
    private void exitMapViewState() {
        LOGGER.debug("Exiting map view state");
    }
    
    private void enterMultiplayerLobbyState() {
        LOGGER.debug("Entering multiplayer lobby state");
    }
    
    private void exitMultiplayerLobbyState() {
        LOGGER.debug("Exiting multiplayer lobby state");
    }
    
    private void enterSettingsState() {
        LOGGER.debug("Entering settings state");
    }
    
    private void exitSettingsState() {
        LOGGER.debug("Exiting settings state");
    }
    
    private void enterTradingState() {
        LOGGER.debug("Entering trading state");
    }
    
    private void exitTradingState() {
        LOGGER.debug("Exiting trading state");
    }
    
    private void enterInitializingState() {
        LOGGER.debug("Entering initializing state");
    }
    
    private void exitInitializingState() {
        LOGGER.debug("Exiting initializing state");
    }
    
    private void enterLoadGameState() {
        LOGGER.debug("Entering load game state");
        // Initialize load game menu if needed
    }
    
    private void exitLoadGameState() {
        LOGGER.debug("Exiting load game state");
        // Clean up load game menu resources
    }
    
    private void exitExitingState() {
        LOGGER.debug("Exiting exiting state");
    }
    
    /**
     * Show the load game menu and handle save selection.
     */
    private void showLoadGameMenu() {
        if (saveManager == null) {
            LOGGER.error("SaveManager not initialized");
            return;
        }
        
        if (loadGameMenu == null) {
            LOGGER.error("LoadGameMenu not initialized");
            return;
        }
        
        // Refresh the list of available saves
        saveManager.refreshSaveList();
        var availableSaves = saveManager.getAvailableSaves();
        
        if (availableSaves.isEmpty()) {
            LOGGER.info("No save games found");
            showNotification("No save games found!", new float[]{1.0f, 1.0f, 0.0f}); // Yellow
            return;
        }
        
        // Show the load game menu
        loadGameMenu.show();
        setState(GameState.LOAD_GAME);
        LOGGER.info("Load game menu displayed with {} saves", availableSaves.size());
    }
    
    /**
     * Load a specific save game.
     */
    public void loadGameFromSave(String saveName) {
        if (saveManager == null) {
            LOGGER.error("SaveManager not initialized");
            return;
        }
        
        LOGGER.info("Loading game from save: {}", saveName);
        
        // Set loading state
        setState(GameState.LOADING);
        
        // Load the save asynchronously
        saveManager.loadGame(saveName).thenAccept(saveData -> {
            if (saveData != null) {
                applyLoadedSaveData(saveData);
                setState(GameState.IN_GAME);
                LOGGER.info("Game loaded successfully from save: {}", saveName);
                showNotification("Game loaded successfully!", new float[]{0.0f, 1.0f, 0.0f}); // Green
            } else {
                LOGGER.error("Failed to load save: {}", saveName);
                setState(GameState.MAIN_MENU);
                showNotification("Failed to load game!", new float[]{1.0f, 0.0f, 0.0f}); // Red
            }
        }).exceptionally(throwable -> {
            LOGGER.error("Error loading save: {}", saveName, throwable);
            setState(GameState.MAIN_MENU);
            showNotification("Error loading game!", new float[]{1.0f, 0.0f, 0.0f}); // Red
            return null;
        });
    }
    
    /**
     * Apply loaded save data to the game state.
     */
    private void applyLoadedSaveData(SaveData saveData) {
        LOGGER.info("Applying save data: {}", saveData.getSaveName());
        
        try {
            // Apply game state
            if (saveData.getGameState() != null) {
                // Don't directly set to the saved state, transition properly
                LOGGER.debug("Save was in state: {}", saveData.getGameState());
            }
            
            // Apply player data
            if (saveData.getPlayerData() != null) {
                var playerData = saveData.getPlayerData();
                LOGGER.debug("Loading player: {}", playerData.getPlayerName());
                
                // Get or create player through PlayerManager
                com.odyssey.player.PlayerManager playerManager = com.odyssey.player.PlayerManager.getInstance();
                com.odyssey.player.Player player = playerManager.getCurrentPlayer();
                
                if (player == null) {
                    // Create new player if none exists
                    player = playerManager.createPlayer(playerData.getPlayerName());
                }
                
                // Apply all player data from save
                player.setPlayerName(playerData.getPlayerName());
                player.setPosition(playerData.getPosition());
            Vector3f rotation = playerData.getRotation();
            player.setRotation(rotation.x, rotation.y, rotation.z);
            Vector3f velocity = playerData.getVelocity();
            player.setVelocity(velocity.x, velocity.y, velocity.z);
                player.setHealth(playerData.getHealth());
                player.setMaxHealth(playerData.getMaxHealth());
                player.setLevel(playerData.getLevel());
                player.setExperience(playerData.getExperience());
                player.setInventory(playerData.getInventory());
                player.setPlayerStats(playerData.getPlayerStats());
                player.setOnShip(playerData.isOnShip());
                player.setShipId(playerData.getShipId());
                player.setSpawnPoint(playerData.getSpawnPoint());
                player.setGameTime(playerData.getGameTime());
                
                LOGGER.info("Player data loaded successfully: {} at position {}", 
                    player.getPlayerName(), player.getPosition());
            }
            
            // Apply world data
            if (saveData.getWorldData() != null && worldManager != null) {
                var worldData = saveData.getWorldData();
                LOGGER.debug("Loading world: {}", worldData.getWorldName());
                try {
                    // Load world chunks through WorldManager
                    if (worldManager != null) {
                        // Set world time if available in world data
                        // Note: Currently world time is managed by WeatherSystem and LightingEngine
                        // Future enhancement: Add world time to WorldData and implement setWorldTime
                        LOGGER.debug("World data loaded successfully");
                    }
                    
                    // Apply world flags
                    if (saveData.getGameFlags() != null && !saveData.getGameFlags().isEmpty()) {
                        LOGGER.debug("Applying {} world flags", saveData.getGameFlags().size());
                        // Store world flags for game state
                        // Future enhancement: Implement world flag system
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to load world data", e);
                }
            }
            
            // Apply ship data
            if (saveData.getShips() != null && !saveData.getShips().isEmpty()) {
                LOGGER.debug("Loading {} ships", saveData.getShips().size());
                try {
                    // Restore ship positions, stats, and inventory
                    for (com.odyssey.ship.Ship ship : saveData.getShips()) {
                        if (ship != null) {
                            // Restore ship position
                            LOGGER.debug("Restoring ship '{}' at position ({}, {}, {})", 
                                       ship.getName(), 
                                       ship.getPosition().x, 
                                       ship.getPosition().y, 
                                       ship.getPosition().z);
                            
                            // Ship stats are already loaded from the Ship object
                            // Inventory restoration would be handled by the Ship class
                            
                            // Future enhancement: Register ship with ship management system
                            // shipManager.registerShip(ship);
                        }
                    }
                    
                    // Set active ship if specified
                    if (saveData.getActiveShipId() != null) {
                        LOGGER.debug("Setting active ship: {}", saveData.getActiveShipId());
                        // Future enhancement: Set active ship in ship management system
                        // shipManager.setActiveShip(saveData.getActiveShipId());
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to restore ship data", e);
                }
            }
            
            // Apply quest progress
            if (saveData.getQuestProgress() != null && !saveData.getQuestProgress().isEmpty()) {
                LOGGER.debug("Loading quest progress: {} quests", saveData.getQuestProgress().size());
                try {
                    // Restore quest states and progress
                    for (Map.Entry<String, Integer> questEntry : saveData.getQuestProgress().entrySet()) {
                        String questId = questEntry.getKey();
                        Integer progress = questEntry.getValue();
                        
                        LOGGER.debug("Restoring quest '{}' with progress: {}", questId, progress);
                        
                        // Future enhancement: Implement quest system integration
                        // questManager.setQuestProgress(questId, progress);
                    }
                    
                    // Apply achievements
                    if (saveData.getAchievements() != null && !saveData.getAchievements().isEmpty()) {
                        LOGGER.debug("Loading {} achievements", saveData.getAchievements().size());
                        for (Map.Entry<String, Boolean> achievement : saveData.getAchievements().entrySet()) {
                            if (achievement.getValue()) {
                                LOGGER.debug("Achievement unlocked: {}", achievement.getKey());
                                // Future enhancement: Implement achievement system
                                // achievementManager.unlockAchievement(achievement.getKey());
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to restore quest progress", e);
                }
            }
            
            LOGGER.info("Save data applied successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to apply save data", e);
            throw new RuntimeException("Failed to load game state", e);
        }
    }
    
    /**
     * Get the save manager instance.
     */
    public SaveManager getSaveManager() {
        return saveManager;
    }
    
    /**
     * Save the current game state.
     * Creates a new save file with the current game data.
     */
    public void saveCurrentGame() {
        if (saveManager == null) {
            LOGGER.warn("Cannot save game - SaveManager not initialized");
            return;
        }
        
        if (currentState != GameState.IN_GAME) {
            LOGGER.warn("Cannot save game - not currently in game (state: {})", currentState);
            return;
        }
        
        try {
            LOGGER.info("Saving current game...");
            
            // Generate save name with timestamp
            String saveName = "quicksave_" + System.currentTimeMillis();
            
            // Save the game asynchronously
            saveManager.saveGame(saveName).thenAccept(success -> {
                if (success) {
                    LOGGER.info("Game saved successfully as '{}'", saveName);
                    showNotification("Game saved successfully!", new float[]{0.0f, 1.0f, 0.0f}); // Green
                } else {
                    LOGGER.error("Failed to save game");
                    showNotification("Failed to save game!", new float[]{1.0f, 0.0f, 0.0f}); // Red
                }
            }).exceptionally(throwable -> {
                LOGGER.error("Error during save operation", throwable);
                showNotification("Error saving game!", new float[]{1.0f, 0.0f, 0.0f}); // Red
                return null;
            });
            
        } catch (Exception e) {
            LOGGER.error("Failed to initiate save operation", e);
            showNotification("Failed to save game!", new float[]{1.0f, 0.0f, 0.0f}); // Red
        }
    }
    
    /**
     * Save the current game state with a specific name.
     * 
     * @param saveName The name for the save file
     */
    public void saveGameAs(String saveName) {
        if (saveManager == null) {
            LOGGER.warn("Cannot save game - SaveManager not initialized");
            return;
        }
        if (currentState != GameState.IN_GAME) {
            LOGGER.warn("Cannot save game - not currently in game (state: {})", currentState);
            return;
        }
        
        try {
            LOGGER.info("Saving game as '{}'...", saveName);
            
            // Save the game asynchronously
            saveManager.saveGame(saveName).thenAccept(success -> {
                if (success) {
                    LOGGER.info("Game saved successfully as '{}'", saveName);
                    showNotification("Game saved as '" + saveName + "'!", new float[]{0.0f, 1.0f, 0.0f}); // Green
                } else {
                    LOGGER.error("Failed to save game as '{}'", saveName);
                    showNotification("Failed to save game!", new float[]{1.0f, 0.0f, 0.0f}); // Red
                }
            }).exceptionally(throwable -> {
                LOGGER.error("Error during save operation for '{}'", saveName, throwable);
                showNotification("Error saving game!", new float[]{1.0f, 0.0f, 0.0f}); // Red
                return null;
            });
            
        } catch (Exception e) {
            LOGGER.error("Failed to initiate save operation for '{}'", saveName, e);
            showNotification("Failed to save game!", new float[]{1.0f, 0.0f, 0.0f}); // Red
        }
    }
    
    /**
     * Get the input manager instance.
     * 
     * @return The input manager
     */
    public InputManager getInputManager() {
        return inputManager;
    }
    
    /**
     * Get the current game time in milliseconds.
     * This represents the total time spent in actual gameplay (not in menus).
     * 
     * @return The current game time in milliseconds
     */
    public long getGameTime() {
        return totalGameTime;
    }
    
    /**
     * Set the game time (used when loading saved games).
     * 
     * @param gameTime The game time to set in milliseconds
     */
    public void setGameTime(long gameTime) {
        this.totalGameTime = gameTime;
    }
    
    /**
     * Get the world seed from the current world.
     * 
     * @return The world seed, or 0 if no world is loaded
     */
    public long getWorldSeed() {
        if (worldManager != null && worldManager.getCurrentWorld() != null) {
            return worldManager.getCurrentWorld().getWorldSeed();
        }
        return 0;
    }
    
    /**
     * Show a notification message to the player.
     * 
     * @param message The message to display
     * @param color RGB color array for the text (values 0.0-1.0)
     */
    public void showNotification(String message, float[] color) {
        this.currentNotification = message;
        this.notificationTimer = NOTIFICATION_DURATION;
        this.notificationColor = color.clone();
        LOGGER.debug("Showing notification: {}", message);
    }
    
    /**
     * Show a notification message with default white color.
     * 
     * @param message The message to display
     */
    public void showNotification(String message) {
        showNotification(message, new float[]{1.0f, 1.0f, 1.0f}); // White
    }
    
    /**
     * Update the notification timer.
     * 
     * @param deltaTime Time elapsed since last update
     */
    private void updateNotificationTimer(float deltaTime) {
        if (currentNotification != null && notificationTimer > 0) {
            notificationTimer -= deltaTime;
            if (notificationTimer <= 0) {
                currentNotification = null;
                notificationTimer = 0;
            }
        }
    }
    
    /**
     * Render current notifications on screen.
     */
    private void renderNotifications() {
        if (currentNotification != null && notificationTimer > 0 && renderEngine != null) {
            // Calculate fade-out alpha based on remaining time
            float alpha = Math.min(1.0f, notificationTimer / 1.0f); // Fade out in last second
            
            // Get screen dimensions for positioning
            int screenWidth = config.getWindowWidth();
            int screenHeight = config.getWindowHeight();
            
            // Position notification at top-center of screen
            float x = screenWidth * 0.5f;
            float y = screenHeight * 0.1f; // 10% from top
            
            // Render the notification text
            try {
                // Get the renderer from render engine to access text rendering
                var renderer = renderEngine.getRenderer();
                if (renderer != null && renderer.getTextRenderer() != null) {
                    renderer.getTextRenderer().renderText(
                        currentNotification,
                        x, y,
                        1.0f, // scale
                        notificationColor,
                        screenWidth, screenHeight
                    );
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to render notification: {}", e.getMessage());
            }
        }
    }
    
    public Map<String, Object> getGameFlags() {
        Map<String, Object> gameFlags = new HashMap<>();
        gameFlags.put("gameStarted", initialized);
        gameFlags.put("currentState", currentState.toString());
        gameFlags.put("totalGameTime", totalGameTime);
        return gameFlags;
    }

    /**
     * Get world flags for saving/loading.
     * 
     * @return Map of world flags
     */
    public Map<String, Object> getWorldFlags() {
        Map<String, Object> worldFlags = new HashMap<>();
        if (worldManager != null && worldManager.getCurrentWorld() != null) {
            // Add world-specific flags here
            worldFlags.put("worldName", worldManager.getCurrentWorld().getWorldName());
            worldFlags.put("worldSeed", worldManager.getCurrentWorld().getWorldSeed());
            worldFlags.put("totalChunksLoaded", worldManager.getCurrentWorld().getTotalChunksLoaded());
        }
        return worldFlags;
    }
    
    /**
     * Get current weather state for saving.
     * 
     * @return Weather state as string
     */
    public String getWeatherState() {
        if (weatherSystem != null) {
            // Get weather at world center or player position
            WeatherCondition weather = weatherSystem.getWeatherAt(0, 0);
            return String.format("TEMP:%.1f,HUM:%.2f,WIND:%.1f,VIS:%.0f", 
                weather.getTemperature(), 
                weather.getHumidity(), 
                weather.getWindSpeed(), 
                weather.getVisibility());
        }
        return "CLEAR"; // Default weather state
    }
    
    /**
     * Get all player ships for saving.
     * 
     * @return List of player ships
     */
    public List<Ship> getPlayerShips() {
        if (shipManager != null) {
            return shipManager.getPlayerShips();
        }
        return new ArrayList<>();
    }
    
    /**
     * Gets the ship management system.
     * 
     * @return The ship manager
     */
    public ShipManager getShipManager() {
        return shipManager;
    }
    
    /**
     * Gets the quest management system.
     * 
     * @return The quest manager
     */
    public QuestManager getQuestManager() {
        return questManager;
    }
    
    /**
     * Gets the achievement management system.
     * 
     * @return The achievement manager
     */
    public AchievementManager getAchievementManager() {
        return achievementManager;
    }
    
    /**
     * Gets quest progress for saving.
     * 
     * @return Map of quest progress
     */
    public Map<String, Integer> getQuestProgress() {
        if (questManager != null) {
            return questManager.getAllQuestProgress();
        }
        return new HashMap<>();
    }

    private void applyWeatherEffectsToShips() {
        if (shipManager != null && weatherSystem != null) {
            List<Ship> ships = shipManager.getPlayerShips();
            for (Ship ship : ships) {
                if (ship != null) {
                    // Get weather at ship's position
                    WeatherCondition weather = weatherSystem.getWeatherAt(
                        (int) ship.getPosition().x,
                        (int) ship.getPosition().z
                    );
                    
                    // Apply weather effects to ship
                    ship.applyWeatherEffects(weather);
                }
            }
        }
    }

    public String getActiveShipId() {
        if (shipManager != null) {
            Ship activeShip = shipManager.getActiveShip();
            return activeShip != null ? activeShip.getId() : null;
        }
        return null;
    }

    public long getPlaytimeSeconds() {
        return totalGameTime / 1000; // Convert milliseconds to seconds
    }

    public Map<String, Boolean> getAchievements() {
        if (achievementManager != null) {
            return achievementManager.getAllAchievements();
        }
        return new HashMap<>();
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Basic game stats
        stats.put("playtime", getPlaytimeSeconds());
        stats.put("gameState", currentState.toString());
        stats.put("worldSeed", worldManager != null ? getWorldSeed() : 0);
        
        // Ship statistics
        if (shipManager != null) {
            stats.put("totalShips", shipManager.getPlayerShips().size());
            stats.put("activeShipId", getActiveShipId());
            stats.put("shipsBuilt", shipManager.getTotalShipsBuilt());
        }
        
        // Quest statistics
        if (questManager != null) {
            stats.put("activeQuests", questManager.getActiveQuests().size());
            stats.put("completedQuests", questManager.getCompletedQuests().size());
            stats.put("questsCompleted", questManager.getTotalQuestsCompleted());
        }
        
        // Achievement statistics
        if (achievementManager != null) {
            stats.put("unlockedAchievements", achievementManager.getUnlockedAchievements().size());
            stats.put("totalAchievements", achievementManager.getAllAvailableAchievements().size());
            stats.put("achievementPoints", achievementManager.getTotalPoints());
            stats.put("achievementProgress", achievementManager.getUnlockPercentage());
        }
        
        // Weather statistics
        if (weatherSystem != null) {
            stats.put("currentWeather", getWeatherState());
            stats.put("windSpeed", weatherSystem.getWindSpeed());
            stats.put("waveHeight", weatherSystem.getWaveHeight());
        }
        
        // Physics statistics
        if (physicsEngine != null) {
            stats.put("physicsObjects", physicsEngine.getActiveObjectCount());
            stats.put("physicsSteps", physicsEngine.getStepCount());
        }
        
        return stats;
    }
}
