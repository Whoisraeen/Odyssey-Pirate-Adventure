package com.odyssey.input;

/**
 * Enumeration of all possible game actions in the Odyssey Pirate Adventure.
 * 
 * This enum provides a centralized definition of all input actions that can be
 * performed in the game, allowing for consistent mapping across different input
 * devices (keyboard, mouse, gamepad). Control mappings follow Minecraft Java Edition
 * and Bedrock Edition standards for familiar gameplay experience.
 * 
 * @author Odyssey Development Team
 * @version 2.0
 * @since Java 25
 */
public enum GameAction {
    
    // === MOVEMENT ACTIONS ===
    /**
     * Move the player/ship forward in the current facing direction.
     * Minecraft mappings: W key, Left stick forward, D-pad up
     */
    MOVE_FORWARD("move_forward", "Move Forward"),
    
    /**
     * Move the player/ship backward in the opposite of facing direction.
     * Minecraft mappings: S key, Left stick backward, D-pad down
     */
    MOVE_BACKWARD("move_backward", "Move Backward"),
    
    /**
     * Strafe left (move left without turning).
     * Minecraft mappings: A key, Left stick left, D-pad left
     */
    STRAFE_LEFT("strafe_left", "Strafe Left"),
    
    /**
     * Strafe right (move right without turning).
     * Minecraft mappings: D key, Left stick right, D-pad right
     */
    STRAFE_RIGHT("strafe_right", "Strafe Right"),
    
    /**
     * Jump action for the player character.
     * Minecraft mappings: Space key, A button (Xbox), X button (PlayStation)
     */
    JUMP("jump", "Jump"),
    
    /**
     * Sneak/crouch action (slower movement, prevents falling).
     * Minecraft mappings: Left Shift key, Right stick click, R3 button
     */
    SNEAK("sneak", "Sneak"),
    
    /**
     * Sprint action (faster movement).
     * Minecraft mappings: Left Ctrl key, Left stick click, L3 button
     */
    SPRINT("sprint", "Sprint"),
    
    /**
     * Swim up when in water.
     * Minecraft mappings: Space key, A button (Xbox), X button (PlayStation)
     */
    SWIM_UP("swim_up", "Swim Up"),
    
    /**
     * Swim down when in water.
     * Minecraft mappings: Left Shift key, Right stick click, R3 button
     */
    SWIM_DOWN("swim_down", "Swim Down"),
    
    // === CAMERA/LOOK ACTIONS ===
    /**
     * Look up (camera pitch up).
     * Minecraft mappings: Mouse Y-, Right stick up
     */
    LOOK_UP("look_up", "Look Up"),
    
    /**
     * Look down (camera pitch down).
     * Minecraft mappings: Mouse Y+, Right stick down
     */
    LOOK_DOWN("look_down", "Look Down"),
    
    /**
     * Look left (camera yaw left).
     * Minecraft mappings: Mouse X-, Right stick left
     */
    LOOK_LEFT("look_left", "Look Left"),
    
    /**
     * Look right (camera yaw right).
     * Minecraft mappings: Mouse X+, Right stick right
     */
    LOOK_RIGHT("look_right", "Look Right"),
    
    // === ACTION BUTTONS ===
    /**
     * Primary attack/use action (break blocks, attack, use tools).
     * Minecraft mappings: Left mouse button, Right trigger (RT/R2)
     */
    ATTACK("attack", "Attack/Use"),
    
    /**
     * Secondary use action (place blocks, use items, interact).
     * Minecraft mappings: Right mouse button, Left trigger (LT/L2)
     */
    USE_ITEM("use_item", "Use Item/Place"),
    
    /**
     * Middle mouse button action (pick block, special actions).
     * Minecraft mappings: Middle mouse button, Y button (Xbox), Triangle (PlayStation)
     */
    PICK_BLOCK("pick_block", "Pick Block"),
    
    /**
     * Interact with objects, NPCs, or environmental elements.
     * Minecraft mappings: Right click context, Y button (Xbox), Triangle (PlayStation)
     */
    INTERACT("interact", "Interact"),
    
    // === INVENTORY & ITEMS ===
    /**
     * Open or close the inventory interface.
     * Minecraft mappings: E key, Y button (Xbox), Triangle button (PlayStation)
     */
    INVENTORY("inventory", "Inventory"),
    
    /**
     * Drop the currently held item.
     * Minecraft mappings: Q key, B button (Xbox), Circle button (PlayStation)
     */
    DROP_ITEM("drop_item", "Drop Item"),
    
    /**
     * Swap items in hands (main hand <-> off hand).
     * Minecraft mappings: F key, X button (Xbox), Square button (PlayStation)
     */
    SWAP_HANDS("swap_hands", "Swap Hands"),
    
    // === HOTBAR ACTIONS ===
    /**
     * Select hotbar slot 1.
     * Minecraft mappings: 1 key, D-pad up
     */
    HOTBAR_1("hotbar_1", "Hotbar Slot 1"),
    
    /**
     * Select hotbar slot 2.
     * Minecraft mappings: 2 key, D-pad right
     */
    HOTBAR_2("hotbar_2", "Hotbar Slot 2"),
    
    /**
     * Select hotbar slot 3.
     * Minecraft mappings: 3 key, D-pad down
     */
    HOTBAR_3("hotbar_3", "Hotbar Slot 3"),
    
    /**
     * Select hotbar slot 4.
     * Minecraft mappings: 4 key, D-pad left
     */
    HOTBAR_4("hotbar_4", "Hotbar Slot 4"),
    
    /**
     * Select hotbar slot 5.
     * Minecraft mappings: 5 key, Left bumper (LB/L1)
     */
    HOTBAR_5("hotbar_5", "Hotbar Slot 5"),
    
    /**
     * Select hotbar slot 6.
     * Minecraft mappings: 6 key, Right bumper (RB/R1)
     */
    HOTBAR_6("hotbar_6", "Hotbar Slot 6"),
    
    /**
     * Select hotbar slot 7.
     * Minecraft mappings: 7 key
     */
    HOTBAR_7("hotbar_7", "Hotbar Slot 7"),
    
    /**
     * Select hotbar slot 8.
     * Minecraft mappings: 8 key
     */
    HOTBAR_8("hotbar_8", "Hotbar Slot 8"),
    
    /**
     * Select hotbar slot 9.
     * Minecraft mappings: 9 key
     */
    HOTBAR_9("hotbar_9", "Hotbar Slot 9"),
    
    // === INTERFACE ACTIONS ===
    /**
     * Open or close the main game menu/pause.
     * Minecraft mappings: Escape key, Start/Menu button
     */
    MENU("menu", "Menu/Pause"),
    
    /**
     * Open chat interface (multiplayer).
     * Minecraft mappings: T key, Start button (hold)
     */
    CHAT("chat", "Chat"),
    
    /**
     * Open command interface.
     * Minecraft mappings: / key, Start button (double tap)
     */
    COMMAND("command", "Command"),
    
    /**
     * Take screenshot.
     * Minecraft mappings: F2 key, Share button (PlayStation)
     */
    SCREENSHOT("screenshot", "Screenshot"),
    
    /**
     * Toggle fullscreen mode.
     * Minecraft mappings: F11 key
     */
    TOGGLE_FULLSCREEN("toggle_fullscreen", "Toggle Fullscreen"),
    
    /**
     * Toggle perspective (first person, third person, etc.).
     * Minecraft mappings: F5 key, Right stick click (hold)
     */
    TOGGLE_PERSPECTIVE("toggle_perspective", "Toggle Perspective"),
    
    // === DEBUG & ADVANCED ===
    /**
     * Toggle debug mode and debug overlays.
     * Minecraft mappings: F3 key, Left stick + Right stick (press together)
     */
    DEBUG_TOGGLE("debug_toggle", "Debug Toggle"),
    
    /**
     * Reload chunks/world.
     * Minecraft mappings: F3 + A keys
     */
    RELOAD_CHUNKS("reload_chunks", "Reload Chunks"),
    
    /**
     * Toggle hitboxes display.
     * Minecraft mappings: F3 + B keys
     */
    TOGGLE_HITBOXES("toggle_hitboxes", "Toggle Hitboxes"),
    
    // === GAME-SPECIFIC ACTIONS ===
    /**
     * Open or close the map interface.
     * Pirate game specific: M key, Map button
     */
    MAP("map", "Map"),
    
    /**
     * Open or close the ship builder interface.
     * Pirate game specific: B key, Build button
     */
    SHIP_BUILDER("ship_builder", "Ship Builder"),
    
    /**
     * Quick save the current game state.
     * Pirate game specific: F5 key, Quick Save button
     */
    QUICK_SAVE("quick_save", "Quick Save"),
    
    // === LEGACY ACTIONS (for backward compatibility) ===
    /**
     * Turn left (legacy - use STRAFE_LEFT for movement, LOOK_LEFT for camera).
     * @deprecated Use STRAFE_LEFT or LOOK_LEFT instead
     */
    @Deprecated
    TURN_LEFT("turn_left", "Turn Left"),
    
    /**
     * Turn right (legacy - use STRAFE_RIGHT for movement, LOOK_RIGHT for camera).
     * @deprecated Use STRAFE_RIGHT or LOOK_RIGHT instead
     */
    @Deprecated
    TURN_RIGHT("turn_right", "Turn Right"),
    
    /**
     * Primary action (confirm, select, activate).
     * Legacy action for menu navigation
     */
    PRIMARY_ACTION("primary_action", "Primary Action"),
    
    /**
     * Settings interface.
     * Legacy action - use MENU instead
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