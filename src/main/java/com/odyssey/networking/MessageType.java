package com.odyssey.networking;

/**
 * Enumeration of all message types used in The Odyssey networking.
 * Each message type represents a different kind of network communication.
 */
public enum MessageType {
    
    // Connection management messages
    /** Client requesting to join the server */
    JOIN_REQUEST,
    
    /** Server response to join request */
    JOIN_RESPONSE,
    
    /** Client or server disconnecting */
    DISCONNECT,
    
    /** Heartbeat/ping message to maintain connection */
    HEARTBEAT,
    
    // Player state messages
    /** Player position and movement update */
    PLAYER_POSITION,
    
    /** Player action (interact, attack, etc.) */
    PLAYER_ACTION,
    
    /** Player inventory changes */
    PLAYER_INVENTORY,
    
    /** Player health/status update */
    PLAYER_STATUS,
    
    // Ship-related messages
    /** Ship position and movement update */
    SHIP_POSITION,
    
    /** Ship component changes (sails, cannons, etc.) */
    SHIP_COMPONENT,
    
    /** Ship damage/repair events */
    SHIP_DAMAGE,
    
    /** Ship crew management */
    SHIP_CREW,
    
    // World state messages
    /** World chunk data */
    WORLD_CHUNK,
    
    /** Block placement/destruction */
    BLOCK_UPDATE,
    
    /** Weather system updates */
    WEATHER_UPDATE,
    
    /** Ocean physics updates (waves, currents) */
    OCEAN_UPDATE,
    
    // Combat messages
    /** Cannon fire event */
    CANNON_FIRE,
    
    /** Projectile trajectory */
    PROJECTILE_UPDATE,
    
    /** Damage dealt to entities */
    DAMAGE_EVENT,
    
    /** Boarding action initiated */
    BOARDING_ACTION,
    
    // Chat and communication
    /** Chat message between players */
    CHAT_MESSAGE,
    
    /** System announcement */
    SYSTEM_MESSAGE,
    
    // Economy and trading
    /** Trade request between players */
    TRADE_REQUEST,
    
    /** Trade response/completion */
    TRADE_RESPONSE,
    
    /** Market price updates */
    MARKET_UPDATE,
    
    // Faction and reputation
    /** Reputation change notification */
    REPUTATION_UPDATE,
    
    /** Faction relationship changes */
    FACTION_UPDATE,
    
    // Server administration
    /** Server configuration changes */
    SERVER_CONFIG,
    
    /** Player kicked/banned */
    PLAYER_KICKED,
    
    /** Server shutdown notification */
    SERVER_SHUTDOWN,
    
    // Error handling
    /** Generic error message */
    ERROR,
    
    /** Invalid request/malformed data */
    INVALID_REQUEST;
    
    /**
     * Check if this message type requires immediate processing.
     * 
     * @return True if the message should be processed immediately
     */
    public boolean isHighPriority() {
        return switch (this) {
            case HEARTBEAT, DISCONNECT, ERROR, INVALID_REQUEST, 
                 PLAYER_POSITION, SHIP_POSITION, DAMAGE_EVENT -> true;
            default -> false;
        };
    }
    
    /**
     * Check if this message type should be broadcast to all clients.
     * 
     * @return True if the message should be broadcast
     */
    public boolean shouldBroadcast() {
        return switch (this) {
            case PLAYER_POSITION, PLAYER_ACTION, SHIP_POSITION, 
                 SHIP_COMPONENT, BLOCK_UPDATE, WEATHER_UPDATE, 
                 OCEAN_UPDATE, CANNON_FIRE, PROJECTILE_UPDATE, 
                 DAMAGE_EVENT, CHAT_MESSAGE, SYSTEM_MESSAGE -> true;
            default -> false;
        };
    }
}