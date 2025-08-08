package com.odyssey.ui;

import com.odyssey.ui.GamepadController;
import org.joml.Vector2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Manages the UI system, handling input distribution, focus management, and rendering coordination.
 */
public class UIManager {
    private static final Logger logger = LoggerFactory.getLogger(UIManager.class);
    
    // Root UI nodes
    private final List<UINode> rootNodes = new ArrayList<>();
    
    // Focus management
    private UINode focusedNode = null;
    private UINode hoveredNode = null;
    
    // Input state
    private final Vector2f lastMousePosition = new Vector2f();
    private boolean[] mouseButtonStates = new boolean[8];
    private boolean[] keyStates = new boolean[512];
    
    // Renderer
    private UIRenderer renderer;
    
    // Controller support
    private GamepadController gamepadController;
    
    /**
     * Creates a new UI manager.
     */
    public UIManager() {
        logger.info("UI Manager initialized");
    }
    
    /**
     * Initializes the UI manager with a renderer.
     */
    public void initialize(UIRenderer renderer) {
        this.renderer = renderer;
        
        // Initialize gamepad controller
        this.gamepadController = new GamepadController();
        this.gamepadController.initialize();
        
        logger.info("UI Manager initialized with renderer and gamepad support");
    }
    
    /**
     * Updates all UI nodes.
     */
    public void update(double deltaTime) {
        // Update gamepad controller
        if (gamepadController != null) {
            gamepadController.update();
        }
        
        // Update all root nodes
        for (UINode node : rootNodes) {
            if (node.isEnabled()) {
                updateNodeRecursive(node, deltaTime);
            }
        }
    }
    
    /**
     * Renders all UI nodes.
     */
    public void render() {
        if (renderer == null) {
            logger.warn("Cannot render UI: no renderer set");
            return;
        }
        
        // Render all root nodes
        for (UINode node : rootNodes) {
            if (node.isVisible()) {
                renderNodeRecursive(node);
            }
        }
    }
    
    /**
     * Handles mouse movement input.
     */
    public void handleMouseMove(double x, double y) {
        Vector2f mousePos = new Vector2f((float)x, (float)y);
        lastMousePosition.set(mousePos);
        
        // Find the topmost node under the mouse
        UINode nodeUnderMouse = findNodeUnderMouse(mousePos);
        
        // Handle mouse exit/enter events
        if (hoveredNode != nodeUnderMouse) {
            if (hoveredNode != null) {
                sendInputToNode(hoveredNode, new UIInputEvent(UIInputEvent.InputType.MOUSE_MOVE, mousePos, -1, false, false, false));
                hoveredNode.onMouseExit();
            }
            
            hoveredNode = nodeUnderMouse;
            
            if (hoveredNode != null) {
                sendInputToNode(hoveredNode, new UIInputEvent(UIInputEvent.InputType.MOUSE_MOVE, mousePos, -1, false, false, false));
            }
        } else if (hoveredNode != null) {
            // Send mouse move to hovered node
            sendInputToNode(hoveredNode, new UIInputEvent(UIInputEvent.InputType.MOUSE_MOVE, mousePos, -1, false, false, false));
        }
    }
    
    /**
     * Handles mouse button input.
     */
    public void handleMouseButton(int button, int action, int mods) {
        if (button < 0 || button >= mouseButtonStates.length) return;
        
        boolean pressed = action == GLFW_PRESS;
        mouseButtonStates[button] = pressed;
        
        boolean shift = (mods & GLFW_MOD_SHIFT) != 0;
        boolean ctrl = (mods & GLFW_MOD_CONTROL) != 0;
        boolean alt = (mods & GLFW_MOD_ALT) != 0;
        
        UIInputEvent.InputType inputType = pressed ? UIInputEvent.InputType.MOUSE_PRESS : UIInputEvent.InputType.MOUSE_RELEASE;
        UIInputEvent event = new UIInputEvent(inputType, lastMousePosition, button, shift, ctrl, alt);
        
        // Send to hovered node first
        if (hoveredNode != null) {
            boolean handled = sendInputToNode(hoveredNode, event);
            
            // Handle focus changes on mouse press
            if (pressed && button == GLFW_MOUSE_BUTTON_LEFT && handled) {
                if (hoveredNode.isFocusable()) {
                    setFocusedNode(hoveredNode);
                } else {
                    // Clear focus if clicking on non-focusable element
                    setFocusedNode(null);
                }
            }
        } else if (pressed && button == GLFW_MOUSE_BUTTON_LEFT) {
            // Clear focus if clicking on empty space
            setFocusedNode(null);
        }
    }
    
    /**
     * Handles mouse scroll input.
     */
    public void handleMouseScroll(double xOffset, double yOffset) {
        UIInputEvent event = new UIInputEvent(lastMousePosition, (float)xOffset, (float)yOffset);
        
        // Send to hovered node
        if (hoveredNode != null) {
            sendInputToNode(hoveredNode, event);
        }
    }
    
    /**
     * Handles keyboard input.
     */
    public void handleKeyboard(int key, int scancode, int action, int mods) {
        if (key < 0 || key >= keyStates.length) return;
        
        boolean pressed = action == GLFW_PRESS || action == GLFW_REPEAT;
        if (action != GLFW_REPEAT) {
            keyStates[key] = pressed;
        }
        
        UIInputEvent.InputType inputType = pressed ? UIInputEvent.InputType.KEY_PRESS : UIInputEvent.InputType.KEY_RELEASE;
        boolean shift = (mods & GLFW_MOD_SHIFT) != 0;
        boolean ctrl = (mods & GLFW_MOD_CONTROL) != 0;
        boolean alt = (mods & GLFW_MOD_ALT) != 0;
        UIInputEvent event = new UIInputEvent(inputType, key, shift, ctrl, alt);
        
        // Handle global key events first
        if (pressed) {
            switch (key) {
                case GLFW_KEY_TAB:
                    if ((mods & GLFW_MOD_SHIFT) != 0) {
                        focusPreviousNode();
                    } else {
                        focusNextNode();
                    }
                    return;
                    
                case GLFW_KEY_ESCAPE:
                    setFocusedNode(null);
                    return;
            }
        }
        
        // Send to focused node
        if (focusedNode != null) {
            sendInputToNode(focusedNode, event);
        }
    }
    
    /**
     * Handles text input.
     */
    public void handleTextInput(String text) {
        if (focusedNode != null && text != null && !text.isEmpty()) {
            UIInputEvent event = new UIInputEvent(text);
            sendInputToNode(focusedNode, event);
        }
    }
    
    /**
     * Adds a root UI node.
     */
    public void addRootNode(UINode node) {
        if (node != null && !rootNodes.contains(node)) {
            rootNodes.add(node);
            logger.debug("Added root UI node: {}", node.getClass().getSimpleName());
        }
    }
    
    /**
     * Removes a root UI node.
     */
    public void removeRootNode(UINode node) {
        if (rootNodes.remove(node)) {
            // Clear focus if the removed node or its children were focused
            if (isNodeOrChildFocused(node)) {
                setFocusedNode(null);
            }
            
            // Clear hover if the removed node or its children were hovered
            if (isNodeOrChildHovered(node)) {
                hoveredNode = null;
            }
            
            logger.debug("Removed root UI node: {}", node.getClass().getSimpleName());
        }
    }
    
    /**
     * Sets the focused node.
     */
    public void setFocusedNode(UINode node) {
        if (focusedNode == node) return;
        
        // Notify old focused node
        if (focusedNode != null) {
            focusedNode.setFocused(false);
            logger.debug("Focus lost: {}", focusedNode.getClass().getSimpleName());
        }
        
        focusedNode = node;
        
        // Notify new focused node
        if (focusedNode != null) {
            focusedNode.setFocused(true);
            logger.debug("Focus gained: {}", focusedNode.getClass().getSimpleName());
        }
    }
    
    /**
     * Focuses the next focusable node.
     */
    public void focusNextNode() {
        List<UINode> focusableNodes = getFocusableNodes();
        if (focusableNodes.isEmpty()) return;
        
        int currentIndex = focusedNode != null ? focusableNodes.indexOf(focusedNode) : -1;
        int nextIndex = (currentIndex + 1) % focusableNodes.size();
        
        setFocusedNode(focusableNodes.get(nextIndex));
    }
    
    /**
     * Focuses the previous focusable node.
     */
    public void focusPreviousNode() {
        List<UINode> focusableNodes = getFocusableNodes();
        if (focusableNodes.isEmpty()) return;
        
        int currentIndex = focusedNode != null ? focusableNodes.indexOf(focusedNode) : -1;
        int prevIndex = currentIndex <= 0 ? focusableNodes.size() - 1 : currentIndex - 1;
        
        setFocusedNode(focusableNodes.get(prevIndex));
    }
    
    /**
     * Gets all focusable nodes in the UI hierarchy.
     */
    private List<UINode> getFocusableNodes() {
        List<UINode> focusableNodes = new ArrayList<>();
        
        for (UINode rootNode : rootNodes) {
            collectFocusableNodes(rootNode, focusableNodes);
        }
        
        return focusableNodes;
    }
    
    /**
     * Recursively collects focusable nodes.
     */
    private void collectFocusableNodes(UINode node, List<UINode> focusableNodes) {
        if (node.isVisible() && node.isEnabled() && node.isFocusable()) {
            focusableNodes.add(node);
        }
        
        for (UINode child : node.getChildren()) {
            collectFocusableNodes(child, focusableNodes);
        }
    }
    
    /**
     * Finds the topmost node under the mouse position.
     */
    private UINode findNodeUnderMouse(Vector2f mousePos) {
        // Search from back to front (topmost first)
        for (int i = rootNodes.size() - 1; i >= 0; i--) {
            UINode node = rootNodes.get(i);
            if (node.isVisible() && node.isEnabled()) {
                UINode result = findNodeUnderMouseRecursive(node, mousePos);
                if (result != null) {
                    return result;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Recursively finds the topmost node under the mouse position.
     */
    private UINode findNodeUnderMouseRecursive(UINode node, Vector2f mousePos) {
        // Check children first (they're on top)
        List<UINode> children = node.getChildren();
        for (int i = children.size() - 1; i >= 0; i--) {
            UINode child = children.get(i);
            if (child.isVisible() && child.isEnabled()) {
                UINode result = findNodeUnderMouseRecursive(child, mousePos);
                if (result != null) {
                    return result;
                }
            }
        }
        
        // Check this node
        if (node.isInteractive() && node.containsPoint(mousePos)) {
            return node;
        }
        
        return null;
    }
    
    /**
     * Sends input to a node and its parents until handled.
     */
    private boolean sendInputToNode(UINode node, UIInputEvent event) {
        UINode current = node;
        
        while (current != null) {
            if (current.isEnabled() && current.handleInput(event)) {
                return true;
            }
            current = current.getParent();
        }
        
        return false;
    }
    
    /**
     * Recursively updates a node and its children.
     */
    private void updateNodeRecursive(UINode node, double deltaTime) {
        node.update(deltaTime);
        
        for (UINode child : node.getChildren()) {
            if (child.isEnabled()) {
                updateNodeRecursive(child, deltaTime);
            }
        }
    }
    
    /**
     * Recursively renders a node and its children.
     */
    private void renderNodeRecursive(UINode node) {
        if (!node.isVisible()) return;
        
        // Render this node
        node.render(renderer);
        
        // Render children
        for (UINode child : node.getChildren()) {
            if (child.isVisible()) {
                renderNodeRecursive(child);
            }
        }
    }
    
    /**
     * Checks if a node or any of its children is focused.
     */
    private boolean isNodeOrChildFocused(UINode node) {
        if (node == focusedNode) return true;
        
        for (UINode child : node.getChildren()) {
            if (isNodeOrChildFocused(child)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Checks if a node or any of its children is hovered.
     */
    private boolean isNodeOrChildHovered(UINode node) {
        if (node == hoveredNode) return true;
        
        for (UINode child : node.getChildren()) {
            if (isNodeOrChildHovered(child)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Cleans up the UI manager.
     */
    public void cleanup() {
        rootNodes.clear();
        focusedNode = null;
        hoveredNode = null;
        
        // Cleanup gamepad controller
        if (gamepadController != null) {
            gamepadController.cleanup();
            gamepadController = null;
        }
        
        logger.info("UI Manager cleaned up");
    }
    
    // Getters
    public List<UINode> getRootNodes() { return new ArrayList<>(rootNodes); }
    public UINode getFocusedNode() { return focusedNode; }
    public UINode getHoveredNode() { return hoveredNode; }
    public UIRenderer getRenderer() { return renderer; }
    
    public boolean isMouseButtonPressed(int button) {
        return button >= 0 && button < mouseButtonStates.length && mouseButtonStates[button];
    }
    
    public boolean isKeyPressed(int key) {
        return key >= 0 && key < keyStates.length && keyStates[key];
    }
    
    public Vector2f getMousePosition() { return new Vector2f(lastMousePosition); }
    
    public GamepadController getGamepadController() { return gamepadController; }
}