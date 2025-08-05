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
        logger.info("Engine created with config: {}", config);
    }
    
    /**
     * Initialize all engine systems
     */
    public void initialize() throws Exception {
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
        
        // Initialize world systems
        world = new World(config);
        world.initialize();
        
        oceanSystem = new OceanSystem(config);
        oceanSystem.initialize();
        
        weatherSystem = new WeatherSystem(config);
        weatherSystem.initialize();
        
        // Initialize game state manager
        gameStateManager = new GameStateManager();
        gameStateManager.initialize();
        
        // Set initial timing
        lastFrameTime = glfwGetTime();
        
        logger.info("Engine initialization complete!");
    }
    
    /**
     * Main game loop
     */
    public void run() {
        logger.info("Starting main game loop...");
        running = true;
        
        while (running && !window.shouldClose()) {
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
            
            // Check for exit conditions
            if (inputManager.isKeyPressed(GLFW_KEY_ESCAPE)) {
                running = false;
            }
        }
        
        cleanup();
    }
    
    /**
     * Update all game systems
     */
    private void update(double deltaTime) {
        // Update input
        inputManager.update();
        
        // Handle camera controls
        updateCameraControls(deltaTime);
        
        // Update game state
        gameStateManager.update(deltaTime);
        
        // Update world systems
        if (config.isEnableWeatherSystem()) {
            weatherSystem.update(deltaTime);
        }
        
        if (config.isEnableTidalSystem()) {
            oceanSystem.update(deltaTime);
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
        
        // Movement controls (WASD)
        if (inputManager.isKeyPressed(GLFW_KEY_W)) {
            camera.moveForward((float) (moveSpeed * deltaTime));
        }
        if (inputManager.isKeyPressed(GLFW_KEY_S)) {
            camera.moveBackward((float) (moveSpeed * deltaTime));
        }
        if (inputManager.isKeyPressed(GLFW_KEY_A)) {
            camera.moveLeft((float) (moveSpeed * deltaTime));
        }
        if (inputManager.isKeyPressed(GLFW_KEY_D)) {
            camera.moveRight((float) (moveSpeed * deltaTime));
        }
        if (inputManager.isKeyPressed(GLFW_KEY_SPACE)) {
            camera.moveUp((float) (moveSpeed * deltaTime));
        }
        if (inputManager.isKeyPressed(GLFW_KEY_LEFT_SHIFT)) {
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
        
        // Mouse look (if right mouse button is held)
        if (inputManager.isMouseButtonPressed(GLFW_MOUSE_BUTTON_RIGHT)) {
            float mouseSensitivity = 0.1f;
            camera.rotateY((float) (-inputManager.getMouseDeltaX() * mouseSensitivity));
            camera.rotateX((float) (-inputManager.getMouseDeltaY() * mouseSensitivity));
        }
    }
    
    /**
     * Render the current frame
     */
    private void render() {
        renderer.beginFrame(deltaTime);
        
        // Render the basic scene (ocean and test cubes)
        renderer.renderScene();
        
        // Render world
        world.render(renderer);
        
        // Render ocean system (additional ocean effects)
        oceanSystem.render(renderer);
        
        // Render weather effects
        weatherSystem.render(renderer);
        
        // Render UI
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
    
    public double getDeltaTime() { return deltaTime; }
    public int getFPS() { return fps; }
    
    public void stop() {
        running = false;
    }
}