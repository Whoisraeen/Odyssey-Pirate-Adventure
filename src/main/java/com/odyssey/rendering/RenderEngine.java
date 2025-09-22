package com.odyssey.rendering;

import com.odyssey.core.GameConfig;
import com.odyssey.render.Window;
import com.odyssey.world.World;
import com.odyssey.ui.MainMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Main rendering engine for The Odyssey.
 * Manages the rendering pipeline, window, and all rendering operations.
 */
public class RenderEngine {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(RenderEngine.class);
    
    private final GameConfig config;
    private Window window;
    private Renderer renderer;
    private World world;
    private MainMenu mainMenu;
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
    
    /**
     * Render the main menu.
     */
    public void renderMainMenu() {
        if (!initialized) {
            LOGGER.warn("RenderEngine not initialized, cannot render main menu");
            return;
        }
        
        try {
            // Clear the screen with ocean blue background
            glClearColor(0.1f, 0.3f, 0.6f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            // Get window dimensions
            int[] width = new int[1];
            int[] height = new int[1];
            glfwGetWindowSize(window.getHandle(), width, height);
            
            // Render the main menu
            mainMenu.render(width[0], height[0]);
            
            // Swap buffers
            glfwSwapBuffers(window.getHandle());
            
        } catch (Exception e) {
            LOGGER.error("Error rendering main menu", e);
        }
    }
    
    /**
     * Render the loading screen.
     */
    public void renderLoadingScreen() {
        if (!initialized) return;
        // TODO: Implement loading screen rendering
        LOGGER.debug("Rendering loading screen");
    }
    
    /**
     * Render the game world.
     */
    public void renderWorld(World world) {
        if (!initialized || world == null) return;
        // TODO: Implement world rendering
        LOGGER.debug("Rendering world");
    }
    
    /**
     * Render the UI.
     */
    public void renderUI() {
        if (!initialized) return;
        // TODO: Implement UI rendering
        LOGGER.debug("Rendering UI");
    }
    
    /**
     * Render the pause menu.
     */
    public void renderPauseMenu() {
        if (!initialized) return;
        // TODO: Implement pause menu rendering
        LOGGER.debug("Rendering pause menu");
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
        
        if (window != null) {
            window.cleanup();
        }
        
        initialized = false;
        LOGGER.info("RenderEngine cleanup complete");
    }
}