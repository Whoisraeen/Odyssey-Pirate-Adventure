package com.odyssey.input;

/**
 * Enumeration of different input contexts in The Odyssey.
 * 
 * Input contexts define different control schemes and input behaviors
 * based on the current game state. This allows for context-sensitive
 * controls similar to Minecraft's different input modes.
 * 
 * @author Odyssey Development Team
 * @version 1.0
 * @since Java 25
 */
public enum InputContext {
    
    /**
     * Gameplay context - normal in-game controls for movement, interaction, combat.
     * Similar to Minecraft's standard gameplay controls.
     */
    GAMEPLAY("Gameplay", "Standard in-game controls for movement and interaction"),
    
    /**
     * Menu context - navigation through menus using keyboard/controller.
     * Similar to Minecraft's menu navigation controls.
     */
    MENU("Menu", "Menu navigation and selection controls"),
    
    /**
     * Inventory context - inventory management and item manipulation.
     * Similar to Minecraft's inventory screen controls.
     */
    INVENTORY("Inventory", "Inventory management and item manipulation controls"),
    
    /**
     * Ship Builder context - ship construction and modification controls.
     * Similar to Minecraft's creative mode building controls.
     */
    SHIP_BUILDER("Ship Builder", "Ship construction and modification controls"),
    
    /**
     * Map context - map navigation and waypoint management.
     * Custom controls for map interaction and navigation.
     */
    MAP("Map", "Map navigation and waypoint management controls"),
    
    /**
     * Combat context - enhanced combat controls with targeting and abilities.
     * Specialized controls for naval and personal combat.
     */
    COMBAT("Combat", "Enhanced combat controls with targeting and abilities"),
    
    /**
     * Dialogue context - conversation and dialogue option selection.
     * Similar to Minecraft's dialogue interaction controls.
     */
    DIALOGUE("Dialogue", "Conversation and dialogue option selection controls"),
    
    /**
     * Trading context - merchant interaction and trading interface.
     * Specialized controls for trading and commerce.
     */
    TRADING("Trading", "Merchant interaction and trading interface controls"),
    
    /**
     * Settings context - settings menu navigation and configuration.
     * Similar to Minecraft's settings menu controls.
     */
    SETTINGS("Settings", "Settings menu navigation and configuration controls");
    
    private final String displayName;
    private final String description;
    
    /**
     * Constructor for InputContext enum values.
     * 
     * @param displayName The human-readable name of this context
     * @param description A description of what this context is used for
     */
    InputContext(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Get the display name of this input context.
     * 
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get the description of this input context.
     * 
     * @return The description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the input context that corresponds to a given game state.
     * 
     * @param gameState The current game state
     * @return The appropriate input context for that state
     */
    public static InputContext fromGameState(com.odyssey.core.GameState gameState) {
        return switch (gameState) {
            case IN_GAME -> GAMEPLAY;
            case MAIN_MENU, LOAD_GAME -> MENU;
            case INVENTORY -> INVENTORY;
            case SHIP_BUILDER -> SHIP_BUILDER;
            case MAP_VIEW -> MAP;
            case COMBAT -> COMBAT;
            case DIALOGUE -> DIALOGUE;
            case TRADING -> TRADING;
            case SETTINGS -> SETTINGS;
            case PAUSED -> MENU; // Pause menu uses menu context
            case LOADING, INITIALIZING, EXITING -> MENU; // Loading states use menu context
            case MULTIPLAYER_LOBBY -> MENU; // Multiplayer lobby uses menu context
            default -> GAMEPLAY; // Default to gameplay context
        };
    }
}