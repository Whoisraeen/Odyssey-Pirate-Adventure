package com.odyssey.world;

import com.odyssey.core.GameConfig;
import com.odyssey.physics.PhysicsEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the game world, including world generation, chunk loading, and world updates.
 */
public class WorldManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldManager.class);
    
    private final GameConfig config;
    private final PhysicsEngine physicsEngine;
    private World currentWorld;
    private WorldGenerator worldGenerator;
    private boolean initialized = false;
    
    public WorldManager(GameConfig config, PhysicsEngine physicsEngine) {
        this.config = config;
        this.physicsEngine = physicsEngine;
    }
    
    /**
     * Initialize the world manager.
     */
    public void initialize() {
        if (initialized) {
            LOGGER.warn("WorldManager is already initialized");
            return;
        }
        
        LOGGER.info("Initializing WorldManager...");
        
        try {
            // Initialize world generator with default seed
            worldGenerator = new WorldGenerator(System.currentTimeMillis());
            
            // Create initial world with default parameters
            currentWorld = new World("default", System.currentTimeMillis());
            
            initialized = true;
            LOGGER.info("WorldManager initialized successfully");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize WorldManager", e);
            cleanup();
            throw new RuntimeException("WorldManager initialization failed", e);
        }
    }
    
    /**
     * Update the world manager.
     */
    public void update(double deltaTime) {
        if (!initialized) return;
        
        if (currentWorld != null) {
            // World.update expects a Camera parameter, not deltaTime
            // For now, we'll skip the update until we have proper camera integration
            // TODO: Pass proper camera instance to world update
        }
    }
    
    /**
     * Get the current world.
     */
    public World getCurrentWorld() {
        return currentWorld;
    }
    
    /**
     * Get the world generator.
     */
    public WorldGenerator getWorldGenerator() {
        return worldGenerator;
    }
    
    /**
     * Load a world chunk at the specified coordinates.
     */
    public void loadChunk(int chunkX, int chunkZ) {
        if (!initialized || currentWorld == null) return;
        
        LOGGER.debug("Loading chunk at ({}, {})", chunkX, chunkZ);
        // TODO: Implement chunk loading
    }
    
    /**
     * Unload a world chunk at the specified coordinates.
     */
    public void unloadChunk(int chunkX, int chunkZ) {
        if (!initialized || currentWorld == null) return;
        
        LOGGER.debug("Unloading chunk at ({}, {})", chunkX, chunkZ);
        // TODO: Implement chunk unloading
    }
    
    /**
     * Generate a new world.
     */
    public void generateWorld(String worldName, long seed) {
        if (!initialized) return;
        
        LOGGER.info("Generating new world: {} with seed: {}", worldName, seed);
        
        try {
            // Clean up existing world
            if (currentWorld != null) {
                currentWorld.cleanup();
            }
            
            // Generate new world
            currentWorld = new World(worldName, seed);
            
            LOGGER.info("World generation complete");
            
        } catch (Exception e) {
            LOGGER.error("Failed to generate world", e);
            throw new RuntimeException("World generation failed", e);
        }
    }
    
    /**
     * Save the current world.
     */
    public void saveWorld() {
        if (!initialized || currentWorld == null) return;
        
        LOGGER.info("Saving world...");
        // TODO: Implement world saving
        LOGGER.info("World saved successfully");
    }
    
    /**
     * Load a world from disk.
     */
    public void loadWorld(String worldName) {
        if (!initialized) return;
        
        LOGGER.info("Loading world: {}", worldName);
        // TODO: Implement world loading
        LOGGER.info("World loaded successfully");
    }
    
    /**
     * Clean up resources.
     */
    public void cleanup() {
        LOGGER.info("Cleaning up WorldManager resources...");
        
        if (currentWorld != null) {
            currentWorld.cleanup();
        }
        
        initialized = false;
        LOGGER.info("WorldManager cleanup complete");
    }
}