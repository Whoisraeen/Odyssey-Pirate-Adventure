package com.odyssey.ship;

import com.odyssey.util.Logger;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all ships in the game world, including player ships and NPC ships.
 * Handles ship registration, tracking, and lifecycle management.
 */
public class ShipManager {
    private static final Logger LOGGER = Logger.getLogger(ShipManager.class);
    
    /** All registered ships by their unique ID */
    private final Map<String, Ship> allShips;
    
    /** Player-owned ships by ship ID */
    private final Map<String, Ship> playerShips;
    
    /** Currently active/selected ship ID */
    private String activeShipId;
    
    /** Ship spawn locations */
    private final List<Vector3f> spawnLocations;
    
    /** Maximum number of ships per player */
    private static final int MAX_PLAYER_SHIPS = 10;
    
    /**
     * Creates a new ShipManager instance.
     */
    public ShipManager() {
        this.allShips = new ConcurrentHashMap<>();
        this.playerShips = new ConcurrentHashMap<>();
        this.spawnLocations = new ArrayList<>();
        this.activeShipId = null;
        
        // Initialize default spawn locations
        initializeSpawnLocations();
        
        LOGGER.info("ShipManager initialized");
    }
    
    /**
     * Registers a ship with the manager.
     * 
     * @param ship The ship to register
     * @return true if registration was successful
     */
    public boolean registerShip(Ship ship) {
        if (ship == null || ship.getName() == null) {
            LOGGER.warn("Cannot register null ship or ship with null name");
            return false;
        }
        
        String shipId = ship.getName(); // Using name as ID for now
        
        if (allShips.containsKey(shipId)) {
            LOGGER.warn("Ship with ID '{}' is already registered", shipId);
            return false;
        }
        
        allShips.put(shipId, ship);
        LOGGER.info("Registered ship '{}' of type {}", shipId, ship.getShipType());
        
        return true;
    }
    
    /**
     * Registers a ship as belonging to the player.
     * 
     * @param ship The player ship to register
     * @return true if registration was successful
     */
    public boolean registerPlayerShip(Ship ship) {
        if (!registerShip(ship)) {
            return false;
        }
        
        if (playerShips.size() >= MAX_PLAYER_SHIPS) {
            LOGGER.warn("Cannot register more player ships, maximum of {} reached", MAX_PLAYER_SHIPS);
            return false;
        }
        
        String shipId = ship.getName();
        playerShips.put(shipId, ship);
        
        // Set as active ship if it's the first one
        if (activeShipId == null) {
            setActiveShip(shipId);
        }
        
        LOGGER.info("Registered player ship '{}'", shipId);
        return true;
    }
    
    /**
     * Sets the active ship for the player.
     * 
     * @param shipId The ID of the ship to make active
     * @return true if the ship was set as active
     */
    public boolean setActiveShip(String shipId) {
        if (shipId == null) {
            activeShipId = null;
            LOGGER.info("Cleared active ship");
            return true;
        }
        
        if (!playerShips.containsKey(shipId)) {
            LOGGER.warn("Cannot set active ship '{}' - not a player ship", shipId);
            return false;
        }
        
        activeShipId = shipId;
        LOGGER.info("Set active ship to '{}'", shipId);
        return true;
    }
    
    /**
     * Gets the currently active ship.
     * 
     * @return The active ship, or null if none is active
     */
    public Ship getActiveShip() {
        if (activeShipId == null) {
            return null;
        }
        return playerShips.get(activeShipId);
    }
    
    /**
     * Gets the active ship ID.
     * 
     * @return The active ship ID, or null if none is active
     */
    public String getActiveShipId() {
        return activeShipId;
    }
    
    /**
     * Gets all player ships.
     * 
     * @return List of all player ships
     */
    public List<Ship> getPlayerShips() {
        return new ArrayList<>(playerShips.values());
    }
    
    /**
     * Gets a ship by its ID.
     * 
     * @param shipId The ship ID
     * @return The ship, or null if not found
     */
    public Ship getShip(String shipId) {
        return allShips.get(shipId);
    }
    
    /**
     * Removes a ship from the manager.
     * 
     * @param shipId The ID of the ship to remove
     * @return true if the ship was removed
     */
    public boolean removeShip(String shipId) {
        Ship ship = allShips.remove(shipId);
        if (ship == null) {
            return false;
        }
        
        // Remove from player ships if it was one
        playerShips.remove(shipId);
        
        // Clear active ship if this was it
        if (shipId.equals(activeShipId)) {
            activeShipId = null;
            // Set another player ship as active if available
            if (!playerShips.isEmpty()) {
                String newActiveId = playerShips.keySet().iterator().next();
                setActiveShip(newActiveId);
            }
        }
        
        LOGGER.info("Removed ship '{}'", shipId);
        return true;
    }
    
    /**
     * Updates all ships managed by this manager.
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        // Update all ships
        for (Ship ship : allShips.values()) {
            try {
                ship.update(deltaTime);
            } catch (Exception e) {
                LOGGER.error("Error updating ship '{}'", ship.getName(), e);
            }
        }
    }
    
    /**
     * Gets the number of registered ships.
     * 
     * @return Total number of ships
     */
    public int getShipCount() {
        return allShips.size();
    }
    
    /**
     * Gets the number of player ships.
     * 
     * @return Number of player ships
     */
    public int getPlayerShipCount() {
        return playerShips.size();
    }
    
    /**
     * Initializes default spawn locations for ships.
     */
    private void initializeSpawnLocations() {
        // Add some default spawn locations around origin
        spawnLocations.add(new Vector3f(0, 0, 0));
        spawnLocations.add(new Vector3f(100, 0, 0));
        spawnLocations.add(new Vector3f(-100, 0, 0));
        spawnLocations.add(new Vector3f(0, 0, 100));
        spawnLocations.add(new Vector3f(0, 0, -100));
    }
    
    /**
     * Gets available spawn locations.
     * 
     * @return List of spawn locations
     */
    public List<Vector3f> getSpawnLocations() {
        return new ArrayList<>(spawnLocations);
    }
    
    /**
     * Clears all ships from the manager.
     */
    public void clear() {
        allShips.clear();
        playerShips.clear();
        activeShipId = null;
        LOGGER.info("Cleared all ships from manager");
    }
    
    /**
     * Get the total number of ships built by the player
     */
    public int getTotalShipsBuilt() {
        // For now, return the total number of player ships
        // In a full implementation, this would track all ships ever built
        return playerShips.size();
    }
}