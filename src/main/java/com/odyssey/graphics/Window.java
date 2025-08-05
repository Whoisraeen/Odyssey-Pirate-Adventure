package com.odyssey.graphics;

import com.odyssey.core.GameConfig;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Window management class for The Odyssey.
 * Handles window creation, OpenGL context setup, and basic window operations.
 */
public class Window {
    private static final Logger logger = LoggerFactory.getLogger(Window.class);
    
    private final GameConfig config;
    private long windowHandle;
    private int width;
    private int height;
    private String title = "The Odyssey: Pirate Adventure";
    
    // Window state
    private boolean resized = false;
    private boolean vSync;
    
    public Window(GameConfig config) {
        this.config = config;
        this.width = config.getWindowWidth();
        this.height = config.getWindowHeight();
        this.vSync = config.isVsync();
    }
    
    public void initialize() {
        logger.info("Initializing window: {}x{}, fullscreen: {}", width, height, config.isFullscreen());
        
        // Setup error callback
        GLFWErrorCallback.createPrint(System.err).set();
        
        // Configure GLFW
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        
        // Enable multisampling for anti-aliasing
        glfwWindowHint(GLFW_SAMPLES, 4);
        
        // Create window
        long monitor = config.isFullscreen() ? glfwGetPrimaryMonitor() : NULL;
        windowHandle = glfwCreateWindow(width, height, title, monitor, NULL);
        
        if (windowHandle == NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }
        
        // Setup callbacks
        setupCallbacks();
        
        // Center window if not fullscreen
        if (!config.isFullscreen()) {
            centerWindow();
        }
        
        // Make OpenGL context current
        glfwMakeContextCurrent(windowHandle);
        
        // Enable v-sync
        glfwSwapInterval(vSync ? 1 : 0);
        
        // Show window
        glfwShowWindow(windowHandle);
        
        // Initialize OpenGL
        GL.createCapabilities();
        
        // Set initial OpenGL state
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glEnable(GL_MULTISAMPLE); // Enable anti-aliasing
        
        // Set clear color to ocean blue
        glClearColor(0.1f, 0.3f, 0.6f, 1.0f);
        
        logger.info("Window initialized successfully");
    }
    
    private void setupCallbacks() {
        // Window resize callback
        glfwSetFramebufferSizeCallback(windowHandle, (window, w, h) -> {
            this.width = w;
            this.height = h;
            this.resized = true;
            glViewport(0, 0, w, h);
            logger.debug("Window resized to {}x{}", w, h);
        });
        
        // Window close callback
        glfwSetWindowCloseCallback(windowHandle, window -> {
            logger.info("Window close requested");
        });
        
        // Key callback for basic window controls
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_F11 && action == GLFW_PRESS) {
                toggleFullscreen();
            }
        });
    }
    
    private void centerWindow() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            
            glfwGetWindowSize(windowHandle, pWidth, pHeight);
            
            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidMode != null) {
                glfwSetWindowPos(windowHandle,
                    (vidMode.width() - pWidth.get(0)) / 2,
                    (vidMode.height() - pHeight.get(0)) / 2);
            }
        }
    }
    
    public void toggleFullscreen() {
        boolean isFullscreen = glfwGetWindowMonitor(windowHandle) != NULL;
        
        if (isFullscreen) {
            // Switch to windowed mode
            glfwSetWindowMonitor(windowHandle, NULL, 100, 100, width, height, GLFW_DONT_CARE);
            logger.info("Switched to windowed mode");
        } else {
            // Switch to fullscreen mode
            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (vidMode != null) {
                glfwSetWindowMonitor(windowHandle, glfwGetPrimaryMonitor(), 0, 0, 
                                   vidMode.width(), vidMode.height(), vidMode.refreshRate());
                logger.info("Switched to fullscreen mode");
            }
        }
    }
    
    public void swapBuffers() {
        glfwSwapBuffers(windowHandle);
    }
    
    public boolean shouldClose() {
        return glfwWindowShouldClose(windowHandle);
    }
    
    public void setTitle(String title) {
        this.title = title;
        glfwSetWindowTitle(windowHandle, title);
    }
    
    public void cleanup() {
        logger.info("Cleaning up window resources");
        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);
    }
    
    // Getters
    public long getHandle() { return windowHandle; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getTitle() { return title; }
    public boolean isResized() { return resized; }
    public boolean isVSync() { return vSync; }
    
    public void setResized(boolean resized) { this.resized = resized; }
    
    public void setVSync(boolean vSync) {
        this.vSync = vSync;
        glfwSwapInterval(vSync ? 1 : 0);
    }
    
    public float getAspectRatio() {
        return (float) width / height;
    }
}