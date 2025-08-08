package com.odyssey.world.save.player;

import java.util.*;

/**
 * Represents persistent player data stored in the world save.
 * Contains all player-specific information that needs to be preserved
 * across game sessions.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class PlayerData {
    // Player identity
    private UUID playerId;
    private String playerName;
    
    // Position and orientation
    private double x, y, z;
    private float yaw, pitch;
    private String dimension = "overworld";
    
    // Player state
    private String gameMode = "survival";
    private float health = 20.0f;
    private int foodLevel = 20;
    private float saturation = 5.0f;
    private int experience = 0;
    private int level = 0;
    
    // Inventory data
    private List<ItemStack> inventory = new ArrayList<>();
    private List<ItemStack> enderChest = new ArrayList<>();
    private ItemStack[] hotbar = new ItemStack[9];
    private int selectedSlot = 0;
    
    // Ship and maritime data
    private UUID currentShipId;
    private String shipRole = "captain"; // captain, crew, passenger
    private Map<String, Object> shipData = new HashMap<>();
    
    // Statistics and achievements
    private Map<String, Integer> statistics = new HashMap<>();
    private Set<String> achievements = new HashSet<>();
    private Map<String, Object> customData = new HashMap<>();
    
    // Timestamps
    private long firstPlayed;
    private long lastPlayed;
    private long lastSaved;
    private long playtime = 0; // Total playtime in milliseconds
    
    // Respawn data
    private double respawnX = 0.0;
    private double respawnY = 64.0;
    private double respawnZ = 0.0;
    private String respawnDimension = "overworld";
    private boolean hasRespawnPoint = false;
    
    // Game settings
    private Map<String, Object> gameSettings = new HashMap<>();
    
    /**
     * Creates new player data for the specified player.
     * 
     * @param playerId the player's UUID
     */
    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        initializeDefaults();
    }
    
    /**
     * Default constructor for JSON deserialization.
     */
    public PlayerData() {
        initializeDefaults();
    }
    
    /**
     * Initializes default values for collections and maps.
     */
    private void initializeDefaults() {
        if (inventory == null) inventory = new ArrayList<>();
        if (enderChest == null) enderChest = new ArrayList<>();
        if (hotbar == null) hotbar = new ItemStack[9];
        if (shipData == null) shipData = new HashMap<>();
        if (statistics == null) statistics = new HashMap<>();
        if (achievements == null) achievements = new HashSet<>();
        if (customData == null) customData = new HashMap<>();
        if (gameSettings == null) gameSettings = new HashMap<>();
    }
    
    /**
     * Initializes the player's inventory with default slots.
     */
    public void initializeInventory() {
        inventory.clear();
        enderChest.clear();
        Arrays.fill(hotbar, null);
        selectedSlot = 0;
        
        // Initialize inventory with 36 empty slots (standard Minecraft size)
        for (int i = 0; i < 36; i++) {
            inventory.add(null);
        }
        
        // Initialize ender chest with 27 empty slots
        for (int i = 0; i < 27; i++) {
            enderChest.add(null);
        }
    }
    
    // Getters and setters for player identity
    public UUID getPlayerId() { return playerId; }
    public void setPlayerId(UUID playerId) { this.playerId = playerId; }
    
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    // Position and orientation
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    
    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    
    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    
    // Player state
    public String getGameMode() { return gameMode; }
    public void setGameMode(String gameMode) { this.gameMode = gameMode; }
    
    public float getHealth() { return health; }
    public void setHealth(float health) { this.health = Math.max(0, Math.min(20, health)); }
    
    public int getFoodLevel() { return foodLevel; }
    public void setFoodLevel(int foodLevel) { this.foodLevel = Math.max(0, Math.min(20, foodLevel)); }
    
    public float getSaturation() { return saturation; }
    public void setSaturation(float saturation) { this.saturation = Math.max(0, Math.min(20, saturation)); }
    
    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = Math.max(0, experience); }
    
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(0, level); }
    
    // Inventory
    public List<ItemStack> getInventory() { return inventory; }
    public void setInventory(List<ItemStack> inventory) { this.inventory = inventory; }
    
    public List<ItemStack> getEnderChest() { return enderChest; }
    public void setEnderChest(List<ItemStack> enderChest) { this.enderChest = enderChest; }
    
    public ItemStack[] getHotbar() { return hotbar; }
    public void setHotbar(ItemStack[] hotbar) { this.hotbar = hotbar; }
    
    public int getSelectedSlot() { return selectedSlot; }
    public void setSelectedSlot(int selectedSlot) { 
        this.selectedSlot = Math.max(0, Math.min(8, selectedSlot)); 
    }
    
    // Ship and maritime data
    public UUID getCurrentShipId() { return currentShipId; }
    public void setCurrentShipId(UUID currentShipId) { this.currentShipId = currentShipId; }
    
    public String getShipRole() { return shipRole; }
    public void setShipRole(String shipRole) { this.shipRole = shipRole; }
    
    public Map<String, Object> getShipData() { return shipData; }
    public void setShipData(Map<String, Object> shipData) { this.shipData = shipData; }
    
    // Statistics and achievements
    public Map<String, Integer> getStatistics() { return statistics; }
    public void setStatistics(Map<String, Integer> statistics) { this.statistics = statistics; }
    
    public void incrementStatistic(String key, int amount) {
        statistics.put(key, statistics.getOrDefault(key, 0) + amount);
    }
    
    public int getStatistic(String key) {
        return statistics.getOrDefault(key, 0);
    }
    
    public Set<String> getAchievements() { return achievements; }
    public void setAchievements(Set<String> achievements) { this.achievements = achievements; }
    
    public void unlockAchievement(String achievement) {
        achievements.add(achievement);
    }
    
    public boolean hasAchievement(String achievement) {
        return achievements.contains(achievement);
    }
    
    public Map<String, Object> getCustomData() { return customData; }
    public void setCustomData(Map<String, Object> customData) { this.customData = customData; }
    
    // Timestamps
    public long getFirstPlayed() { return firstPlayed; }
    public void setFirstPlayed(long firstPlayed) { this.firstPlayed = firstPlayed; }
    
    public long getLastPlayed() { return lastPlayed; }
    public void setLastPlayed(long lastPlayed) { this.lastPlayed = lastPlayed; }
    
    public long getLastSaved() { return lastSaved; }
    public void setLastSaved(long lastSaved) { this.lastSaved = lastSaved; }
    
    public long getPlaytime() { return playtime; }
    public void setPlaytime(long playtime) { this.playtime = playtime; }
    
    public void addPlaytime(long milliseconds) {
        this.playtime += milliseconds;
    }
    
    // Respawn data
    public double getRespawnX() { return respawnX; }
    public double getRespawnY() { return respawnY; }
    public double getRespawnZ() { return respawnZ; }
    
    public void setRespawnPoint(double x, double y, double z, String dimension) {
        this.respawnX = x;
        this.respawnY = y;
        this.respawnZ = z;
        this.respawnDimension = dimension;
        this.hasRespawnPoint = true;
    }
    
    public String getRespawnDimension() { return respawnDimension; }
    public void setRespawnDimension(String respawnDimension) { this.respawnDimension = respawnDimension; }
    
    public boolean hasRespawnPoint() { return hasRespawnPoint; }
    public void setHasRespawnPoint(boolean hasRespawnPoint) { this.hasRespawnPoint = hasRespawnPoint; }
    
    public void clearRespawnPoint() {
        this.hasRespawnPoint = false;
    }
    
    // Game settings
    public Map<String, Object> getGameSettings() { return gameSettings; }
    public void setGameSettings(Map<String, Object> gameSettings) { this.gameSettings = gameSettings; }
    
    public void setSetting(String key, Object value) {
        gameSettings.put(key, value);
    }
    
    public Object getSetting(String key) {
        return gameSettings.get(key);
    }
    
    public Object getSetting(String key, Object defaultValue) {
        return gameSettings.getOrDefault(key, defaultValue);
    }
    
    /**
     * Updates the last played timestamp to current time.
     */
    public void updateLastPlayed() {
        this.lastPlayed = System.currentTimeMillis();
    }
    
    /**
     * Validates the player data for consistency.
     * 
     * @return true if the data is valid
     */
    public boolean validate() {
        if (playerId == null) return false;
        if (health < 0 || health > 20) return false;
        if (foodLevel < 0 || foodLevel > 20) return false;
        if (saturation < 0 || saturation > 20) return false;
        if (experience < 0 || level < 0) return false;
        if (selectedSlot < 0 || selectedSlot > 8) return false;
        
        // Ensure collections are initialized
        initializeDefaults();
        
        return true;
    }
    
    @Override
    public String toString() {
        return String.format("PlayerData{id=%s, name='%s', pos=(%.1f,%.1f,%.1f), health=%.1f}", 
            playerId, playerName, x, y, z, health);
    }
    
    /**
     * Simple item stack representation for inventory storage.
     */
    public static class ItemStack {
        private String itemType;
        private int count;
        private Map<String, Object> metadata;
        
        public ItemStack() {}
        
        public ItemStack(String itemType, int count) {
            this.itemType = itemType;
            this.count = count;
            this.metadata = new HashMap<>();
        }
        
        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        
        public int getCount() { return count; }
        public void setCount(int count) { this.count = Math.max(0, count); }
        
        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
        
        public boolean isEmpty() {
            return itemType == null || count <= 0;
        }
        
        @Override
        public String toString() {
            return String.format("ItemStack{type='%s', count=%d}", itemType, count);
        }
    }
}