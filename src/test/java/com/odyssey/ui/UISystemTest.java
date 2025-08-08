package com.odyssey.ui;

import com.odyssey.ui.widgets.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.joml.Vector2f;
import org.joml.Vector4f;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the UI system components.
 */
public class UISystemTest {
    
    private UIManager uiManager;
    private UIRenderer mockRenderer;
    
    @BeforeEach
    public void setUp() {
        uiManager = new UIManager();
        mockRenderer = new MockUIRenderer();
        uiManager.initialize(mockRenderer);
    }
    
    @Test
    public void testUINodeHierarchy() {
        // Create a simple hierarchy
        UINode root = new UINode();
        root.setPosition(0, 0);
        root.setSize(800, 600);
        
        UINode child1 = new UINode();
        child1.setPosition(10, 10);
        child1.setSize(100, 50);
        
        UINode child2 = new UINode();
        child2.setPosition(20, 20);
        child2.setSize(80, 30);
        
        root.addChild(child1);
        child1.addChild(child2);
        
        // Test hierarchy
        assertEquals(root, child1.getParent());
        assertEquals(child1, child2.getParent());
        assertEquals(1, root.getChildren().size());
        assertEquals(1, child1.getChildren().size());
        assertEquals(0, child2.getChildren().size());
        
        // Test computed positions
        Vector2f child1Computed = child1.getComputedPosition();
        assertEquals(10.0f, child1Computed.x, 0.001f);
        assertEquals(10.0f, child1Computed.y, 0.001f);
        
        Vector2f child2Computed = child2.getComputedPosition();
        assertEquals(30.0f, child2Computed.x, 0.001f); // 10 + 20
        assertEquals(30.0f, child2Computed.y, 0.001f); // 10 + 20
    }
    
    @Test
    public void testButtonWidget() {
        Button button = new Button("Test Button");
        
        // Test initial state
        assertEquals("Test Button", button.getText());
        assertFalse(button.isPressed());
        
        // Test click handler
        boolean[] clicked = {false};
        button.setClickHandler(() -> clicked[0] = true);
        
        // Simulate mouse press and release
        Vector2f buttonPos = button.getComputedPosition();
        Vector2f buttonSize = button.getComputedSize();
        Vector2f clickPos = new Vector2f(buttonPos.x + buttonSize.x / 2, buttonPos.y + buttonSize.y / 2);
        
        UIInputEvent pressEvent = new UIInputEvent(UIInputEvent.InputType.MOUSE_PRESS, clickPos, 0, false, false, false);
        UIInputEvent releaseEvent = new UIInputEvent(UIInputEvent.InputType.MOUSE_RELEASE, clickPos, 0, false, false, false);
        
        button.handleInput(pressEvent);
        assertTrue(button.isPressed());
        
        button.handleInput(releaseEvent);
        assertFalse(button.isPressed());
        assertTrue(clicked[0]);
    }
    
    @Test
    public void testSliderWidget() {
        Slider slider = new Slider(0.0f, 100.0f, 50.0f);
        
        // Test initial state
        assertEquals(0.0f, slider.getMinValue(), 0.001f);
        assertEquals(100.0f, slider.getMaxValue(), 0.001f);
        assertEquals(50.0f, slider.getValue(), 0.001f);
        
        // Test value change
        boolean[] valueChanged = {false};
        float[] newValue = {0.0f};
        slider.setValueChangeListener((oldVal, newVal) -> {
            valueChanged[0] = true;
            newValue[0] = newVal;
        });
        
        slider.setValue(75.0f);
        assertEquals(75.0f, slider.getValue(), 0.001f);
        assertTrue(valueChanged[0]);
        assertEquals(75.0f, newValue[0], 0.001f);
        
        // Test range clamping
        slider.setValue(150.0f);
        assertEquals(100.0f, slider.getValue(), 0.001f);
        
        slider.setValue(-10.0f);
        assertEquals(0.0f, slider.getValue(), 0.001f);
    }
    
    @Test
    public void testCheckboxWidget() {
        Checkbox checkbox = new Checkbox("Test Checkbox", false);
        
        // Test initial state
        assertEquals("Test Checkbox", checkbox.getText());
        assertFalse(checkbox.isChecked());
        
        // Test check change
        boolean[] checkChanged = {false};
        boolean[] newChecked = {false};
        checkbox.setCheckChangeListener(checked -> {
            checkChanged[0] = true;
            newChecked[0] = checked;
        });
        
        checkbox.setChecked(true);
        assertTrue(checkbox.isChecked());
        assertTrue(checkChanged[0]);
        assertTrue(newChecked[0]);
    }
    
    @Test
    public void testTextBoxWidget() {
        TextBox textBox = new TextBox("Enter text...");
        
        // Test initial state
        assertEquals("", textBox.getText());
        assertEquals("Enter text...", textBox.getPlaceholder());
        
        // Test text change
        boolean[] textChanged = {false};
        String[] newText = {""};
        textBox.setTextChangeListener((oldText, newTextValue) -> {
            textChanged[0] = true;
            newText[0] = newTextValue;
        });
        
        textBox.setText("Hello World");
        assertEquals("Hello World", textBox.getText());
        assertTrue(textChanged[0]);
        assertEquals("Hello World", newText[0]);
        
        // Test max length
        textBox.setMaxLength(5);
        textBox.setText("This is a very long text");
        assertEquals("This ", textBox.getText());
    }
    
    @Test
    public void testScrollViewWidget() {
        ScrollView scrollView = new ScrollView();
        scrollView.setSize(200, 150);
        
        // Add some content
        UINode content = new UINode();
        content.setSize(400, 300); // Larger than scroll view
        scrollView.setContent(content);
        
        // Test initial state
        Vector2f scrollOffset = scrollView.getScrollOffset();
        assertEquals(0.0f, scrollOffset.x, 0.001f);
        assertEquals(0.0f, scrollOffset.y, 0.001f);
        
        // Test scrolling
        scrollView.scrollTo(50, 75);
        scrollView.update(0.1); // Update to apply scroll
        
        Vector2f newScrollOffset = scrollView.getScrollOffset();
        assertTrue(newScrollOffset.x > 0);
        assertTrue(newScrollOffset.y > 0);
    }
    
    @Test
    public void testUIManager() {
        UINode rootNode = new UINode();
        rootNode.setSize(800, 600);
        rootNode.setInteractive(true);
        rootNode.setFocusable(true);
        
        uiManager.addRootNode(rootNode);
        
        // Test root node management
        assertEquals(1, uiManager.getRootNodes().size());
        assertTrue(uiManager.getRootNodes().contains(rootNode));
        
        // Test focus management
        uiManager.setFocusedNode(rootNode);
        assertEquals(rootNode, uiManager.getFocusedNode());
        assertTrue(rootNode.isFocused());
        
        // Test mouse input
        uiManager.handleMouseMove(100, 100);
        assertEquals(100.0f, uiManager.getMousePosition().x, 0.001f);
        assertEquals(100.0f, uiManager.getMousePosition().y, 0.001f);
        
        // Clean up
        uiManager.removeRootNode(rootNode);
        assertEquals(0, uiManager.getRootNodes().size());
        assertNull(uiManager.getFocusedNode());
    }
    
    @Test
    public void testUIEvents() {
        // Test mouse event
        Vector2f mousePos = new Vector2f(100, 200);
        UIEvent.MouseEvent mouseEvent = new UIEvent.MouseEvent(
            UIEvent.UIEventType.MOUSE_CLICK, mousePos, 0, false, true, false);
        
        assertEquals(UIEvent.UIEventType.MOUSE_CLICK, mouseEvent.getType());
        assertEquals(mousePos, mouseEvent.getPosition());
        assertEquals(0, mouseEvent.getButton());
        assertFalse(mouseEvent.isShiftDown());
        assertTrue(mouseEvent.isCtrlDown());
        assertFalse(mouseEvent.isAltDown());
        
        // Test key event
        UIEvent.KeyEvent keyEvent = new UIEvent.KeyEvent(
            UIEvent.UIEventType.KEY_PRESS, 65, 0, 1);
        
        assertEquals(UIEvent.UIEventType.KEY_PRESS, keyEvent.getType());
        assertEquals(65, keyEvent.getKey());
        assertEquals(0, keyEvent.getScancode());
        assertEquals(1, keyEvent.getMods());
        
        // Test value change event
        UIEvent.ValueChangeEvent valueEvent = new UIEvent.ValueChangeEvent(10.0f, 20.0f);
        
        assertEquals(UIEvent.UIEventType.VALUE_CHANGED, valueEvent.getType());
        assertEquals(10.0f, valueEvent.getOldValue(), 0.001f);
        assertEquals(20.0f, valueEvent.getNewValue(), 0.001f);
    }
    
    /**
     * Mock UI renderer for testing.
     */
    private static class MockUIRenderer extends UIRenderer {
        
        public MockUIRenderer() {
            // Initialize with dummy values for testing
        }
        
        @Override
        public void drawRectangle(float x, float y, float width, float height, Vector4f color) {
            // Mock implementation - do nothing
        }
        
        @Override
        public void drawText(String text, float x, float y, Vector4f color) {
            // Mock implementation - do nothing
        }
        
        @Override
        public void cleanup() {
            // Mock implementation - do nothing
        }
    }
}