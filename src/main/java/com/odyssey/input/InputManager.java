package com.odyssey.input;

import com.odyssey.graphics.Window;
import org.lwjgl.glfw.GLFWGamepadState;
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

    // Gamepad state
    private final Map<Integer, Gamepad> gamepads = new HashMap<>();
    
    // Input settings
    private boolean mouseCaptured = false;
    private float mouseSensitivity = 0.1f;
    private float gamepadDeadzone = 0.2f;

    public static class Gamepad {
        private final int id;
        private final String name;
        private final GLFWGamepadState state = GLFWGamepadState.create();
        private final GLFWGamepadState prevState = GLFWGamepadState.create();

        public Gamepad(int id) {
            this.id = id;
            this.name = glfwGetGamepadName(id);
            logger.info("Gamepad connected: {} (ID: {})", name, id);
            update(); // Initial state
        }

        public void update() {
            // Copy current state to previous state
            prevState.set(state);
            // Get new state
            if (!glfwGetGamepadState(id, state)) {
                logger.warn("Failed to get state for gamepad {}", id);
            }
        }

        public boolean isButtonPressed(int button) {
            return state.buttons(button) == GLFW_PRESS;
        }

        public boolean isButtonJustPressed(int button) {
            return isButtonPressed(button) && (prevState.buttons(button) == GLFW_RELEASE);
        }

        public boolean isButtonJustReleased(int button) {
            return !isButtonPressed(button) && (prevState.buttons(button) == GLFW_PRESS);
        }

        public float getAxis(int axis) {
            return state.axes(axis);
        }

        public String getName() {
            return name;
        }
    }
    
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

        // Set up joystick callback for gamepad connection/disconnection
        glfwSetJoystickCallback((jid, event) -> {
            if (event == GLFW_CONNECTED) {
                if (glfwJoystickIsGamepad(jid)) {
                    gamepads.put(jid, new Gamepad(jid));
                }
            } else if (event == GLFW_DISCONNECTED) {
                if (gamepads.containsKey(jid)) {
                    logger.info("Gamepad disconnected: {} (ID: {})", gamepads.get(jid).getName(), jid);
                    gamepads.remove(jid);
                }
            }
        });

        // Detect initially connected gamepads
        for (int jid = GLFW_JOYSTICK_1; jid <= GLFW_JOYSTICK_LAST; jid++) {
            if (glfwJoystickPresent(jid) && glfwJoystickIsGamepad(jid)) {
                gamepads.put(jid, new Gamepad(jid));
            }
        }
        
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

        // Update gamepad states
        for (Gamepad gamepad : gamepads.values()) {
            gamepad.update();
        }
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

    // Gamepad input methods
    public Gamepad getGamepad(int jid) {
        return gamepads.get(jid);
    }

    public Map<Integer, Gamepad> getGamepads() {
        return gamepads;
    }

    private float applyDeadzone(float value) {
        if (Math.abs(value) < gamepadDeadzone) {
            return 0.0f;
        }
        return value;
    }
    
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
        boolean gamepadForward = false;
        for (Gamepad pad : gamepads.values()) {
            if (applyDeadzone(pad.getAxis(GLFW_GAMEPAD_AXIS_LEFT_Y)) < -gamepadDeadzone) {
                gamepadForward = true;
                break;
            }
        }
        return isKeyPressed(GLFW_KEY_W) || isKeyPressed(GLFW_KEY_UP) || gamepadForward;
    }
    
    public boolean isMovingBackward() {
        boolean gamepadBackward = false;
        for (Gamepad pad : gamepads.values()) {
            if (applyDeadzone(pad.getAxis(GLFW_GAMEPAD_AXIS_LEFT_Y)) > gamepadDeadzone) {
                gamepadBackward = true;
                break;
            }
        }
        return isKeyPressed(GLFW_KEY_S) || isKeyPressed(GLFW_KEY_DOWN) || gamepadBackward;
    }
    
    public boolean isMovingLeft() {
        boolean gamepadLeft = false;
        for (Gamepad pad : gamepads.values()) {
            if (applyDeadzone(pad.getAxis(GLFW_GAMEPAD_AXIS_LEFT_X)) < -gamepadDeadzone) {
                gamepadLeft = true;
                break;
            }
        }
        return isKeyPressed(GLFW_KEY_A) || isKeyPressed(GLFW_KEY_LEFT) || gamepadLeft;
    }
    
    public boolean isMovingRight() {
        boolean gamepadRight = false;
        for (Gamepad pad : gamepads.values()) {
            if (applyDeadzone(pad.getAxis(GLFW_GAMEPAD_AXIS_LEFT_X)) > gamepadDeadzone) {
                gamepadRight = true;
                break;
            }
        }
        return isKeyPressed(GLFW_KEY_D) || isKeyPressed(GLFW_KEY_RIGHT) || gamepadRight;
    }
    
    public boolean isMovingUp() {
        boolean gamepadUp = false;
        for (Gamepad pad : gamepads.values()) {
            if (pad.isButtonPressed(GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER)) {
                gamepadUp = true;
                break;
            }
        }
        return isKeyPressed(GLFW_KEY_SPACE) || gamepadUp;
    }
    
    public boolean isMovingDown() {
        boolean gamepadDown = false;
        for (Gamepad pad : gamepads.values()) {
            if (pad.isButtonPressed(GLFW_GAMEPAD_BUTTON_LEFT_BUMPER)) {
                gamepadDown = true;
                break;
            }
        }
        return isKeyPressed(GLFW_KEY_LEFT_SHIFT) || isKeyPressed(GLFW_KEY_C) || gamepadDown;
    }
    
    public boolean isRunning() {
        boolean gamepadRun = false;
        for (Gamepad pad : gamepads.values()) {
            if (pad.isButtonPressed(GLFW_GAMEPAD_BUTTON_LEFT_THUMB)) {
                gamepadRun = true;
                break;
            }
        }
        return isKeyPressed(GLFW_KEY_LEFT_CONTROL) || gamepadRun;
    }
    
    public boolean isJumping() {
        boolean gamepadJump = false;
        for (Gamepad pad : gamepads.values()) {
            if (pad.isButtonJustPressed(GLFW_GAMEPAD_BUTTON_A)) {
                gamepadJump = true;
                break;
            }
        }
        return isKeyJustPressed(GLFW_KEY_SPACE) || gamepadJump;
    }
    
    // Ship controls
    public boolean isRaisingAnchor() {
        boolean gamepadRaise = false;
        for (Gamepad pad : gamepads.values()) {
            if (pad.isButtonJustPressed(GLFW_GAMEPAD_BUTTON_DPAD_UP)) {
                gamepadRaise = true;
                break;
            }
        }
        return isKeyJustPressed(GLFW_KEY_R) || gamepadRaise;
    }
    
    public boolean isLoweringAnchor() {
        boolean gamepadLower = false;
        for (Gamepad pad : gamepads.values()) {
            if (pad.isButtonJustPressed(GLFW_GAMEPAD_BUTTON_DPAD_DOWN)) {
                gamepadLower = true;
                break;
            }
        }
        return isKeyJustPressed(GLFW_KEY_F) || gamepadLower;
    }
    
    public boolean isFiringSails() {
        boolean gamepadSails = false;
        for (Gamepad pad : gamepads.values()) {
            if (pad.isButtonJustPressed(GLFW_GAMEPAD_BUTTON_X)) {
                gamepadSails = true;
                break;
            }
        }
        return isKeyJustPressed(GLFW_KEY_E) || gamepadSails;
    }
    
    public boolean isFiringCannons() {
        boolean gamepadFire = false;
        for (Gamepad pad : gamepads.values()) {
            if (pad.getAxis(GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER) > 0.5f) {
                gamepadFire = true;
                break;
            }
        }
        return isMouseButtonJustPressed(GLFW_MOUSE_BUTTON_LEFT) || gamepadFire;
    }
    
    public boolean isOpeningInventory() {
        boolean gamepadInventory = false;
        for (Gamepad pad : gamepads.values()) {
            if (pad.isButtonJustPressed(GLFW_GAMEPAD_BUTTON_Y)) {
                gamepadInventory = true;
                break;
            }
        }
        return isKeyJustPressed(GLFW_KEY_I) || isKeyJustPressed(GLFW_KEY_TAB) || gamepadInventory;
    }
    
    public boolean isOpeningMap() {
        boolean gamepadMap = false;
        for (Gamepad pad : gamepads.values()) {
            if (pad.isButtonJustPressed(GLFW_GAMEPAD_BUTTON_BACK)) {
                gamepadMap = true;
                break;
            }
        }
        return isKeyJustPressed(GLFW_KEY_M) || gamepadMap;
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
        glfwSetJoystickCallback(null);
    }
}