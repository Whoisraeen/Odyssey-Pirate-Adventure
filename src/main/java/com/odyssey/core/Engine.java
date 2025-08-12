package com.odyssey.core;

import com.odyssey.game.managers.InventoryManager;
import com.odyssey.game.managers.PlayerManager;
import com.odyssey.game.managers.QuestManager;
import com.odyssey.game.managers.ShipManager;
import com.odyssey.game.managers.WorldManager;
import com.odyssey.graphics.Renderer;
import com.odyssey.graphics.Window;
import com.odyssey.graphics.TimeOfDaySystem;
import com.odyssey.input.InputManager;
import com.odyssey.world.World;
import com.odyssey.world.ocean.OceanSystem;
import com.odyssey.world.save.SaveManager;
import com.odyssey.world.weather.WeatherSystem;
import com.odyssey.audio.AudioManager;
import com.odyssey.game.GameStateManager;
import com.odyssey.core.memory.MemoryManager;
import com.odyssey.core.jobs.JobSystem;

import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The main game engine for The Odyssey.
 * Manages all core systems including rendering, input, world simulation, and game state.
 */
public class Engine {
    private static final Logger logger = LoggerFactory.getLogger(Engine.class);
    private static Engine instance;
    
    private final GameConfig config;
    private boolean running = false;
    
    // Core systems
    private Window window;
    private Renderer renderer;
    private InputManager inputManager;
    private AudioManager audioManager;
    private GameStateManager gameStateManager;
    private CrashReporter crashReporter;
    private MemoryManager memoryManager;
    private JobSystem jobSystem;
    
    // World systems
    private World world;
    private OceanSystem oceanSystem;
    private WeatherSystem weatherSystem;
    private TimeOfDaySystem timeOfDaySystem;

    // Manager systems
    private SaveManager saveManager;
    private PlayerManager playerManager;
    private WorldManager worldManager;
    private ShipManager shipManager;
    private InventoryManager inventoryManager;
    private QuestManager questManager;
    
    // Timing
    private double lastFrameTime;
    private double deltaTime;
    private int frameCount = 0;
    private double fpsTimer = 0.0;
    private int fps = 0;
    
    public Engine(GameConfig config) {
        this.config = config;
        this.crashReporter = new CrashReporter(config);
        this.memoryManager = new MemoryManager(config);
        instance = this; // Set singleton instance
        logger.info("Engine created with config: {}", config);
    }
    
    /**
     * Gets the singleton instance of the Engine
     * @return The Engine instance
     */
    public static Engine getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Engine has not been initialized yet");
        }
        return instance;
    }
    
    /**
     * Initialize all engine systems
     */
    public void initialize() throws Exception {
        try {
            logger.info("Initializing The Odyssey Engine...");
            
            // Initialize GLFW
            if (!glfwInit()) {
                throw new RuntimeException("Failed to initialize GLFW");
            }
            
            // Create window
            window = new Window(config);
            window.initialize();
            
            // Initialize input manager
            inputManager = new InputManager(window);
            inputManager.initialize();
            
            // Set mouse sensitivity from config
            inputManager.setMouseSensitivity(config.getMouseSensitivity());
            
            // Initialize renderer
            renderer = new Renderer(config, window);
            renderer.initialize();
            
            // Initialize audio
            audioManager = new AudioManager(config);
            audioManager.initialize();
            
            // Initialize memory manager
            memoryManager.initialize();
            
            // Initialize job system
            jobSystem = new JobSystem(config);
            jobSystem.initialize();
            
            // Initialize world systems
            world = new World(config);
            world.initialize();
            
            // Ocean system is now initialized by the Renderer
            // oceanSystem = new OceanSystem(config);
            // oceanSystem.initialize();
            
            weatherSystem = new WeatherSystem(config);
            weatherSystem.initialize();
            
            // Initialize time of day system
            timeOfDaySystem = new TimeOfDaySystem();
            timeOfDaySystem.initialize();

            // Initialize managers
            saveManager = new SaveManager();
            playerManager = new PlayerManager();
            worldManager = new WorldManager();
            shipManager = new ShipManager();
            inventoryManager = new InventoryManager();
            questManager = new QuestManager();
            
            // Initialize game state manager
            gameStateManager = new GameStateManager(this);
            gameStateManager.initialize();
            
            // Set initial timing
            lastFrameTime = glfwGetTime();
            
            logger.info("Engine initialization complete!");
        } catch (Exception e) {
            crashReporter.reportCrash("Engine initialization", e);
            throw e;
        }
    }
    
    /**
     * Main game loop
     */
    public void run() {
        logger.info("Starting main game loop...");
        running = true;
        
        try {
            while (running && !window.shouldClose()) {
                try {
                    // Calculate delta time
                    double currentTime = glfwGetTime();
                    deltaTime = currentTime - lastFrameTime;
                    lastFrameTime = currentTime;
                    
                    // Update FPS counter
                    updateFPS(deltaTime);
                    
                    // Debug: Log first few frames
                    if (frameCount < 5) {
                        logger.info("Frame {}: running={}, windowShouldClose={}, deltaTime={}", 
                            frameCount, running, window.shouldClose(), deltaTime);
                        logger.info("About to call update(deltaTime)...");
                    }
                    
                    // Update game systems
                    update(deltaTime);
                    
                    if (frameCount < 5) {
                        logger.info("Update completed, about to call render()...");
                    }
                    
                    // Render frame
                    try {
                        logger.info("ABOUT TO CALL RENDER METHOD - Line before method call");
                        render();
                        logger.info("RENDER METHOD RETURNED SUCCESSFULLY");
                        if (frameCount < 5) {
                            logger.info("Render completed successfully!");
                        }
                    } catch (Exception e) {
                        logger.error("EXCEPTION IN RENDER METHOD: ", e);
                        throw e; // Re-throw to trigger main loop exception handler
                    } catch (Error e) {
                        logger.error("ERROR IN RENDER METHOD: ", e);
                        throw e; // Re-throw to trigger main loop exception handler
                    } catch (Throwable e) {
                        logger.error("THROWABLE IN RENDER METHOD: ", e);
                        throw e; // Re-throw to trigger main loop exception handler
                    }
                    
                    if (frameCount < 5) {
                        logger.info("Render completed, about to poll events...");
                    }
                    
                    // Poll events and swap buffers
                    glfwPollEvents();
                    window.swapBuffers();
                    
                    if (frameCount < 5) {
                        logger.info("Frame {} completed successfully", frameCount);
                    }
                    
                    // Exit conditions are now handled by the game state manager
                } catch (Exception e) {
                    crashReporter.reportCrash("Main game loop", e);
                    logger.error("Error in main game loop, attempting to continue...", e);
                    // Continue running unless it's a critical error
                    running = false; // For safety, stop on any exception in the main loop
                } catch (OutOfMemoryError | StackOverflowError e) {
                    crashReporter.reportCrash("Critical system error", e);
                    logger.error("Critical system error, shutting down...", e);
                    running = false;
                }
            }
            
            // Debug: Log why the loop exited
            logger.info("Main game loop exited: running={}, windowShouldClose={}", running, window.shouldClose());
        } catch (Exception e) {
            crashReporter.reportCrash("Game loop fatal error", e);
            throw new RuntimeException("Fatal error in game loop", e);
        } finally {
            cleanup();
        }
    }
    
    /**
     * Update all game systems
     */
    private void update(double deltaTime) {
        // Update input
        inputManager.update();
        
        // Handle ESC key for mouse release
        if (inputManager.isKeyJustPressed(GLFW_KEY_ESCAPE)) {
            if (inputManager.isMouseCaptured()) {
                inputManager.setMouseCaptured(false);
                logger.debug("Mouse capture released via ESC key");
            }
        }
        
        // Update game state first
        gameStateManager.update(deltaTime);
        
        // Only handle camera controls when in playing state
        GameStateManager.GameState currentState = gameStateManager.getCurrentState();
        boolean isPlayingState = currentState instanceof GameStateManager.PlayingState;
        
        // Debug logging every 60 frames (once per second at 60 FPS)
        if (frameCount % 60 == 0) {
            logger.info("Current game state: {} | Is PlayingState: {} | Camera controls active: {}", 
                currentState.getClass().getSimpleName(), isPlayingState, isPlayingState);
        }
        
        if (isPlayingState) {
            updateCameraControls(deltaTime);
        }
        
        // Update world systems
        timeOfDaySystem.update(deltaTime);
        
        if (config.isEnableWeatherSystem()) {
            weatherSystem.update(deltaTime);
        }
        
        if (config.isEnableTidalSystem() && renderer.getOceanSystem() != null) {
            // Update environmental data for ocean system
            updateOceanEnvironmentalData();
            
            renderer.getOceanSystem().update(deltaTime);
        }
        
        world.update(deltaTime);
        
        // Update audio
        audioManager.update(deltaTime);
    }
    
    /**
     * Handle camera movement and rotation controls
     */
    private void updateCameraControls(double deltaTime) {
        var camera = renderer.getCamera();
        float moveSpeed = 50.0f; // units per second
        float rotateSpeed = 90.0f; // degrees per second
        
        // Movement controls (WASD + Gamepad)
        if (inputManager.isMovingForward()) {
            camera.moveForward((float) (moveSpeed * deltaTime));
        }
        if (inputManager.isMovingBackward()) {
            camera.moveBackward((float) (moveSpeed * deltaTime));
        }
        if (inputManager.isMovingLeft()) {
            camera.moveLeft((float) (moveSpeed * deltaTime));
        }
        if (inputManager.isMovingRight()) {
            camera.moveRight((float) (moveSpeed * deltaTime));
        }
        if (inputManager.isMovingUp()) {
            camera.moveUp((float) (moveSpeed * deltaTime));
        }
        if (inputManager.isMovingDown()) {
            camera.moveDown((float) (moveSpeed * deltaTime));
        }
        
        // Rotation controls (arrow keys)
        if (inputManager.isKeyPressed(GLFW_KEY_LEFT)) {
            camera.rotateY((float) (rotateSpeed * deltaTime));
        }
        if (inputManager.isKeyPressed(GLFW_KEY_RIGHT)) {
            camera.rotateY((float) (-rotateSpeed * deltaTime));
        }
        if (inputManager.isKeyPressed(GLFW_KEY_UP)) {
            camera.rotateX((float) (rotateSpeed * deltaTime));
        }
        if (inputManager.isKeyPressed(GLFW_KEY_DOWN)) {
            camera.rotateX((float) (-rotateSpeed * deltaTime));
        }
        
        // Gamepad right stick camera rotation (inverted Y-axis for natural feel)
        for (var gamepad : inputManager.getAllGamepads()) {
            if (gamepad.isConnected()) {
                float rightStickX = gamepad.getAxis(GLFW_GAMEPAD_AXIS_RIGHT_X);
                float rightStickY = gamepad.getAxis(GLFW_GAMEPAD_AXIS_RIGHT_Y);
                
                // Apply deadzone
                if (Math.abs(rightStickX) > 0.1f) {
                    camera.rotateY((float) (rightStickX * rotateSpeed * deltaTime));
                }
                if (Math.abs(rightStickY) > 0.1f) {
                    // Invert Y-axis for natural gamepad feel
                    camera.rotateX((float) (rightStickY * rotateSpeed * deltaTime));
                }
            }
        }
        
        // Raw mouse input - only auto-capture when in playing state
        double mouseDeltaX = inputManager.getRawMouseDeltaX();
        double mouseDeltaY = inputManager.getRawMouseDeltaY();
        
        // Only auto-capture mouse when in playing state
        GameStateManager.GameState currentState = gameStateManager.getCurrentState();
        boolean isPlayingState = currentState instanceof GameStateManager.PlayingState;
        
        // Auto-capture mouse in playing state
        if (isPlayingState && !inputManager.isMouseCaptured()) {
            inputManager.setMouseCaptured(true);
        }
        
        // Note: ESC key handling for mouse release is in the main game loop
        
        // Apply mouse look when captured
        if (inputManager.isMouseCaptured()) {
            // Use the InputManager's sensitivity-adjusted deltas for smooth movement
            double adjustedDeltaX = inputManager.getMouseDeltaX();
            double adjustedDeltaY = inputManager.getMouseDeltaY();
            
            camera.rotateY((float) adjustedDeltaX);
            camera.rotateX((float) adjustedDeltaY);
            
            // Debug logging for mouse input (every 30 frames to avoid spam)
            if (frameCount % 30 == 0 && (Math.abs(adjustedDeltaX) > 0.001 || Math.abs(adjustedDeltaY) > 0.001)) {
                logger.debug("Mouse captured: deltaX={}, deltaY={}, sensitivity={}", 
                    adjustedDeltaX, adjustedDeltaY, inputManager.getMouseSensitivity());
            }
        } else if (Math.abs(mouseDeltaX) > 0.001 || Math.abs(mouseDeltaY) > 0.001) {
            // Debug: Mouse is moving but not captured
            if (frameCount % 30 == 0) {
                logger.debug("Mouse moving but not captured: deltaX={}, deltaY={}, isPlaying={}", 
                    mouseDeltaX, mouseDeltaY, isPlayingState);
            }
        }
    }
    
    /**
     * Updates environmental data for the ocean system from weather and biome systems
     */
    private void updateOceanEnvironmentalData() {
        OceanSystem oceanSystem = renderer.getOceanSystem();
        if (oceanSystem == null) return;
        
        // Get weather intensity from weather system
        float weatherIntensity = 0.0f;
        if (weatherSystem != null) {
            weatherIntensity = weatherSystem.getWeatherIntensity();
        }
        
        // Get biome water color from world's biome manager
        Vector3f biomeWaterColor = new Vector3f(0.1f, 0.3f, 0.8f); // Default ocean blue
        if (world != null && world.getBiomeManager() != null) {
            // Get camera position to determine current biome
            Vector3f cameraPos = renderer.getCamera().getPosition();
            var biomeManager = world.getBiomeManager();
            var biomeType = biomeManager.getBiomeAt((int)cameraPos.x, (int)cameraPos.z);
            biomeWaterColor = biomeManager.getWaterColor(biomeType);
        }
        
        // Calculate underwater depth based on camera position
        float underwaterDepth = 0.0f;
        if (oceanSystem.isUnderwater(renderer.getCamera().getPosition())) {
            underwaterDepth = oceanSystem.getDepthAt(renderer.getCamera().getPosition());
        }
        
        // Update ocean system with environmental data
        oceanSystem.updateEnvironmentalData(weatherIntensity, biomeWaterColor, underwaterDepth);
    }
    
    /**
     * Render the current frame
     */
    private void render() {
        logger.info("Starting render() method...");
        
        // Handle window resize
        if (window.isResized()) {
            logger.info("Handling window resize...");
            renderer.handleResize(window.getWidth(), window.getHeight());
            gameStateManager.getUIRenderer().resize(window.getWidth(), window.getHeight());
            window.setResized(false);
            logger.info("Window resize handled.");
        }
        
        // Determine render mode and clear color based on game state
        GameStateManager.GameState currentState = gameStateManager.getCurrentState();
        boolean shouldRender3D = currentState instanceof GameStateManager.PlayingState;
        logger.info("Current state: {}, shouldRender3D: {}", currentState.getClass().getSimpleName(), shouldRender3D);
        
        if (shouldRender3D) {
            logger.info("Rendering 3D scene...");
            // Update celestial lighting based on time of day
            renderer.updateCelestialLighting(timeOfDaySystem);
            
            // Ocean blue for 3D gameplay
            renderer.beginFrame(deltaTime, 0.1f, 0.3f, 0.8f, 1.0f);
            
            // Render the basic scene (ocean and test cubes)
            renderer.renderScene();
            
            // Render world
            world.render(renderer);
            
            // Ocean rendering is now handled by the renderer's renderScene method
            // oceanSystem.render(renderer);
            
            // Render weather effects
            weatherSystem.render(renderer);
        } else {
            logger.info("Rendering menu state with black background...");
            logger.info("Renderer object: {}", renderer);
            logger.info("Renderer null check: {}", renderer == null);
            if (renderer != null) {
                logger.info("About to call renderer.beginFrame with deltaTime={}", deltaTime);
                
                // Check renderer's critical member variables
                try {
                    logger.info("Checking engine config...");
                    boolean debugMode = config.isDebugMode();
                    logger.info("Config debug mode: {}", debugMode);
                } catch (Exception e) {
                    logger.error("Error accessing engine config: ", e);
                }
                
                try {
                    logger.info("Checking renderer camera...");
                    var camera = renderer.getCamera();
                    logger.info("Camera: {}", camera);
                } catch (Exception e) {
                    logger.error("Error accessing renderer camera: ", e);
                }
                
                // Black for menu states
                renderer.beginFrame(deltaTime, 0.0f, 0.0f, 0.0f, 1.0f);
                logger.info("beginFrame completed for menu state.");
            } else {
                logger.error("CRITICAL: Renderer is null!");
                throw new RuntimeException("Renderer is null during render call");
            }
        }
        
        logger.info("About to render UI/game state...");
        // Always render UI (which includes menu states)
        gameStateManager.render(renderer);
        logger.info("Game state render completed.");
        
        // Render debug info if enabled
        if (config.isDebugMode()) {
            logger.info("Rendering debug info...");
            renderDebugInfo();
            logger.info("Debug info rendered.");
        }
        
        logger.info("About to call endFrame...");
        renderer.endFrame();
        logger.info("endFrame completed, render() method finished.");
    }
    
    /**
     * Render debug information
     */
    private void renderDebugInfo() {
        // This will be implemented when we have UI rendering
        // For now, just log some debug info occasionally
        if (frameCount % 60 == 0) { // Every second at 60 FPS
            logger.debug("FPS: {}, Delta: {}ms", fps, String.format("%.3f", deltaTime * 1000));
        }
    }
    
    /**
     * Update FPS counter
     */
    private void updateFPS(double deltaTime) {
        frameCount++;
        fpsTimer += deltaTime;
        
        if (fpsTimer >= 1.0) {
            fps = frameCount;
            frameCount = 0;
            fpsTimer = 0.0;
            
            // Update window title with FPS
            window.setTitle(String.format("The Odyssey - FPS: %d", fps));
        }
    }
    
    /**
     * Clean up all resources
     */
    private void cleanup() {
        logger.info("Cleaning up engine resources...");
        
        if (gameStateManager != null) gameStateManager.cleanup();
        if (timeOfDaySystem != null) timeOfDaySystem.cleanup();
        if (weatherSystem != null) weatherSystem.cleanup();
        if (oceanSystem != null) oceanSystem.cleanup();
        if (world != null) world.cleanup();
        if (audioManager != null) audioManager.cleanup();
        if (renderer != null) renderer.cleanup();
        if (inputManager != null) inputManager.cleanup();
        if (window != null) window.cleanup();
        if (memoryManager != null) memoryManager.cleanup();
        if (jobSystem != null) {
            if (!jobSystem.shutdown(5)) {
                logger.warn("JobSystem did not shutdown cleanly within timeout");
            }
        }
        
        glfwTerminate();
        
        logger.info("Engine cleanup complete");
    }
    
    // Getters for systems
    public GameConfig getConfig() { return config; }
    public Window getWindow() { return window; }
    public Renderer getRenderer() { return renderer; }
    public InputManager getInputManager() { return inputManager; }
    public AudioManager getAudioManager() { return audioManager; }
    public World getWorld() { return world; }
    public OceanSystem getOceanSystem() { return oceanSystem; }
    public WeatherSystem getWeatherSystem() { return weatherSystem; }
    public TimeOfDaySystem getTimeOfDaySystem() { return timeOfDaySystem; }
    public GameStateManager getGameStateManager() { return gameStateManager; }
    public CrashReporter getCrashReporter() { return crashReporter; }
    public MemoryManager getMemoryManager() { return memoryManager; }
    public JobSystem getJobSystem() { return jobSystem; }
    
    public double getDeltaTime() { return deltaTime; }
    public int getFPS() { return fps; }
    
    public void stop() {
        running = false;
    }

    public SaveManager getSaveManager() {
        return saveManager;
    }

    public String getVersion() {
        return "0.0.1-ALPHA"; // Placeholder
    }

    public PlayerManager getPlayerManager() {
        return playerManager;
    }

    public WorldManager getWorldManager() {
        return worldManager;
    }

    public ShipManager getShipManager() {
        return shipManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public GameConfig getGameConfig() {
        return config;
    }
}