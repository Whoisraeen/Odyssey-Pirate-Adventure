package com.odyssey.world.physics;

import com.odyssey.core.GameConfig;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * System for managing structural integrity across multiple physics regions.
 * Handles global structural calculations and collapse propagation.
 */
public class StructuralIntegritySystem {
    private static final Logger logger = LoggerFactory.getLogger(StructuralIntegritySystem.class);
    
    private final GameConfig config;
    private final Set<PhysicsRegion> activeRegions = ConcurrentHashMap.newKeySet();
    private final AtomicLong structuralChecks = new AtomicLong(0);
    
    /**
     * Creates a new structural integrity system.
     * @param config The game configuration
     */
    public StructuralIntegritySystem(GameConfig config) {
        this.config = config;
        logger.info("Created structural integrity system");
    }
    
    /**
     * Initializes the structural integrity system.
     */
    public void initialize() {
        logger.info("Initialized structural integrity system");
    }
    
    /**
     * Updates structural integrity across all active regions.
     * @param deltaTime Time elapsed since last update
     */
    public void update(double deltaTime) {
        for (PhysicsRegion region : activeRegions) {
            if (region.getStructuralIntegrity() != null) {
                region.getStructuralIntegrity().update(deltaTime);
                structuralChecks.incrementAndGet();
            }
        }
    }
    
    /**
     * Adds a physics region to be managed by this system.
     * @param region The region to add
     */
    public void addRegion(PhysicsRegion region) {
        activeRegions.add(region);
    }
    
    /**
     * Removes a physics region from management.
     * @param region The region to remove
     */
    public void removeRegion(PhysicsRegion region) {
        activeRegions.remove(region);
    }
    
    /**
     * Gets the number of structural checks performed.
     * @return The structural check count
     */
    public long getStructuralChecks() {
        return structuralChecks.get();
    }
    
    /**
     * Shuts down the structural integrity system.
     */
    public void shutdown() {
        cleanup();
    }
    
    /**
     * Cleans up system resources.
     */
    public void cleanup() {
        activeRegions.clear();
        logger.info("Cleaned up structural integrity system");
    }
}