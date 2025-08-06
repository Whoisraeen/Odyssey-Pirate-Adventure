package com.odyssey.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Unified input abstraction layer that combines keyboard, mouse, gamepad, and touch input.
 * Provides a single interface for all input handling with action-based input mapping.
 */
public class InputAbstractionLayer implements GestureRecognizer.GestureListener {
    private static final Logger logger = LoggerFactory.getLogger(InputAbstractionLayer.class);
    
    private final InputManager inputManager;
    private final KeybindingSystem keybindingSystem;
    private final InputBuffer inputBuffer;
    private final GestureRecognizer gestureRecognizer;
    
    // Action state tracking
    private final Map<InputAction, Float> actionValues = new ConcurrentHashMap<>();
    private final Map<InputAction, Boolean> actionStates = new ConcurrentHashMap<>();
    private final Map<InputAction, Long> lastActionTime = new ConcurrentHashMap<>();
    
    // Input processing settings
    private boolean touchInputEnabled = false;
    private float analogDeadzone = 0.15f;
    private float mouseSensitivity = 1.0f;
    private float gamepadSensitivity = 1.0f;
    
    public InputAbstractionLayer(InputManager inputManager, String keybindingsFile) {
        this.inputManager = inputManager;
        this.keybindingSystem = new KeybindingSystem(keybindingsFile);
        this.inputBuffer = new InputBuffer(100, 1000); // 100ms buffer, max 1000 events
        this.gestureRecognizer = new GestureRecognizer();
        
        // Register for gesture events
        gestureRecognizer.addGestureListener(this);
        
        // Initialize action states
        for (InputAction action : InputAction.values()) {
            actionValues.put(action, 0.0f);
            actionStates.put(action, false);
        }
        
        // Register some common input sequences
        registerCommonSequences();
    }
    
    /**
     * Initialize the input abstraction layer
     */
    public void initialize() {
        keybindingSystem.loadFromFile();
        logger.info("Input abstraction layer initialized");
    }
    
    /**
     * Update the input system - call this every frame
     */
    public void update() {
        inputManager.update();
        updateActionStates();
        processAnalogInputs();
    }
    
    /**
     * Update action states based on current input
     */
    private void updateActionStates() {
        long currentTime = System.currentTimeMillis();
        
        for (InputAction action : InputAction.values()) {
            boolean wasActive = actionStates.get(action);
            boolean isActive = false;
            float value = 0.0f;
            
            // Check all bindings for this action
            Set<InputBinding> bindings = keybindingSystem.getBindings(action);
            for (InputBinding binding : bindings) {
                float bindingValue = getBindingValue(binding);
                
                if (Math.abs(bindingValue) > Math.abs(value)) {
                    value = bindingValue;
                }
                
                if (isBindingActive(binding)) {
                    isActive = true;
                }
            }
            
            // Apply deadzone for analog inputs
            if (Math.abs(value) < analogDeadzone) {
                value = 0.0f;
            }
            
            // Update state
            actionValues.put(action, value);
            boolean stateChanged = wasActive != isActive;
            actionStates.put(action, isActive);
            
            // Add to input buffer if state changed
            if (stateChanged) {
                inputBuffer.addInputEvent(action, isActive, value);
                lastActionTime.put(action, currentTime);
            }
        }
    }
    
    /**
     * Process analog inputs with proper scaling and sensitivity
     */
    private void processAnalogInputs() {
        // Process mouse look
        double mouseDeltaX = inputManager.getMouseDeltaX() * mouseSensitivity;
        double mouseDeltaY = inputManager.getMouseDeltaY() * mouseSensitivity;
        
        if (Math.abs(mouseDeltaX) > 0.001) {
            actionValues.put(InputAction.LOOK_HORIZONTAL, (float) mouseDeltaX);
            inputBuffer.addInputEvent(InputAction.LOOK_HORIZONTAL, true, (float) mouseDeltaX);
        }
        
        if (Math.abs(mouseDeltaY) > 0.001) {
            actionValues.put(InputAction.LOOK_VERTICAL, (float) mouseDeltaY);
            inputBuffer.addInputEvent(InputAction.LOOK_VERTICAL, true, (float) mouseDeltaY);
        }
        
        // Process gamepad analog sticks
        for (InputManager.Gamepad gamepad : inputManager.getGamepads().values()) {
            // Right stick for camera
            float rightX = gamepad.getAxis(GLFW_GAMEPAD_AXIS_RIGHT_X) * gamepadSensitivity;
            float rightY = gamepad.getAxis(GLFW_GAMEPAD_AXIS_RIGHT_Y) * gamepadSensitivity;
            
            if (Math.abs(rightX) > analogDeadzone) {
                actionValues.put(InputAction.LOOK_HORIZONTAL, rightX);
                inputBuffer.addInputEvent(InputAction.LOOK_HORIZONTAL, true, rightX);
            }
            
            if (Math.abs(rightY) > analogDeadzone) {
                actionValues.put(InputAction.LOOK_VERTICAL, rightY);
                inputBuffer.addInputEvent(InputAction.LOOK_VERTICAL, true, rightY);
            }
        }
    }
    
    /**
     * Get the current value of an action (0.0 to 1.0 for digital, -1.0 to 1.0 for analog)
     */
    public float getActionValue(InputAction action) {
        return actionValues.getOrDefault(action, 0.0f);
    }
    
    /**
     * Check if action was just triggered this frame
     */
    public boolean isActionJustTriggered(InputAction action) {
        return inputBuffer.wasActionJustPressed(action);
    }
    
    /**
     * Check if an action is currently active
     */
    public boolean isActionActive(InputAction action) {
        return actionStates.getOrDefault(action, false) || inputBuffer.isActionActive(action);
    }
    
    /**
     * Check if an action was just pressed this frame
     */
    public boolean wasActionJustPressed(InputAction action) {
        return inputBuffer.wasActionJustPressed(action);
    }
    
    /**
     * Check if an action was just released this frame
     */
    public boolean wasActionJustReleased(InputAction action) {
        return inputBuffer.wasActionJustReleased(action);
    }
    
    /**
     * Get the time since an action was last triggered
     */
    public long getTimeSinceAction(InputAction action) {
        Long lastTime = lastActionTime.get(action);
        if (lastTime == null) return Long.MAX_VALUE;
        return System.currentTimeMillis() - lastTime;
    }
    
    /**
     * Check if multiple actions are active simultaneously
     */
    public boolean areActionsActive(InputAction... actions) {
        for (InputAction action : actions) {
            if (!isActionActive(action)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Get the value of a specific input binding
     */
    private float getBindingValue(InputBinding binding) {
        switch (binding.getType()) {
            case KEYBOARD:
                return inputManager.isKeyPressed(binding.getCode()) ? 1.0f : 0.0f;
                
            case MOUSE_BUTTON:
                return inputManager.isMouseButtonPressed(binding.getCode()) ? 1.0f : 0.0f;
                
            case MOUSE_AXIS:
                if (binding.getCode() == 0) { // X axis
                    return (float) inputManager.getMouseDeltaX();
                } else if (binding.getCode() == 1) { // Y axis
                    return (float) inputManager.getMouseDeltaY();
                }
                break;
                
            case GAMEPAD_BUTTON:
                for (InputManager.Gamepad gamepad : inputManager.getGamepads().values()) {
                    if (gamepad.isButtonPressed(binding.getCode())) {
                        return 1.0f;
                    }
                }
                break;
                
            case GAMEPAD_AXIS:
                for (InputManager.Gamepad gamepad : inputManager.getGamepads().values()) {
                    float value = gamepad.getAxis(binding.getCode());
                    
                    // Apply threshold and inversion
                    if (binding.getThreshold() > 0) {
                        // Positive threshold
                        if (value > binding.getThreshold()) {
                            return binding.isInverted() ? -value : value;
                        }
                    } else if (binding.getThreshold() < 0) {
                        // Negative threshold
                        if (value < binding.getThreshold()) {
                            return binding.isInverted() ? -value : value;
                        }
                    } else {
                        // No threshold, return raw value
                        return binding.isInverted() ? -value : value;
                    }
                }
                break;
                
            case TOUCH_GESTURE:
                // Touch gestures are handled through the gesture recognizer
                return 0.0f;
        }
        
        return 0.0f;
    }
    
    /**
     * Check if a binding is currently active
     */
    private boolean isBindingActive(InputBinding binding) {
        switch (binding.getType()) {
            case KEYBOARD:
                return inputManager.isKeyPressed(binding.getCode());
                
            case MOUSE_BUTTON:
                return inputManager.isMouseButtonPressed(binding.getCode());
                
            case GAMEPAD_BUTTON:
                for (InputManager.Gamepad gamepad : inputManager.getGamepads().values()) {
                    if (gamepad.isButtonPressed(binding.getCode())) {
                        return true;
                    }
                }
                break;
                
            case GAMEPAD_AXIS:
                for (InputManager.Gamepad gamepad : inputManager.getGamepads().values()) {
                    float value = gamepad.getAxis(binding.getCode());
                    
                    if (binding.getThreshold() > 0) {
                        return value > binding.getThreshold();
                    } else if (binding.getThreshold() < 0) {
                        return value < binding.getThreshold();
                    } else {
                        return Math.abs(value) > analogDeadzone;
                    }
                }
                break;
                
            case MOUSE_AXIS:
            case TOUCH_GESTURE:
                return false; // These don't have persistent "active" states
        }
        
        return false;
    }
    
    /**
     * Register common input sequences for combo detection
     */
    private void registerCommonSequences() {
        // Example: Quick double-tap for dodge
        inputBuffer.registerSequence(new InputBuffer.InputSequence(
            "Quick Dodge", 200, InputAction.MOVE_LEFT, InputAction.MOVE_LEFT));
        
        // Example: Combat combo
        inputBuffer.registerSequence(new InputBuffer.InputSequence(
            "Attack Combo", 500, InputAction.ATTACK_PRIMARY, InputAction.ATTACK_PRIMARY, InputAction.ATTACK_SECONDARY));
    }
    
    /**
     * Handle gesture recognition events
     */
    @Override
    public void onGestureDetected(GestureRecognizer.Gesture gesture) {
        // Convert gesture to input action
        inputBuffer.addInputEvent(gesture.action, true, 1.0f);
        actionStates.put(gesture.action, true);
        lastActionTime.put(gesture.action, System.currentTimeMillis());
        
        logger.debug("Gesture converted to action: {}", gesture.action);
    }
    
    /**
     * Enable or disable touch input
     */
    public void setTouchInputEnabled(boolean enabled) {
        this.touchInputEnabled = enabled;
        if (!enabled) {
            gestureRecognizer.clear();
        }
        logger.info("Touch input {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Process touch input events
     */
    public void onTouchDown(int touchId, float x, float y, float pressure) {
        if (touchInputEnabled) {
            gestureRecognizer.onTouchDown(touchId, x, y, pressure);
        }
    }
    
    public void onTouchMove(int touchId, float x, float y, float pressure) {
        if (touchInputEnabled) {
            gestureRecognizer.onTouchMove(touchId, x, y, pressure);
        }
    }
    
    public void onTouchUp(int touchId, float x, float y) {
        if (touchInputEnabled) {
            gestureRecognizer.onTouchUp(touchId, x, y);
        }
    }
    
    /**
     * Remap an action to a new binding
     */
    public boolean remapAction(InputAction action, InputBinding oldBinding, InputBinding newBinding) {
        return keybindingSystem.remapAction(action, oldBinding, newBinding);
    }
    
    /**
     * Get all bindings for an action
     */
    public Set<InputBinding> getBindings(InputAction action) {
        return keybindingSystem.getBindings(action);
    }
    
    /**
     * Get human-readable description of bindings for an action
     */
    public String getBindingDescription(InputAction action) {
        return keybindingSystem.getBindingDescription(action);
    }
    
    /**
     * Save current keybindings to file
     */
    public void saveKeybindings() {
        keybindingSystem.saveToFile();
    }
    
    /**
     * Reset all keybindings to defaults
     */
    public void resetKeybindings() {
        keybindingSystem.resetToDefaults();
    }
    
    /**
     * Set analog input deadzone
     */
    public void setAnalogDeadzone(float deadzone) {
        this.analogDeadzone = Math.max(0.0f, Math.min(1.0f, deadzone));
    }
    
    /**
     * Set mouse sensitivity
     */
    public void setMouseSensitivity(float sensitivity) {
        this.mouseSensitivity = Math.max(0.1f, Math.min(10.0f, sensitivity));
    }
    
    /**
     * Set gamepad sensitivity
     */
    public void setGamepadSensitivity(float sensitivity) {
        this.gamepadSensitivity = Math.max(0.1f, Math.min(10.0f, sensitivity));
    }
    
    /**
     * Get input system statistics
     */
    public String getInputStats() {
        return String.format("InputAbstraction{actions=%d, bindings=%d, buffer=%s, gestures=%s}", 
                           actionStates.size(), 
                           keybindingSystem.getAllActions().size(),
                           inputBuffer.getBufferStats(),
                           gestureRecognizer.getStats());
    }
    
    /**
     * Handle gamepad connection event
     */
    public void onGamepadConnected(InputManager.Gamepad gamepad) {
        logger.info("Input abstraction layer: Gamepad connected - {} ({})", 
                   gamepad.getName(), gamepad.getId());
        // Could trigger specific actions or update UI here
    }
    
    /**
     * Handle gamepad disconnection event
     */
    public void onGamepadDisconnected(InputManager.Gamepad gamepad) {
        logger.info("Input abstraction layer: Gamepad disconnected - {} ({})", 
                   gamepad.getName(), gamepad.getId());
        // Could trigger specific actions or update UI here
    }
    
    /**
     * Get debug information as a formatted string
     */
    public String getDebugInfo() {
        StringBuilder debug = new StringBuilder();
        debug.append("Active Actions: ").append(actionStates.size()).append("\n");
        debug.append("Buffered Events: ").append(inputBuffer.getEventCount()).append("\n");
        debug.append("Total Bindings: ").append(keybindingSystem.getAllBindings().size()).append("\n");
        debug.append("Gestures Enabled: ").append(gestureRecognizer != null).append("\n");
        
        // Show some active actions
        if (!actionStates.isEmpty()) {
            debug.append("Currently Active:\n");
            actionStates.entrySet().stream()
                .filter(entry -> entry.getValue())
                .limit(5)
                .forEach(entry -> debug.append("  - ").append(entry.getKey()).append("\n"));
        }
        
        return debug.toString();
    }

    /**
     * Get input statistics for debugging
     */
    public Map<String, Object> getInputStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeActions", actionStates.size());
        stats.put("bufferedEvents", inputBuffer.getEventCount());
        stats.put("totalBindings", keybindingSystem.getAllBindings().size());
        stats.put("gesturesEnabled", gestureRecognizer != null);
        return stats;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        keybindingSystem.saveToFile();
        inputBuffer.clear();
        gestureRecognizer.clear();
        logger.info("Input abstraction layer cleaned up");
    }
}