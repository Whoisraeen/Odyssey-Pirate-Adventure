package com.odyssey.input;

import com.odyssey.graphics.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Input management system for The Odyssey.
 * Handles keyboard, mouse, and gamepad input with proper state tracking.
 */
public class InputManager {
    private static final Logger logger = LoggerFactory.getLogger(InputManager.class);
    
    private final Window window;
    
    // Keyboard state
    private final Map<Integer, Boolean> keyPressed = new HashMap<>();
    private final Map<Integer, Boolean> keyJustPressed = new HashMap<>();
    private final Map<Integer, Boolean> keyJustReleased = new HashMap<>();
    
    // Mouse state
    private final Map<Integer, Boolean> mousePressed = new HashMap<>();
    private final Map<Integer, Boolean> mouseJustPressed = new HashMap<>();
    private final Map<Integer, Boolean> mouseJustReleased = new HashMap<>();
    
    private double mouseX, mouseY;
    private double mouseDeltaX, mouseDeltaY;
    private double lastMouseX, lastMouseY;
    private double scrollX, scrollY;
    
    // Input settings
    private boolean mouseCaptured = false;
    private float mouseSensitivity = 0.1f;
    
    public InputManager(Window window) {
        this.window = window;
    }
    
    public void initialize() {
        logger.info("Initializing input manager...");
        
        // Set up keyboard callback
        glfwSetKeyCallback(window.getHandle(), (windowHandle, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                keyPressed.put(key, true);
                keyJustPressed.put(key, true);
            } else if (action == GLFW_RELEASE) {
                keyPressed.put(key, false);
                keyJustReleased.put(key, true);
            }
        });
        
        // Set up mouse button callback
        glfwSetMouseButtonCallback(window.getHandle(), (windowHandle, button, action, mods) -> {
            if (action == GLFW_PRESS) {
                mousePressed.put(button, true);
                mouseJustPressed.put(button, true);
            } else if (action == GLFW_RELEASE) {
                mousePressed.put(button, false);
                mouseJustReleased.put(button, true);
            }
        });
        
        // Set up cursor position callback
        glfwSetCursorPosCallback(window.getHandle(), (windowHandle, xpos, ypos) -> {
            mouseX = xpos;
            mouseY = ypos;
        });
        
        // Set up scroll callback
        glfwSetScrollCallback(window.getHandle(), (windowHandle, xoffset, yoffset) -> {
            scrollX = xoffset;
            scrollY = yoffset;
        });
        
        // Initialize mouse position
        double[] xPos = new double[1];
        double[] yPos = new double[1];
        glfwGetCursorPos(window.getHandle(), xPos, yPos);
        mouseX = lastMouseX = xPos[0];
        mouseY = lastMouseY = yPos[0];
        
        logger.info("Input manager initialized successfully");
    }
    
    public void update() {
        // Calculate mouse delta
        mouseDeltaX = mouseX - lastMouseX;
        mouseDeltaY = mouseY - lastMouseY;
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        
        // Clear just pressed/released states
        keyJustPressed.clear();
        keyJustReleased.clear();
        mouseJustPressed.clear();
        mouseJustReleased.clear();
        
        // Reset scroll
        scrollX = scrollY = 0;
    }
    
    // Keyboard input methods
    public boolean isKeyPressed(int key) {
        return keyPressed.getOrDefault(key, false);
    }
    
    public boolean isKeyJustPressed(int key) {
        return keyJustPressed.getOrDefault(key, false);
    }
    
    public boolean isKeyJustReleased(int key) {
        return keyJustReleased.getOrDefault(key, false);
    }
    
    // Mouse input methods
    public boolean isMouseButtonPressed(int button) {
        return mousePressed.getOrDefault(button, false);
    }
    
    public boolean isMouseButtonJustPressed(int button) {
        return mouseJustPressed.getOrDefault(button, false);
    }
    
    public boolean isMouseButtonJustReleased(int button) {
        return mouseJustReleased.getOrDefault(button, false);
    }
    
    public double getMouseX() { return mouseX; }
    public double getMouseY() { return mouseY; }
    public double getMouseDeltaX() { return mouseDeltaX * mouseSensitivity; }
    public double getMouseDeltaY() { return mouseDeltaY * mouseSensitivity; }
    
    public double getScrollX() { return scrollX; }
    public double getScrollY() { return scrollY; }
    
    // Mouse capture for first-person camera
    public void setMouseCaptured(boolean captured) {
        this.mouseCaptured = captured;
        if (captured) {
            glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        } else {
            glfwSetInputMode(window.getHandle(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }
    }
    
    public boolean isMouseCaptured() { return mouseCaptured; }
    
    public float getMouseSensitivity() { return mouseSensitivity; }
    public void setMouseSensitivity(float sensitivity) { this.mouseSensitivity = sensitivity; }
    
    // Convenience methods for common game controls
    public boolean isMovingForward() {
        return isKeyPressed(GLFW_KEY_W) || isKeyPressed(GLFW_KEY_UP);
    }
    
    public boolean isMovingBackward() {
        return isKeyPressed(GLFW_KEY_S) || isKeyPressed(GLFW_KEY_DOWN);
    }
    
    public boolean isMovingLeft() {
        return isKeyPressed(GLFW_KEY_A) || isKeyPressed(GLFW_KEY_LEFT);
    }
    
    public boolean isMovingRight() {
        return isKeyPressed(GLFW_KEY_D) || isKeyPressed(GLFW_KEY_RIGHT);
    }
    
    public boolean isMovingUp() {
        return isKeyPressed(GLFW_KEY_SPACE);
    }
    
    public boolean isMovingDown() {
        return isKeyPressed(GLFW_KEY_LEFT_SHIFT) || isKeyPressed(GLFW_KEY_C);
    }
    
    public boolean isRunning() {
        return isKeyPressed(GLFW_KEY_LEFT_CONTROL);
    }
    
    public boolean isJumping() {
        return isKeyJustPressed(GLFW_KEY_SPACE);
    }
    
    // Ship controls
    public boolean isRaisingAnchor() {
        return isKeyJustPressed(GLFW_KEY_R);
    }
    
    public boolean isLoweringAnchor() {
        return isKeyJustPressed(GLFW_KEY_F);
    }
    
    public boolean isFiringSails() {
        return isKeyJustPressed(GLFW_KEY_E);
    }
    
    public boolean isFiringCannons() {
        return isMouseButtonJustPressed(GLFW_MOUSE_BUTTON_LEFT);
    }
    
    public boolean isOpeningInventory() {
        return isKeyJustPressed(GLFW_KEY_I) || isKeyJustPressed(GLFW_KEY_TAB);
    }
    
    public boolean isOpeningMap() {
        return isKeyJustPressed(GLFW_KEY_M);
    }
    
    public boolean isToggleDebug() {
        return isKeyJustPressed(GLFW_KEY_F3);
    }
    
    public boolean isToggleWireframe() {
        return isKeyJustPressed(GLFW_KEY_F4);
    }
    
    public void cleanup() {
        logger.info("Cleaning up input manager");
        // Reset callbacks
        glfwSetKeyCallback(window.getHandle(), null);
        glfwSetMouseButtonCallback(window.getHandle(), null);
        glfwSetCursorPosCallback(window.getHandle(), null);
        glfwSetScrollCallback(window.getHandle(), null);
    }
}