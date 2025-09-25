package com.odyssey.rendering;

import com.odyssey.core.GameConfig;
import com.odyssey.core.GameState;
import com.odyssey.render.Window;
import com.odyssey.world.World;
import com.odyssey.ui.MainMenu;
import com.odyssey.ui.LoadGameMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;

/**
 * Main rendering engine for The Odyssey.
 * Manages the rendering pipeline, window, and all rendering operations.
 */
public class RenderEngine {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderEngine.class);
    
    private final GameConfig config;
    private Window window;
    private Renderer renderer;
    private MainMenu mainMenu;
    private LoadGameMenu loadGameMenu;
    private boolean initialized = false;
    
    public RenderEngine(GameConfig config) {
        this.config = config;
    }
    
    /**
     * Initialize the rendering engine.
     */
    public void initialize() {
        if (initialized) {
            LOGGER.warn("RenderEngine is already initialized");
            return;
        }
        
        LOGGER.info("Initializing RenderEngine...");
        
        try {
            // Create window
            int width = config.getInt("graphics.width", 1920);
            int height = config.getInt("graphics.height", 1080);
            boolean fullscreen = config.getBoolean("graphics.fullscreen", false);
            
            window = new Window(width, height, "The Odyssey - Pirate Adventure", fullscreen);
            window.initialize();
            
            // Initialize renderer
            renderer = new Renderer();
            renderer.initialize(width, height);
            
            // Initialize main menu
        mainMenu = new MainMenu();
        mainMenu.initialize();
        
        // Initialize load game menu (will be set up later with SaveManager)
        loadGameMenu = null; // Will be initialized when SaveManager is available
        
        initialized = true;
        LOGGER.info("RenderEngine initialized successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize RenderEngine", e);
            cleanup();
            throw new RuntimeException("RenderEngine initialization failed", e);
        }
    }
    
    /**
     * Begin a new frame.
     */
    public void beginFrame() {
        if (!initialized) return;
        
        glfwPollEvents();
        renderer.beginFrame();
    }
    
    /**
     * End the current frame.
     */
    public void endFrame() {
        if (!initialized) return;
        
        renderer.endFrame();
        glfwSwapBuffers(window.getHandle());
    }

    public void render(GameState gameState, World world) {
        if (!initialized) return;

        switch (gameState) {
            case MAIN_MENU -> renderer.renderMainMenu(mainMenu);
            case LOAD_GAME -> renderer.renderLoadGameMenu(loadGameMenu);
            case LOADING -> renderer.renderLoadingScreen("Loading...", 0.5f);
            case PAUSED -> renderer.renderPauseMenu();
            case IN_GAME -> renderer.render(world, null);
            default -> {
                // Render the world by default for most game states
                if (world != null) {
                    renderer.render(world, null);
                } else {
                    // Fallback to a clear screen if no world is available
                    renderer.clearScreen();
                }
            }
        }
    }

    /**
     * Set the world reference for rendering.
     * 
     * @param world The world to render
     */
    public void setWorld(World world) {
        renderer.setWorld(world);
    }
    
    /**
     * Get the current world reference.
     * 
     * @return The current world, or null if not set
     */
    public World getWorld() {
        return renderer.getWorld();
    }
    
    /**
     * Get the window.
     */
    public Window getWindow() {
        return window;
    }
    
    /**
     * Get the renderer.
     */
    public Renderer getRenderer() {
        return renderer;
    }
    
    /**
     * Get the main menu.
     */
    public MainMenu getMainMenu() {
        return mainMenu;
    }
    
    /**
     * Get the load game menu.
     */
    public LoadGameMenu getLoadGameMenu() {
        return loadGameMenu;
    }
    
    /**
     * Set the load game menu.
     */
    public void setLoadGameMenu(LoadGameMenu loadGameMenu) {
        this.loadGameMenu = loadGameMenu;
    }
    
    /**
     * Check if the window should close.
     */
    public boolean shouldClose() {
        return window != null && window.shouldClose();
    }
    
    /**
     * Clean up resources.
     */
    public void cleanup() {
        LOGGER.info("Cleaning up RenderEngine resources...");
        
        if (renderer != null) {
            renderer.cleanup();
        }
        
        if (mainMenu != null) {
            mainMenu.cleanup();
        }
        
        if (loadGameMenu != null) {
            loadGameMenu.cleanup();
        }
        
        if (window != null) {
            window.cleanup();
        }
        
        initialized = false;
        LOGGER.info("RenderEngine cleanup complete");
    }
}