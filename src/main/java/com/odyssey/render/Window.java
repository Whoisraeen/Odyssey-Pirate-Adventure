package com.odyssey.render;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryUtil.*;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.odyssey.core.GameConfig;

/**
 * Window management class for The Odyssey.
 * 
 * This class handles the creation and management of the GLFW window,
 * OpenGL context initialization, and window-related operations such as
 * resizing, fullscreen toggling, and VSync control.
 */
public class Window {
    
    private static final Logger logger = LoggerFactory.getLogger(Window.class);
    
    // Window properties
    private long windowHandle;
    private String title;
    private int width;
    private int height;
    private boolean fullscreen;
    private boolean vsync;
    private boolean resized;
    
    // OpenGL capabilities
    private GLCapabilities glCapabilities;
    
    // Callbacks
    private WindowResizeCallback resizeCallback;
    
    /**
     * Functional interface for window resize callbacks.
     */
    @FunctionalInterface
    public interface WindowResizeCallback {
        void onResize(int width, int height);
    }
    
    /**
     * Create a new window with the specified properties.
     * 
     * @param title The window title
     * @param width The window width
     * @param height The window height
     * @param fullscreen Whether to start in fullscreen mode
     * @param vsync Whether to enable VSync
     */
    public Window(String title, int width, int height, boolean fullscreen, boolean vsync) {
        this.title = title;
        this.width = width;
        this.height = height;
        this.fullscreen = fullscreen;
        this.vsync = vsync;
        this.resized = false;
    }
    
    /**
     * Create a new window with the specified properties and default VSync enabled.
     * 
     * @param width The window width
     * @param height The window height
     * @param title The window title
     * @param fullscreen Whether to start in fullscreen mode
     */
    public Window(int width, int height, String title, boolean fullscreen) {
        this(title, width, height, fullscreen, true); // Default VSync to true
    }
    
    /**
     * Initialize the window and OpenGL context.
     */
    public void initialize() {
        logger.info("Initializing window: {}x{} (fullscreen: {}, vsync: {})", 
                   width, height, fullscreen, vsync);
        
        // Set up error callback
        GLFWErrorCallback.createPrint(System.err).set();
        
        // Initialize GLFW
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }
        
        // Configure GLFW window hints
        configureWindowHints();
        
        // Create the window
        createWindow();
        
        // Set up window callbacks
        setupCallbacks();
        
        // Make the OpenGL context current
        glfwMakeContextCurrent(windowHandle);
        
        // Initialize OpenGL capabilities
        glCapabilities = GL.createCapabilities();
        if (glCapabilities == null) {
            throw new RuntimeException("Failed to create OpenGL capabilities");
        }
        
        // Configure OpenGL
        configureOpenGL();
        
        // Set VSync
        setVSync(vsync);
        
        // Show the window
        glfwShowWindow(windowHandle);
        
        logger.info("Window initialized successfully");
        logSystemInfo();
    }
    
    /**
     * Configure GLFW window hints.
     */
    private void configureWindowHints() {
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        
        // Enable debug context in debug mode
        GameConfig config = GameConfig.getInstance();
        if (config.isDebugMode()) {
            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
        }
        
        // Multi-sampling for anti-aliasing
        glfwWindowHint(GLFW_SAMPLES, 4);
    }
    
    /**
     * Create the GLFW window.
     */
    private void createWindow() {
        long monitor = fullscreen ? glfwGetPrimaryMonitor() : NULL;
        
        windowHandle = glfwCreateWindow(width, height, title, monitor, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }
        
        // Center the window if not fullscreen
        if (!fullscreen) {
            centerWindow();
        }
    }
    
    /**
     * Center the window on the primary monitor.
     */
    private void centerWindow() {
        GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (vidMode != null) {
            int centerX = (vidMode.width() - width) / 2;
            int centerY = (vidMode.height() - height) / 2;
            glfwSetWindowPos(windowHandle, centerX, centerY);
        }
    }
    
    /**
     * Set up window callbacks.
     */
    private void setupCallbacks() {
        // Window resize callback
        glfwSetFramebufferSizeCallback(windowHandle, (_, w, h) -> {
            this.width = w;
            this.height = h;
            this.resized = true;
            
            // Update OpenGL viewport
            glViewport(0, 0, w, h);
            
            // Notify resize callback if set
            if (resizeCallback != null) {
                resizeCallback.onResize(w, h);
            }
            
            logger.debug("Window resized to {}x{}", w, h);
        });
        
        // Window close callback
        glfwSetWindowCloseCallback(windowHandle, _ -> {
            logger.info("Window close requested");
        });
    }
    
    /**
     * Configure OpenGL settings.
     */
    private void configureOpenGL() {
        // Enable depth testing
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LEQUAL);
        
        // Enable face culling
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
        
        // Enable blending for transparency
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Set clear color to ocean blue
        glClearColor(0.1f, 0.3f, 0.6f, 1.0f);
        
        // Set initial viewport
        glViewport(0, 0, width, height);
        
        logger.debug("OpenGL configured");
    }
    
    /**
     * Log system and OpenGL information.
     */
    private void logSystemInfo() {
        logger.info("OpenGL Version: {}", glGetString(GL_VERSION));
        logger.info("OpenGL Vendor: {}", glGetString(GL_VENDOR));
        logger.info("OpenGL Renderer: {}", glGetString(GL_RENDERER));
        logger.info("GLSL Version: {}", glGetString(GL_SHADING_LANGUAGE_VERSION));
        
        // Log available extensions (first 10 for brevity)
        String extensions = glGetString(GL_EXTENSIONS);
        if (extensions != null) {
            String[] extensionArray = extensions.split(" ");
            logger.debug("Available OpenGL extensions (showing first 10):");
            for (int i = 0; i < Math.min(10, extensionArray.length); i++) {
                logger.debug("  {}", extensionArray[i]);
            }
            logger.debug("  ... and {} more extensions", Math.max(0, extensionArray.length - 10));
        }
    }
    
    /**
     * Poll for window events. Should be called once per frame.
     */
    public void pollEvents() {
        glfwPollEvents();
    }
    
    /**
     * Swap the front and back buffers. Should be called once per frame.
     */
    public void swapBuffers() {
        glfwSwapBuffers(windowHandle);
    }
    
    /**
     * Update the window. Should be called once per frame.
     */
    public void update() {
        glfwSwapBuffers(windowHandle);
        
        // Reset resize flag
        resized = false;
    }
    
    /**
     * Check if the window should close.
     * 
     * @return true if the window should close
     */
    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }
    
    /**
     * Set whether the window should close.
     * 
     * @param shouldClose true to request window closure
     */
    public void setShouldClose(boolean shouldClose) {
        glfwSetWindowShouldClose(windowHandle, shouldClose);
    }
    
    /**
     * Toggle fullscreen mode.
     */
    public void toggleFullscreen() {
        setFullscreen(!fullscreen);
    }
    
    /**
     * Set fullscreen mode.
     * 
     * @param fullscreen true for fullscreen, false for windowed
     */
    public void setFullscreen(boolean fullscreen) {
        if (this.fullscreen == fullscreen) {
            return;
        }
        
        this.fullscreen = fullscreen;
        
        if (fullscreen) {
            // Switch to fullscreen
            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidMode != null) {
                glfwSetWindowMonitor(windowHandle, glfwGetPrimaryMonitor(), 
                                   0, 0, vidMode.width(), vidMode.height(), vidMode.refreshRate());
            }
        } else {
            // Switch to windowed
            glfwSetWindowMonitor(windowHandle, NULL, 100, 100, width, height, GLFW_DONT_CARE);
            centerWindow();
        }
        
        logger.info("Switched to {} mode", fullscreen ? "fullscreen" : "windowed");
    }
    
    /**
     * Set VSync enabled/disabled.
     * 
     * @param vsync true to enable VSync
     */
    public void setVSync(boolean vsync) {
        this.vsync = vsync;
        glfwSwapInterval(vsync ? 1 : 0);
        logger.debug("VSync {}", vsync ? "enabled" : "disabled");
    }
    
    /**
     * Set the window title.
     * 
     * @param title The new window title
     */
    public void setTitle(String title) {
        this.title = title;
        glfwSetWindowTitle(windowHandle, title);
    }
    
    /**
     * Set the window resize callback.
     * 
     * @param callback The resize callback
     */
    public void setResizeCallback(WindowResizeCallback callback) {
        this.resizeCallback = callback;
    }
    
    /**
     * Clear the framebuffer.
     */
    public void clear() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }
    
    // Getters
    
    public long getHandle() {
        return windowHandle;
    }
    
    public String getTitle() {
        return title;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public boolean isFullscreen() {
        return fullscreen;
    }
    
    public boolean isVSync() {
        return vsync;
    }
    
    public boolean wasResized() {
        return resized;
    }
    
    public float getAspectRatio() {
        return (float) width / (float) height;
    }
    
    public GLCapabilities getGLCapabilities() {
        return glCapabilities;
    }
    
    /**
     * Clean up resources.
     */
    public void cleanup() {
        logger.info("Cleaning up window");
        
        // Free callbacks (GLFW automatically frees callbacks when window is destroyed)
        // glfwFreeCallbacks(windowHandle); // This method doesn't exist in GLFW
        
        // Destroy window
        glfwDestroyWindow(windowHandle);
        
        // Terminate GLFW
        glfwTerminate();
        
        // Free error callback
        GLFWErrorCallback errorCallback = glfwSetErrorCallback(null);
        if (errorCallback != null) {
            errorCallback.free();
        }
        
        logger.info("Window cleanup complete");
    }
}