package com.odyssey.input;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Manages keybindings with conflict detection and persistence.
 * Supports remappable controls for keyboard, mouse, and gamepad inputs.
 */
public class KeybindingSystem {
    private static final Logger logger = LoggerFactory.getLogger(KeybindingSystem.class);
    
    private final Map<InputAction, Set<InputBinding>> actionBindings = new ConcurrentHashMap<>();
    private final Map<InputBinding, InputAction> bindingToAction = new ConcurrentHashMap<>();
    private final String configFile;
    
    public KeybindingSystem(String configFile) {
        this.configFile = configFile;
        initializeDefaultBindings();
    }
    
    /**
     * Initialize default keybindings for all actions
     */
    private void initializeDefaultBindings() {
        // Movement bindings
        addBinding(InputAction.MOVE_FORWARD, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_W, "W"));
        addBinding(InputAction.MOVE_FORWARD, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_UP, "Up Arrow"));
        addBinding(InputAction.MOVE_FORWARD, new InputBinding(InputBinding.InputType.GAMEPAD_AXIS, GLFW_GAMEPAD_AXIS_LEFT_Y, -0.2f, false, "Left Stick Up"));
        
        addBinding(InputAction.MOVE_BACKWARD, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_S, "S"));
        addBinding(InputAction.MOVE_BACKWARD, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_DOWN, "Down Arrow"));
        addBinding(InputAction.MOVE_BACKWARD, new InputBinding(InputBinding.InputType.GAMEPAD_AXIS, GLFW_GAMEPAD_AXIS_LEFT_Y, 0.2f, false, "Left Stick Down"));
        
        addBinding(InputAction.MOVE_LEFT, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_A, "A"));
        addBinding(InputAction.MOVE_LEFT, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_LEFT, "Left Arrow"));
        addBinding(InputAction.MOVE_LEFT, new InputBinding(InputBinding.InputType.GAMEPAD_AXIS, GLFW_GAMEPAD_AXIS_LEFT_X, -0.2f, false, "Left Stick Left"));
        
        addBinding(InputAction.MOVE_RIGHT, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_D, "D"));
        addBinding(InputAction.MOVE_RIGHT, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_RIGHT, "Right Arrow"));
        addBinding(InputAction.MOVE_RIGHT, new InputBinding(InputBinding.InputType.GAMEPAD_AXIS, GLFW_GAMEPAD_AXIS_LEFT_X, 0.2f, false, "Left Stick Right"));
        
        addBinding(InputAction.MOVE_UP, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_SPACE, "Space"));
        addBinding(InputAction.MOVE_UP, new InputBinding(InputBinding.InputType.GAMEPAD_BUTTON, GLFW_GAMEPAD_BUTTON_RIGHT_BUMPER, "Right Bumper"));
        
        addBinding(InputAction.MOVE_DOWN, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_LEFT_SHIFT, "Left Shift"));
        addBinding(InputAction.MOVE_DOWN, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_C, "C"));
        addBinding(InputAction.MOVE_DOWN, new InputBinding(InputBinding.InputType.GAMEPAD_BUTTON, GLFW_GAMEPAD_BUTTON_LEFT_BUMPER, "Left Bumper"));
        
        addBinding(InputAction.RUN, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_LEFT_CONTROL, "Left Ctrl"));
        addBinding(InputAction.RUN, new InputBinding(InputBinding.InputType.GAMEPAD_BUTTON, GLFW_GAMEPAD_BUTTON_LEFT_THUMB, "Left Stick Click"));
        
        addBinding(InputAction.JUMP, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_SPACE, "Space"));
        addBinding(InputAction.JUMP, new InputBinding(InputBinding.InputType.GAMEPAD_BUTTON, GLFW_GAMEPAD_BUTTON_A, "A Button"));
        
        // Camera bindings
        addBinding(InputAction.LOOK_HORIZONTAL, new InputBinding(InputBinding.InputType.MOUSE_AXIS, 0, 0.0f, false, "Mouse X"));
        addBinding(InputAction.LOOK_HORIZONTAL, new InputBinding(InputBinding.InputType.GAMEPAD_AXIS, GLFW_GAMEPAD_AXIS_RIGHT_X, 0.1f, false, "Right Stick X"));
        
        addBinding(InputAction.LOOK_VERTICAL, new InputBinding(InputBinding.InputType.MOUSE_AXIS, 1, 0.0f, false, "Mouse Y"));
        addBinding(InputAction.LOOK_VERTICAL, new InputBinding(InputBinding.InputType.GAMEPAD_AXIS, GLFW_GAMEPAD_AXIS_RIGHT_Y, 0.1f, false, "Right Stick Y"));
        
        // Ship controls
        addBinding(InputAction.RAISE_ANCHOR, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_R, "R"));
        addBinding(InputAction.RAISE_ANCHOR, new InputBinding(InputBinding.InputType.GAMEPAD_BUTTON, GLFW_GAMEPAD_BUTTON_DPAD_UP, "D-Pad Up"));
        
        addBinding(InputAction.LOWER_ANCHOR, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_F, "F"));
        addBinding(InputAction.LOWER_ANCHOR, new InputBinding(InputBinding.InputType.GAMEPAD_BUTTON, GLFW_GAMEPAD_BUTTON_DPAD_DOWN, "D-Pad Down"));
        
        addBinding(InputAction.FIRE_SAILS, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_E, "E"));
        addBinding(InputAction.FIRE_SAILS, new InputBinding(InputBinding.InputType.GAMEPAD_BUTTON, GLFW_GAMEPAD_BUTTON_X, "X Button"));
        
        addBinding(InputAction.FIRE_CANNONS, new InputBinding(InputBinding.InputType.MOUSE_BUTTON, GLFW_MOUSE_BUTTON_LEFT, "Left Mouse"));
        addBinding(InputAction.FIRE_CANNONS, new InputBinding(InputBinding.InputType.GAMEPAD_AXIS, GLFW_GAMEPAD_AXIS_RIGHT_TRIGGER, 0.5f, false, "Right Trigger"));
        
        // Interface bindings
        addBinding(InputAction.OPEN_INVENTORY, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_I, "I"));
        addBinding(InputAction.OPEN_INVENTORY, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_TAB, "Tab"));
        addBinding(InputAction.OPEN_INVENTORY, new InputBinding(InputBinding.InputType.GAMEPAD_BUTTON, GLFW_GAMEPAD_BUTTON_Y, "Y Button"));
        
        addBinding(InputAction.OPEN_MAP, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_M, "M"));
        addBinding(InputAction.OPEN_MAP, new InputBinding(InputBinding.InputType.GAMEPAD_BUTTON, GLFW_GAMEPAD_BUTTON_BACK, "Back/Select"));
        
        addBinding(InputAction.OPEN_MENU, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_ESCAPE, "Escape"));
        addBinding(InputAction.OPEN_MENU, new InputBinding(InputBinding.InputType.GAMEPAD_BUTTON, GLFW_GAMEPAD_BUTTON_START, "Start/Menu"));
        
        addBinding(InputAction.CONFIRM, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_ENTER, "Enter"));
        addBinding(InputAction.CONFIRM, new InputBinding(InputBinding.InputType.GAMEPAD_BUTTON, GLFW_GAMEPAD_BUTTON_A, "A Button"));
        
        addBinding(InputAction.CANCEL, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_ESCAPE, "Escape"));
        addBinding(InputAction.CANCEL, new InputBinding(InputBinding.InputType.GAMEPAD_BUTTON, GLFW_GAMEPAD_BUTTON_B, "B Button"));
        
        // Debug bindings
        addBinding(InputAction.TOGGLE_DEBUG, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_F3, "F3"));
        addBinding(InputAction.TOGGLE_WIREFRAME, new InputBinding(InputBinding.InputType.KEYBOARD, GLFW_KEY_F4, "F4"));
        
        logger.info("Initialized {} default keybindings for {} actions", 
                   bindingToAction.size(), actionBindings.size());
    }
    
    /**
     * Add a binding for an action
     */
    public void addBinding(InputAction action, InputBinding binding) {
        actionBindings.computeIfAbsent(action, k -> new HashSet<>()).add(binding);
        bindingToAction.put(binding, action);
    }
    
    /**
     * Remove a binding for an action
     */
    public void removeBinding(InputAction action, InputBinding binding) {
        Set<InputBinding> bindings = actionBindings.get(action);
        if (bindings != null) {
            bindings.remove(binding);
            if (bindings.isEmpty()) {
                actionBindings.remove(action);
            }
        }
        bindingToAction.remove(binding);
    }
    
    /**
     * Get all bindings for an action
     */
    public Set<InputBinding> getBindings(InputAction action) {
        return actionBindings.getOrDefault(action, Collections.emptySet());
    }
    
    /**
     * Get all bindings in the system
     */
    public Map<InputAction, Set<InputBinding>> getAllBindings() {
        return new HashMap<>(actionBindings);
    }
    
    /**
     * Get the action for a binding
     */
    public InputAction getAction(InputBinding binding) {
        return bindingToAction.get(binding);
    }
    
    /**
     * Check for conflicts when adding a new binding
     */
    public List<InputAction> checkConflicts(InputBinding newBinding) {
        List<InputAction> conflicts = new ArrayList<>();
        
        for (Map.Entry<InputBinding, InputAction> entry : bindingToAction.entrySet()) {
            InputBinding existing = entry.getKey();
            
            // Check for exact conflicts (same type and code)
            if (existing.getType() == newBinding.getType() && 
                existing.getCode() == newBinding.getCode()) {
                
                // For axis bindings, check if thresholds overlap
                if (newBinding.getType() == InputBinding.InputType.GAMEPAD_AXIS ||
                    newBinding.getType() == InputBinding.InputType.MOUSE_AXIS) {
                    
                    // Check if the threshold ranges overlap
                    float existingThreshold = Math.abs(existing.getThreshold());
                    float newThreshold = Math.abs(newBinding.getThreshold());
                    
                    if (Math.abs(existingThreshold - newThreshold) < 0.1f) {
                        conflicts.add(entry.getValue());
                    }
                } else {
                    // For buttons and keys, exact match is a conflict
                    conflicts.add(entry.getValue());
                }
            }
        }
        
        return conflicts;
    }
    
    /**
     * Remap an action to a new binding, handling conflicts
     */
    public boolean remapAction(InputAction action, InputBinding oldBinding, InputBinding newBinding) {
        List<InputAction> conflicts = checkConflicts(newBinding);
        
        // Remove conflicts if they exist
        for (InputAction conflictAction : conflicts) {
            removeBinding(conflictAction, newBinding);
            logger.warn("Removed conflicting binding {} from action {}", newBinding, conflictAction);
        }
        
        // Remove old binding and add new one
        if (oldBinding != null) {
            removeBinding(action, oldBinding);
        }
        addBinding(action, newBinding);
        
        logger.info("Remapped action {} from {} to {}", action, oldBinding, newBinding);
        return true;
    }
    
    /**
     * Reset all bindings to defaults
     */
    public void resetToDefaults() {
        actionBindings.clear();
        bindingToAction.clear();
        initializeDefaultBindings();
        logger.info("Reset all keybindings to defaults");
    }
    
    /**
     * Save keybindings to file
     */
    public void saveToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(configFile))) {
            writer.println("# Odyssey Keybindings Configuration");
            writer.println("# Format: ACTION=TYPE:CODE:THRESHOLD:INVERTED:DESCRIPTION");
            writer.println();
            
            for (Map.Entry<InputAction, Set<InputBinding>> entry : actionBindings.entrySet()) {
                InputAction action = entry.getKey();
                for (InputBinding binding : entry.getValue()) {
                    writer.printf("%s=%s:%d:%.3f:%s:%s%n",
                                action.name(),
                                binding.getType().name(),
                                binding.getCode(),
                                binding.getThreshold(),
                                binding.isInverted(),
                                binding.getDescription());
                }
            }
            
            logger.info("Saved keybindings to {}", configFile);
        } catch (IOException e) {
            logger.error("Failed to save keybindings to {}", configFile, e);
        }
    }
    
    /**
     * Load keybindings from file
     */
    public void loadFromFile() {
        File file = new File(configFile);
        if (!file.exists()) {
            logger.info("Keybindings file {} not found, using defaults", configFile);
            return;
        }
        
        actionBindings.clear();
        bindingToAction.clear();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                try {
                    String[] parts = line.split("=", 2);
                    if (parts.length != 2) {
                        logger.warn("Invalid keybinding format at line {}: {}", lineNumber, line);
                        continue;
                    }
                    
                    InputAction action = InputAction.valueOf(parts[0]);
                    String[] bindingParts = parts[1].split(":", 5);
                    
                    if (bindingParts.length != 5) {
                        logger.warn("Invalid binding format at line {}: {}", lineNumber, line);
                        continue;
                    }
                    
                    InputBinding.InputType type = InputBinding.InputType.valueOf(bindingParts[0]);
                    int code = Integer.parseInt(bindingParts[1]);
                    float threshold = Float.parseFloat(bindingParts[2]);
                    boolean inverted = Boolean.parseBoolean(bindingParts[3]);
                    String description = bindingParts[4];
                    
                    InputBinding binding = new InputBinding(type, code, threshold, inverted, description);
                    addBinding(action, binding);
                    
                } catch (Exception e) {
                    logger.warn("Failed to parse keybinding at line {}: {}", lineNumber, line, e);
                }
            }
            
            logger.info("Loaded {} keybindings from {}", bindingToAction.size(), configFile);
            
        } catch (IOException e) {
            logger.error("Failed to load keybindings from {}", configFile, e);
            initializeDefaultBindings();
        }
        
        // Ensure we have at least default bindings
        if (actionBindings.isEmpty()) {
            logger.warn("No valid keybindings loaded, using defaults");
            initializeDefaultBindings();
        }
    }
    
    /**
     * Get all configured actions
     */
    public Set<InputAction> getAllActions() {
        return new HashSet<>(actionBindings.keySet());
    }
    
    /**
     * Get human-readable description of all bindings for an action
     */
    public String getBindingDescription(InputAction action) {
        Set<InputBinding> bindings = getBindings(action);
        if (bindings.isEmpty()) {
            return "Not bound";
        }
        
        return bindings.stream()
                      .map(InputBinding::getDescription)
                      .reduce((a, b) -> a + ", " + b)
                      .orElse("Not bound");
    }
}