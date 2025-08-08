package com.odyssey.ui;

import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles gamepad input and provides controller-specific UI layouts.
 * Supports Xbox, PlayStation, and generic controllers.
 */
public class GamepadController {
    private static final Logger logger = LoggerFactory.getLogger(GamepadController.class);
    
    // Gamepad button mappings (Xbox controller layout)
    public static final int BUTTON_A = 0;
    public static final int BUTTON_B = 1;
    public static final int BUTTON_X = 2;
    public static final int BUTTON_Y = 3;
    public static final int BUTTON_LEFT_BUMPER = 4;
    public static final int BUTTON_RIGHT_BUMPER = 5;
    public static final int BUTTON_BACK = 6;
    public static final int BUTTON_START = 7;
    public static final int BUTTON_GUIDE = 8;
    public static final int BUTTON_LEFT_THUMB = 9;
    public static final int BUTTON_RIGHT_THUMB = 10;
    public static final int BUTTON_DPAD_UP = 11;
    public static final int BUTTON_DPAD_RIGHT = 12;
    public static final int BUTTON_DPAD_DOWN = 13;
    public static final int BUTTON_DPAD_LEFT = 14;
    
    // Gamepad axes
    public static final int AXIS_LEFT_X = 0;
    public static final int AXIS_LEFT_Y = 1;
    public static final int AXIS_RIGHT_X = 2;
    public static final int AXIS_RIGHT_Y = 3;
    public static final int AXIS_LEFT_TRIGGER = 4;
    public static final int AXIS_RIGHT_TRIGGER = 5;
    
    private final Map<Integer, GamepadState> connectedGamepads = new HashMap<>();
    private int primaryGamepadId = -1;
    
    /**
     * Represents the state of a connected gamepad.
     */
    public static class GamepadState {
        private final int id;
        private final String name;
        private final ControllerType type;
        private final boolean[] buttons = new boolean[15];
        private final boolean[] previousButtons = new boolean[15];
        private final float[] axes = new float[6];
        private final float[] previousAxes = new float[6];
        private boolean connected = true;
        
        public GamepadState(int id, String name, ControllerType type) {
            this.id = id;
            this.name = name;
            this.type = type;
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public ControllerType getType() { return type; }
        public boolean isConnected() { return connected; }
        
        public boolean isButtonPressed(int button) {
            return button >= 0 && button < buttons.length && buttons[button];
        }
        
        public boolean isButtonJustPressed(int button) {
            return button >= 0 && button < buttons.length && 
                   buttons[button] && !previousButtons[button];
        }
        
        public boolean isButtonJustReleased(int button) {
            return button >= 0 && button < buttons.length && 
                   !buttons[button] && previousButtons[button];
        }
        
        public float getAxis(int axis) {
            return axis >= 0 && axis < axes.length ? axes[axis] : 0.0f;
        }
        
        public Vector2f getLeftStick() {
            return new Vector2f(getAxis(AXIS_LEFT_X), getAxis(AXIS_LEFT_Y));
        }
        
        public Vector2f getRightStick() {
            return new Vector2f(getAxis(AXIS_RIGHT_X), getAxis(AXIS_RIGHT_Y));
        }
        
        public float getLeftTrigger() {
            return getAxis(AXIS_LEFT_TRIGGER);
        }
        
        public float getRightTrigger() {
            return getAxis(AXIS_RIGHT_TRIGGER);
        }
        
        void updateButtons(ByteBuffer buttonBuffer) {
            System.arraycopy(buttons, 0, previousButtons, 0, buttons.length);
            for (int i = 0; i < Math.min(buttons.length, buttonBuffer.remaining()); i++) {
                buttons[i] = buttonBuffer.get(i) == GLFW.GLFW_PRESS;
            }
        }
        
        void updateAxes(FloatBuffer axisBuffer) {
            System.arraycopy(axes, 0, previousAxes, 0, axes.length);
            for (int i = 0; i < Math.min(axes.length, axisBuffer.remaining()); i++) {
                axes[i] = axisBuffer.get(i);
            }
        }
        
        void setConnected(boolean connected) {
            this.connected = connected;
        }
    }
    
    /**
     * Controller types for different button layouts and icons.
     */
    public enum ControllerType {
        XBOX("Xbox Controller"),
        PLAYSTATION("PlayStation Controller"),
        NINTENDO("Nintendo Controller"),
        GENERIC("Generic Controller");
        
        private final String displayName;
        
        ControllerType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Initializes the gamepad controller system.
     */
    public void initialize() {
        logger.info("Initializing GamepadController");
        
        // Set up gamepad connection callbacks
        GLFW.glfwSetJoystickCallback((jid, event) -> {
            if (event == GLFW.GLFW_CONNECTED) {
                onGamepadConnected(jid);
            } else if (event == GLFW.GLFW_DISCONNECTED) {
                onGamepadDisconnected(jid);
            }
        });
        
        // Check for already connected gamepads
        for (int jid = GLFW.GLFW_JOYSTICK_1; jid <= GLFW.GLFW_JOYSTICK_LAST; jid++) {
            if (GLFW.glfwJoystickPresent(jid)) {
                onGamepadConnected(jid);
            }
        }
        
        logger.info("GamepadController initialized with {} connected gamepads", connectedGamepads.size());
    }
    
    /**
     * Updates all connected gamepad states.
     */
    public void update() {
        for (GamepadState gamepad : connectedGamepads.values()) {
            if (!gamepad.isConnected()) {
                continue;
            }
            
            int jid = gamepad.getId();
            
            // Update button states
            ByteBuffer buttons = GLFW.glfwGetJoystickButtons(jid);
            if (buttons != null) {
                gamepad.updateButtons(buttons);
            }
            
            // Update axis states
            FloatBuffer axes = GLFW.glfwGetJoystickAxes(jid);
            if (axes != null) {
                gamepad.updateAxes(axes);
            }
        }
    }
    
    /**
     * Gets the primary gamepad (first connected gamepad).
     */
    public GamepadState getPrimaryGamepad() {
        if (primaryGamepadId >= 0 && connectedGamepads.containsKey(primaryGamepadId)) {
            return connectedGamepads.get(primaryGamepadId);
        }
        return null;
    }
    
    /**
     * Gets a gamepad by ID.
     */
    public GamepadState getGamepad(int id) {
        return connectedGamepads.get(id);
    }
    
    /**
     * Gets all connected gamepads.
     */
    public Map<Integer, GamepadState> getConnectedGamepads() {
        return new HashMap<>(connectedGamepads);
    }
    
    /**
     * Checks if any gamepad is connected.
     */
    public boolean hasConnectedGamepad() {
        return !connectedGamepads.isEmpty() && 
               connectedGamepads.values().stream().anyMatch(GamepadState::isConnected);
    }
    
    /**
     * Gets button name for display purposes.
     */
    public String getButtonName(int button, ControllerType type) {
        return switch (type) {
            case XBOX -> getXboxButtonName(button);
            case PLAYSTATION -> getPlayStationButtonName(button);
            case NINTENDO -> getNintendoButtonName(button);
            default -> getGenericButtonName(button);
        };
    }
    
    private String getXboxButtonName(int button) {
        return switch (button) {
            case BUTTON_A -> "A";
            case BUTTON_B -> "B";
            case BUTTON_X -> "X";
            case BUTTON_Y -> "Y";
            case BUTTON_LEFT_BUMPER -> "LB";
            case BUTTON_RIGHT_BUMPER -> "RB";
            case BUTTON_BACK -> "Back";
            case BUTTON_START -> "Start";
            case BUTTON_GUIDE -> "Guide";
            case BUTTON_LEFT_THUMB -> "LS";
            case BUTTON_RIGHT_THUMB -> "RS";
            case BUTTON_DPAD_UP -> "D-Pad Up";
            case BUTTON_DPAD_RIGHT -> "D-Pad Right";
            case BUTTON_DPAD_DOWN -> "D-Pad Down";
            case BUTTON_DPAD_LEFT -> "D-Pad Left";
            default -> "Button " + button;
        };
    }
    
    private String getPlayStationButtonName(int button) {
        return switch (button) {
            case BUTTON_A -> "Cross";
            case BUTTON_B -> "Circle";
            case BUTTON_X -> "Square";
            case BUTTON_Y -> "Triangle";
            case BUTTON_LEFT_BUMPER -> "L1";
            case BUTTON_RIGHT_BUMPER -> "R1";
            case BUTTON_BACK -> "Share";
            case BUTTON_START -> "Options";
            case BUTTON_GUIDE -> "PS";
            case BUTTON_LEFT_THUMB -> "L3";
            case BUTTON_RIGHT_THUMB -> "R3";
            case BUTTON_DPAD_UP -> "D-Pad Up";
            case BUTTON_DPAD_RIGHT -> "D-Pad Right";
            case BUTTON_DPAD_DOWN -> "D-Pad Down";
            case BUTTON_DPAD_LEFT -> "D-Pad Left";
            default -> "Button " + button;
        };
    }
    
    private String getNintendoButtonName(int button) {
        return switch (button) {
            case BUTTON_A -> "B";
            case BUTTON_B -> "A";
            case BUTTON_X -> "Y";
            case BUTTON_Y -> "X";
            case BUTTON_LEFT_BUMPER -> "L";
            case BUTTON_RIGHT_BUMPER -> "R";
            case BUTTON_BACK -> "-";
            case BUTTON_START -> "+";
            case BUTTON_GUIDE -> "Home";
            case BUTTON_LEFT_THUMB -> "LS";
            case BUTTON_RIGHT_THUMB -> "RS";
            case BUTTON_DPAD_UP -> "D-Pad Up";
            case BUTTON_DPAD_RIGHT -> "D-Pad Right";
            case BUTTON_DPAD_DOWN -> "D-Pad Down";
            case BUTTON_DPAD_LEFT -> "D-Pad Left";
            default -> "Button " + button;
        };
    }
    
    private String getGenericButtonName(int button) {
        return "Button " + button;
    }
    
    /**
     * Detects controller type from name.
     */
    private ControllerType detectControllerType(String name) {
        String lowerName = name.toLowerCase();
        
        if (lowerName.contains("xbox") || lowerName.contains("xinput")) {
            return ControllerType.XBOX;
        } else if (lowerName.contains("playstation") || lowerName.contains("ps4") || 
                   lowerName.contains("ps5") || lowerName.contains("dualshock") || 
                   lowerName.contains("dualsense")) {
            return ControllerType.PLAYSTATION;
        } else if (lowerName.contains("nintendo") || lowerName.contains("switch") || 
                   lowerName.contains("pro controller")) {
            return ControllerType.NINTENDO;
        } else {
            return ControllerType.GENERIC;
        }
    }
    
    /**
     * Called when a gamepad is connected.
     */
    private void onGamepadConnected(int jid) {
        String name = GLFW.glfwGetJoystickName(jid);
        if (name == null) {
            name = "Unknown Controller";
        }
        
        ControllerType type = detectControllerType(name);
        GamepadState gamepad = new GamepadState(jid, name, type);
        
        connectedGamepads.put(jid, gamepad);
        
        // Set as primary if it's the first connected gamepad
        if (primaryGamepadId == -1) {
            primaryGamepadId = jid;
        }
        
        logger.info("Gamepad connected: {} (ID: {}, Type: {})", name, jid, type);
    }
    
    /**
     * Called when a gamepad is disconnected.
     */
    private void onGamepadDisconnected(int jid) {
        GamepadState gamepad = connectedGamepads.get(jid);
        if (gamepad != null) {
            gamepad.setConnected(false);
            logger.info("Gamepad disconnected: {} (ID: {})", gamepad.getName(), jid);
            
            // Find new primary gamepad if this was the primary
            if (primaryGamepadId == jid) {
                primaryGamepadId = connectedGamepads.keySet().stream()
                    .filter(id -> connectedGamepads.get(id).isConnected())
                    .findFirst()
                    .orElse(-1);
            }
        }
    }
    
    /**
     * Cleans up resources.
     */
    public void cleanup() {
        logger.info("Cleaning up GamepadController");
        connectedGamepads.clear();
        primaryGamepadId = -1;
    }
}