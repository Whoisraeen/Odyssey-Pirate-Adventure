package com.odyssey.input;

import com.odyssey.core.GameConfig;
import com.odyssey.util.Logger;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;
import org.lwjgl.glfw.GLFW;
import org.joml.Vector2f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Comprehensive input management system for the Odyssey Pirate Adventure.
 * 
 * This class provides a unified interface for handling input from multiple device types:
 * - Keyboard and mouse via LWJGL/GLFW
 * - Game controllers via jamepad (SDL2 GameController API)
 * 
 * Features:
 * - Action-based input mapping (GameAction enum)
 * - Hot-swapping between input devices
 * - Centralized input dispatch system
 * - Default control schemes for keyboard and gamepad
 * - Device-specific UI adaptation
 * 
 * @author Odyssey Development Team
 * @version 2.0
 * @since Java 25
 */
public class InputManager {
    
    private static final Logger logger = Logger.getLogger(InputManager.class);
    
    // Input thresholds
    private float controllerDeadzone;
    private float controllerSensitivity;
    private boolean invertControllerY;
    private static final float CONTROLLER_TRIGGER_THRESHOLD = 0.5f; // Reserved for future trigger-based actions
    
    // Window handle for GLFW
    private long windowHandle;
    
    // Controller management
    private ControllerManager controllerManager;
    private int activeControllerIndex = -1;
    
    // Device tracking
    private DeviceType activeDevice = DeviceType.KEYBOARD;
    private long lastInputTime = 0;
    
    // Input context management
    private InputContext currentContext = InputContext.GAMEPLAY;
    private final Map<InputContext, Map<GameAction, Set<Integer>>> contextKeyboardMappings = new EnumMap<>(InputContext.class);
    private final Map<InputContext, Map<GameAction, Set<Integer>>> contextMouseMappings = new EnumMap<>(InputContext.class);
    private final Map<InputContext, Map<GameAction, Set<ControllerButton>>> contextGamepadButtonMappings = new EnumMap<>(InputContext.class);
    private final Map<InputContext, Map<GameAction, ControllerAxis>> contextGamepadAxisMappings = new EnumMap<>(InputContext.class);
    
    // Action mappings (current context)
    private final Map<GameAction, Set<Integer>> keyboardMappings = new EnumMap<>(GameAction.class);
    private final Map<GameAction, Set<Integer>> mouseMappings = new EnumMap<>(GameAction.class);
    private final Map<GameAction, Set<ControllerButton>> gamepadButtonMappings = new EnumMap<>(GameAction.class);
    private final Map<GameAction, ControllerAxis> gamepadAxisMappings = new EnumMap<>(GameAction.class);
    
    // Input state tracking
    private final Map<Integer, Boolean> keyStates = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> keyPressed = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> keyReleased = new ConcurrentHashMap<>();
    
    private final Map<Integer, Boolean> mouseButtonStates = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> mouseButtonPressed = new ConcurrentHashMap<>();
    private final Map<Integer, Boolean> mouseButtonReleased = new ConcurrentHashMap<>();
    
    private final Vector2f mousePosition = new Vector2f();
    private final Vector2f mouseDelta = new Vector2f();
    private final Vector2f lastMousePosition = new Vector2f();
    private float scrollDelta = 0.0f;
    
    // Action state tracking
    private final Set<GameAction> activeActions = EnumSet.noneOf(GameAction.class);
    private final Set<GameAction> pressedActions = EnumSet.noneOf(GameAction.class);
    private final Set<GameAction> releasedActions = EnumSet.noneOf(GameAction.class);
    
    // Input dispatch callback
    private InputDispatcher inputDispatcher;
    
    /**
     * Functional interface for handling input dispatch events.
     */
    @FunctionalInterface
    public interface InputDispatcher {
        /**
         * Called when a game action is triggered.
         * 
         * @param action The triggered action
         * @param device The device that triggered the action
         */
        void dispatch(GameAction action, DeviceType device);
    }
    
    /**
     * Constructs a new InputManager with the specified window handle.
     * 
     * @param windowHandle The GLFW window handle
     */
    public InputManager(long windowHandle) {
        this.windowHandle = windowHandle;
        
        // Load controller settings from GameConfig
        GameConfig config = GameConfig.getInstance();
        this.controllerDeadzone = config.getControllerDeadzone();
        this.controllerSensitivity = config.getControllerSensitivity();
        this.invertControllerY = config.isInvertControllerY();
        
        initializeContextMappings();
        initializeDefaultMappings();
    }
    
    /**
     * Initializes the input manager and sets up input callbacks.
     * Must be called before using the input manager.
     */
    public void initialize() {
        logger.info("Initializing InputManager with LWJGL {} and jamepad controller support", 
                   org.lwjgl.Version.getVersion());
        
        setupKeyboardCallbacks();
        setupMouseCallbacks();
        setupScrollCallback();
        initializeControllers();
        
        logger.info("InputManager initialized successfully");
    }
    
    /**
     * Sets up keyboard input callbacks with GLFW.
     */
    private void setupKeyboardCallbacks() {
        glfwSetKeyCallback(windowHandle, (_, key, _, action, _) -> {
            var currentTime = System.currentTimeMillis();
            
            if (action == GLFW_PRESS) {
                keyStates.put(key, true);
                keyPressed.put(key, true);
                updateActiveDevice(DeviceType.KEYBOARD, currentTime);
            } else if (action == GLFW_RELEASE) {
                keyStates.put(key, false);
                keyReleased.put(key, true);
                updateActiveDevice(DeviceType.KEYBOARD, currentTime);
            }
        });
    }
    
    /**
     * Sets up mouse input callbacks with GLFW.
     */
    private void setupMouseCallbacks() {
        // Mouse button callback
        glfwSetMouseButtonCallback(windowHandle, (_, button, action, _) -> {
            var currentTime = System.currentTimeMillis();
            
            if (action == GLFW_PRESS) {
                mouseButtonStates.put(button, true);
                mouseButtonPressed.put(button, true);
                updateActiveDevice(DeviceType.KEYBOARD, currentTime);
            } else if (action == GLFW_RELEASE) {
                mouseButtonStates.put(button, false);
                mouseButtonReleased.put(button, true);
                updateActiveDevice(DeviceType.KEYBOARD, currentTime);
            }
        });
        
        // Mouse position callback
        glfwSetCursorPosCallback(windowHandle, (_, xpos, ypos) -> {
            mouseDelta.set((float) xpos - lastMousePosition.x, (float) ypos - lastMousePosition.y);
            mousePosition.set((float) xpos, (float) ypos);
            lastMousePosition.set(mousePosition);
            
            // Only update active device if there's significant movement
            if (mouseDelta.lengthSquared() > 1.0f) {
                updateActiveDevice(DeviceType.KEYBOARD, System.currentTimeMillis());
            }
        });
    }
    
    /**
     * Sets up scroll wheel callback with GLFW.
     */
    private void setupScrollCallback() {
        glfwSetScrollCallback(windowHandle, (_, _, yoffset) -> {
            scrollDelta = (float) yoffset;
            updateActiveDevice(DeviceType.KEYBOARD, System.currentTimeMillis());
        });
    }
    
    /**
     * Initializes controller support using jamepad.
     */
    private void initializeControllers() {
        try {
            controllerManager = new ControllerManager();
            controllerManager.initSDLGamepad();
            logger.info("Controller support initialized successfully");
        } catch (Exception e) {
            logger.warn("Failed to initialize controller support: {}", e.getMessage());
            controllerManager = null;
        }
    }
    
    /**
     * Sets up default control mappings for keyboard, mouse, and gamepad.
     * Follows Minecraft Java Edition and Bedrock Edition control schemes.
     */
    private void initializeDefaultMappings() {
        // === MINECRAFT JAVA EDITION KEYBOARD MAPPINGS ===
        
        // Movement (WASD)
        addKeyboardMapping(GameAction.MOVE_FORWARD, GLFW_KEY_W);
        addKeyboardMapping(GameAction.MOVE_BACKWARD, GLFW_KEY_S);
        addKeyboardMapping(GameAction.STRAFE_LEFT, GLFW_KEY_A);
        addKeyboardMapping(GameAction.STRAFE_RIGHT, GLFW_KEY_D);
        
        // Jump and movement modifiers
        addKeyboardMapping(GameAction.JUMP, GLFW_KEY_SPACE);
        addKeyboardMapping(GameAction.SWIM_UP, GLFW_KEY_SPACE);
        addKeyboardMapping(GameAction.SNEAK, GLFW_KEY_LEFT_SHIFT);
        addKeyboardMapping(GameAction.SWIM_DOWN, GLFW_KEY_LEFT_SHIFT);
        addKeyboardMapping(GameAction.SPRINT, GLFW_KEY_LEFT_CONTROL);
        
        // Inventory and items
        addKeyboardMapping(GameAction.INVENTORY, GLFW_KEY_E);
        addKeyboardMapping(GameAction.DROP_ITEM, GLFW_KEY_Q);
        addKeyboardMapping(GameAction.SWAP_HANDS, GLFW_KEY_F);
        
        // Hotbar slots (1-9)
        addKeyboardMapping(GameAction.HOTBAR_1, GLFW_KEY_1);
        addKeyboardMapping(GameAction.HOTBAR_2, GLFW_KEY_2);
        addKeyboardMapping(GameAction.HOTBAR_3, GLFW_KEY_3);
        addKeyboardMapping(GameAction.HOTBAR_4, GLFW_KEY_4);
        addKeyboardMapping(GameAction.HOTBAR_5, GLFW_KEY_5);
        addKeyboardMapping(GameAction.HOTBAR_6, GLFW_KEY_6);
        addKeyboardMapping(GameAction.HOTBAR_7, GLFW_KEY_7);
        addKeyboardMapping(GameAction.HOTBAR_8, GLFW_KEY_8);
        addKeyboardMapping(GameAction.HOTBAR_9, GLFW_KEY_9);
        
        // Interface actions
        addKeyboardMapping(GameAction.MENU, GLFW_KEY_ESCAPE);
        addKeyboardMapping(GameAction.CHAT, GLFW_KEY_T);
        addKeyboardMapping(GameAction.COMMAND, GLFW_KEY_SLASH);
        addKeyboardMapping(GameAction.SCREENSHOT, GLFW_KEY_F2);
        addKeyboardMapping(GameAction.TOGGLE_FULLSCREEN, GLFW_KEY_F11);
        addKeyboardMapping(GameAction.TOGGLE_PERSPECTIVE, GLFW_KEY_F5);
        
        // Debug actions
        addKeyboardMapping(GameAction.DEBUG_TOGGLE, GLFW_KEY_F3);
        
        // Game-specific actions
        addKeyboardMapping(GameAction.MAP, GLFW_KEY_M);
        addKeyboardMapping(GameAction.SHIP_BUILDER, GLFW_KEY_B);
        addKeyboardMapping(GameAction.QUICK_SAVE, GLFW_KEY_F5);
        
        // Legacy mappings for backward compatibility
        addKeyboardMapping(GameAction.TURN_LEFT, GLFW_KEY_A);
        addKeyboardMapping(GameAction.TURN_RIGHT, GLFW_KEY_D);
        addKeyboardMapping(GameAction.PRIMARY_ACTION, GLFW_KEY_ENTER);
        addKeyboardMapping(GameAction.INTERACT, GLFW_KEY_E);
        addKeyboardMapping(GameAction.SETTINGS, GLFW_KEY_ESCAPE);
        
        // === MINECRAFT JAVA EDITION MOUSE MAPPINGS ===
        
        // Primary actions
        addMouseMapping(GameAction.ATTACK, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        addMouseMapping(GameAction.USE_ITEM, GLFW.GLFW_MOUSE_BUTTON_RIGHT);
        addMouseMapping(GameAction.PICK_BLOCK, GLFW.GLFW_MOUSE_BUTTON_MIDDLE);
        
        // === MINECRAFT BEDROCK EDITION CONTROLLER MAPPINGS ===
        
        // Face buttons (Xbox layout)
        addGamepadButtonMapping(GameAction.JUMP, ControllerButton.A);           // A button
        addGamepadButtonMapping(GameAction.SWIM_UP, ControllerButton.A);
        addGamepadButtonMapping(GameAction.DROP_ITEM, ControllerButton.B);      // B button
        addGamepadButtonMapping(GameAction.SWAP_HANDS, ControllerButton.X);     // X button
        addGamepadButtonMapping(GameAction.INVENTORY, ControllerButton.Y);      // Y button
        addGamepadButtonMapping(GameAction.INTERACT, ControllerButton.Y);
        
        // Shoulder buttons
        addGamepadButtonMapping(GameAction.HOTBAR_5, ControllerButton.LEFT_BUMPER);   // LB/L1
        addGamepadButtonMapping(GameAction.HOTBAR_6, ControllerButton.RIGHT_BUMPER);  // RB/R1
        
        // Triggers
        addGamepadAxisMapping(GameAction.USE_ITEM, ControllerAxis.LEFT_TRIGGER);  // LT/L2
        addGamepadAxisMapping(GameAction.ATTACK, ControllerAxis.RIGHT_TRIGGER);   // RT/R2
        
        // D-pad for hotbar
        addGamepadButtonMapping(GameAction.HOTBAR_1, ControllerButton.DPAD_UP);
        addGamepadButtonMapping(GameAction.HOTBAR_2, ControllerButton.DPAD_RIGHT);
        addGamepadButtonMapping(GameAction.HOTBAR_3, ControllerButton.DPAD_DOWN);
        addGamepadButtonMapping(GameAction.HOTBAR_4, ControllerButton.DPAD_LEFT);
        
        // Menu buttons
        addGamepadButtonMapping(GameAction.MENU, ControllerButton.START);       // Start/Menu button
        addGamepadButtonMapping(GameAction.MAP, ControllerButton.BACK);         // Back/Select button
        
        // Stick clicks
        addGamepadButtonMapping(GameAction.SPRINT, ControllerButton.LEFT_STICK);     // L3
        addGamepadButtonMapping(GameAction.SNEAK, ControllerButton.RIGHT_STICK);    // R3
        addGamepadButtonMapping(GameAction.SWIM_DOWN, ControllerButton.RIGHT_STICK);
        
        // === CONTROLLER AXIS MAPPINGS ===
        
        // Left stick for movement (Minecraft Bedrock style)
        gamepadAxisMappings.put(GameAction.MOVE_FORWARD, ControllerAxis.LEFT_Y);
        gamepadAxisMappings.put(GameAction.MOVE_BACKWARD, ControllerAxis.LEFT_Y);
        gamepadAxisMappings.put(GameAction.STRAFE_LEFT, ControllerAxis.LEFT_X);
        gamepadAxisMappings.put(GameAction.STRAFE_RIGHT, ControllerAxis.LEFT_X);
        
        // Right stick for camera/look (Minecraft Bedrock style)
        gamepadAxisMappings.put(GameAction.LOOK_UP, ControllerAxis.RIGHT_Y);
        gamepadAxisMappings.put(GameAction.LOOK_DOWN, ControllerAxis.RIGHT_Y);
        gamepadAxisMappings.put(GameAction.LOOK_LEFT, ControllerAxis.RIGHT_X);
        gamepadAxisMappings.put(GameAction.LOOK_RIGHT, ControllerAxis.RIGHT_X);
        
        // Triggers for actions (Minecraft Bedrock style)
        gamepadAxisMappings.put(GameAction.ATTACK, ControllerAxis.RIGHT_TRIGGER);    // RT/R2 for attack
        gamepadAxisMappings.put(GameAction.USE_ITEM, ControllerAxis.LEFT_TRIGGER);   // LT/L2 for use/place
        
        // Legacy axis mappings for backward compatibility
        gamepadAxisMappings.put(GameAction.TURN_LEFT, ControllerAxis.LEFT_X);
        gamepadAxisMappings.put(GameAction.TURN_RIGHT, ControllerAxis.LEFT_X);
    }
    
    /**
     * Updates the active device type and timestamp.
     * 
     * @param deviceType The device type that generated input
     * @param timestamp The timestamp of the input
     */
    private void updateActiveDevice(DeviceType deviceType, long timestamp) {
        if (activeDevice != deviceType || timestamp - lastInputTime > 100) {
            activeDevice = deviceType;
            lastInputTime = timestamp;
            logger.debug("Active input device changed to: {}", deviceType);
        }
    }
    
    /**
     * Polls keyboard and mouse input using GLFW.
     * This method processes the current state and updates action mappings.
     */
    public void pollKeyboardAndMouse() {
        // Clear previous frame's pressed/released states
        keyPressed.clear();
        keyReleased.clear();
        mouseButtonPressed.clear();
        mouseButtonReleased.clear();
        scrollDelta = 0.0f;
        mouseDelta.set(0, 0);
        
        // Poll GLFW events
        GLFW.glfwPollEvents();
        
        // Process keyboard actions - use context-specific mappings
        Map<GameAction, Set<Integer>> contextKeyboardMappings = this.contextKeyboardMappings.get(currentContext);
        if (contextKeyboardMappings == null) {
            contextKeyboardMappings = keyboardMappings; // Fallback to default mappings
        }
        
        for (var entry : contextKeyboardMappings.entrySet()) {
            var action = entry.getKey();
            var keys = entry.getValue();
            
            boolean isActive = keys.stream().anyMatch(key -> keyStates.getOrDefault(key, false));
            boolean wasPressed = keys.stream().anyMatch(key -> keyPressed.getOrDefault(key, false));
            boolean wasReleased = keys.stream().anyMatch(key -> keyReleased.getOrDefault(key, false));
            
            updateActionState(action, isActive, wasPressed, wasReleased, DeviceType.KEYBOARD);
        }
        
        // Process mouse actions - use context-specific mappings
        Map<GameAction, Set<Integer>> contextMouseMappings = this.contextMouseMappings.get(currentContext);
        if (contextMouseMappings == null) {
            contextMouseMappings = mouseMappings; // Fallback to default mappings
        }
        
        for (var entry : contextMouseMappings.entrySet()) {
            var action = entry.getKey();
            var buttons = entry.getValue();
            
            boolean isActive = buttons.stream().anyMatch(button -> mouseButtonStates.getOrDefault(button, false));
            boolean wasPressed = buttons.stream().anyMatch(button -> mouseButtonPressed.getOrDefault(button, false));
            boolean wasReleased = buttons.stream().anyMatch(button -> mouseButtonReleased.getOrDefault(button, false));
            
            updateActionState(action, isActive, wasPressed, wasReleased, DeviceType.KEYBOARD);
        }
    }
    
    /**
     * Polls controller input using jamepad.
     * This method processes controller state and updates action mappings.
     */
    public void pollControllers() {
        if (controllerManager == null) {
            return;
        }
        
        // Find active controller
        if (activeControllerIndex == -1) {
            for (int i = 0; i < controllerManager.getNumControllers(); i++) {
                if (controllerManager.getState(i).isConnected) {
                    activeControllerIndex = i;
                    logger.info("Controller {} connected and set as active", i);
                    break;
                }
            }
        }
        
        if (activeControllerIndex == -1) {
            return; // No controllers connected
        }
        
        var controllerState = controllerManager.getState(activeControllerIndex);
        if (!controllerState.isConnected) {
            logger.info("Controller {} disconnected", activeControllerIndex);
            activeControllerIndex = -1;
            return;
        }
        
        var currentTime = System.currentTimeMillis();
        boolean hasInput = false;
        
        // Process button actions - use context-specific mappings
        Map<GameAction, Set<ControllerButton>> contextButtonMappings = contextGamepadButtonMappings.get(currentContext);
        if (contextButtonMappings == null) {
            contextButtonMappings = gamepadButtonMappings; // Fallback to default mappings
        }
        
        for (var entry : contextButtonMappings.entrySet()) {
            var action = entry.getKey();
            var buttons = entry.getValue();
            
            boolean isActive = buttons.stream().anyMatch(button -> getButtonState(controllerState, button));
            // For now, we'll treat any button press as both pressed and active
            // since ControllerState doesn't track just-pressed/just-released states
            boolean wasPressed = isActive;
            boolean wasReleased = false;
            
            if (isActive || wasPressed || wasReleased) {
                hasInput = true;
                updateActionState(action, isActive, wasPressed, wasReleased, DeviceType.GAMEPAD);
            }
        }
        
        // Process axis actions - use context-specific mappings
        Map<GameAction, ControllerAxis> contextAxisMappings = contextGamepadAxisMappings.get(currentContext);
        if (contextAxisMappings == null) {
            contextAxisMappings = gamepadAxisMappings; // Fallback to default mappings
        }
        
        for (var entry : contextAxisMappings.entrySet()) {
            var action = entry.getKey();
            var axis = entry.getValue();
            
            float axisValue = getAxisState(controllerState, axis);
            
            // Apply controller sensitivity and invert Y axis if needed
            if (axis == ControllerAxis.RIGHT_Y && invertControllerY) {
                axisValue = -axisValue;
            }
            axisValue *= controllerSensitivity;
            
            boolean isActive = Math.abs(axisValue) > controllerDeadzone;
            
            if (isActive) {
                hasInput = true;
                
                // Handle directional axes
                switch (action) {
                    case MOVE_FORWARD -> {
                        if (axisValue < -controllerDeadzone) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case MOVE_BACKWARD -> {
                        if (axisValue > controllerDeadzone) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case STRAFE_LEFT -> {
                        if (axisValue < -controllerDeadzone) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case STRAFE_RIGHT -> {
                        if (axisValue > controllerDeadzone) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case LOOK_LEFT -> {
                        if (axisValue < -controllerDeadzone) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case LOOK_RIGHT -> {
                        if (axisValue > controllerDeadzone) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case LOOK_UP -> {
                        if (axisValue < -controllerDeadzone) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case LOOK_DOWN -> {
                        if (axisValue > controllerDeadzone) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case TURN_LEFT -> {
                        if (axisValue < -controllerDeadzone) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case TURN_RIGHT -> {
                        if (axisValue > controllerDeadzone) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case ATTACK -> {
                        // Attack mapped to right trigger (RT/R2) - Minecraft Bedrock style
                        if (Math.abs(axisValue) > CONTROLLER_TRIGGER_THRESHOLD) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case USE_ITEM -> {
                        // Use item mapped to left trigger (LT/L2) - Minecraft Bedrock style
                        if (Math.abs(axisValue) > CONTROLLER_TRIGGER_THRESHOLD) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case SHIP_BUILDER -> {
                        // Ship builder can be mapped to shoulder buttons
                        if (Math.abs(axisValue) > CONTROLLER_TRIGGER_THRESHOLD) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case SETTINGS -> {
                        // Settings can be mapped to specific axis combinations
                        if (Math.abs(axisValue) > CONTROLLER_TRIGGER_THRESHOLD) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case INVENTORY -> {
                        // Inventory can be mapped to triggers
                        if (Math.abs(axisValue) > CONTROLLER_TRIGGER_THRESHOLD) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case PRIMARY_ACTION -> {
                        // Primary action can be mapped to triggers
                        if (Math.abs(axisValue) > CONTROLLER_TRIGGER_THRESHOLD) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case INTERACT -> {
                        // Interact can be mapped to triggers
                        if (Math.abs(axisValue) > CONTROLLER_TRIGGER_THRESHOLD) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case DEBUG_TOGGLE -> {
                        // Debug toggle can be mapped to specific axis combinations
                        if (Math.abs(axisValue) > CONTROLLER_TRIGGER_THRESHOLD) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case MAP -> {
                        // Map can be mapped to triggers
                        if (Math.abs(axisValue) > CONTROLLER_TRIGGER_THRESHOLD) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case MENU -> {
                        // Menu can be mapped to triggers
                        if (Math.abs(axisValue) > CONTROLLER_TRIGGER_THRESHOLD) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                    case JUMP -> {
                        // Jump can be mapped to triggers
                        if (Math.abs(axisValue) > CONTROLLER_TRIGGER_THRESHOLD) {
                            updateActionState(action, true, false, false, DeviceType.GAMEPAD);
                        }
                    }
                }
            }
        }
        
        if (hasInput) {
            updateActiveDevice(DeviceType.GAMEPAD, currentTime);
        }
    }
    
    /**
     * Updates the state of a game action and dispatches events if necessary.
     * 
     * @param action The game action
     * @param isActive Whether the action is currently active
     * @param wasPressed Whether the action was just pressed
     * @param wasReleased Whether the action was just released
     * @param device The device that triggered the action
     */
    private void updateActionState(GameAction action, boolean isActive, boolean wasPressed, boolean wasReleased, DeviceType device) {
        if (isActive) {
            activeActions.add(action);
        } else {
            activeActions.remove(action);
        }
        
        if (wasPressed) {
            pressedActions.add(action);
            dispatch(action, device);
        }
        
        if (wasReleased) {
            releasedActions.add(action);
        }
    }
    
    /**
     * Main update method that should be called once per frame.
     * Processes all input sources and updates action states.
     */
    public void update() {
        // Clear previous frame's action states
        pressedActions.clear();
        releasedActions.clear();
        
        // Poll all input sources
        pollKeyboardAndMouse();
        pollControllers();
    }
    
    /**
     * Dispatches a game action to the registered dispatcher.
     * 
     * @param action The action to dispatch
     * @param device The device that triggered the action
     */
    private void dispatch(GameAction action, DeviceType device) {
        if (inputDispatcher != null) {
            inputDispatcher.dispatch(action, device);
        }
    }
    
    /**
     * Sets the input dispatcher for handling game actions.
     * 
     * @param dispatcher The input dispatcher
     */
    public void setInputDispatcher(InputDispatcher dispatcher) {
        this.inputDispatcher = dispatcher;
    }
    
    /**
     * Checks if a game action is currently active (held down).
     * 
     * @param action The action to check
     * @return true if the action is active, false otherwise
     */
    public boolean isActionActive(GameAction action) {
        return activeActions.contains(action);
    }
    
    /**
     * Checks if a game action was just pressed this frame.
     * 
     * @param action The action to check
     * @return true if the action was just pressed, false otherwise
     */
    public boolean isActionPressed(GameAction action) {
        return pressedActions.contains(action);
    }
    
    /**
     * Checks if a game action was just released this frame.
     * 
     * @param action The action to check
     * @return true if the action was just released, false otherwise
     */
    public boolean isActionReleased(GameAction action) {
        return releasedActions.contains(action);
    }
    
    /**
     * Gets the currently active input device type.
     * 
     * @return The active device type
     */
    public DeviceType getActiveDevice() {
        return activeDevice;
    }
    
    /**
     * Gets the current mouse position.
     * 
     * @return The mouse position as a Vector2f
     */
    public Vector2f getMousePosition() {
        return new Vector2f(mousePosition);
    }
    
    /**
     * Gets the mouse movement delta for this frame.
     * 
     * @return The mouse delta as a Vector2f
     */
    public Vector2f getMouseDelta() {
        return new Vector2f(mouseDelta);
    }
    
    /**
     * Gets the scroll wheel delta for this frame.
     * 
     * @return The scroll delta
     */
    public float getScrollDelta() {
        return scrollDelta;
    }
    
    /**
     * Adds a keyboard key mapping for a game action.
     * 
     * @param action The game action
     * @param key The GLFW key code
     */
    public void addKeyboardMapping(GameAction action, int key) {
        keyboardMappings.computeIfAbsent(action, _ -> ConcurrentHashMap.newKeySet()).add(key);
    }
    
    /**
     * Adds a mouse button mapping for a game action.
     * 
     * @param action The game action
     * @param button The GLFW mouse button code
     */
    public void addMouseMapping(GameAction action, int button) {
        mouseMappings.computeIfAbsent(action, _ -> ConcurrentHashMap.newKeySet()).add(button);
    }
    
    /**
     * Adds a gamepad button mapping for a game action.
     * 
     * @param action The game action
     * @param button The controller button
     */
    public void addGamepadButtonMapping(GameAction action, ControllerButton button) {
        gamepadButtonMappings.computeIfAbsent(action, _ -> ConcurrentHashMap.newKeySet()).add(button);
    }
    
    /**
     * Adds a gamepad axis mapping for a game action.
     * 
     * @param action The game action
     * @param axis The controller axis
     */
    public void addGamepadAxisMapping(GameAction action, ControllerAxis axis) {
        gamepadAxisMappings.put(action, axis);
    }
    
    /**
     * Removes all mappings for a game action.
     * 
     * @param action The game action to clear mappings for
     */
    public void clearMappings(GameAction action) {
        keyboardMappings.remove(action);
        mouseMappings.remove(action);
        gamepadButtonMappings.remove(action);
        gamepadAxisMappings.remove(action);
    }
    
    /**
     * Checks if any controllers are connected.
     * 
     * @return true if at least one controller is connected, false otherwise
     */
    public boolean hasControllerConnected() {
        if (controllerManager == null) {
            return false;
        }
        
        for (int i = 0; i < controllerManager.getNumControllers(); i++) {
            if (controllerManager.getState(i).isConnected) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the number of connected controllers.
     * 
     * @return The number of connected controllers
     */
    public int getConnectedControllerCount() {
        if (controllerManager == null) {
            return 0;
        }
        
        int count = 0;
        for (int i = 0; i < controllerManager.getNumControllers(); i++) {
            if (controllerManager.getState(i).isConnected) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Cleans up resources and shuts down the input manager.
     * Should be called when the application is closing.
     */
    public void cleanup() {
        logger.info("Cleaning up InputManager");
        
        if (controllerManager != null) {
            controllerManager.quitSDLGamepad();
            controllerManager = null;
        }
        
        // Clear all state
        keyStates.clear();
        keyPressed.clear();
        keyReleased.clear();
        mouseButtonStates.clear();
        mouseButtonPressed.clear();
        mouseButtonReleased.clear();
        activeActions.clear();
        pressedActions.clear();
        releasedActions.clear();
        
        logger.info("InputManager cleanup completed");
    }
    
    /**
     * Helper method to get button state from ControllerState using direct field access.
     * Maps ControllerButton enum to the appropriate field in ControllerState.
     */
    private boolean getButtonState(ControllerState state, ControllerButton button) {
        switch (button) {
            case A -> { return state.a; }
            case B -> { return state.b; }
            case X -> { return state.x; }
            case Y -> { return state.y; }
            case LEFT_BUMPER -> { return state.lb; }
            case RIGHT_BUMPER -> { return state.rb; }
            case BACK -> { return state.back; }
            case START -> { return state.start; }
            case LEFT_STICK -> { return state.leftStickClick; }
            case RIGHT_STICK -> { return state.rightStickClick; }
            case DPAD_UP -> { return state.dpadUp; }
            case DPAD_DOWN -> { return state.dpadDown; }
            case DPAD_LEFT -> { return state.dpadLeft; }
            case DPAD_RIGHT -> { return state.dpadRight; }
            default -> { return false; }
        }
    }
    
    /**
     * Helper method to get axis state from ControllerState using direct field access.
     * Maps ControllerAxis enum to the appropriate field in ControllerState.
     */
    private float getAxisState(ControllerState state, ControllerAxis axis) {
        switch (axis) {
            case LEFT_X -> { return state.leftStickX; }
            case LEFT_Y -> { return state.leftStickY; }
            case RIGHT_X -> { return state.rightStickX; }
            case RIGHT_Y -> { return state.rightStickY; }
            case LEFT_TRIGGER -> { return state.leftTrigger; }
            case RIGHT_TRIGGER -> { return state.rightTrigger; }
            default -> { return 0.0f; }
        }
    }
    
    /**
     * Initializes context-specific input mappings for all input contexts.
     * This method sets up different control schemes for different game states.
     */
    private void initializeContextMappings() {
        // Initialize mapping containers for each context
        for (InputContext context : InputContext.values()) {
            contextKeyboardMappings.put(context, new EnumMap<>(GameAction.class));
            contextMouseMappings.put(context, new EnumMap<>(GameAction.class));
            contextGamepadButtonMappings.put(context, new EnumMap<>(GameAction.class));
            contextGamepadAxisMappings.put(context, new EnumMap<>(GameAction.class));
        }
        
        // Initialize gameplay context (default Minecraft-style controls)
        initializeGameplayContext();
        
        // Initialize menu context (menu navigation)
        initializeMenuContext();
        
        // Initialize inventory context (inventory management)
        initializeInventoryContext();
        
        // Initialize ship builder context (building controls)
        initializeShipBuilderContext();
        
        // Initialize other contexts with basic navigation
        initializeOtherContexts();
    }
    
    /**
     * Initializes gameplay context mappings (standard Minecraft-style controls).
     */
    private void initializeGameplayContext() {
        Map<GameAction, Set<Integer>> keyMap = contextKeyboardMappings.get(InputContext.GAMEPLAY);
        Map<GameAction, Set<Integer>> mouseMap = contextMouseMappings.get(InputContext.GAMEPLAY);
        Map<GameAction, Set<ControllerButton>> buttonMap = contextGamepadButtonMappings.get(InputContext.GAMEPLAY);
        Map<GameAction, ControllerAxis> axisMap = contextGamepadAxisMappings.get(InputContext.GAMEPLAY);
        
        // Movement (WASD + Arrow keys)
        keyMap.put(GameAction.MOVE_FORWARD, Set.of(GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP));
        keyMap.put(GameAction.MOVE_BACKWARD, Set.of(GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN));
        keyMap.put(GameAction.STRAFE_LEFT, Set.of(GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT));
        keyMap.put(GameAction.STRAFE_RIGHT, Set.of(GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT));
        keyMap.put(GameAction.JUMP, Set.of(GLFW.GLFW_KEY_SPACE));
        keyMap.put(GameAction.SNEAK, Set.of(GLFW.GLFW_KEY_LEFT_SHIFT));
        keyMap.put(GameAction.SPRINT, Set.of(GLFW.GLFW_KEY_LEFT_CONTROL));
        
        // Actions
        keyMap.put(GameAction.ATTACK, Set.of(GLFW.GLFW_KEY_LEFT_ALT));
        keyMap.put(GameAction.USE_ITEM, Set.of(GLFW.GLFW_KEY_RIGHT_ALT));
        keyMap.put(GameAction.INTERACT, Set.of(GLFW.GLFW_KEY_E));
        keyMap.put(GameAction.DROP_ITEM, Set.of(GLFW.GLFW_KEY_Q));
        
        // Mouse actions
        mouseMap.put(GameAction.ATTACK, Set.of(GLFW.GLFW_MOUSE_BUTTON_LEFT));
        mouseMap.put(GameAction.USE_ITEM, Set.of(GLFW.GLFW_MOUSE_BUTTON_RIGHT));
        mouseMap.put(GameAction.PICK_BLOCK, Set.of(GLFW.GLFW_MOUSE_BUTTON_MIDDLE));
        
        // Controller mappings (Minecraft Bedrock style)
        buttonMap.put(GameAction.JUMP, Set.of(ControllerButton.A));
        buttonMap.put(GameAction.SNEAK, Set.of(ControllerButton.B));
        buttonMap.put(GameAction.INTERACT, Set.of(ControllerButton.X));
        buttonMap.put(GameAction.INVENTORY, Set.of(ControllerButton.Y));
        buttonMap.put(GameAction.SPRINT, Set.of(ControllerButton.LEFT_STICK));
        
        // Controller axes
        axisMap.put(GameAction.MOVE_FORWARD, ControllerAxis.LEFT_Y);
        axisMap.put(GameAction.STRAFE_RIGHT, ControllerAxis.LEFT_X);
        axisMap.put(GameAction.LOOK_UP, ControllerAxis.RIGHT_Y);
        axisMap.put(GameAction.LOOK_RIGHT, ControllerAxis.RIGHT_X);
        axisMap.put(GameAction.ATTACK, ControllerAxis.RIGHT_TRIGGER);
        axisMap.put(GameAction.USE_ITEM, ControllerAxis.LEFT_TRIGGER);
    }
    
    /**
     * Initializes menu context mappings (menu navigation).
     */
    private void initializeMenuContext() {
        Map<GameAction, Set<Integer>> keyMap = contextKeyboardMappings.get(InputContext.MENU);
        Map<GameAction, Set<ControllerButton>> buttonMap = contextGamepadButtonMappings.get(InputContext.MENU);
        
        // Menu navigation
        keyMap.put(GameAction.MENU_UP, Set.of(GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP));
        keyMap.put(GameAction.MENU_DOWN, Set.of(GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN));
        keyMap.put(GameAction.MENU_LEFT, Set.of(GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT));
        keyMap.put(GameAction.MENU_RIGHT, Set.of(GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT));
        keyMap.put(GameAction.MENU_SELECT, Set.of(GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_SPACE));
        keyMap.put(GameAction.MENU_BACK, Set.of(GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_BACKSPACE));
        
        // Controller menu navigation
        buttonMap.put(GameAction.MENU_UP, Set.of(ControllerButton.DPAD_UP));
        buttonMap.put(GameAction.MENU_DOWN, Set.of(ControllerButton.DPAD_DOWN));
        buttonMap.put(GameAction.MENU_LEFT, Set.of(ControllerButton.DPAD_LEFT));
        buttonMap.put(GameAction.MENU_RIGHT, Set.of(ControllerButton.DPAD_RIGHT));
        buttonMap.put(GameAction.MENU_SELECT, Set.of(ControllerButton.A));
        buttonMap.put(GameAction.MENU_BACK, Set.of(ControllerButton.B));
    }
    
    /**
     * Initializes inventory context mappings (inventory management).
     */
    private void initializeInventoryContext() {
        Map<GameAction, Set<Integer>> keyMap = contextKeyboardMappings.get(InputContext.INVENTORY);
        Map<GameAction, Set<ControllerButton>> buttonMap = contextGamepadButtonMappings.get(InputContext.INVENTORY);
        
        // Inventory navigation (similar to menu but with inventory-specific actions)
        keyMap.put(GameAction.MENU_UP, Set.of(GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP));
        keyMap.put(GameAction.MENU_DOWN, Set.of(GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN));
        keyMap.put(GameAction.MENU_LEFT, Set.of(GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT));
        keyMap.put(GameAction.MENU_RIGHT, Set.of(GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT));
        keyMap.put(GameAction.MENU_SELECT, Set.of(GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_SPACE));
        keyMap.put(GameAction.MENU_BACK, Set.of(GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_I));
        
        // Inventory-specific actions
        keyMap.put(GameAction.DROP_ITEM, Set.of(GLFW.GLFW_KEY_Q));
        keyMap.put(GameAction.SWAP_HANDS, Set.of(GLFW.GLFW_KEY_LEFT_SHIFT));
        
        // Controller inventory navigation
        buttonMap.put(GameAction.MENU_UP, Set.of(ControllerButton.DPAD_UP));
        buttonMap.put(GameAction.MENU_DOWN, Set.of(ControllerButton.DPAD_DOWN));
        buttonMap.put(GameAction.MENU_LEFT, Set.of(ControllerButton.DPAD_LEFT));
        buttonMap.put(GameAction.MENU_RIGHT, Set.of(ControllerButton.DPAD_RIGHT));
        buttonMap.put(GameAction.MENU_SELECT, Set.of(ControllerButton.A));
        buttonMap.put(GameAction.MENU_BACK, Set.of(ControllerButton.B, ControllerButton.Y));
        buttonMap.put(GameAction.DROP_ITEM, Set.of(ControllerButton.X));
    }
    
    /**
     * Initializes ship builder context mappings (building controls).
     */
    private void initializeShipBuilderContext() {
        Map<GameAction, Set<Integer>> keyMap = contextKeyboardMappings.get(InputContext.SHIP_BUILDER);
        Map<GameAction, Set<Integer>> mouseMap = contextMouseMappings.get(InputContext.SHIP_BUILDER);
        Map<GameAction, Set<ControllerButton>> buttonMap = contextGamepadButtonMappings.get(InputContext.SHIP_BUILDER);
        
        // Movement (same as gameplay)
        keyMap.put(GameAction.MOVE_FORWARD, Set.of(GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP));
        keyMap.put(GameAction.MOVE_BACKWARD, Set.of(GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN));
        keyMap.put(GameAction.STRAFE_LEFT, Set.of(GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT));
        keyMap.put(GameAction.STRAFE_RIGHT, Set.of(GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT));
        keyMap.put(GameAction.JUMP, Set.of(GLFW.GLFW_KEY_SPACE));
        keyMap.put(GameAction.SNEAK, Set.of(GLFW.GLFW_KEY_LEFT_SHIFT));
        
        // Building actions
        keyMap.put(GameAction.USE_ITEM, Set.of(GLFW.GLFW_KEY_E));
        keyMap.put(GameAction.ATTACK, Set.of(GLFW.GLFW_KEY_Q));
        keyMap.put(GameAction.PICK_BLOCK, Set.of(GLFW.GLFW_KEY_F));
        
        // Mouse building actions
        mouseMap.put(GameAction.USE_ITEM, Set.of(GLFW.GLFW_MOUSE_BUTTON_RIGHT));
        mouseMap.put(GameAction.ATTACK, Set.of(GLFW.GLFW_MOUSE_BUTTON_LEFT));
        mouseMap.put(GameAction.PICK_BLOCK, Set.of(GLFW.GLFW_MOUSE_BUTTON_MIDDLE));
        
        // Controller building
        buttonMap.put(GameAction.USE_ITEM, Set.of(ControllerButton.A));
        buttonMap.put(GameAction.ATTACK, Set.of(ControllerButton.X));
        buttonMap.put(GameAction.MENU_BACK, Set.of(ControllerButton.B));
    }
    
    /**
     * Initializes other contexts with basic navigation controls.
     */
    private void initializeOtherContexts() {
        // Map, Combat, Dialogue, Trading, Settings contexts use menu-like navigation
        InputContext[] menuLikeContexts = {
            InputContext.MAP, InputContext.COMBAT, InputContext.DIALOGUE, 
            InputContext.TRADING, InputContext.SETTINGS
        };
        
        for (InputContext context : menuLikeContexts) {
            Map<GameAction, Set<Integer>> keyMap = contextKeyboardMappings.get(context);
            Map<GameAction, Set<ControllerButton>> buttonMap = contextGamepadButtonMappings.get(context);
            
            // Basic navigation
            keyMap.put(GameAction.MENU_UP, Set.of(GLFW.GLFW_KEY_W, GLFW.GLFW_KEY_UP));
            keyMap.put(GameAction.MENU_DOWN, Set.of(GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_DOWN));
            keyMap.put(GameAction.MENU_LEFT, Set.of(GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT));
            keyMap.put(GameAction.MENU_RIGHT, Set.of(GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_RIGHT));
            keyMap.put(GameAction.MENU_SELECT, Set.of(GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_SPACE));
            keyMap.put(GameAction.MENU_BACK, Set.of(GLFW.GLFW_KEY_ESCAPE));
            
            // Controller navigation
            buttonMap.put(GameAction.MENU_UP, Set.of(ControllerButton.DPAD_UP));
            buttonMap.put(GameAction.MENU_DOWN, Set.of(ControllerButton.DPAD_DOWN));
            buttonMap.put(GameAction.MENU_LEFT, Set.of(ControllerButton.DPAD_LEFT));
            buttonMap.put(GameAction.MENU_RIGHT, Set.of(ControllerButton.DPAD_RIGHT));
            buttonMap.put(GameAction.MENU_SELECT, Set.of(ControllerButton.A));
            buttonMap.put(GameAction.MENU_BACK, Set.of(ControllerButton.B));
        }
    }
    
    /**
     * Sets the current input context and updates active mappings.
     * 
     * @param context The new input context
     */
    public void setInputContext(InputContext context) {
        if (context == null || context == currentContext) {
            return;
        }
        
        logger.debug("Switching input context from {} to {}", currentContext, context);
        currentContext = context;
        
        // Clear current mappings
        keyboardMappings.clear();
        mouseMappings.clear();
        gamepadButtonMappings.clear();
        gamepadAxisMappings.clear();
        
        // Load context-specific mappings
        Map<GameAction, Set<Integer>> contextKeyMap = contextKeyboardMappings.get(context);
        Map<GameAction, Set<Integer>> contextMouseMap = contextMouseMappings.get(context);
        Map<GameAction, Set<ControllerButton>> contextButtonMap = contextGamepadButtonMappings.get(context);
        Map<GameAction, ControllerAxis> contextAxisMap = contextGamepadAxisMappings.get(context);
        
        // Copy context mappings to active mappings
        for (Map.Entry<GameAction, Set<Integer>> entry : contextKeyMap.entrySet()) {
            keyboardMappings.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        
        for (Map.Entry<GameAction, Set<Integer>> entry : contextMouseMap.entrySet()) {
            mouseMappings.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        
        for (Map.Entry<GameAction, Set<ControllerButton>> entry : contextButtonMap.entrySet()) {
            gamepadButtonMappings.put(entry.getKey(), new HashSet<>(entry.getValue()));
        }
        
        gamepadAxisMappings.putAll(contextAxisMap);
        
        logger.info("Input context switched to: {}", context.getDisplayName());
    }
    
    /**
     * Gets the current input context.
     * 
     * @return The current input context
     */
    public InputContext getCurrentContext() {
        return currentContext;
    }
    
    /**
     * Updates the input context based on the current game state.
     * 
     * @param gameState The current game state
     */
    public void updateContextFromGameState(com.odyssey.core.GameState gameState) {
        InputContext newContext = InputContext.fromGameState(gameState);
        setInputContext(newContext);
    }
    
    /**
     * Updates the input context based on the current game state.
     * This method should be called whenever the game state changes.
     * 
     * @param gameState The current game state
     */
    public void updateInputContext(com.odyssey.core.GameState gameState) {
        InputContext newContext = InputContext.fromGameState(gameState);
        if (newContext != currentContext) {
            setInputContext(newContext);
        }
    }
    
    /**
     * Checks if an action is available in the current input context.
     * This can be used to filter UI elements or provide context-sensitive help.
     * 
     * @param action The action to check
     * @return true if the action is available in the current context
     */
    public boolean isActionAvailable(GameAction action) {
        Map<GameAction, Set<Integer>> keyboardMap = contextKeyboardMappings.get(currentContext);
        Map<GameAction, Set<Integer>> mouseMap = contextMouseMappings.get(currentContext);
        Map<GameAction, Set<ControllerButton>> gamepadButtonMap = contextGamepadButtonMappings.get(currentContext);
        Map<GameAction, ControllerAxis> gamepadAxisMap = contextGamepadAxisMappings.get(currentContext);
        
        return (keyboardMap != null && keyboardMap.containsKey(action)) ||
               (mouseMap != null && mouseMap.containsKey(action)) ||
               (gamepadButtonMap != null && gamepadButtonMap.containsKey(action)) ||
               (gamepadAxisMap != null && gamepadAxisMap.containsKey(action));
    }
}