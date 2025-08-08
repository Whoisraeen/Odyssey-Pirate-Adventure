package com.odyssey.world.save.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents serializable entity data for persistent storage.
 * Contains all necessary information to recreate an entity when loading a world.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class EntityData {
    private UUID uuid;
    private String entityType;
    private double x, y, z;
    private float yaw, pitch;
    private double velocityX, velocityY, velocityZ;
    private int health;
    private int maxHealth;
    private boolean onGround;
    private long lastUpdate;
    private Map<String, Object> customData;
    
    // Transient fields for internal management
    private transient String lastRegionKey;
    private transient int chunkX;
    private transient int chunkZ;
    
    /**
     * Default constructor for JSON deserialization.
     */
    public EntityData() {
        this.customData = new HashMap<>();
        this.lastUpdate = System.currentTimeMillis();
    }
    
    /**
     * Creates entity data with basic information.
     * 
     * @param uuid the entity UUID
     * @param entityType the entity type
     * @param x the X position
     * @param y the Y position
     * @param z the Z position
     */
    public EntityData(UUID uuid, String entityType, double x, double y, double z) {
        this();
        this.uuid = uuid;
        this.entityType = entityType;
        this.x = x;
        this.y = y;
        this.z = z;
        updateChunkCoordinates();
    }
    
    /**
     * Updates the chunk coordinates based on current position.
     */
    public void updateChunkCoordinates() {
        this.chunkX = (int) Math.floor(x / 16.0);
        this.chunkZ = (int) Math.floor(z / 16.0);
    }
    
    // Getters and setters
    
    public UUID getUuid() {
        return uuid;
    }
    
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
    
    public double getX() {
        return x;
    }
    
    public void setX(double x) {
        this.x = x;
        updateChunkCoordinates();
    }
    
    public double getY() {
        return y;
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    public double getZ() {
        return z;
    }
    
    public void setZ(double z) {
        this.z = z;
        updateChunkCoordinates();
    }
    
    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        updateChunkCoordinates();
    }
    
    public float getYaw() {
        return yaw;
    }
    
    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
    
    public float getPitch() {
        return pitch;
    }
    
    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
    
    public void setRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }
    
    public double getVelocityX() {
        return velocityX;
    }
    
    public void setVelocityX(double velocityX) {
        this.velocityX = velocityX;
    }
    
    public double getVelocityY() {
        return velocityY;
    }
    
    public void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }
    
    public double getVelocityZ() {
        return velocityZ;
    }
    
    public void setVelocityZ(double velocityZ) {
        this.velocityZ = velocityZ;
    }
    
    public void setVelocity(double velocityX, double velocityY, double velocityZ) {
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
    }
    
    public int getHealth() {
        return health;
    }
    
    public void setHealth(int health) {
        this.health = health;
    }
    
    public int getMaxHealth() {
        return maxHealth;
    }
    
    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }
    
    public boolean isOnGround() {
        return onGround;
    }
    
    public void setOnGround(boolean onGround) {
        this.onGround = onGround;
    }
    
    public long getLastUpdate() {
        return lastUpdate;
    }
    
    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
    
    public void updateTimestamp() {
        this.lastUpdate = System.currentTimeMillis();
    }
    
    public Map<String, Object> getCustomData() {
        return customData;
    }
    
    public void setCustomData(Map<String, Object> customData) {
        this.customData = customData != null ? customData : new HashMap<>();
    }
    
    public void setCustomData(String key, Object value) {
        this.customData.put(key, value);
    }
    
    public Object getCustomData(String key) {
        return this.customData.get(key);
    }
    
    public <T> T getCustomData(String key, Class<T> type) {
        Object value = this.customData.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
    
    // Transient getters and setters
    
    public String getLastRegionKey() {
        return lastRegionKey;
    }
    
    public void setLastRegionKey(String lastRegionKey) {
        this.lastRegionKey = lastRegionKey;
    }
    
    public int getChunkX() {
        if (chunkX == 0 && x != 0) {
            updateChunkCoordinates();
        }
        return chunkX;
    }
    
    public int getChunkZ() {
        if (chunkZ == 0 && z != 0) {
            updateChunkCoordinates();
        }
        return chunkZ;
    }
    
    /**
     * Checks if this entity is alive (health > 0).
     * 
     * @return true if the entity is alive
     */
    public boolean isAlive() {
        return health > 0;
    }
    
    /**
     * Checks if this entity data is stale (hasn't been updated recently).
     * 
     * @param maxAgeMs the maximum age in milliseconds
     * @return true if the entity data is stale
     */
    public boolean isStale(long maxAgeMs) {
        return System.currentTimeMillis() - lastUpdate > maxAgeMs;
    }
    
    /**
     * Creates a copy of this entity data.
     * 
     * @return a copy of this entity data
     */
    public EntityData copy() {
        EntityData copy = new EntityData(uuid, entityType, x, y, z);
        copy.yaw = this.yaw;
        copy.pitch = this.pitch;
        copy.velocityX = this.velocityX;
        copy.velocityY = this.velocityY;
        copy.velocityZ = this.velocityZ;
        copy.health = this.health;
        copy.maxHealth = this.maxHealth;
        copy.onGround = this.onGround;
        copy.lastUpdate = this.lastUpdate;
        copy.customData = new HashMap<>(this.customData);
        copy.lastRegionKey = this.lastRegionKey;
        return copy;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        EntityData that = (EntityData) obj;
        return uuid != null ? uuid.equals(that.uuid) : that.uuid == null;
    }
    
    @Override
    public int hashCode() {
        return uuid != null ? uuid.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return String.format("EntityData{uuid=%s, type=%s, pos=(%.2f,%.2f,%.2f), health=%d/%d}", 
            uuid, entityType, x, y, z, health, maxHealth);
    }
}