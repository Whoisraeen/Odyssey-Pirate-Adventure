package com.odyssey.ui;

import org.joml.Vector2f;

/**
 * Input event for UI system.
 * Represents raw input that needs to be processed by UI nodes.
 */
public class UIInputEvent {
    private final InputType type;
    private final Vector2f position;
    private final int button;
    private final int key;
    private final int scancode;
    private final int mods;
    private final String text;
    private final boolean shift, ctrl, alt;
    private final float scrollX, scrollY;
    
    public enum InputType {
        MOUSE_MOVE,
        MOUSE_PRESS,
        MOUSE_RELEASE,
        MOUSE_SCROLL,
        KEY_PRESS,
        KEY_RELEASE,
        TEXT_INPUT,
        SCROLL
    }
    
    // Mouse event constructor
    public UIInputEvent(InputType type, Vector2f position, int button, boolean shift, boolean ctrl, boolean alt) {
        this.type = type;
        this.position = new Vector2f(position);
        this.button = button;
        this.key = -1;
        this.scancode = -1;
        this.mods = (shift ? 1 : 0) | (ctrl ? 2 : 0) | (alt ? 4 : 0);
        this.text = null;
        this.shift = shift;
        this.ctrl = ctrl;
        this.alt = alt;
        this.scrollX = 0.0f;
        this.scrollY = 0.0f;
    }
    
    // Keyboard event constructor
    public UIInputEvent(InputType type, int key, boolean shift, boolean ctrl, boolean alt) {
        this.type = type;
        this.position = new Vector2f();
        this.button = -1;
        this.key = key;
        this.scancode = -1;
        this.mods = (shift ? 1 : 0) | (ctrl ? 2 : 0) | (alt ? 4 : 0);
        this.text = null;
        this.shift = shift;
        this.ctrl = ctrl;
        this.alt = alt;
        this.scrollX = 0.0f;
        this.scrollY = 0.0f;
    }
    
    // Text input constructor
    public UIInputEvent(String text) {
        this.type = InputType.TEXT_INPUT;
        this.position = new Vector2f();
        this.button = -1;
        this.key = -1;
        this.scancode = -1;
        this.mods = 0;
        this.text = text;
        this.shift = false;
        this.ctrl = false;
        this.alt = false;
        this.scrollX = 0.0f;
        this.scrollY = 0.0f;
    }
    
    // Scroll event constructor
    public UIInputEvent(Vector2f position, float scrollX, float scrollY) {
        this.type = InputType.SCROLL;
        this.position = new Vector2f(position);
        this.button = -1;
        this.key = -1;
        this.scancode = -1;
        this.mods = 0;
        this.text = null;
        this.shift = false;
        this.ctrl = false;
        this.alt = false;
        this.scrollX = scrollX;
        this.scrollY = scrollY;
    }
    
    // Getters
    public InputType getType() { return type; }
    public Vector2f getPosition() { return new Vector2f(position); }
    public int getButton() { return button; }
    public int getKey() { return key; }
    public String getText() { return text; }
    public boolean isShiftDown() { return shift; }
    public boolean isCtrlDown() { return ctrl; }
    public boolean isAltDown() { return alt; }
    public float getScrollX() { return scrollX; }
    public float getScrollY() {
        return scrollY;
    }

    public int getScancode() {
        return scancode;
    }

    public int getMods() {
        return mods;
    }
}