package com.odyssey.core;

import com.odyssey.graphics.Renderer;
import com.odyssey.graphics.Window;
import com.odyssey.input.InputManager;
import com.odyssey.world.World;
import com.odyssey.world.ocean.OceanSystem;
import com.odyssey.world.weather.WeatherSystem;
import com.odyssey.audio.AudioManager;
import com.odyssey.game.GameState;
import com.odyssey.game.GameStateManager;
import com.odyssey.core.memory.MemoryManager;
import com.odyssey.core.jobs.JobSystem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;

/**
 * The main game engine for The Odyssey.
 * Manages all core systems including rendering, input, world simulation, and game state.
 */
public class Engine {
    private static final Logger logger = LoggerFactory.getLogger(Engine.class);
    
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
        logger.info("Engine created with config: {}", config);
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
            
            // Initialize renderer
            renderer = new Renderer(config);
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
                    
                    // Poll input events
                    glfwPollEvents();
                    
                    // Update systems
                    update(deltaTime);
                    
                    // Render frame
                    render();
                    
                    // Swap buffers
                    window.swapBuffers();
                    
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
        if (config.isEnableWeatherSystem()) {
            weatherSystem.update(deltaTime);
        }
        
        if (config.isEnableTidalSystem() && renderer.getOceanSystem() != null) {
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
            float mouseSensitivity = 0.1f;
            camera.rotateY((float) (mouseDeltaX * mouseSensitivity));
            camera.rotateX((float) (mouseDeltaY * mouseSensitivity));
            
            // Debug logging for mouse input (every 30 frames to avoid spam)
            if (frameCount % 30 == 0 && (Math.abs(mouseDeltaX) > 0.001 || Math.abs(mouseDeltaY) > 0.001)) {
                logger.debug("Mouse captured: deltaX={}, deltaY={}, sensitivity={}", 
                    mouseDeltaX, mouseDeltaY, mouseSensitivity);
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
     * Render the current frame
     */
    private void render() {
        // Handle window resize
        if (window.isResized()) {
            renderer.handleResize(window.getWidth(), window.getHeight());
            gameStateManager.getUIRenderer().resize(window.getWidth(), window.getHeight());
            window.setResized(false);
        }
        
        // Determine render mode and clear color based on game state
        GameStateManager.GameState currentState = gameStateManager.getCurrentState();
        boolean shouldRender3D = currentState instanceof GameStateManager.PlayingState;
        
        if (shouldRender3D) {
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
            // Black for menu states
            renderer.beginFrame(deltaTime, 0.0f, 0.0f, 0.0f, 1.0f);
        }
        
        // Always render UI (which includes menu states)
        gameStateManager.render(renderer);
        
        // Render debug info if enabled
        if (config.isDebugMode()) {
            renderDebugInfo();
        }
        
        renderer.endFrame();
    }
    
    /**
     * Render debug information
     */
    private void renderDebugInfo() {
        // This will be implemented when we have UI rendering
        // For now, just log some debug info occasionally
        if (frameCount % 60 == 0) { // Every second at 60 FPS
            logger.debug("FPS: {}, Delta: {:.3f}ms", fps, deltaTime * 1000);
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
    public GameStateManager getGameStateManager() { return gameStateManager; }
    public CrashReporter getCrashReporter() { return crashReporter; }
    public MemoryManager getMemoryManager() { return memoryManager; }
    public JobSystem getJobSystem() { return jobSystem; }
    
    public double getDeltaTime() { return deltaTime; }
    public int getFPS() { return fps; }
    
    public void stop() {
        running = false;
    }
}