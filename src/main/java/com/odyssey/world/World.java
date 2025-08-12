package com.odyssey.world;

import com.odyssey.core.GameConfig;
import com.odyssey.graphics.Renderer;
import com.odyssey.world.generation.WorldGenerator;
import com.odyssey.world.EntityManager;
import com.odyssey.world.generation.WorldGenerator.BlockType;
import com.odyssey.world.chunk.ChunkManager;
import com.odyssey.world.biome.BiomeManager;
import com.odyssey.world.gamerules.GameRuleManager;
import com.odyssey.world.entity.EntityManager;
import com.odyssey.world.item.Item;
import com.odyssey.world.systems.DecaySystem;
import com.odyssey.world.systems.TemperatureSystem;
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
    private RedstoneSystem redstoneSystem;
    private FluidSystem fluidSystem;
    private FireSystem fireSystem;
    private PhysicsSystem physicsSystem;
    private AgricultureSystem agricultureSystem;
    private SpreadSystem spreadSystem;
    private DecaySystem decaySystem;
    private TemperatureSystem temperatureSystem;
    private RandomTickScheduler randomTickScheduler;
    private EntityManager entityManager;
    
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
        
        // Initialize redstone and fluid systems
        redstoneSystem = new RedstoneSystem(this);
        fluidSystem = new FluidSystem(this);
        fireSystem = new FireSystem(this);
        physicsSystem = new PhysicsSystem(this);
        agricultureSystem = new AgricultureSystem(this);
        spreadSystem = new SpreadSystem(this);
        decaySystem = new DecaySystem(this);
        temperatureSystem = new TemperatureSystem(this);
        randomTickScheduler = new RandomTickScheduler(this);
        entityManager = new EntityManager(this);
        
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
    
    /**
     * Gets the block type name at the specified position.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the block type name as a string
     */
    public String getBlock(int x, int y, int z) {
        int blockId = getBlockAt(x, y, z);
        return getBlockTypeName(blockId);
    }
    
    /**
     * Sets a block using a string block type name.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param blockTypeName the block type name as a string
     */
    public void setBlock(int x, int y, int z, String blockTypeName) {
        int blockId = getBlockTypeId(blockTypeName);
        setBlockAt(x, y, z, blockId);
    }

    /**
     * Drops an item into the world as an entity.
     *
     * @param x The x-coordinate.
     * @param y The y-coordinate.
     * @param z The z-coordinate.
     * @param itemType The type of item to drop.
     * @param quantity The number of items to drop.
     */
    public void dropItem(int x, int y, int z, String itemType, int quantity) {
        entityManager.createItemEntity(x, y, z, itemType, quantity);
    }
    
    /**
     * Converts a block ID to its corresponding block type name.
     * 
     * @param blockId the block ID
     * @return the block type name
     */
    private String getBlockTypeName(int blockId) {
        for (BlockType blockType : BlockType.values()) {
            if (blockType.id == blockId) {
                return blockType.name().toLowerCase();
            }
        }
        return "air"; // Default to air for unknown block IDs
    }
    
    /**
     * Converts a block type name to its corresponding block ID.
     * 
     * @param blockTypeName the block type name
     * @return the block ID
     */
    private int getBlockTypeId(String blockTypeName) {
        if (blockTypeName == null) return 0; // Air
        
        String normalizedName = blockTypeName.toUpperCase();
        for (BlockType blockType : BlockType.values()) {
            if (blockType.name().equals(normalizedName)) {
                return blockType.id;
            }
        }
        return 0; // Default to air for unknown block names
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
    public RedstoneSystem getRedstoneSystem() { return redstoneSystem; }
    public FluidSystem getFluidSystem() {
        return fluidSystem;
    }
    
    public FireSystem getFireSystem() {
        return fireSystem;
    }
    
    public PhysicsSystem getPhysicsSystem() {
        return physicsSystem;
    }
    
    public AgricultureSystem getAgricultureSystem() {
        return agricultureSystem;
    }
    
    public SpreadSystem getSpreadSystem() {
        return spreadSystem;
    }
    
    public DecaySystem getDecaySystem() {
        return decaySystem;
    }
    
    public TemperatureSystem getTemperatureSystem() {
        return temperatureSystem;
    }
    
    /**
     * Sets the sky light level for the world.
     * This method is called by the day/night cycle system.
     */
    public void setSkyLightLevel(int lightLevel) {
        // TODO: Implement sky light level setting
        // This would update the lighting system
        logger.debug("Sky light level set to: {}", lightLevel);
    }
    
    /**
     * Gets a placeholder mob spawner.
     * TODO: Implement proper mob spawning system.
     */
    public MobSpawner getMobSpawner() {
        // Return a placeholder for now
        return new MobSpawner();
    }
    
    /**
     * Gets a placeholder player manager.
     * TODO: Implement proper player management system.
     */
    public PlayerManager getPlayerManager() {
        // Return a placeholder for now
        return new PlayerManager();
    }
    
    /**
     * Gets the random tick scheduler.
     */
    public RandomTickScheduler getRandomTickScheduler() {
        return randomTickScheduler;
    }
    
    /**
     * Gets a placeholder entity manager.
     * TODO: Implement proper entity management system.
     */
    public EntityManager getEntityManager() {
        return entityManager;
    }
    
    /**
     * Gets a placeholder statistics manager.
     * TODO: Implement proper statistics system.
     */
    public Statistics getStatistics() {
        // Return a placeholder for now
        return new Statistics();
    }
    
    /**
     * Gets a placeholder weather system.
     * TODO: Implement proper weather system.
     */
    public WeatherSystem getWeatherSystem() {
        // Return a placeholder for now
        return new WeatherSystem();
    }
    
    /**
     * Gets a placeholder event bus.
     * TODO: Implement proper event system.
     */
    public EventBus getEventBus() {
        // Return a placeholder for now
        return new EventBus();
    }
    
    public static int getChunkSize() { return CHUNK_SIZE; }
    public static int getWorldHeight() { return WORLD_HEIGHT; }
    public static int getSeaLevel() { return SEA_LEVEL; }
}