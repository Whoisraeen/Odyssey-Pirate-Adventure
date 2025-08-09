package com.odyssey.world;

import com.odyssey.core.GameConfig;
import com.odyssey.graphics.Renderer;
import com.odyssey.world.generation.WorldGenerator;
import com.odyssey.world.chunk.ChunkManager;
import com.odyssey.world.biome.BiomeManager;
import com.odyssey.world.gamerules.GameRuleManager;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main world management class for The Odyssey.
 * Handles the procedurally generated voxel world including islands, ocean, and biomes.
 */
public class World {
    private static final Logger logger = LoggerFactory.getLogger(World.class);
    
    private final GameConfig config;
    private final long seed;
    
    // World systems
    private WorldGenerator worldGenerator;
    private ChunkManager chunkManager;
    private BiomeManager biomeManager;
    private GameRuleManager gameRuleManager;
    
    // World properties
    private static final int CHUNK_SIZE = 16;
    private static final int WORLD_HEIGHT = 256;
    private static final int SEA_LEVEL = 64;
    
    // Player position for chunk loading
    private Vector3f playerPosition = new Vector3f(0, SEA_LEVEL + 10, 0);
    
    public World(GameConfig config) {
        this.config = config;
        this.seed = config.getWorldSeed();
    }
    
    public void initialize() {
        logger.info("Initializing world with seed: {}", seed);
        
        // Initialize game rules
        gameRuleManager = new GameRuleManager();
        
        // Initialize world generation
        worldGenerator = new WorldGenerator(seed);
        worldGenerator.initialize();
        
        // Initialize biome system
        biomeManager = new BiomeManager(seed);
        biomeManager.initialize();
        
        // Initialize chunk management
        chunkManager = new ChunkManager(config, worldGenerator, biomeManager);
        chunkManager.initialize();
        
        logger.info("World initialized successfully");
    }
    
    public void update(double deltaTime) {
        // Update chunk loading based on player position
        chunkManager.updateChunks(playerPosition, config.getRenderDistance());
        
        // Update world systems
        worldGenerator.update(deltaTime);
        biomeManager.update(deltaTime);
    }
    
    public void render(Renderer renderer) {
        // Render loaded chunks
        chunkManager.render(renderer);
        
        // Render debug information if enabled
        if (config.isDebugMode()) {
            renderDebugInfo(renderer);
        }
    }
    
    private void renderDebugInfo(Renderer renderer) {
        // Render chunk boundaries, biome information, etc.
        // This will be implemented when we have proper debug rendering
    }
    
    public void setPlayerPosition(Vector3f position) {
        this.playerPosition.set(position);
    }
    
    public Vector3f getPlayerPosition() {
        return new Vector3f(playerPosition);
    }
    
    // World queries
    public int getBlockAt(int x, int y, int z) {
        return chunkManager.getBlockAt(x, y, z);
    }
    
    public void setBlockAt(int x, int y, int z, int blockType) {
        chunkManager.setBlockAt(x, y, z, blockType);
    }
    
    public float getHeightAt(float x, float z) {
        return worldGenerator.getHeightAt(x, z);
    }
    
    public boolean isInWater(Vector3f position) {
        return position.y < SEA_LEVEL;
    }
    
    public boolean isOnLand(Vector3f position) {
        float terrainHeight = getHeightAt(position.x, position.z);
        return position.y <= terrainHeight && terrainHeight > SEA_LEVEL;
    }
    
    public void cleanup() {
        logger.info("Cleaning up world resources");
        
        if (chunkManager != null) chunkManager.cleanup();
        if (biomeManager != null) biomeManager.cleanup();
        if (worldGenerator != null) worldGenerator.cleanup();
    }
    
    // Getters
    public long getSeed() { return seed; }
    public WorldGenerator getWorldGenerator() { return worldGenerator; }
    public ChunkManager getChunkManager() { return chunkManager; }
    public BiomeManager getBiomeManager() { return biomeManager; }
    public GameRuleManager getGameRuleManager() { return gameRuleManager; }
    
    public static int getChunkSize() { return CHUNK_SIZE; }
    public static int getWorldHeight() { return WORLD_HEIGHT; }
    public static int getSeaLevel() { return SEA_LEVEL; }
}