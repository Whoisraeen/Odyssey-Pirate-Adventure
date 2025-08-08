package com.odyssey.world.save.poi;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents serializable POI (Point of Interest) data for persistent storage.
 * Contains all necessary information to recreate a POI when loading a world.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class PoiData {
    private UUID id;
    private PoiType type;
    private String name;
    private double x, y, z;
    private int chunkX, chunkZ;
    private boolean active;
    private long createdTime;
    private long lastAccessTime;
    private UUID ownerId; // For player-created POIs
    private Map<String, Object> properties;
    
    // Transient fields for internal management
    private transient String lastRegionKey;
    
    /**
     * Default constructor for JSON deserialization.
     */
    public PoiData() {
        this.properties = new HashMap<>();
        this.active = true;
        this.createdTime = System.currentTimeMillis();
        this.lastAccessTime = this.createdTime;
    }
    
    /**
     * Creates POI data with basic information.
     * 
     * @param type the POI type
     * @param name the POI name
     * @param x the X position
     * @param y the Y position
     * @param z the Z position
     */
    public PoiData(PoiType type, String name, double x, double y, double z) {
        this();
        this.id = UUID.randomUUID();
        this.type = type;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        updateChunkCoordinates();
    }
    
    /**
     * Creates POI data with an owner.
     * 
     * @param type the POI type
     * @param name the POI name
     * @param x the X position
     * @param y the Y position
     * @param z the Z position
     * @param ownerId the owner UUID
     */
    public PoiData(PoiType type, String name, double x, double y, double z, UUID ownerId) {
        this(type, name, x, y, z);
        this.ownerId = ownerId;
    }
    
    /**
     * Updates the chunk coordinates based on current position.
     */
    public void updateChunkCoordinates() {
        this.chunkX = (int) Math.floor(x / 16.0);
        this.chunkZ = (int) Math.floor(z / 16.0);
    }
    
    /**
     * Updates the last access time to current time.
     */
    public void updateAccessTime() {
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    // Getters and setters
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public PoiType getType() {
        return type;
    }
    
    public void setType(PoiType type) {
        this.type = type;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
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
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public long getCreatedTime() {
        return createdTime;
    }
    
    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }
    
    public long getLastAccessTime() {
        return lastAccessTime;
    }
    
    public void setLastAccessTime(long lastAccessTime) {
        this.lastAccessTime = lastAccessTime;
    }
    
    public UUID getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }
    
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties != null ? properties : new HashMap<>();
    }
    
    public void setProperty(String key, Object value) {
        this.properties.put(key, value);
    }
    
    public Object getProperty(String key) {
        return this.properties.get(key);
    }
    
    public <T> T getProperty(String key, Class<T> type) {
        Object value = this.properties.get(key);
        if (value != null && type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }
    
    public <T> T getProperty(String key, Class<T> type, T defaultValue) {
        T value = getProperty(key, type);
        return value != null ? value : defaultValue;
    }
    
    public boolean hasProperty(String key) {
        return this.properties.containsKey(key);
    }
    
    public void removeProperty(String key) {
        this.properties.remove(key);
    }
    
    // Transient getters and setters
    
    public String getLastRegionKey() {
        return lastRegionKey;
    }
    
    public void setLastRegionKey(String lastRegionKey) {
        this.lastRegionKey = lastRegionKey;
    }
    
    /**
     * Calculates the distance to another position.
     * 
     * @param otherX the other X coordinate
     * @param otherZ the other Z coordinate
     * @return the distance
     */
    public double distanceTo(double otherX, double otherZ) {
        return Math.sqrt(Math.pow(x - otherX, 2) + Math.pow(z - otherZ, 2));
    }
    
    /**
     * Calculates the 3D distance to another position.
     * 
     * @param otherX the other X coordinate
     * @param otherY the other Y coordinate
     * @param otherZ the other Z coordinate
     * @return the 3D distance
     */
    public double distanceTo3D(double otherX, double otherY, double otherZ) {
        return Math.sqrt(Math.pow(x - otherX, 2) + Math.pow(y - otherY, 2) + Math.pow(z - otherZ, 2));
    }
    
    /**
     * Checks if this POI is within detection range of a position.
     * 
     * @param posX the position X coordinate
     * @param posZ the position Z coordinate
     * @return true if within detection range
     */
    public boolean isWithinDetectionRange(double posX, double posZ) {
        return distanceTo(posX, posZ) <= type.getDetectionRange();
    }
    
    /**
     * Checks if this POI is owned by a specific player.
     * 
     * @param playerId the player UUID
     * @return true if owned by the player
     */
    public boolean isOwnedBy(UUID playerId) {
        return ownerId != null && ownerId.equals(playerId);
    }
    
    /**
     * Checks if this POI has been accessed recently.
     * 
     * @param maxAgeMs the maximum age in milliseconds
     * @return true if accessed recently
     */
    public boolean isRecentlyAccessed(long maxAgeMs) {
        return System.currentTimeMillis() - lastAccessTime <= maxAgeMs;
    }
    
    /**
     * Gets the age of this POI in milliseconds.
     * 
     * @return the age in milliseconds
     */
    public long getAge() {
        return System.currentTimeMillis() - createdTime;
    }
    
    /**
     * Gets the time since last access in milliseconds.
     * 
     * @return the time since last access in milliseconds
     */
    public long getTimeSinceLastAccess() {
        return System.currentTimeMillis() - lastAccessTime;
    }
    
    /**
     * Creates a copy of this POI data.
     * 
     * @return a copy of this POI data
     */
    public PoiData copy() {
        PoiData copy = new PoiData(type, name, x, y, z, ownerId);
        copy.id = this.id;
        copy.active = this.active;
        copy.createdTime = this.createdTime;
        copy.lastAccessTime = this.lastAccessTime;
        copy.properties = new HashMap<>(this.properties);
        copy.lastRegionKey = this.lastRegionKey;
        return copy;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        PoiData poiData = (PoiData) obj;
        return id != null ? id.equals(poiData.id) : poiData.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return String.format("PoiData{id=%s, type=%s, name='%s', pos=(%.2f,%.2f,%.2f), active=%s}", 
            id, type, name, x, y, z, active);
    }
}