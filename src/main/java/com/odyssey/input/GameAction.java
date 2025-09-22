package com.odyssey.input;

/**
 * Enumeration of all possible game actions in the Odyssey Pirate Adventure.
 * 
 * This enum provides a centralized definition of all input actions that can be
 * performed in the game, allowing for consistent mapping across different input
 * devices (keyboard, mouse, gamepad).
 * 
 * @author Odyssey Development Team
 * @version 1.0
 * @since Java 25
 */
public enum GameAction {
    
    /**
     * Move the player/ship forward in the current facing direction.
     * Default mappings: W key, Left stick up, D-pad up
     */
    MOVE_FORWARD("move_forward", "Move Forward"),
    
    /**
     * Move the player/ship backward in the opposite of facing direction.
     * Default mappings: S key, Left stick down, D-pad down
     */
    MOVE_BACKWARD("move_backward", "Move Backward"),
    
    /**
     * Turn the player/ship to the left.
     * Default mappings: A key, Left stick left, D-pad left
     */
    TURN_LEFT("turn_left", "Turn Left"),
    
    /**
     * Turn the player/ship to the right.
     * Default mappings: D key, Left stick right, D-pad right
     */
    TURN_RIGHT("turn_right", "Turn Right"),
    
    /**
     * Perform an attack action (fire cannons, sword strike, etc.).
     * Default mappings: Left mouse button, X button (Xbox), Square button (PlayStation)
     */
    ATTACK("attack", "Attack"),
    
    /**
     * Jump action for the player character.
     * Default mappings: Space key, A button (Xbox), X button (PlayStation)
     */
    JUMP("jump", "Jump"),
    
    /**
     * Interact with objects, NPCs, or environmental elements.
     * Default mappings: E key, Y button (Xbox), Triangle button (PlayStation)
     */
    INTERACT("interact", "Interact"),
    
    /**
     * Open or close the inventory interface.
     * Default mappings: I key, Back/Select button
     */
    INVENTORY("inventory", "Inventory"),
    
    /**
     * Open or close the main game menu.
     * Default mappings: Escape key, Start/Menu button
     */
    MENU("menu", "Menu"),
    
    /**
     * Open or close the map interface.
     * Default mappings: M key, Map button
     */
    MAP("map", "Map"),
    
    /**
     * Open or close the ship builder interface.
     * Default mappings: B key, Build button
     */
    SHIP_BUILDER("ship_builder", "Ship Builder"),
    
    /**
     * Toggle debug mode and debug overlays.
     * Default mappings: F3 key, Debug button
     */
    DEBUG_TOGGLE("debug_toggle", "Debug Toggle"),
    
    /**
     * Primary action (confirm, select, activate).
     * Default mappings: Enter key, A button (Xbox), X button (PlayStation)
     */
    PRIMARY_ACTION("primary_action", "Primary Action"),
    
    /**
     * Open or close the settings interface.
     * Default mappings: Tab key, Settings button
     */
    SETTINGS("settings", "Settings");
    
    private final String id;
    private final String displayName;
    
    /**
     * Constructs a GameAction with the specified identifier and display name.
     * 
     * @param id The unique identifier for this action
     * @param displayName The human-readable name for this action
     */
    GameAction(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }
    
    /**
     * Gets the unique identifier for this action.
     * 
     * @return The action identifier
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the human-readable display name for this action.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Finds a GameAction by its identifier.
     * 
     * @param id The identifier to search for
     * @return The matching GameAction, or null if not found
     */
    public static GameAction fromId(String id) {
        for (GameAction action : values()) {
            if (action.id.equals(id)) {
                return action;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}