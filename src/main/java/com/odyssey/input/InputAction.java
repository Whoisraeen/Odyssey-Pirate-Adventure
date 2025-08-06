package com.odyssey.input;

/**
 * Represents a game action that can be triggered by various input methods.
 * This provides an abstraction layer between raw input and game logic.
 */
public enum InputAction {
    // Movement actions
    MOVE_FORWARD("Move Forward"),
    MOVE_BACKWARD("Move Backward"),
    MOVE_LEFT("Move Left"),
    MOVE_RIGHT("Move Right"),
    MOVE_UP("Move Up"),
    MOVE_DOWN("Move Down"),
    RUN("Run"),
    JUMP("Jump"),
    
    // Camera actions
    LOOK_HORIZONTAL("Look Horizontal"),
    LOOK_VERTICAL("Look Vertical"),
    ZOOM_IN("Zoom In"),
    ZOOM_OUT("Zoom Out"),
    
    // Ship controls
    RAISE_ANCHOR("Raise Anchor"),
    LOWER_ANCHOR("Lower Anchor"),
    FIRE_SAILS("Fire Sails"),
    FIRE_CANNONS("Fire Cannons"),
    STEER_LEFT("Steer Left"),
    STEER_RIGHT("Steer Right"),
    
    // Interface actions
    OPEN_INVENTORY("Open Inventory"),
    OPEN_MAP("Open Map"),
    OPEN_MENU("Open Menu"),
    CONFIRM("Confirm"),
    CANCEL("Cancel"),
    
    // Combat actions
    ATTACK_PRIMARY("Primary Attack"),
    ATTACK_SECONDARY("Secondary Attack"),
    BLOCK("Block"),
    DODGE("Dodge"),
    
    // Debug actions
    TOGGLE_DEBUG("Toggle Debug"),
    TOGGLE_WIREFRAME("Toggle Wireframe"),
    
    // Touch/Mobile specific
    TAP("Tap"),
    DOUBLE_TAP("Double Tap"),
    LONG_PRESS("Long Press"),
    PINCH("Pinch"),
    SWIPE_UP("Swipe Up"),
    SWIPE_DOWN("Swipe Down"),
    SWIPE_LEFT("Swipe Left"),
    SWIPE_RIGHT("Swipe Right");
    
    private final String displayName;
    
    InputAction(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}