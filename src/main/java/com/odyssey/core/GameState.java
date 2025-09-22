package com.odyssey.core;

/**
 * Enumeration of all possible game states in The Odyssey.
 * 
 * This enum defines the various states the game can be in, allowing for
 * proper state management and transitions throughout the game lifecycle.
 * Each state represents a distinct mode of operation with its own update
 * and rendering logic.
 */
public enum GameState {
    
    /**
     * Game is initializing - loading core systems and resources.
     */
    INITIALIZING,
    
    /**
     * Main menu state - player can start new game, load save, access settings.
     */
    MAIN_MENU,
    
    /**
     * Loading state - world generation, asset loading, or save game loading.
     */
    LOADING,
    
    /**
     * Active gameplay state - player is sailing the seas, building ships, exploring.
     */
    IN_GAME,
    
    /**
     * Game is paused - world simulation is halted, pause menu is displayed.
     */
    PAUSED,
    
    /**
     * Settings/Options menu state.
     */
    SETTINGS,
    
    /**
     * Inventory management state.
     */
    INVENTORY,
    
    /**
     * Ship building/modification state.
     */
    SHIP_BUILDER,
    
    /**
     * Map/Navigation state - viewing the world map and planning routes.
     */
    MAP_VIEW,
    
    /**
     * Trading interface state - interacting with merchants and markets.
     */
    TRADING,
    
    /**
     * Combat state - engaged in naval or personal combat.
     */
    COMBAT,
    
    /**
     * Dialogue state - conversing with NPCs.
     */
    DIALOGUE,
    
    /**
     * Multiplayer lobby state - joining or hosting multiplayer sessions.
     */
    MULTIPLAYER_LOBBY,
    
    /**
     * Game is shutting down - saving data and cleaning up resources.
     */
    EXITING;
    
    /**
     * Check if this state allows world simulation to continue.
     * 
     * @return true if the world should continue updating in this state
     */
    public boolean allowsWorldUpdate() {
        return switch (this) {
            case IN_GAME, INVENTORY, SHIP_BUILDER, MAP_VIEW, TRADING, DIALOGUE -> true;
            default -> false;
        };
    }
    
    /**
     * Check if this state should render the 3D world.
     * 
     * @return true if the 3D world should be rendered in this state
     */
    public boolean shouldRenderWorld() {
        return switch (this) {
            case IN_GAME, PAUSED, INVENTORY, SHIP_BUILDER, MAP_VIEW, TRADING, COMBAT, DIALOGUE -> true;
            default -> false;
        };
    }
    
    /**
     * Check if this state should accept player input for world interaction.
     * 
     * @return true if player can interact with the world in this state
     */
    public boolean allowsWorldInteraction() {
        return switch (this) {
            case IN_GAME, SHIP_BUILDER -> true;
            default -> false;
        };
    }
    
    /**
     * Check if this state represents an overlay state (rendered on top of the world).
     * 
     * @return true if this is an overlay state
     */
    public boolean isOverlayState() {
        return switch (this) {
            case PAUSED, INVENTORY, MAP_VIEW, TRADING, DIALOGUE, SETTINGS -> true;
            default -> false;
        };
    }
    
    /**
     * Get a human-readable description of this game state.
     * 
     * @return A descriptive string for this state
     */
    public String getDescription() {
        return switch (this) {
            case INITIALIZING -> "Initializing game systems";
            case MAIN_MENU -> "Main Menu";
            case LOADING -> "Loading world";
            case IN_GAME -> "Sailing the seven seas";
            case PAUSED -> "Game Paused";
            case SETTINGS -> "Game Settings";
            case INVENTORY -> "Managing inventory";
            case SHIP_BUILDER -> "Building and customizing ships";
            case MAP_VIEW -> "Charting the course";
            case TRADING -> "Trading with merchants";
            case COMBAT -> "Engaged in combat";
            case DIALOGUE -> "In conversation";
            case MULTIPLAYER_LOBBY -> "Multiplayer Lobby";
            case EXITING -> "Shutting down";
        };
    }
}