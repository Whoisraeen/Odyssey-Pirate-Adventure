package com.odyssey.world.save.metadata;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains world-level metadata and settings for the save file.
 * This includes spawn point, game rules, weather state, and other
 * global world properties.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class LevelMetadata {
    // World identity
    private String worldName;
    private long seed;
    private String generatorName = "odyssey_default";
    private String generatorVersion = "1.0.0";
    private Map<String, Object> generatorOptions = new HashMap<>();
    
    // World spawn
    private double spawnX = 0.0;
    private double spawnY = 64.0;
    private double spawnZ = 0.0;
    private float spawnYaw = 0.0f;
    private float spawnPitch = 0.0f;
    
    // Time and weather
    private long worldTime = 0; // Ticks since world creation
    private long dayTime = 0; // Time of day (0-24000)
    private boolean raining = false;
    private int rainTime = 0;
    private boolean thundering = false;
    private int thunderTime = 0;
    private float rainStrength = 0.0f;
    private float thunderStrength = 0.0f;
    
    // Maritime-specific weather
    private String windDirection = "north";
    private float windStrength = 0.5f;
    private float waveHeight = 1.0f;
    private String tidalPhase = "low"; // low, rising, high, falling
    private long tidalTime = 0; // Time within current tidal cycle
    
    // Game rules
    private Map<String, Object> gameRules = new HashMap<>();
    
    // Difficulty and game mode
    private String difficulty = "normal"; // peaceful, easy, normal, hard
    private boolean hardcore = false;
    private String defaultGameMode = "survival";
    
    // World features
    private boolean generateStructures = true;
    private boolean generateOceanMonuments = true;
    private boolean generateShipwrecks = true;
    private boolean generateTreasureIslands = true;
    
    // Version information
    private String gameVersion = "1.0.0";
    private int dataVersion = 1;
    private long lastPlayed = System.currentTimeMillis();
    private long sizeOnDisk = 0;
    
    // Player tracking
    private int playerCount = 0;
    private String lastPlayerName = "";
    
    // World border
    private double borderCenterX = 0.0;
    private double borderCenterZ = 0.0;
    private double borderSize = 60000000.0; // 60 million blocks (Minecraft default)
    private double borderSafeZone = 5.0;
    private double borderDamagePerBlock = 0.2;
    private int borderWarningBlocks = 5;
    private int borderWarningTime = 15;
    
    // Custom data for mods/plugins
    private Map<String, Object> customData = new HashMap<>();
    
    /**
     * Creates new level metadata with default values.
     */
    public LevelMetadata() {
        initializeDefaults();
    }
    
    /**
     * Creates level metadata for a new world.
     * 
     * @param worldName the world name
     * @param seed the world seed
     */
    public LevelMetadata(String worldName, long seed) {
        this.worldName = worldName;
        this.seed = seed;
        initializeDefaults();
    }
    
    /**
     * Initializes default game rules and settings.
     */
    private void initializeDefaults() {
        if (gameRules == null) gameRules = new HashMap<>();
        if (generatorOptions == null) generatorOptions = new HashMap<>();
        if (customData == null) customData = new HashMap<>();
        
        // Set default game rules
        gameRules.putIfAbsent("doDaylightCycle", true);
        gameRules.putIfAbsent("doWeatherCycle", true);
        gameRules.putIfAbsent("doMobSpawning", true);
        gameRules.putIfAbsent("keepInventory", false);
        gameRules.putIfAbsent("mobGriefing", true);
        gameRules.putIfAbsent("doFireTick", true);
        gameRules.putIfAbsent("doTileDrops", true);
        gameRules.putIfAbsent("doEntityDrops", true);
        gameRules.putIfAbsent("commandBlockOutput", true);
        gameRules.putIfAbsent("naturalRegeneration", true);
        gameRules.putIfAbsent("showDeathMessages", true);
        gameRules.putIfAbsent("sendCommandFeedback", true);
        gameRules.putIfAbsent("reducedDebugInfo", false);
        
        // Maritime-specific game rules
        gameRules.putIfAbsent("doTidalCycle", true);
        gameRules.putIfAbsent("doOceanCurrents", true);
        gameRules.putIfAbsent("doShipPhysics", true);
        gameRules.putIfAbsent("allowShipCrafting", true);
        gameRules.putIfAbsent("treasureRespawnTime", 72000); // 1 hour in ticks
    }
    
    // World identity getters/setters
    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) { this.worldName = worldName; }
    
    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }
    
    public String getGeneratorName() { return generatorName; }
    public void setGeneratorName(String generatorName) { this.generatorName = generatorName; }
    
    public String getGeneratorVersion() { return generatorVersion; }
    public void setGeneratorVersion(String generatorVersion) { this.generatorVersion = generatorVersion; }
    
    public Map<String, Object> getGeneratorOptions() { return generatorOptions; }
    public void setGeneratorOptions(Map<String, Object> generatorOptions) { this.generatorOptions = generatorOptions; }
    
    // Spawn point getters/setters
    public double getSpawnX() { return spawnX; }
    public double getSpawnY() { return spawnY; }
    public double getSpawnZ() { return spawnZ; }
    
    public void setSpawnPoint(double x, double y, double z) {
        this.spawnX = x;
        this.spawnY = y;
        this.spawnZ = z;
    }
    
    public float getSpawnYaw() { return spawnYaw; }
    public float getSpawnPitch() { return spawnPitch; }
    
    public void setSpawnRotation(float yaw, float pitch) {
        this.spawnYaw = yaw;
        this.spawnPitch = pitch;
    }
    
    // Time and weather getters/setters
    public long getWorldTime() { return worldTime; }
    public void setWorldTime(long worldTime) { this.worldTime = worldTime; }
    
    public long getDayTime() { return dayTime; }
    public void setDayTime(long dayTime) { this.dayTime = dayTime % 24000; }
    
    public boolean isRaining() { return raining; }
    public void setRaining(boolean raining) { this.raining = raining; }
    
    public int getRainTime() { return rainTime; }
    public void setRainTime(int rainTime) { this.rainTime = rainTime; }
    
    public boolean isThundering() { return thundering; }
    public void setThundering(boolean thundering) { this.thundering = thundering; }
    
    public int getThunderTime() { return thunderTime; }
    public void setThunderTime(int thunderTime) { this.thunderTime = thunderTime; }
    
    public float getRainStrength() { return rainStrength; }
    public void setRainStrength(float rainStrength) { this.rainStrength = Math.max(0, Math.min(1, rainStrength)); }
    
    public float getThunderStrength() { return thunderStrength; }
    public void setThunderStrength(float thunderStrength) { this.thunderStrength = Math.max(0, Math.min(1, thunderStrength)); }
    
    // Maritime weather getters/setters
    public String getWindDirection() { return windDirection; }
    public void setWindDirection(String windDirection) { this.windDirection = windDirection; }
    
    public float getWindStrength() { return windStrength; }
    public void setWindStrength(float windStrength) { this.windStrength = Math.max(0, Math.min(2, windStrength)); }
    
    public float getWaveHeight() { return waveHeight; }
    public void setWaveHeight(float waveHeight) { this.waveHeight = Math.max(0, Math.min(10, waveHeight)); }
    
    public String getTidalPhase() { return tidalPhase; }
    public void setTidalPhase(String tidalPhase) { this.tidalPhase = tidalPhase; }
    
    public long getTidalTime() { return tidalTime; }
    public void setTidalTime(long tidalTime) { this.tidalTime = tidalTime; }
    
    // Game rules
    public Map<String, Object> getGameRules() { return gameRules; }
    public void setGameRules(Map<String, Object> gameRules) { this.gameRules = gameRules; }
    
    public void setGameRule(String rule, Object value) {
        gameRules.put(rule, value);
    }
    
    public Object getGameRule(String rule) {
        return gameRules.get(rule);
    }
    
    public Object getGameRule(String rule, Object defaultValue) {
        return gameRules.getOrDefault(rule, defaultValue);
    }
    
    // Difficulty and game mode
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    
    public boolean isHardcore() { return hardcore; }
    public void setHardcore(boolean hardcore) { this.hardcore = hardcore; }
    
    public String getDefaultGameMode() { return defaultGameMode; }
    public void setDefaultGameMode(String defaultGameMode) { this.defaultGameMode = defaultGameMode; }
    
    // World features
    public boolean isGenerateStructures() { return generateStructures; }
    public void setGenerateStructures(boolean generateStructures) { this.generateStructures = generateStructures; }
    
    public boolean isGenerateOceanMonuments() { return generateOceanMonuments; }
    public void setGenerateOceanMonuments(boolean generateOceanMonuments) { this.generateOceanMonuments = generateOceanMonuments; }
    
    public boolean isGenerateShipwrecks() { return generateShipwrecks; }
    public void setGenerateShipwrecks(boolean generateShipwrecks) { this.generateShipwrecks = generateShipwrecks; }
    
    public boolean isGenerateTreasureIslands() { return generateTreasureIslands; }
    public void setGenerateTreasureIslands(boolean generateTreasureIslands) { this.generateTreasureIslands = generateTreasureIslands; }
    
    // Version information
    public String getGameVersion() { return gameVersion; }
    public void setGameVersion(String gameVersion) { this.gameVersion = gameVersion; }
    
    public int getDataVersion() { return dataVersion; }
    public void setDataVersion(int dataVersion) { this.dataVersion = dataVersion; }
    
    public long getLastPlayed() { return lastPlayed; }
    public void setLastPlayed(long lastPlayed) { this.lastPlayed = lastPlayed; }
    
    public long getSizeOnDisk() { return sizeOnDisk; }
    public void setSizeOnDisk(long sizeOnDisk) { this.sizeOnDisk = sizeOnDisk; }
    
    // Player tracking
    public int getPlayerCount() { return playerCount; }
    public void setPlayerCount(int playerCount) { this.playerCount = playerCount; }
    
    public String getLastPlayerName() { return lastPlayerName; }
    public void setLastPlayerName(String lastPlayerName) { this.lastPlayerName = lastPlayerName; }
    
    // World border
    public double getBorderCenterX() { return borderCenterX; }
    public double getBorderCenterZ() { return borderCenterZ; }
    
    public void setBorderCenter(double x, double z) {
        this.borderCenterX = x;
        this.borderCenterZ = z;
    }
    
    public double getBorderSize() { return borderSize; }
    public void setBorderSize(double borderSize) { this.borderSize = Math.max(1, borderSize); }
    
    public double getBorderSafeZone() { return borderSafeZone; }
    public void setBorderSafeZone(double borderSafeZone) { this.borderSafeZone = borderSafeZone; }
    
    public double getBorderDamagePerBlock() { return borderDamagePerBlock; }
    public void setBorderDamagePerBlock(double borderDamagePerBlock) { this.borderDamagePerBlock = borderDamagePerBlock; }
    
    public int getBorderWarningBlocks() { return borderWarningBlocks; }
    public void setBorderWarningBlocks(int borderWarningBlocks) { this.borderWarningBlocks = borderWarningBlocks; }
    
    public int getBorderWarningTime() { return borderWarningTime; }
    public void setBorderWarningTime(int borderWarningTime) { this.borderWarningTime = borderWarningTime; }
    
    // Custom data
    public Map<String, Object> getCustomData() { return customData; }
    public void setCustomData(Map<String, Object> customData) { this.customData = customData; }
    
    public void setCustomValue(String key, Object value) {
        customData.put(key, value);
    }
    
    public Object getCustomValue(String key) {
        return customData.get(key);
    }
    
    public Object getCustomValue(String key, Object defaultValue) {
        return customData.getOrDefault(key, defaultValue);
    }
    
    /**
     * Updates the last played timestamp to current time.
     */
    public void updateLastPlayed() {
        this.lastPlayed = System.currentTimeMillis();
    }
    
    /**
     * Advances world time by the specified number of ticks.
     * 
     * @param ticks the number of ticks to advance
     */
    public void advanceTime(long ticks) {
        this.worldTime += ticks;
        this.dayTime = (this.dayTime + ticks) % 24000;
    }
    
    /**
     * Validates the metadata for consistency.
     * 
     * @return true if the metadata is valid
     */
    public boolean validate() {
        if (worldName == null || worldName.trim().isEmpty()) return false;
        if (dayTime < 0 || dayTime >= 24000) return false;
        if (rainStrength < 0 || rainStrength > 1) return false;
        if (thunderStrength < 0 || thunderStrength > 1) return false;
        if (windStrength < 0 || windStrength > 2) return false;
        if (waveHeight < 0 || waveHeight > 10) return false;
        if (borderSize < 1) return false;
        
        // Ensure collections are initialized
        initializeDefaults();
        
        return true;
    }
    
    @Override
    public String toString() {
        return String.format("LevelMetadata{name='%s', seed=%d, time=%d, difficulty='%s'}", 
            worldName, seed, worldTime, difficulty);
    }
}