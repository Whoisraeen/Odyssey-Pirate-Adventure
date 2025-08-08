package com.odyssey.ui.widgets;

import com.odyssey.ui.*;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A text input widget for single-line text entry.
 */
public class TextBox extends UINode {
    private static final Logger logger = LoggerFactory.getLogger(TextBox.class);
    
    // Text properties
    private StringBuilder text = new StringBuilder();
    private String placeholder = "";
    private int cursorPosition = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private int maxLength = 256;
    
    // Visual properties
    private final Vector4f backgroundColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private final Vector4f focusedBackgroundColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private final Vector4f borderColor = new Vector4f(0.6f, 0.6f, 0.6f, 1.0f);
    private final Vector4f focusedBorderColor = new Vector4f(0.3f, 0.6f, 1.0f, 1.0f);
    private final Vector4f textColor = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
    private final Vector4f placeholderColor = new Vector4f(0.6f, 0.6f, 0.6f, 1.0f);
    private final Vector4f selectionColor = new Vector4f(0.3f, 0.6f, 1.0f, 0.3f);
    private final Vector4f cursorColor = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
    
    // Layout properties
    private float padding = 8.0f;
    private float borderWidth = 1.0f;
    
    // State
    private boolean hovered = false;
    private boolean dragging = false;
    private double cursorBlinkTime = 0.0;
    private boolean cursorVisible = true;
    
    // Event handlers
    private TextChangeListener textChangeListener;
    private TextSubmitListener textSubmitListener;
    
    /**
     * Interface for text change events.
     */
    @FunctionalInterface
    public interface TextChangeListener {
        void onTextChanged(String oldText, String newText);
    }
    
    /**
     * Interface for text submit events (Enter key).
     */
    @FunctionalInterface
    public interface TextSubmitListener {
        void onTextSubmitted(String text);
    }
    
    /**
     * Creates a new text box.
     */
    public TextBox() {
        super();
        setSize(200.0f, 30.0f);
        setInteractive(true);
        setFocusable(true);
        
        logger.debug("Created TextBox");
    }
    
    /**
     * Creates a new text box with placeholder text.
     */
    public TextBox(String placeholder) {
        this();
        setPlaceholder(placeholder);
    }
    
    @Override
    protected void onUpdate(double deltaTime) {
        // Handle cursor blinking
        if (isFocused()) {
            cursorBlinkTime += deltaTime;
            if (cursorBlinkTime >= 0.5) {
                cursorVisible = !cursorVisible;
                cursorBlinkTime = 0.0;
            }
        } else {
            cursorVisible = false;
        }
    }
    
    @Override
    protected void onRender(UIRenderer renderer) {
        Vector2f pos = getComputedPosition();
        Vector2f size = getComputedSize();
        
        // Determine colors based on state
        Vector4f bgColor = isFocused() ? focusedBackgroundColor : backgroundColor;
        Vector4f borderCol = isFocused() ? focusedBorderColor : borderColor;
        
        // Draw background
        renderer.drawRectangle(pos.x, pos.y, size.x, size.y, bgColor);
        
        // Draw border
        // Top border
        renderer.drawRectangle(pos.x, pos.y, size.x, borderWidth, borderCol);
        // Bottom border
        renderer.drawRectangle(pos.x, pos.y + size.y - borderWidth, size.x, borderWidth, borderCol);
        // Left border
        renderer.drawRectangle(pos.x, pos.y, borderWidth, size.y, borderCol);
        // Right border
        renderer.drawRectangle(pos.x + size.x - borderWidth, pos.y, borderWidth, size.y, borderCol);
        
        // Calculate text area
        float textX = pos.x + padding;
        float textY = pos.y + (size.y * 0.5f);
        float textWidth = size.x - (padding * 2);
        
        // Draw selection if present
        if (hasSelection()) {
            int selStart = Math.min(selectionStart, selectionEnd);
            int selEnd = Math.max(selectionStart, selectionEnd);
            
            // This is a simplified selection rendering
            // In a real implementation, you'd calculate actual character positions
            float charWidth = 8.0f; // Rough estimate
            float selectionX = textX + (selStart * charWidth);
            float selectionWidth = (selEnd - selStart) * charWidth;
            
            renderer.drawRectangle(selectionX, pos.y + borderWidth, 
                                 Math.min(selectionWidth, textWidth), 
                                 size.y - (borderWidth * 2), selectionColor);
        }
        
        // Draw text or placeholder
        String displayText = text.length() > 0 ? text.toString() : placeholder;
        Vector4f displayColor = text.length() > 0 ? textColor : placeholderColor;
        
        if (!displayText.isEmpty()) {
            // Note: This is a placeholder for text rendering
            // In a real implementation, you'd use a proper text renderer with clipping
            float fontSize = 12.0f; // Default font size
            renderer.drawText(displayText, textX, textY, fontSize, displayColor);
        }
        
        // Draw cursor if focused and visible
        if (isFocused() && cursorVisible && !hasSelection()) {
            // This is a simplified cursor rendering
            float charWidth = 8.0f; // Rough estimate
            float cursorX = textX + (cursorPosition * charWidth);
            
            if (cursorX >= textX && cursorX <= textX + textWidth) {
                renderer.drawRectangle(cursorX, pos.y + padding, 1.0f, 
                                     size.y - (padding * 2), cursorColor);
            }
        }
    }
    
    @Override
    protected boolean onInput(UIInputEvent event) {
        Vector2f pos = getComputedPosition();
        Vector2f size = getComputedSize();
        Vector2f mousePos = event.getPosition();
        
        // Check if mouse is within bounds
        boolean inBounds = mousePos.x >= pos.x && mousePos.x <= pos.x + size.x &&
                          mousePos.y >= pos.y && mousePos.y <= pos.y + size.y;
        
        switch (event.getType()) {
            case MOUSE_MOVE:
                boolean wasHovered = hovered;
                hovered = inBounds;
                
                if (hovered != wasHovered) {
                    if (hovered) {
                        fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_ENTER, 
                            event.getPosition(), event.getButton(), 
                            event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                    } else {
                        fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_EXIT, 
                            event.getPosition(), event.getButton(), 
                            event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                    }
                }
                
                // Handle text selection dragging
                if (dragging && isFocused()) {
                    int newCursorPos = getCharacterPositionFromMouse(mousePos.x - pos.x - padding);
                    selectionEnd = newCursorPos;
                    cursorPosition = newCursorPos;
                    resetCursorBlink();
                }
                
                return hovered;
                
            case MOUSE_PRESS:
                if (event.getButton() == GLFW_MOUSE_BUTTON_LEFT && inBounds) {
                    // Request focus
                    requestFocus();
                    
                    // Set cursor position
                    int newCursorPos = getCharacterPositionFromMouse(mousePos.x - pos.x - padding);
                    cursorPosition = newCursorPos;
                    
                    // Start selection if shift is held
                    if (event.isShiftDown() && hasSelection()) {
                        selectionEnd = cursorPosition;
                    } else {
                        clearSelection();
                        if (event.isShiftDown()) {
                            selectionStart = cursorPosition;
                            selectionEnd = cursorPosition;
                        }
                    }
                    
                    dragging = true;
                    resetCursorBlink();
                    
                    fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_PRESS, 
                        event.getPosition(), event.getButton(), 
                        event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                    
                    return true;
                }
                break;
                
            case MOUSE_RELEASE:
                if (event.getButton() == GLFW_MOUSE_BUTTON_LEFT && dragging) {
                    dragging = false;
                    
                    fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_RELEASE, 
                        event.getPosition(), event.getButton(), 
                        event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                    
                    return true;
                }
                break;
                
            case KEY_PRESS:
                if (isFocused()) {
                    return handleKeyPress(event.getKey(), event.getMods());
                }
                break;
                
            case TEXT_INPUT:
                if (isFocused()) {
                    return handleTextInput(event.getText());
                }
                break;
        }
        
        return false;
    }
    
    @Override
    protected void onLayout() {
        // Ensure minimum size for text input
        if (size.x < padding * 2 + 50) {
            size.x = padding * 2 + 50;
        }
        if (size.y < padding * 2 + 16) {
            size.y = padding * 2 + 16;
        }
    }
    
    /**
     * Handles key press events.
     */
    private boolean handleKeyPress(int key, int mods) {
        boolean shift = (mods & GLFW_MOD_SHIFT) != 0;
        boolean ctrl = (mods & GLFW_MOD_CONTROL) != 0;
        
        switch (key) {
            case GLFW_KEY_LEFT:
                if (ctrl) {
                    moveCursorToWordBoundary(-1, shift);
                } else {
                    moveCursor(-1, shift);
                }
                return true;
                
            case GLFW_KEY_RIGHT:
                if (ctrl) {
                    moveCursorToWordBoundary(1, shift);
                } else {
                    moveCursor(1, shift);
                }
                return true;
                
            case GLFW_KEY_HOME:
                moveCursorToPosition(0, shift);
                return true;
                
            case GLFW_KEY_END:
                moveCursorToPosition(text.length(), shift);
                return true;
                
            case GLFW_KEY_BACKSPACE:
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPosition > 0) {
                    deleteCharacter(cursorPosition - 1);
                    moveCursor(-1, false);
                }
                return true;
                
            case GLFW_KEY_DELETE:
                if (hasSelection()) {
                    deleteSelection();
                } else if (cursorPosition < text.length()) {
                    deleteCharacter(cursorPosition);
                }
                return true;
                
            case GLFW_KEY_A:
                if (ctrl) {
                    selectAll();
                    return true;
                }
                break;
                
            case GLFW_KEY_C:
                if (ctrl && hasSelection()) {
                    copySelection();
                    return true;
                }
                break;
                
            case GLFW_KEY_V:
                if (ctrl) {
                    pasteFromClipboard();
                    return true;
                }
                break;
                
            case GLFW_KEY_X:
                if (ctrl && hasSelection()) {
                    cutSelection();
                    return true;
                }
                break;
                
            case GLFW_KEY_ENTER:
                if (textSubmitListener != null) {
                    textSubmitListener.onTextSubmitted(text.toString());
                }
                
                fireEvent(new UIEvent.KeyEvent(UIEvent.UIEventType.KEY_PRESS, 
                    key, 0, mods));
                
                return true;
                
            case GLFW_KEY_ESCAPE:
                releaseFocus();
                return true;
        }
        
        return false;
    }
    
    /**
     * Handles text input events.
     */
    private boolean handleTextInput(String inputText) {
        if (inputText == null || inputText.isEmpty()) {
            return false;
        }
        
        // Filter out control characters
        StringBuilder filtered = new StringBuilder();
        for (char c : inputText.toCharArray()) {
            if (c >= 32 && c != 127) { // Printable ASCII characters
                filtered.append(c);
            }
        }
        
        if (filtered.length() > 0) {
            insertText(filtered.toString());
            return true;
        }
        
        return false;
    }
    
    /**
     * Inserts text at the current cursor position.
     */
    private void insertText(String newText) {
        if (hasSelection()) {
            deleteSelection();
        }
        
        String oldText = text.toString();
        
        // Check length limit
        if (text.length() + newText.length() > maxLength) {
            newText = newText.substring(0, Math.max(0, maxLength - text.length()));
        }
        
        if (!newText.isEmpty()) {
            text.insert(cursorPosition, newText);
            cursorPosition += newText.length();
            resetCursorBlink();
            
            fireTextChangeEvent(oldText, text.toString());
        }
    }
    
    /**
     * Deletes a character at the specified position.
     */
    private void deleteCharacter(int position) {
        if (position >= 0 && position < text.length()) {
            String oldText = text.toString();
            text.deleteCharAt(position);
            resetCursorBlink();
            
            fireTextChangeEvent(oldText, text.toString());
        }
    }
    
    /**
     * Deletes the current selection.
     */
    private void deleteSelection() {
        if (!hasSelection()) return;
        
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        
        String oldText = text.toString();
        text.delete(start, end);
        cursorPosition = start;
        clearSelection();
        resetCursorBlink();
        
        fireTextChangeEvent(oldText, text.toString());
    }
    
    /**
     * Moves the cursor by the specified offset.
     */
    private void moveCursor(int offset, boolean extendSelection) {
        int newPosition = Math.max(0, Math.min(text.length(), cursorPosition + offset));
        moveCursorToPosition(newPosition, extendSelection);
    }
    
    /**
     * Moves the cursor to the specified position.
     */
    private void moveCursorToPosition(int position, boolean extendSelection) {
        cursorPosition = Math.max(0, Math.min(text.length(), position));
        
        if (extendSelection) {
            if (!hasSelection()) {
                selectionStart = cursorPosition - (position - cursorPosition);
            }
            selectionEnd = cursorPosition;
        } else {
            clearSelection();
        }
        
        resetCursorBlink();
    }
    
    /**
     * Moves cursor to word boundary.
     */
    private void moveCursorToWordBoundary(int direction, boolean extendSelection) {
        int newPosition = cursorPosition;
        
        if (direction < 0) {
            // Move to previous word boundary
            while (newPosition > 0 && Character.isWhitespace(text.charAt(newPosition - 1))) {
                newPosition--;
            }
            while (newPosition > 0 && !Character.isWhitespace(text.charAt(newPosition - 1))) {
                newPosition--;
            }
        } else {
            // Move to next word boundary
            while (newPosition < text.length() && !Character.isWhitespace(text.charAt(newPosition))) {
                newPosition++;
            }
            while (newPosition < text.length() && Character.isWhitespace(text.charAt(newPosition))) {
                newPosition++;
            }
        }
        
        moveCursorToPosition(newPosition, extendSelection);
    }
    
    /**
     * Gets character position from mouse x coordinate.
     */
    private int getCharacterPositionFromMouse(float mouseX) {
        // This is a simplified implementation
        // In a real implementation, you'd use actual font metrics
        float charWidth = 8.0f; // Rough estimate
        int position = Math.round(mouseX / charWidth);
        return Math.max(0, Math.min(text.length(), position));
    }
    
    /**
     * Resets cursor blink timing.
     */
    private void resetCursorBlink() {
        cursorBlinkTime = 0.0;
        cursorVisible = true;
    }
    
    /**
     * Fires a text change event.
     */
    private void fireTextChangeEvent(String oldText, String newText) {
        fireEvent(new UIEvent.TextEvent(newText));
        
        if (textChangeListener != null) {
            textChangeListener.onTextChanged(oldText, newText);
        }
    }
    
    // Selection methods
    private boolean hasSelection() {
        return selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd;
    }
    
    private void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
    }
    
    private void selectAll() {
        selectionStart = 0;
        selectionEnd = text.length();
        cursorPosition = text.length();
        resetCursorBlink();
    }
    
    private void copySelection() {
        if (hasSelection()) {
            int start = Math.min(selectionStart, selectionEnd);
            int end = Math.max(selectionStart, selectionEnd);
            String selectedText = text.substring(start, end);
            
            // Note: This is a placeholder for clipboard operations
            // In a real implementation, you'd use GLFW clipboard functions
            logger.debug("Copy to clipboard: {}", selectedText);
        }
    }
    
    private void cutSelection() {
        if (hasSelection()) {
            copySelection();
            deleteSelection();
        }
    }
    
    private void pasteFromClipboard() {
        // Note: This is a placeholder for clipboard operations
        // In a real implementation, you'd use GLFW clipboard functions
        String clipboardText = ""; // glfwGetClipboardString(window);
        if (!clipboardText.isEmpty()) {
            insertText(clipboardText);
        }
    }
    
    // Getters and setters
    public String getText() { return text.toString(); }
    public void setText(String text) {
        String oldText = this.text.toString();
        this.text.setLength(0);
        this.text.append(text != null ? text : "");
        cursorPosition = Math.min(cursorPosition, this.text.length());
        clearSelection();
        resetCursorBlink();
        
        fireTextChangeEvent(oldText, this.text.toString());
    }
    
    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { 
        this.placeholder = placeholder != null ? placeholder : ""; 
    }
    
    public int getMaxLength() { return maxLength; }
    public void setMaxLength(int maxLength) { 
        this.maxLength = Math.max(1, maxLength);
        if (text.length() > this.maxLength) {
            setText(text.substring(0, this.maxLength));
        }
    }
    
    public float getPadding() { return padding; }
    public void setPadding(float padding) { 
        this.padding = Math.max(0.0f, padding); 
        markNeedsLayout();
    }
    
    public Vector4f getBackgroundColor() { return new Vector4f(backgroundColor); }
    public void setBackgroundColor(float r, float g, float b, float a) { 
        backgroundColor.set(r, g, b, a); 
    }
    
    public Vector4f getBorderColor() { return new Vector4f(borderColor); }
    public void setBorderColor(float r, float g, float b, float a) { 
        borderColor.set(r, g, b, a); 
    }
    
    public Vector4f getTextColor() { return new Vector4f(textColor); }
    public void setTextColor(float r, float g, float b, float a) { 
        textColor.set(r, g, b, a); 
    }
    
    public TextChangeListener getTextChangeListener() { return textChangeListener; }
    public void setTextChangeListener(TextChangeListener listener) { 
        this.textChangeListener = listener; 
    }
    
    public TextSubmitListener getTextSubmitListener() { return textSubmitListener; }
    public void setTextSubmitListener(TextSubmitListener listener) { 
        this.textSubmitListener = listener; 
    }
}