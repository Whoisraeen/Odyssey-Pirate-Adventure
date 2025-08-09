package com.odyssey.ui.widgets;

import com.odyssey.commands.CommandManager;
import com.odyssey.commands.CommandResult;
import com.odyssey.commands.ConsoleSender;
import com.odyssey.ui.UINode;
import com.odyssey.ui.UIRenderer;
import com.odyssey.ui.UIInputEvent;
import com.odyssey.ui.TextNode;
import org.joml.Vector4f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Console widget for displaying command output and accepting input.
 * Provides a command-line interface for the game.
 */
public class Console extends UINode {
    private static final int MAX_HISTORY_SIZE = 1000;
    private static final int MAX_COMMAND_HISTORY = 50;
    
    private final List<ConsoleMessage> messages = new ArrayList<>();
    private final List<String> commandHistory = new ArrayList<>();
    private final CommandManager commandManager;
    
    private TextBox inputField;
    private ScrollView scrollView;
    private boolean visible = false;
    private int historyIndex = -1;
    private String currentInput = "";
    
    public Console(CommandManager commandManager) {
        super();
        this.commandManager = commandManager;
        initializeComponents();
    }
    
    private void initializeComponents() {
        // Create scroll view for message display
        scrollView = new ScrollView();
        scrollView.setPosition(10, 10);
        scrollView.setSize(780, 350);
        scrollView.setBackgroundColor(0.0f, 0.0f, 0.0f, 0.8f);
        addChild(scrollView);
        
        // Create input field
        inputField = new TextBox();
        inputField.setPosition(10, 370);
        inputField.setSize(780, 30);
        inputField.setPlaceholder("Enter command...");
        inputField.setTextSubmitListener(this::executeCommand);
        addChild(inputField);
        
        // Set console size and background
        setSize(800, 410);
        setBackgroundColor(0.1f, 0.1f, 0.1f, 0.9f);
        
        // Add welcome message
        addMessage("Console initialized. Type 'help' for available commands.", MessageType.INFO);
    }
    
    /**
     * Toggles console visibility.
     */
    public void toggle() {
        visible = !visible;
        setVisible(visible);
        
        if (visible) {
            inputField.focus();
            scrollToBottom();
        } else {
            inputField.blur();
        }
    }
    
    /**
     * Shows the console.
     */
    public void show() {
        visible = true;
        setVisible(true);
        inputField.focus();
        scrollToBottom();
    }
    
    /**
     * Hides the console.
     */
    public void hide() {
        visible = false;
        setVisible(false);
        inputField.blur();
    }
    
    /**
     * Checks if the console is currently visible.
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Executes a command from the input field.
     */
    private void executeCommand(String command) {
        if (command.trim().isEmpty()) {
            return;
        }
        
        // Add to command history
        addToCommandHistory(command);
        
        // Display the command
        addMessage("> " + command, MessageType.COMMAND);
        
        // Execute the command
        CommandResult result = commandManager.executeCommand(ConsoleSender.getInstance(), command);
        
        // Display the result
        MessageType messageType = result.isSuccess() ? MessageType.SUCCESS : MessageType.ERROR;
        addMessage(result.getMessage(), messageType);
        
        // Clear input and reset history index
        inputField.setText("");
        historyIndex = -1;
        currentInput = "";
        
        scrollToBottom();
    }
    
    /**
     * Adds a message to the console.
     */
    public void addMessage(String message, MessageType type) {
        ConsoleMessage consoleMessage = new ConsoleMessage(message, type, System.currentTimeMillis());
        messages.add(consoleMessage);
        
        // Limit message history
        while (messages.size() > MAX_HISTORY_SIZE) {
            messages.remove(0);
        }
        
        updateScrollView();
        scrollToBottom();
    }
    
    /**
     * Adds a command to the command history.
     */
    private void addToCommandHistory(String command) {
        // Remove if already exists to avoid duplicates
        commandHistory.remove(command);
        
        // Add to end
        commandHistory.add(command);
        
        // Limit history size
        while (commandHistory.size() > MAX_COMMAND_HISTORY) {
            commandHistory.remove(0);
        }
    }
    
    /**
     * Handles up arrow key for command history navigation.
     */
    public void navigateHistoryUp() {
        if (commandHistory.isEmpty()) return;
        
        if (historyIndex == -1) {
            currentInput = inputField.getText();
            historyIndex = commandHistory.size() - 1;
        } else if (historyIndex > 0) {
            historyIndex--;
        }
        
        if (historyIndex >= 0 && historyIndex < commandHistory.size()) {
            inputField.setText(commandHistory.get(historyIndex));
        }
    }
    
    /**
     * Handles down arrow key for command history navigation.
     */
    public void navigateHistoryDown() {
        if (historyIndex == -1) return;
        
        historyIndex++;
        
        if (historyIndex >= commandHistory.size()) {
            historyIndex = -1;
            inputField.setText(currentInput);
        } else {
            inputField.setText(commandHistory.get(historyIndex));
        }
    }
    
    /**
     * Handles tab key for command completion.
     */
    public void handleTabCompletion() {
        String currentText = inputField.getText();
        List<String> completions = commandManager.getTabCompletions(ConsoleSender.getInstance(), currentText);
        
        if (completions.size() == 1) {
            // Single completion - apply it
            inputField.setText(completions.get(0));
        } else if (completions.size() > 1) {
            // Multiple completions - show them
            addMessage("Possible completions:", MessageType.INFO);
            StringBuilder sb = new StringBuilder();
            for (String completion : completions) {
                sb.append(completion).append("  ");
            }
            addMessage(sb.toString().trim(), MessageType.INFO);
        }
    }
    
    /**
     * Updates the scroll view with current messages.
     */
    private void updateScrollView() {
        // Clear existing content
        scrollView.clearContent();
        
        // Create a container for all messages
        ConsoleContentContainer messageContainer = new ConsoleContentContainer(messages);
        messageContainer.setSize(760, messages.size() * 20); // 20px per line
        
        scrollView.setContent(messageContainer);
    }
    
    /**
     * Scrolls to the bottom of the console.
     */
    private void scrollToBottom() {
        if (scrollView != null) {
            scrollView.scrollToBottom();
        }
    }
    
    /**
     * Clears all messages from the console.
     */
    public void clear() {
        messages.clear();
        updateScrollView();
    }
    
    /**
     * Gets the current input text.
     */
    public String getCurrentInput() {
        return inputField.getText();
    }
    
    /**
     * Sets the input text.
     */
    public void setInput(String text) {
        inputField.setText(text);
    }
    
    /**
     * Represents a console message with type and timestamp.
     */
    public static class ConsoleMessage {
        private final String message;
        private final MessageType type;
        private final long timestamp;
        
        public ConsoleMessage(String message, MessageType type, long timestamp) {
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
        }
        
        public String getMessage() { return message; }
        public MessageType getType() { return type; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Types of console messages with associated colors.
     */
    public enum MessageType {
        INFO(new Vector4f(1.0f, 1.0f, 1.0f, 1.0f)),      // White
        SUCCESS(new Vector4f(0.0f, 1.0f, 0.0f, 1.0f)),   // Green
        ERROR(new Vector4f(1.0f, 0.0f, 0.0f, 1.0f)),     // Red
        WARNING(new Vector4f(1.0f, 1.0f, 0.0f, 1.0f)),   // Yellow
        COMMAND(new Vector4f(0.7f, 0.7f, 1.0f, 1.0f));   // Light blue
        
        private final Vector4f color;
        
        MessageType(Vector4f color) {
            this.color = color;
        }
        
        public Vector4f getColor() { return new Vector4f(color); }
    }
    
    @Override
    protected void onUpdate(double deltaTime) {
        // Update console-specific logic here if needed
    }
    
    @Override
    protected void onRender(UIRenderer renderer) {
        // Render console background and content
        // The actual rendering is handled by child components (scrollView, inputField)
    }
    
    @Override
    protected boolean onInput(UIInputEvent event) {
        // Handle console-specific input events
        return false; // Let child components handle input
    }
    
    @Override
    protected void onLayout() {
        // Console uses fixed layout, no special layout logic needed
    }
    
    /**
     * Content container for console messages that handles text rendering.
     */
    private static class ConsoleContentContainer extends UINode {
        private final List<ConsoleMessage> messages;
        
        public ConsoleContentContainer(List<ConsoleMessage> messages) {
            this.messages = new ArrayList<>(messages);
        }
        
        @Override
        protected void onRender(UIRenderer renderer) {
            Vector2f pos = getComputedPosition();
            
            // Render each message
            for (int i = 0; i < messages.size(); i++) {
                ConsoleMessage msg = messages.get(i);
                float y = pos.y + (i * 20);
                renderer.drawText(msg.getMessage(), pos.x + 5, y, 14.0f, msg.getType().getColor());
            }
        }
        
        @Override
        protected void onUpdate(double deltaTime) {
            // No update logic needed
        }
        
        @Override
        protected boolean onInput(UIInputEvent event) {
            return false; // Don't handle input
        }
        
        @Override
        protected void onLayout() {
            // No layout logic needed
        }
    }
}