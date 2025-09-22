package com.odyssey.core;

import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.odyssey.input.InputManager;
import com.odyssey.input.GameAction;

/**
 * State management system for The Odyssey.
 * 
 * This class manages the current game state and handles transitions between
 * different states. It supports a stack-based state system where states can
 * be pushed and popped, allowing for overlay states like pause menus and
 * inventory screens.
 */
public class StateManager {
    
    private static final Logger logger = LoggerFactory.getLogger(StateManager.class);
    
    // State stack for managing overlay states
    private final Stack<GameState> stateStack = new Stack<>();
    
    // Current primary state
    private GameState currentState = GameState.INITIALIZING;
    
    // Previous state for transition handling
    private GameState previousState = null;
    
    // State transition flags
    private boolean stateChanged = false;
    private GameState pendingState = null;
    
    /**
     * Initialize the state manager.
     */
    public void initialize() {
        logger.info("State manager initialized with initial state: {}", currentState);
    }
    
    /**
     * Update the state manager. Should be called once per frame.
     * 
     * @param deltaTime Time elapsed since last frame in seconds
     * @param inputManager The input manager for handling input
     */
    public void update(float deltaTime, InputManager inputManager) {
        // Handle pending state transitions
        if (pendingState != null) {
            performStateTransition();
        }
        
        // Handle input for current state
        handleStateInput(inputManager);
        
        // Reset state change flag
        stateChanged = false;
    }
    
    /**
     * Handle input specific to the current state.
     * 
     * @param inputManager The input manager
     */
    private void handleStateInput(InputManager inputManager) {
        switch (currentState) {
            case IN_GAME -> handleInGameInput(inputManager);
            case MAIN_MENU -> handleMainMenuInput(inputManager);
            case PAUSED -> handlePausedInput(inputManager);
            case SETTINGS -> handleSettingsInput(inputManager);
            case INVENTORY -> handleInventoryInput(inputManager);
            case SHIP_BUILDER -> handleShipBuilderInput(inputManager);
            case MAP_VIEW -> handleMapViewInput(inputManager);
            case TRADING -> handleTradingInput(inputManager);
            case DIALOGUE -> handleDialogueInput(inputManager);
            default -> {
                // Default input handling
            }
        }
    }
    
    /**
     * Handle input when in the IN_GAME state.
     */
    private void handleInGameInput(InputManager inputManager) {
        // Pause game
        if (inputManager.isActionPressed(GameAction.MENU)) {
            pushState(GameState.PAUSED);
        }
        
        // Open inventory
        if (inputManager.isActionPressed(GameAction.INVENTORY)) {
            pushState(GameState.INVENTORY);
        }
        
        // Open map
        if (inputManager.isActionPressed(GameAction.MAP)) {
            pushState(GameState.MAP_VIEW);
        }
        
        // Open ship builder
        if (inputManager.isActionPressed(GameAction.SHIP_BUILDER)) {
            setState(GameState.SHIP_BUILDER);
        }
        
        // Debug toggles
        if (inputManager.isActionPressed(GameAction.DEBUG_TOGGLE)) {
            // Toggle debug mode (handled by renderer)
        }
    }
    
    /**
     * Handle input when in the MAIN_MENU state.
     */
    private void handleMainMenuInput(InputManager inputManager) {
        // Start game (placeholder - would normally be handled by UI)
        if (inputManager.isActionPressed(GameAction.PRIMARY_ACTION)) {
            setState(GameState.LOADING);
        }
        
        // Open settings
        if (inputManager.isActionPressed(GameAction.SETTINGS)) {
            pushState(GameState.SETTINGS);
        }
    }
    
    /**
     * Handle input when in the PAUSED state.
     */
    private void handlePausedInput(InputManager inputManager) {
        // Resume game
        if (inputManager.isActionPressed(GameAction.MENU)) {
            popState();
        }
        
        // Open settings
        if (inputManager.isActionPressed(GameAction.SETTINGS)) {
            pushState(GameState.SETTINGS);
        }
    }
    
    /**
     * Handle input when in the SETTINGS state.
     */
    private void handleSettingsInput(InputManager inputManager) {
        // Close settings
        if (inputManager.isActionPressed(GameAction.MENU)) {
            popState();
        }
    }
    
    /**
     * Handle input when in the INVENTORY state.
     */
    private void handleInventoryInput(InputManager inputManager) {
        // Close inventory
        if (inputManager.isActionPressed(GameAction.INVENTORY) || inputManager.isActionPressed(GameAction.MENU)) {
            popState();
        }
    }
    
    /**
     * Handle input when in the SHIP_BUILDER state.
     */
    private void handleShipBuilderInput(InputManager inputManager) {
        // Exit ship builder
        if (inputManager.isActionPressed(GameAction.SHIP_BUILDER) || inputManager.isActionPressed(GameAction.MENU)) {
            setState(GameState.IN_GAME);
        }
    }
    
    /**
     * Handle input when in the MAP_VIEW state.
     */
    private void handleMapViewInput(InputManager inputManager) {
        // Close map
        if (inputManager.isActionPressed(GameAction.MAP) || inputManager.isActionPressed(GameAction.MENU)) {
            popState();
        }
    }
    
    /**
     * Handle input when in the TRADING state.
     */
    private void handleTradingInput(InputManager inputManager) {
        // Close trading interface
        if (inputManager.isActionPressed(GameAction.MENU)) {
            popState();
        }
    }
    
    /**
     * Handle input when in the DIALOGUE state.
     */
    private void handleDialogueInput(InputManager inputManager) {
        // Close dialogue (placeholder - would normally advance dialogue)
        if (inputManager.isActionPressed(GameAction.PRIMARY_ACTION) || inputManager.isActionPressed(GameAction.MENU)) {
            popState();
        }
    }
    
    /**
     * Set the current game state.
     * 
     * @param newState The new state to set
     */
    public void setState(GameState newState) {
        if (newState == currentState) {
            return;
        }
        
        pendingState = newState;
        logger.debug("State change requested: {} -> {}", currentState, newState);
    }
    
    /**
     * Push a state onto the state stack (for overlay states).
     * 
     * @param state The state to push
     */
    public void pushState(GameState state) {
        if (state.isOverlayState()) {
            stateStack.push(currentState);
            pendingState = state;
            logger.debug("Pushing overlay state: {} (current: {})", state, currentState);
        } else {
            logger.warn("Attempted to push non-overlay state: {}", state);
            setState(state);
        }
    }
    
    /**
     * Pop the current state from the stack and return to the previous state.
     */
    public void popState() {
        if (!stateStack.isEmpty()) {
            pendingState = stateStack.pop();
            logger.debug("Popping state, returning to: {}", pendingState);
        } else {
            logger.warn("Attempted to pop state but stack is empty");
        }
    }
    
    /**
     * Perform the actual state transition.
     */
    private void performStateTransition() {
        previousState = currentState;
        currentState = pendingState;
        pendingState = null;
        stateChanged = true;
        
        logger.info("State changed: {} -> {}", previousState, currentState);
        
        // Handle state-specific initialization
        onStateEnter(currentState, previousState);
        onStateExit(previousState, currentState);
    }
    
    /**
     * Called when entering a new state.
     * 
     * @param newState The state being entered
     * @param oldState The previous state
     */
    private void onStateEnter(GameState newState, GameState oldState) {
        switch (newState) {
            case LOADING -> {
                logger.info("Entering loading state");
                // Start loading process
            }
            case IN_GAME -> {
                logger.info("Entering gameplay state");
                // Initialize or resume gameplay systems
            }
            case PAUSED -> {
                logger.info("Game paused");
                // Pause game systems
            }
            case MAIN_MENU -> {
                logger.info("Returning to main menu");
                // Initialize main menu
            }
            default -> {
                logger.debug("Entered state: {}", newState);
            }
        }
    }
    
    /**
     * Called when exiting a state.
     * 
     * @param oldState The state being exited
     * @param newState The new state
     */
    private void onStateExit(GameState oldState, GameState newState) {
        switch (oldState) {
            case LOADING -> {
                logger.info("Loading complete");
            }
            case PAUSED -> {
                logger.info("Game resumed");
            }
            case SHIP_BUILDER -> {
                logger.info("Exiting ship builder");
                // Save ship modifications
            }
            default -> {
                logger.debug("Exited state: {}", oldState);
            }
        }
    }
    
    /**
     * Check if the current state allows world updates.
     * 
     * @return true if the world should be updated
     */
    public boolean shouldUpdateWorld() {
        return currentState.allowsWorldUpdate();
    }
    
    /**
     * Check if the current state should render the world.
     * 
     * @return true if the world should be rendered
     */
    public boolean shouldRenderWorld() {
        return currentState.shouldRenderWorld();
    }
    
    /**
     * Check if the current state allows world interaction.
     * 
     * @return true if the player can interact with the world
     */
    public boolean allowsWorldInteraction() {
        return currentState.allowsWorldInteraction();
    }
    
    /**
     * Get the current game state.
     * 
     * @return The current state
     */
    public GameState getCurrentState() {
        return currentState;
    }
    
    /**
     * Get the previous game state.
     * 
     * @return The previous state, or null if no previous state
     */
    public GameState getPreviousState() {
        return previousState;
    }
    
    /**
     * Check if the state changed this frame.
     * 
     * @return true if the state changed
     */
    public boolean hasStateChanged() {
        return stateChanged;
    }
    
    /**
     * Check if there are any overlay states active.
     * 
     * @return true if overlay states are active
     */
    public boolean hasOverlayStates() {
        return !stateStack.isEmpty();
    }
    
    /**
     * Get the number of states in the stack.
     * 
     * @return The stack depth
     */
    public int getStackDepth() {
        return stateStack.size();
    }
    
    /**
     * Clear all states and return to the specified state.
     * 
     * @param state The state to return to
     */
    public void clearAndSetState(GameState state) {
        stateStack.clear();
        setState(state);
        logger.info("Cleared state stack and set state to: {}", state);
    }
}