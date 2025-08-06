package com.odyssey.world.physics;

import org.joml.Vector3f;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.Set;

/**
 * Represents a spatial region for physics simulation optimization.
 * 
 * <p>Physics regions divide the world into manageable chunks for efficient
 * physics processing. Each region tracks active physics blocks and manages
 * their simulation state.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Spatial partitioning for efficient collision detection</li>
 *   <li>Automatic activation/deactivation based on activity</li>
 *   <li>Memory-efficient storage of physics data</li>
 *   <li>Thread-safe operations for concurrent access</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class PhysicsRegion {
    private static final Logger logger = LoggerFactory.getLogger(PhysicsRegion.class);
    
    /** Minimum activity threshold to keep region active. */
    private static final int MIN_ACTIVITY_THRESHOLD = 5;
    
    /** Maximum inactive time before region cleanup (milliseconds). */
    private static final long MAX_INACTIVE_TIME = 30000; // 30 seconds
    
    private final int regionX;
    private final int regionZ;
    private final int size;
    private final Vector3f center;
    
    // Region state
    private volatile boolean active = true;
    private final AtomicLong lastActivityTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong activityCounter = new AtomicLong(0);
    
    // Physics data
    private final Set<Vector3f> activeBlocks = ConcurrentHashMap.newKeySet();
    private final Map<Vector3i, PhysicsBlock> physicsBlocks = new ConcurrentHashMap<>();
    private final Map<String, Object> regionProperties = new ConcurrentHashMap<>();
    
    // Performance tracking
    private final AtomicLong updateCount = new AtomicLong(0);
    private final AtomicLong lastUpdateTime = new AtomicLong(0);
    
    /**
     * Creates a new physics region.
     * 
     * @param regionX the region X coordinate
     * @param regionZ the region Z coordinate
     * @param size the region size in blocks
     * @param center the center position of the region
     */
    public PhysicsRegion(int regionX, int regionZ, int size, Vector3f center) {
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.size = size;
        this.center = new Vector3f(center);
        
        // Initialize region properties
        regionProperties.put("creation_time", System.currentTimeMillis());
        regionProperties.put("fluid_level", 0.0f);
        regionProperties.put("temperature", 20.0f);
        regionProperties.put("pressure", 1.0f);
    }
    
    /**
     * Updates the physics region state.
     */
    public void update() {
        long currentTime = System.currentTimeMillis();
        updateCount.incrementAndGet();
        lastUpdateTime.set(currentTime);
        
        // Check if region should remain active
        if (active && activityCounter.get() < MIN_ACTIVITY_THRESHOLD) {
            long inactiveTime = currentTime - lastActivityTime.get();
            if (inactiveTime > MAX_INACTIVE_TIME) {
                deactivate();
            }
        }
        
        // Reset activity counter for next evaluation
        activityCounter.set(0);
    }
    
    /**
     * Adds a physics block to this region.
     * 
     * @param blockPosition the block position
     */
    public void addPhysicsBlock(Vector3f blockPosition) {
        if (containsPosition(blockPosition)) {
            activeBlocks.add(new Vector3f(blockPosition));
            recordActivity();
        }
    }
    
    /**
     * Removes a physics block from this region.
     * 
     * @param blockPosition the block position
     */
    public void removePhysicsBlock(Vector3f blockPosition) {
        activeBlocks.remove(blockPosition);
        recordActivity();
    }
    
    /**
     * Checks if this region contains the specified position.
     * 
     * @param position the position to check
     * @return true if the position is within this region
     */
    public boolean containsPosition(Vector3f position) {
        float halfSize = size / 2.0f;
        return position.x >= center.x - halfSize && position.x < center.x + halfSize &&
               position.z >= center.z - halfSize && position.z < center.z + halfSize;
    }
    
    /**
     * Records activity in this region.
     */
    public void recordActivity() {
        activityCounter.incrementAndGet();
        lastActivityTime.set(System.currentTimeMillis());
        
        if (!active) {
            activate();
        }
    }
    
    /**
     * Activates this physics region.
     */
    private void activate() {
        active = true;
        logger.debug("Activated physics region at ({}, {})", regionX, regionZ);
    }
    
    /**
     * Deactivates this physics region.
     */
    private void deactivate() {
        active = false;
        
        // Clear physics data to free memory
        activeBlocks.clear();
        
        logger.debug("Deactivated physics region at ({}, {}) due to inactivity", regionX, regionZ);
    }
    
    /**
     * Gets a region property.
     * 
     * @param key the property key
     * @return the property value, or null if not found
     */
    public Object getProperty(String key) {
        return regionProperties.get(key);
    }
    
    /**
     * Sets a region property.
     * 
     * @param key the property key
     * @param value the property value
     */
    public void setProperty(String key, Object value) {
        regionProperties.put(key, value);
        recordActivity();
    }
    
    /**
     * Gets the fluid level in this region.
     * 
     * @return the fluid level (0.0 to 1.0)
     */
    public float getFluidLevel() {
        Object level = regionProperties.get("fluid_level");
        return level instanceof Float ? (Float) level : 0.0f;
    }
    
    /**
     * Sets the fluid level in this region.
     * 
     * @param level the fluid level (0.0 to 1.0)
     */
    public void setFluidLevel(float level) {
        setProperty("fluid_level", Math.max(0.0f, Math.min(1.0f, level)));
    }
    
    /**
     * Gets the temperature in this region.
     * 
     * @return the temperature in degrees Celsius
     */
    public float getTemperature() {
        Object temp = regionProperties.get("temperature");
        return temp instanceof Float ? (Float) temp : 20.0f;
    }
    
    /**
     * Sets the temperature in this region.
     * 
     * @param temperature the temperature in degrees Celsius
     */
    public void setTemperature(float temperature) {
        setProperty("temperature", temperature);
    }
    
    /**
     * Gets the structural integrity of this region.
     * 
     * @return the structural integrity (0.0 to 1.0)
     */
    public StructuralIntegrity getStructuralIntegrity() {
        // Return a default structural integrity instance for this region
        // This could be enhanced to track actual structural data
        return new StructuralIntegrity(this);
    }
    
    /**
     * Gets a physics block at the specified position.
     * 
     * @param position the block position
     * @return the physics block, or null if not found
     */
    public PhysicsBlock getBlock(Vector3i position) {
        return physicsBlocks.get(position);
    }
    
    /**
     * Sets a physics block at the specified position.
     * 
     * @param position the block position
     * @param block the physics block
     */
    public void setBlock(Vector3i position, PhysicsBlock block) {
        if (block != null) {
            physicsBlocks.put(new Vector3i(position), block);
            addPhysicsBlock(new Vector3f(position.x, position.y, position.z));
        } else {
            physicsBlocks.remove(position);
            removePhysicsBlock(new Vector3f(position.x, position.y, position.z));
        }
    }
    
    /**
     * Removes a physics block at the specified position.
     * 
     * @param position the block position
     * @return the removed physics block, or null if not found
     */
    public PhysicsBlock removeBlock(Vector3i position) {
        PhysicsBlock removed = physicsBlocks.remove(position);
        if (removed != null) {
            removePhysicsBlock(new Vector3f(position.x, position.y, position.z));
        }
        return removed;
    }
    
    // Getters
    public int getRegionX() { return regionX; }
    public int getRegionZ() { return regionZ; }
    public int getSize() { return size; }
    public Vector3f getCenter() { return new Vector3f(center); }
    public boolean isActive() { return active; }
    public int getActiveBlockCount() { return activeBlocks.size(); }
    public long getActivityCount() { return activityCounter.get(); }
    public long getUpdateCount() { return updateCount.get(); }
    public long getLastUpdateTime() { return lastUpdateTime.get(); }
    public Set<Vector3f> getActiveBlocks() { return Set.copyOf(activeBlocks); }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PhysicsRegion that = (PhysicsRegion) obj;
        return regionX == that.regionX && regionZ == that.regionZ;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(regionX, regionZ);
    }
    
    @Override
    public String toString() {
        return String.format("PhysicsRegion{x=%d, z=%d, size=%d, active=%s, blocks=%d}", 
                           regionX, regionZ, size, active, activeBlocks.size());
    }
}