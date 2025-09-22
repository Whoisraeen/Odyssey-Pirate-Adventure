package com.odyssey.input;

/**
 * Enumeration of controller axis types for gamepad input.
 * 
 * This enum maps to the axis fields available in jamepad's ControllerState class.
 * Each axis corresponds to a specific field in the ControllerState object.
 * 
 * @author Odyssey Development Team
 * @version 1.0
 * @since Java 25
 */
public enum ControllerAxis {
    
    /**
     * Left stick X-axis (horizontal movement)
     */
    LEFT_X,
    
    /**
     * Left stick Y-axis (vertical movement)
     */
    LEFT_Y,
    
    /**
     * Right stick X-axis (horizontal movement)
     */
    RIGHT_X,
    
    /**
     * Right stick Y-axis (vertical movement)
     */
    RIGHT_Y,
    
    /**
     * Left trigger (analog pressure)
     */
    LEFT_TRIGGER,
    
    /**
     * Right trigger (analog pressure)
     */
    RIGHT_TRIGGER;
}
