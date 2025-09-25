package com.odyssey.networking;

/**
 * Network message sent by clients when requesting to join a server.
 * Contains player information and connection details.
 */
public class JoinRequestMessage extends NetworkMessage {
    
    private static final long serialVersionUID = 1L;
    
    /** Player's display name */
    private final String playerName;
    
    /** Client version for compatibility checking */
    private final String clientVersion;
    
    /** Player's unique identifier */
    private final String playerId;
    
    /** Optional password for protected servers */
    private final String password;
    
    /** Client's preferred language */
    private final String language;
    
    /**
     * Constructor for JoinRequestMessage.
     * 
     * @param playerName The player's display name
     * @param clientVersion The client version string
     * @param playerId The player's unique identifier
     * @param password Optional server password (null if none)
     * @param language The client's preferred language
     */
    public JoinRequestMessage(String playerName, String clientVersion, 
                            String playerId, String password, String language) {
        super(MessageType.JOIN_REQUEST);
        this.playerName = playerName;
        this.clientVersion = clientVersion;
        this.playerId = playerId;
        this.password = password;
        this.language = language;
    }
    
    /**
     * Get the player's display name.
     * 
     * @return The player name
     */
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * Get the client version.
     * 
     * @return The client version string
     */
    public String getClientVersion() {
        return clientVersion;
    }
    
    /**
     * Get the player's unique identifier.
     * 
     * @return The player ID
     */
    public String getPlayerId() {
        return playerId;
    }
    
    /**
     * Get the server password (if provided).
     * 
     * @return The password or null if none provided
     */
    public String getPassword() {
        return password;
    }
    
    /**
     * Get the client's preferred language.
     * 
     * @return The language code
     */
    public String getLanguage() {
        return language;
    }
    
    /**
     * Check if a password was provided.
     * 
     * @return True if password is not null and not empty
     */
    public boolean hasPassword() {
        return password != null && !password.trim().isEmpty();
    }
    
    @Override
    public String toString() {
        return String.format("JoinRequestMessage{player=%s, id=%s, version=%s, hasPassword=%b, language=%s}", 
                           playerName, playerId, clientVersion, hasPassword(), language);
    }
}