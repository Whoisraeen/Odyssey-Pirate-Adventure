package com.odyssey.player;

import com.odyssey.faction.FactionManager;
import com.odyssey.faction.FactionType;
import com.odyssey.faction.ReputationLevel;
import org.joml.Vector3f;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a player in the game world.
 * Manages player state including position, health, inventory, experience, and stats.
 */
public class Player {
    
    /** Unique identifier for this player */
    private final String playerId;
    
    /** Player's display name */
    private String playerName;
    
    /** Player's world position */
    private Vector3f position;
    
    /** Player's rotation (yaw, pitch, roll) */
    private Vector3f rotation;
    
    /** Player's velocity */
    private Vector3f velocity;
    
    /** Player's health system */
    private float health;
    private float maxHealth;
    
    /** Player's experience and level */
    private long experience;
    private int level;
    
    /** Player's inventory (item name -> quantity) */
    private Map<String, Integer> inventory;
    
    /** Player's stats and attributes */
    private Map<String, Object> playerStats;
    
    /** Movement state flags */
    private boolean isWalking;
    private boolean isRunning;
    private boolean isSwimming;
    private boolean isOnShip;
    
    /** ID of the ship the player is on (null if not on ship) */
    private String shipId;
    
    /** Player's spawn point */
    private Vector3f spawnPoint;
    
    /** Player's game time (time spent in game) */
    private long gameTime;
    
    /** Faction reputation manager for this player */
    private FactionManager factionManager;
    
    /**
     * Create a new player with default values.
     * 
     * @param playerName The player's display name
     */
    public Player(String playerName) {
        this.playerId = UUID.randomUUID().toString();
        this.playerName = playerName;
        this.position = new Vector3f(0, 64, 0); // Default spawn at sea level
        this.rotation = new Vector3f(0, 0, 0);
        this.velocity = new Vector3f(0, 0, 0);
        this.health = 100.0f;
        this.maxHealth = 100.0f;
        this.experience = 0;
        this.level = 1;
        this.inventory = new HashMap<>();
        this.playerStats = new HashMap<>();
        this.spawnPoint = new Vector3f(0, 64, 0);
        this.gameTime = 0;
        
        // Initialize faction manager
        this.factionManager = new FactionManager();
        
        // Initialize default stats
        initializeDefaultStats();
        
        // Initialize starting inventory
        initializeStartingInventory();
    }
    
    /**
     * Create a player with specific ID (for loading from save).
     * 
     * @param playerId The player's unique ID
     * @param playerName The player's display name
     */
    public Player(String playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.position = new Vector3f(0, 64, 0);
        this.rotation = new Vector3f(0, 0, 0);
        this.velocity = new Vector3f(0, 0, 0);
        this.health = 100.0f;
        this.maxHealth = 100.0f;
        this.experience = 0;
        this.level = 1;
        this.inventory = new HashMap<>();
        this.playerStats = new HashMap<>();
        this.spawnPoint = new Vector3f(0, 64, 0);
        this.gameTime = 0;
        
        // Initialize faction manager
        this.factionManager = new FactionManager();
        
        initializeDefaultStats();
        initializeStartingInventory();
    }
    
    /**
     * Initialize default player stats.
     */
    private void initializeDefaultStats() {
        playerStats.put("strength", 10);
        playerStats.put("dexterity", 10);
        playerStats.put("intelligence", 10);
        playerStats.put("constitution", 10);
        playerStats.put("charisma", 10);
        playerStats.put("seamanship", 1);
        playerStats.put("navigation", 1);
        playerStats.put("combat", 1);
        playerStats.put("crafting", 1);
        playerStats.put("trading", 1);
    }
    
    /**
     * Initialize starting inventory items.
     */
    private void initializeStartingInventory() {
        // Starting items for a castaway
        inventory.put("wooden_plank", 10);
        inventory.put("rope", 5);
        inventory.put("cloth", 3);
        inventory.put("bread", 2);
        inventory.put("water_bottle", 1);
    }
    
    /**
     * Update the player's state.
     * 
     * @param deltaTime Time since last update in seconds
     */
    public void update(float deltaTime) {
        // Update game time
        gameTime += (long)(deltaTime * 1000);
        
        // Update position based on velocity
        position.add(velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);
        
        // Apply natural health regeneration
        if (health < maxHealth) {
            float regenRate = 0.1f; // Health per second
            health = Math.min(maxHealth, health + regenRate * deltaTime);
        }
        
        // Update movement states based on velocity
        float speed = velocity.length();
        isWalking = speed > 0.1f && speed <= 3.0f;
        isRunning = speed > 3.0f;
    }
    
    /**
     * Damage the player.
     * 
     * @param damage Amount of damage to deal
     * @return true if the player is still alive, false if they died
     */
    public boolean takeDamage(float damage) {
        health = Math.max(0, health - damage);
        return health > 0;
    }
    
    /**
     * Heal the player.
     * 
     * @param healAmount Amount of health to restore
     */
    public void heal(float healAmount) {
        health = Math.min(maxHealth, health + healAmount);
    }
    
    /**
     * Add experience points and handle level ups.
     * 
     * @param exp Experience points to add
     */
    public void addExperience(long exp) {
        experience += exp;
        
        // Check for level up (simple formula: level = sqrt(experience / 100))
        int newLevel = (int) Math.sqrt(experience / 100.0) + 1;
        if (newLevel > level) {
            level = newLevel;
            onLevelUp();
        }
    }
    
    /**
     * Handle level up effects.
     */
    private void onLevelUp() {
        // Increase max health
        maxHealth += 5;
        health = maxHealth; // Full heal on level up
        
        // Increase stats slightly
        playerStats.put("strength", (Integer) playerStats.get("strength") + 1);
        playerStats.put("constitution", (Integer) playerStats.get("constitution") + 1);
    }
    
    /**
     * Add an item to the player's inventory.
     * 
     * @param itemName Name of the item
     * @param quantity Quantity to add
     */
    public void addItem(String itemName, int quantity) {
        inventory.put(itemName, inventory.getOrDefault(itemName, 0) + quantity);
    }
    
    /**
     * Remove an item from the player's inventory.
     * 
     * @param itemName Name of the item
     * @param quantity Quantity to remove
     * @return true if the item was successfully removed, false if not enough quantity
     */
    public boolean removeItem(String itemName, int quantity) {
        int currentQuantity = inventory.getOrDefault(itemName, 0);
        if (currentQuantity >= quantity) {
            if (currentQuantity == quantity) {
                inventory.remove(itemName);
            } else {
                inventory.put(itemName, currentQuantity - quantity);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Check if the player has a specific item.
     * 
     * @param itemName Name of the item
     * @param quantity Minimum quantity required
     * @return true if the player has at least the specified quantity
     */
    public boolean hasItem(String itemName, int quantity) {
        return inventory.getOrDefault(itemName, 0) >= quantity;
    }
    
    /**
     * Set the player's position.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }
    
    /**
     * Set the player's position.
     * 
     * @param newPosition New position vector
     */
    public void setPosition(Vector3f newPosition) {
        position.set(newPosition);
    }
    
    /**
     * Set the player's rotation.
     * 
     * @param yaw Yaw rotation
     * @param pitch Pitch rotation
     * @param roll Roll rotation
     */
    public void setRotation(float yaw, float pitch, float roll) {
        rotation.set(yaw, pitch, roll);
    }
    
    /**
     * Set the player's velocity.
     * 
     * @param vx X velocity
     * @param vy Y velocity
     * @param vz Z velocity
     */
    public void setVelocity(float vx, float vy, float vz) {
        velocity.set(vx, vy, vz);
    }
    
    /**
     * Respawn the player at their spawn point.
     */
    public void respawn() {
        position.set(spawnPoint);
        health = maxHealth;
        velocity.set(0, 0, 0);
        isOnShip = false;
        shipId = null;
    }
    
    // Getters
    public String getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public Vector3f getPosition() { return new Vector3f(position); }
    public Vector3f getRotation() { return new Vector3f(rotation); }
    public Vector3f getVelocity() { return new Vector3f(velocity); }
    
    public float getHealth() { return health; }
    public void setHealth(float health) { this.health = Math.max(0, Math.min(maxHealth, health)); }
    
    /**
     * Check if the player is dead (health is 0 or below).
     * @return true if the player is dead, false otherwise
     */
    public boolean isDead() {
        return health <= 0.0f;
    }
    
    public float getMaxHealth() { return maxHealth; }
    public void setMaxHealth(float maxHealth) { 
        this.maxHealth = maxHealth;
        this.health = Math.min(this.health, maxHealth);
    }
    
    public long getExperience() { return experience; }
    public void setExperience(long experience) { this.experience = experience; }
    
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    
    public Map<String, Integer> getInventory() { return new HashMap<>(inventory); }
    public void setInventory(Map<String, Integer> inventory) { 
        this.inventory = new HashMap<>(inventory); 
    }
    
    public Map<String, Object> getPlayerStats() { return new HashMap<>(playerStats); }
    public void setPlayerStats(Map<String, Object> playerStats) { 
        this.playerStats = new HashMap<>(playerStats); 
    }
    
    public boolean isWalking() { return isWalking; }
    public void setWalking(boolean walking) { this.isWalking = walking; }
    
    public boolean isRunning() { return isRunning; }
    public void setRunning(boolean running) { this.isRunning = running; }
    
    public boolean isSwimming() { return isSwimming; }
    public void setSwimming(boolean swimming) { this.isSwimming = swimming; }
    
    public boolean isOnShip() { return isOnShip; }
    public void setOnShip(boolean onShip) { this.isOnShip = onShip; }
    
    public String getShipId() { return shipId; }
    public void setShipId(String shipId) { this.shipId = shipId; }
    
    public Vector3f getSpawnPoint() { return new Vector3f(spawnPoint); }
    public void setSpawnPoint(Vector3f spawnPoint) { this.spawnPoint.set(spawnPoint); }
    
    public long getGameTime() { return gameTime; }
    public void setGameTime(long gameTime) { this.gameTime = gameTime; }
    
    /**
     * Get a specific stat value.
     * 
     * @param statName Name of the stat
     * @return The stat value, or 0 if not found
     */
    public int getStatValue(String statName) {
        Object value = playerStats.get(statName);
        return value instanceof Integer ? (Integer) value : 0;
    }
    
    /**
     * Set a specific stat value.
     * 
     * @param statName Name of the stat
     * @param value New value for the stat
     */
    public void setStatValue(String statName, int value) {
        playerStats.put(statName, value);
    }
    
    // Faction-related methods
    
    /**
     * Get the faction manager for this player.
     * 
     * @return The FactionManager instance
     */
    public FactionManager getFactionManager() {
        return factionManager;
    }
    
    /**
     * Get reputation with a specific faction.
     * 
     * @param faction The faction to check
     * @return The reputation value
     */
    public int getReputationWith(FactionType faction) {
        return factionManager.getReputation(faction).getReputation();
    }
    
    /**
     * Get reputation level with a specific faction.
     * 
     * @param faction The faction to check
     * @return The reputation level
     */
    public ReputationLevel getReputationLevelWith(FactionType faction) {
        return factionManager.getReputationLevel(faction);
    }
    
    /**
     * Modify reputation with a specific faction.
     * 
     * @param faction The faction to modify
     * @param change The reputation change (positive or negative)
     * @param reason The reason for the change
     */
    public void modifyReputation(FactionType faction, int change, String reason) {
        factionManager.addReputation(faction, change, reason, com.odyssey.faction.FactionReputation.ReputationAction.ActionType.OTHER);
    }
    
    /**
     * Check if the player can trade with a faction.
     * 
     * @param faction The faction to check
     * @return true if trading is allowed
     */
    public boolean canTradeWith(FactionType faction) {
        return factionManager.canTradeWith(faction);
    }
    
    /**
     * Check if a faction is hostile to the player.
     * 
     * @param faction The faction to check
     * @return true if the faction is hostile
     */
    public boolean isHostileWith(FactionType faction) {
        return factionManager.isHostile(faction);
    }
    
    @Override
    public String toString() {
        return String.format("Player{id='%s', name='%s', pos=(%.1f,%.1f,%.1f), health=%.1f/%.1f, level=%d, exp=%d}", 
                           playerId, playerName, position.x, position.y, position.z, health, maxHealth, level, experience);
    }
}