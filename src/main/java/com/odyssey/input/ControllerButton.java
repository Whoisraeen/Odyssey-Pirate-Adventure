package com.odyssey.input;

/**
 * Enumeration of controller button types for gamepad input.
 * 
 * This enum maps to the button fields available in jamepad's ControllerState class.
 * Each button corresponds to a specific field in the ControllerState object.
 * 
 * @author Odyssey Development Team
 * @version 1.0
 * @since Java 25
 */
public enum ControllerButton {
    
    /**
     * A button (Xbox: A, PlayStation: X)
     */
    A,
    
    /**
     * B button (Xbox: B, PlayStation: Circle)
     */
    B,
    
    /**
     * X button (Xbox: X, PlayStation: Square)
     */
    X,
    
    /**
     * Y button (Xbox: Y, PlayStation: Triangle)
     */
    Y,
    
    /**
     * Left bumper/trigger (L1 on PlayStation)
     */
    LEFT_BUMPER,
    
    /**
     * Right bumper/trigger (R1 on PlayStation)
     */
    RIGHT_BUMPER,
    
    /**
     * Back/Select button
     */
    BACK,
    
    /**
     * Start/Menu button
     */
    START,
    
    /**
     * Left stick click/press
     */
    LEFT_STICK,
    
    /**
     * Right stick click/press
     */
    RIGHT_STICK,
    
    /**
     * D-pad up
     */
    DPAD_UP,
    
    /**
     * D-pad down
     */
    DPAD_DOWN,
    
    /**
     * D-pad left
     */
    DPAD_LEFT,
    
    /**
     * D-pad right
     */
    DPAD_RIGHT;
}
