package com.odyssey.player;

import org.joml.Vector3f;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all players in the game world.
 * Handles player lifecycle, state management, and provides access to player data.
 */
public class PlayerManager {
    
    /** Map of player ID to Player objects */
    private final Map<String, Player> players;
    
    /** The current active player (for single-player mode) */
    private Player currentPlayer;
    
    /** Default spawn position for new players */
    private Vector3f defaultSpawnPosition;
    
    /** Singleton instance */
    private static PlayerManager instance;
    
    /**
     * Private constructor for singleton pattern.
     */
    private PlayerManager() {
        this.players = new ConcurrentHashMap<>();
        this.defaultSpawnPosition = new Vector3f(0, 64, 0); // Sea level spawn
    }
    
    /**
     * Get the singleton instance of PlayerManager.
     * 
     * @return The PlayerManager instance
     */
    public static PlayerManager getInstance() {
        if (instance == null) {
            synchronized (PlayerManager.class) {
                if (instance == null) {
                    instance = new PlayerManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * Create a new player and add them to the game.
     * 
     * @param playerName The player's display name
     * @return The created Player object
     */
    public Player createPlayer(String playerName) {
        Player player = new Player(playerName);
        player.setSpawnPoint(defaultSpawnPosition);
        player.setPosition(defaultSpawnPosition);
        
        players.put(player.getPlayerId(), player);
        
        // Set as current player if this is the first player
        if (currentPlayer == null) {
            currentPlayer = player;
        }
        
        return player;
    }
    
    /**
     * Create a player with a specific ID (for loading from save).
     * 
     * @param playerId The player's unique ID
     * @param playerName The player's display name
     * @return The created Player object
     */
    public Player createPlayer(String playerId, String playerName) {
        Player player = new Player(playerId, playerName);
        player.setSpawnPoint(defaultSpawnPosition);
        player.setPosition(defaultSpawnPosition);
        
        players.put(playerId, player);
        
        // Set as current player if this is the first player
        if (currentPlayer == null) {
            currentPlayer = player;
        }
        
        return player;
    }
    
    /**
     * Remove a player from the game.
     * 
     * @param playerId The player's unique ID
     * @return true if the player was removed, false if not found
     */
    public boolean removePlayer(String playerId) {
        Player removedPlayer = players.remove(playerId);
        
        // If we removed the current player, set a new current player
        if (removedPlayer == currentPlayer) {
            currentPlayer = players.isEmpty() ? null : players.values().iterator().next();
        }
        
        return removedPlayer != null;
    }
    
    /**
     * Get a player by their ID.
     * 
     * @param playerId The player's unique ID
     * @return The Player object, or null if not found
     */
    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }
    
    /**
     * Get a player by their name.
     * 
     * @param playerName The player's display name
     * @return The Player object, or null if not found
     */
    public Player getPlayerByName(String playerName) {
        return players.values().stream()
                .filter(player -> player.getPlayerName().equals(playerName))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get the current active player.
     * 
     * @return The current Player object, or null if no players exist
     */
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
    
    /**
     * Set the current active player.
     * 
     * @param playerId The ID of the player to set as current
     * @return true if the player was set as current, false if not found
     */
    public boolean setCurrentPlayer(String playerId) {
        Player player = players.get(playerId);
        if (player != null) {
            currentPlayer = player;
            return true;
        }
        return false;
    }
    
    /**
     * Get all players in the game.
     * 
     * @return A map of player ID to Player objects
     */
    public Map<String, Player> getAllPlayers() {
        return new HashMap<>(players);
    }
    
    /**
     * Get the number of players in the game.
     * 
     * @return The player count
     */
    public int getPlayerCount() {
        return players.size();
    }
    
    /**
     * Update all players.
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void updateAllPlayers(float deltaTime) {
        for (Player player : players.values()) {
            player.update(deltaTime);
        }
    }
    
    /**
     * Get the default spawn position for new players.
     * 
     * @return The default spawn position
     */
    public Vector3f getDefaultSpawnPosition() {
        return new Vector3f(defaultSpawnPosition);
    }
    
    /**
     * Set the default spawn position for new players.
     * 
     * @param spawnPosition The new default spawn position
     */
    public void setDefaultSpawnPosition(Vector3f spawnPosition) {
        this.defaultSpawnPosition.set(spawnPosition);
    }
    
    /**
     * Find the closest player to a given position.
     * 
     * @param position The position to search from
     * @return The closest Player object, or null if no players exist
     */
    public Player getClosestPlayer(Vector3f position) {
        Player closestPlayer = null;
        float closestDistance = Float.MAX_VALUE;
        
        for (Player player : players.values()) {
            float distance = position.distance(player.getPosition());
            if (distance < closestDistance) {
                closestDistance = distance;
                closestPlayer = player;
            }
        }
        
        return closestPlayer;
    }
    
    /**
     * Get all players within a certain radius of a position.
     * 
     * @param position The center position
     * @param radius The search radius
     * @return A map of player ID to Player objects within the radius
     */
    public Map<String, Player> getPlayersInRadius(Vector3f position, float radius) {
        Map<String, Player> nearbyPlayers = new HashMap<>();
        
        for (Map.Entry<String, Player> entry : players.entrySet()) {
            if (position.distance(entry.getValue().getPosition()) <= radius) {
                nearbyPlayers.put(entry.getKey(), entry.getValue());
            }
        }
        
        return nearbyPlayers;
    }
    
    /**
     * Respawn a player at their spawn point.
     * 
     * @param playerId The player's unique ID
     * @return true if the player was respawned, false if not found
     */
    public boolean respawnPlayer(String playerId) {
        Player player = players.get(playerId);
        if (player != null) {
            player.respawn();
            return true;
        }
        return false;
    }
    
    /**
     * Respawn the current player.
     * 
     * @return true if the current player was respawned, false if no current player
     */
    public boolean respawnCurrentPlayer() {
        if (currentPlayer != null) {
            currentPlayer.respawn();
            return true;
        }
        return false;
    }
    
    /**
     * Clear all players from the manager.
     * Used when loading a new game or resetting.
     */
    public void clearAllPlayers() {
        players.clear();
        currentPlayer = null;
    }
    
    /**
     * Check if a player exists.
     * 
     * @param playerId The player's unique ID
     * @return true if the player exists, false otherwise
     */
    public boolean hasPlayer(String playerId) {
        return players.containsKey(playerId);
    }
    
    /**
     * Check if a player name is already taken.
     * 
     * @param playerName The player name to check
     * @return true if the name is taken, false otherwise
     */
    public boolean isPlayerNameTaken(String playerName) {
        return players.values().stream()
                .anyMatch(player -> player.getPlayerName().equals(playerName));
    }
    
    /**
     * Get the current player's position.
     * Convenience method for common operations.
     * 
     * @return The current player's position, or (0,0,0) if no current player
     */
    public Vector3f getCurrentPlayerPosition() {
        if (currentPlayer != null) {
            return currentPlayer.getPosition();
        }
        return new Vector3f(0, 0, 0);
    }
    
    /**
     * Set the current player's position.
     * Convenience method for common operations.
     * 
     * @param position The new position
     * @return true if the position was set, false if no current player
     */
    public boolean setCurrentPlayerPosition(Vector3f position) {
        if (currentPlayer != null) {
            currentPlayer.setPosition(position);
            return true;
        }
        return false;
    }
    
    /**
     * Get the current player's health.
     * Convenience method for common operations.
     * 
     * @return The current player's health, or 0 if no current player
     */
    public float getCurrentPlayerHealth() {
        if (currentPlayer != null) {
            return currentPlayer.getHealth();
        }
        return 0.0f;
    }
    
    /**
     * Get the current player's inventory.
     * Convenience method for common operations.
     * 
     * @return The current player's inventory, or empty map if no current player
     */
    public Map<String, Integer> getCurrentPlayerInventory() {
        if (currentPlayer != null) {
            return currentPlayer.getInventory();
        }
        return new HashMap<>();
    }
    
    @Override
    public String toString() {
        return String.format("PlayerManager{playerCount=%d, currentPlayer=%s}", 
                           players.size(), 
                           currentPlayer != null ? currentPlayer.getPlayerName() : "none");
    }
}