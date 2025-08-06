package com.odyssey.input;

import java.util.Objects;

/**
 * Represents a binding between an input source and a game action.
 * This allows for flexible input mapping and remapping.
 */
public class InputBinding {
    public enum InputType {
        KEYBOARD,
        MOUSE_BUTTON,
        MOUSE_AXIS,
        GAMEPAD_BUTTON,
        GAMEPAD_AXIS,
        TOUCH_GESTURE
    }
    
    private final InputType type;
    private final int code;
    private final float threshold;
    private final boolean inverted;
    private final String description;
    
    // For keyboard and button inputs
    public InputBinding(InputType type, int code, String description) {
        this(type, code, 0.0f, false, description);
    }
    
    // For axis inputs with threshold
    public InputBinding(InputType type, int code, float threshold, boolean inverted, String description) {
        this.type = type;
        this.code = code;
        this.threshold = threshold;
        this.inverted = inverted;
        this.description = description;
    }
    
    public InputType getType() { return type; }
    public int getCode() { return code; }
    public float getThreshold() { return threshold; }
    public boolean isInverted() { return inverted; }
    public String getDescription() { return description; }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InputBinding that = (InputBinding) o;
        return code == that.code && 
               Float.compare(that.threshold, threshold) == 0 && 
               inverted == that.inverted && 
               type == that.type;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, code, threshold, inverted);
    }
    
    @Override
    public String toString() {
        return String.format("InputBinding{type=%s, code=%d, threshold=%.2f, inverted=%s, desc='%s'}", 
                           type, code, threshold, inverted, description);
    }
}