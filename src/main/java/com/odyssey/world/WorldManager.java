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
        try {
            // Delegate chunk loading to the current world
            currentWorld.loadChunk(new org.joml.Vector2i(chunkX, chunkZ));
            LOGGER.debug("Chunk ({}, {}) loaded successfully", chunkX, chunkZ);
        } catch (Exception e) {
            LOGGER.error("Failed to load chunk ({}, {})", chunkX, chunkZ, e);
        }
    }
    
    /**
     * Unload a world chunk at the specified coordinates.
     */
    public void unloadChunk(int chunkX, int chunkZ) {
        if (!initialized || currentWorld == null) return;
        
        LOGGER.debug("Unloading chunk at ({}, {})", chunkX, chunkZ);
        try {
            // Delegate chunk unloading to the current world
            currentWorld.unloadChunk(new org.joml.Vector2i(chunkX, chunkZ));
            LOGGER.debug("Chunk ({}, {}) unloaded successfully", chunkX, chunkZ);
        } catch (Exception e) {
            LOGGER.error("Failed to unload chunk ({}, {})", chunkX, chunkZ, e);
        }
    }
    
    /**
     * Check if a chunk is loaded at the specified coordinates.
     */
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        if (!initialized || currentWorld == null) return false;
        
        return currentWorld.getChunk(chunkX, chunkZ) != null;
    }
    
    /**
     * Get the number of loaded chunks.
     */
    public int getLoadedChunkCount() {
        if (!initialized || currentWorld == null) return 0;
        
        return currentWorld.getLoadedChunks().size();
    }
    
    /**
     * Force unload all chunks outside the specified radius from the center.
     */
    public void unloadDistantChunks(int centerX, int centerZ, int maxRadius) {
        if (!initialized || currentWorld == null) return;
        
        LOGGER.debug("Unloading chunks beyond radius {} from center ({}, {})", maxRadius, centerX, centerZ);
        try {
            // currentWorld.unloadDistantChunks(centerX, centerZ, maxRadius);
            LOGGER.debug("Distant chunks unloaded successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to unload distant chunks", e);
        }
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
        try {
            // Save world data including chunks, entities, and world state
            String worldName = currentWorld.getWorldName();
            long worldSeed = currentWorld.getWorldSeed();
            
            LOGGER.debug("Saving world '{}' with seed: {}", worldName, worldSeed);
            
            // Save world metadata
            saveWorldMetadata(worldName, worldSeed);
            
            // Save loaded chunks
            saveLoadedChunks();
            
            // Save world entities and state
            saveWorldEntities();
            
            LOGGER.info("World '{}' saved successfully", worldName);
        } catch (Exception e) {
            LOGGER.error("Failed to save world", e);
            throw new RuntimeException("World saving failed", e);
        }
    }
    
    /**
     * Load a world from disk.
     */
    public void loadWorld(String worldName) {
        if (!initialized) return;
        
        LOGGER.info("Loading world: {}", worldName);
        try {
            // Clean up existing world
            if (currentWorld != null) {
                currentWorld.cleanup();
            }
            
            // Load world metadata
            WorldMetadata metadata = loadWorldMetadata(worldName);
            if (metadata == null) {
                throw new RuntimeException("World metadata not found for: " + worldName);
            }
            
            // Create new world instance
            currentWorld = new World(metadata.worldName, metadata.worldSeed);
            
            // Load world chunks
            loadWorldChunks(worldName);
            
            // Load world entities and state
            loadWorldEntities(worldName);
            
            LOGGER.info("World '{}' loaded successfully", worldName);
        } catch (Exception e) {
            LOGGER.error("Failed to load world: {}", worldName, e);
            throw new RuntimeException("World loading failed", e);
        }
    }
    
    /**
     * Save world metadata to disk.
     */
    private void saveWorldMetadata(String worldName, long worldSeed) {
        LOGGER.debug("Saving world metadata for '{}'", worldName);
        // TODO: Implement world metadata saving to file system
        // This would typically save to a world directory structure like:
        // saves/worldName/world.dat containing seed, creation time, last played, etc.
    }
    
    /**
     * Load world metadata from disk.
     */
    private WorldMetadata loadWorldMetadata(String worldName) {
        LOGGER.debug("Loading world metadata for '{}'", worldName);
        // TODO: Implement world metadata loading from file system
        // For now, return null to indicate metadata not found
        return null;
    }
    
    /**
     * Save all loaded chunks to disk.
     */
    private void saveLoadedChunks() {
        LOGGER.debug("Saving loaded chunks");
        // TODO: Implement chunk data saving
        // This would iterate through all loaded chunks and save their voxel data
        if (currentWorld != null) {
            int chunkCount = currentWorld.getChunks().size();
            LOGGER.debug("Saving {} loaded chunks", chunkCount);
        }
    }
    
    /**
     * Load world chunks from disk.
     */
    private void loadWorldChunks(String worldName) {
        LOGGER.debug("Loading world chunks for '{}'", worldName);
        // TODO: Implement chunk data loading
        // This would load previously saved chunk data from the world directory
    }
    
    /**
     * Save world entities and state.
     */
    private void saveWorldEntities() {
        LOGGER.debug("Saving world entities and state");
        // TODO: Implement entity and world state saving
        // This would save ships, NPCs, dynamic objects, weather state, etc.
    }
    
    /**
     * Load world entities and state.
     */
    private void loadWorldEntities(String worldName) {
        LOGGER.debug("Loading world entities and state for '{}'", worldName);
        // TODO: Implement entity and world state loading
        // This would restore ships, NPCs, dynamic objects, weather state, etc.
    }
    
    /**
     * Simple data class for world metadata.
     */
    private static class WorldMetadata {
        final String worldName;
        final long worldSeed;
        final long creationTime;
        final long lastPlayed;
        
        WorldMetadata(String worldName, long worldSeed, long creationTime, long lastPlayed) {
            this.worldName = worldName;
            this.worldSeed = worldSeed;
            this.creationTime = creationTime;
            this.lastPlayed = lastPlayed;
        }
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