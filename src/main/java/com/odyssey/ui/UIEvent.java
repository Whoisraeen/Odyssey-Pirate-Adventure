package com.odyssey.ui;

import org.joml.Vector2f;

/**
 * Base class for UI events.
 */
public abstract class UIEvent {
    private final UIEventType type;
    private final long timestamp;
    private boolean consumed = false;
    
    public UIEvent(UIEventType type) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }
    
    public UIEventType getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public boolean isConsumed() { return consumed; }
    public void consume() { consumed = true; }
    
    /**
     * Types of UI events.
     */
    public enum UIEventType {
        MOUSE_ENTER,
        MOUSE_EXIT,
        MOUSE_MOVE,
        MOUSE_PRESS,
        MOUSE_RELEASE,
        MOUSE_CLICK,
        KEY_PRESS,
        KEY_RELEASE,
        TEXT_INPUT,
        FOCUS_GAINED,
        FOCUS_LOST,
        VALUE_CHANGED
    }
    
    /**
     * Mouse-related UI event.
     */
    public static class MouseEvent extends UIEvent {
        private final Vector2f position;
        private final int button;
        private final boolean shift, ctrl, alt;
        
        public MouseEvent(UIEventType type, Vector2f position, int button, boolean shift, boolean ctrl, boolean alt) {
            super(type);
            this.position = new Vector2f(position);
            this.button = button;
            this.shift = shift;
            this.ctrl = ctrl;
            this.alt = alt;
        }
        
        public Vector2f getPosition() { return new Vector2f(position); }
        public int getButton() { return button; }
        public boolean isShiftDown() { return shift; }
        public boolean isCtrlDown() { return ctrl; }
        public boolean isAltDown() { return alt; }
    }
    
    /**
     * Keyboard-related UI event.
     */
    public static class KeyEvent extends UIEvent {
        private final int key;
        private final int scancode;
        private final int mods;
        
        public KeyEvent(UIEventType type, int key, int scancode, int mods) {
            super(type);
            this.key = key;
            this.scancode = scancode;
            this.mods = mods;
        }
        
        public int getKey() { return key; }
        public int getScancode() { return scancode; }
        public int getMods() { return mods; }
    }
    
    /**
     * Text input UI event.
     */
    public static class TextEvent extends UIEvent {
        private final String text;
        
        public TextEvent(String text) {
            super(UIEventType.TEXT_INPUT);
            this.text = text;
        }
        
        public String getText() { return text; }
    }
    
    /**
     * Value change UI event.
     */
    public static class ValueChangeEvent extends UIEvent {
        private final Object oldValue;
        private final Object newValue;
        
        public ValueChangeEvent(Object oldValue, Object newValue) {
            super(UIEventType.VALUE_CHANGED);
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
        
        public Object getOldValue() { return oldValue; }
        public Object getNewValue() { return newValue; }
    }
}