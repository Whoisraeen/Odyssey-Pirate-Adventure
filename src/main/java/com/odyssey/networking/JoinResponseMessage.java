package com.odyssey.networking;

/**
 * Network message sent by server in response to a client join request.
 * Contains the result of the join attempt and server information.
 */
public class JoinResponseMessage extends NetworkMessage {
    
    private static final long serialVersionUID = 1L;
    
    /** Whether the join request was accepted */
    private final boolean accepted;
    
    /** Reason for rejection (null if accepted) */
    private final String rejectionReason;
    
    /** Server name/description */
    private final String serverName;
    
    /** Server version */
    private final String serverVersion;
    
    /** Maximum number of players allowed */
    private final int maxPlayers;
    
    /** Current number of players online */
    private final int currentPlayers;
    
    /** Server message of the day */
    private final String motd;
    
    /** Assigned player ID for this session */
    private final String assignedPlayerId;
    
    /**
     * Constructor for accepted join response.
     * 
     * @param serverName The server name
     * @param serverVersion The server version
     * @param maxPlayers Maximum players allowed
     * @param currentPlayers Current players online
     * @param motd Message of the day
     * @param assignedPlayerId The assigned player ID
     */
    public JoinResponseMessage(String serverName, String serverVersion, 
                             int maxPlayers, int currentPlayers, 
                             String motd, String assignedPlayerId) {
        super(MessageType.JOIN_RESPONSE);
        this.accepted = true;
        this.rejectionReason = null;
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = currentPlayers;
        this.motd = motd;
        this.assignedPlayerId = assignedPlayerId;
    }
    
    /**
     * Constructor for rejected join response.
     * 
     * @param rejectionReason The reason for rejection
     * @param serverName The server name
     * @param serverVersion The server version
     * @param maxPlayers Maximum players allowed
     * @param currentPlayers Current players online
     */
    public JoinResponseMessage(String rejectionReason, String serverName, 
                             String serverVersion, int maxPlayers, int currentPlayers) {
        super(MessageType.JOIN_RESPONSE);
        this.accepted = false;
        this.rejectionReason = rejectionReason;
        this.serverName = serverName;
        this.serverVersion = serverVersion;
        this.maxPlayers = maxPlayers;
        this.currentPlayers = currentPlayers;
        this.motd = null;
        this.assignedPlayerId = null;
    }
    
    /**
     * Check if the join request was accepted.
     * 
     * @return True if accepted, false if rejected
     */
    public boolean isAccepted() {
        return accepted;
    }
    
    /**
     * Get the rejection reason (if rejected).
     * 
     * @return The rejection reason or null if accepted
     */
    public String getRejectionReason() {
        return rejectionReason;
    }
    
    /**
     * Get the server name.
     * 
     * @return The server name
     */
    public String getServerName() {
        return serverName;
    }
    
    /**
     * Get the server version.
     * 
     * @return The server version
     */
    public String getServerVersion() {
        return serverVersion;
    }
    
    /**
     * Get the maximum number of players.
     * 
     * @return The max players
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }
    
    /**
     * Get the current number of players.
     * 
     * @return The current players
     */
    public int getCurrentPlayers() {
        return currentPlayers;
    }
    
    /**
     * Get the message of the day.
     * 
     * @return The MOTD or null if not provided
     */
    public String getMotd() {
        return motd;
    }
    
    /**
     * Get the assigned player ID.
     * 
     * @return The assigned player ID or null if rejected
     */
    public String getAssignedPlayerId() {
        return assignedPlayerId;
    }
    
    /**
     * Check if the server is full.
     * 
     * @return True if current players equals max players
     */
    public boolean isServerFull() {
        return currentPlayers >= maxPlayers;
    }
    
    @Override
    public String toString() {
        if (accepted) {
            return String.format("JoinResponseMessage{accepted=true, server=%s, version=%s, " +
                               "players=%d/%d, playerId=%s}", 
                               serverName, serverVersion, currentPlayers, maxPlayers, assignedPlayerId);
        } else {
            return String.format("JoinResponseMessage{accepted=false, reason=%s, server=%s, " +
                               "players=%d/%d}", 
                               rejectionReason, serverName, currentPlayers, maxPlayers);
        }
    }
}