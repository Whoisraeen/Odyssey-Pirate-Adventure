package com.odyssey.world;

import com.odyssey.util.Logger;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Handles dynamic lighting calculations for the voxel world.
 * Manages sunlight propagation, block light sources, and underwater lighting effects.
 */
public class LightingEngine {
    
    private final World world;
    private final Logger logger; // Added logger instance
    private final ConcurrentMap<ChunkCoordinate, LightUpdateTask> pendingUpdates;
    private final Queue<LightNode> lightQueue;
    private final Queue<LightNode> removalQueue;
    
    // Light levels (0-15, where 15 is brightest)
    public static final int MAX_LIGHT_LEVEL = 15;
    public static final int MIN_LIGHT_LEVEL = 0;
    
    // Light types
    public static final int SUNLIGHT = 0;
    public static final int BLOCKLIGHT = 1;
    
    // Time of day affects sunlight intensity
    private float timeOfDay = 0.5f; // 0.0 = midnight, 0.5 = noon, 1.0 = midnight
    private int currentSunlightLevel = MAX_LIGHT_LEVEL;
    
    public LightingEngine(World world) {
        this.world = world;
        this.logger = Logger.getLogger(LightingEngine.class); // Initialize logger
        this.pendingUpdates = new ConcurrentHashMap<>();
        this.lightQueue = new ArrayDeque<>();
        this.removalQueue = new ArrayDeque<>();
        
        logger.debug(Logger.WORLD, "Initialized lighting engine");
    }
    
    /**
     * Updates lighting for all pending chunks
     */
    public void update() {
        updateSunlightLevel();
        
        // Process pending light updates
        for (LightUpdateTask task : pendingUpdates.values()) {
            processLightUpdate(task);
        }
        pendingUpdates.clear();
        
        // Process light propagation queue
        processLightQueue();
        
        // Process light removal queue
        processRemovalQueue();
    }
    
    /**
     * Calculates initial lighting for a newly generated chunk
     */
    public void calculateInitialLighting(Chunk chunk) {
        logger.debug(Logger.WORLD, "Calculating initial lighting for chunk ({}, {})", 
                    chunk.getChunkX(), chunk.getChunkZ());
        
        // Calculate sunlight from top down
        calculateSunlight(chunk);
        
        // Calculate block light from light sources
        calculateBlockLight(chunk);
        
        // Propagate light to neighboring chunks if needed
        scheduleNeighborUpdates(chunk);
    }
    
    /**
     * Schedules a light update for when a block changes
     */
    public void scheduleBlockUpdate(int worldX, int worldY, int worldZ, Block.BlockType oldBlock, Block.BlockType newBlock) {
        ChunkCoordinate coord = new ChunkCoordinate(worldX >> 4, worldZ >> 4);
        
        LightUpdateTask task = pendingUpdates.computeIfAbsent(coord, _ -> new LightUpdateTask(coord));
        task.addBlockChange(worldX & 15, worldY, worldZ & 15, oldBlock, newBlock);
    }
    
    /**
     * Gets the combined light level at a position
     */
    public int getLightLevel(int worldX, int worldY, int worldZ) {
        Chunk chunk = world.getChunk(worldX >> 4, worldZ >> 4);
        if (chunk == null) {
            return 0;
        }
        
        int sunlight = chunk.getSunlight(worldX & 15, worldY, worldZ & 15);
        int blocklight = chunk.getBlocklight(worldX & 15, worldY, worldZ & 15);
        
        return Math.max(sunlight, blocklight);
    }
    
    /**
     * Gets sunlight level at a position
     */
    public int getSunlight(int worldX, int worldY, int worldZ) {
        Chunk chunk = world.getChunk(worldX >> 4, worldZ >> 4);
        if (chunk == null) {
            return 0;
        }
        
        return chunk.getSunlight(worldX & 15, worldY, worldZ & 15);
    }
    
    /**
     * Gets block light level at a position
     */
    public int getBlocklight(int worldX, int worldY, int worldZ) {
        Chunk chunk = world.getChunk(worldX >> 4, worldZ >> 4);
        if (chunk == null) {
            return 0;
        }
        
        return chunk.getBlocklight(worldX & 15, worldY, worldZ & 15);
    }
    
    /**
     * Sets the time of day (affects sunlight intensity)
     */
    public void setTimeOfDay(float timeOfDay) {
        this.timeOfDay = Math.max(0.0f, Math.min(1.0f, timeOfDay));
        updateSunlightLevel();
    }
    
    /**
     * Gets the current time of day
     */
    public float getTimeOfDay() {
        return timeOfDay;
    }
    
    /**
     * Gets the current sunlight level based on time of day
     */
    public int getCurrentSunlightLevel() {
        return currentSunlightLevel;
    }
    
    /**
     * Updates sunlight level based on time of day
     */
    private void updateSunlightLevel() {
        // Calculate sunlight intensity based on time of day
        // 0.0-0.25: night (0-4 light)
        // 0.25-0.75: day (15 light)
        // 0.75-1.0: night (4-0 light)
        
        if (timeOfDay < 0.25f) {
            // Night to dawn
            float progress = timeOfDay / 0.25f;
            currentSunlightLevel = (int) (progress * 4);
        } else if (timeOfDay < 0.75f) {
            // Day
            currentSunlightLevel = MAX_LIGHT_LEVEL;
        } else {
            // Dusk to night
            float progress = (1.0f - timeOfDay) / 0.25f;
            currentSunlightLevel = (int) (progress * 4);
        }
        
        currentSunlightLevel = Math.max(MIN_LIGHT_LEVEL, Math.min(MAX_LIGHT_LEVEL, currentSunlightLevel));
    }
    
    /**
     * Calculates sunlight for a chunk
     */
    private void calculateSunlight(Chunk chunk) {
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                calculateSunlightColumn(chunk, x, z);
            }
        }
    }
    
    /**
     * Calculates sunlight for a single column
     */
    private void calculateSunlightColumn(Chunk chunk, int x, int z) {
        int lightLevel = currentSunlightLevel;
        
        // Start from top and work down
        for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 0; y--) {
            Block.BlockType block = chunk.getBlock(x, y, z);
            
            // Set current light level
            chunk.setSunlight(x, y, z, lightLevel);
            
            // Reduce light based on block opacity
            if (block != Block.BlockType.AIR) {
                int opacity = getBlockOpacity(block);
                lightLevel = Math.max(0, lightLevel - opacity);
                
                // Special case for water - reduces light gradually
                if (block == Block.BlockType.WATER) {
                    lightLevel = Math.max(0, lightLevel - 1);
                }
            }
        }
    }
    
    /**
     * Calculates block light from light sources
     */
    private void calculateBlockLight(Chunk chunk) {
        // First pass: find all light sources
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    Block.BlockType block = chunk.getBlock(x, y, z);
                    int lightEmission = getBlockLightEmission(block);
                    
                    if (lightEmission > 0) {
                        chunk.setBlocklight(x, y, z, lightEmission);
                        
                        // Add to propagation queue
                        int worldX = chunk.getChunkX() * Chunk.CHUNK_SIZE + x;
                        int worldZ = chunk.getChunkZ() * Chunk.CHUNK_SIZE + z;
                        lightQueue.offer(new LightNode(worldX, y, worldZ, lightEmission, BLOCKLIGHT));
                    }
                }
            }
        }
        
        // Second pass: propagate light
        propagateBlockLight();
    }
    
    /**
     * Propagates block light from sources
     */
    private void propagateBlockLight() {
        int[] dx = {-1, 1, 0, 0, 0, 0};
        int[] dy = {0, 0, -1, 1, 0, 0};
        int[] dz = {0, 0, 0, 0, -1, 1};
        
        while (!lightQueue.isEmpty()) {
            LightNode node = lightQueue.poll();
            
            if (node.lightLevel <= 1) {
                continue;
            }
            
            // Propagate to all 6 neighbors
            for (int i = 0; i < 6; i++) {
                int nx = node.x + dx[i];
                int ny = node.y + dy[i];
                int nz = node.z + dz[i];
                
                // Check bounds
                if (ny < 0 || ny >= Chunk.CHUNK_HEIGHT) {
                    continue;
                }
                
                Chunk neighborChunk = world.getChunk(nx >> 4, nz >> 4);
                if (neighborChunk == null) {
                    continue;
                }
                
                int localX = nx & 15;
                int localZ = nz & 15;
                
                Block.BlockType neighborBlock = neighborChunk.getBlock(localX, ny, localZ);
                int opacity = getBlockOpacity(neighborBlock);
                int newLightLevel = node.lightLevel - Math.max(1, opacity);
                
                if (newLightLevel > 0) {
                    int currentLight = neighborChunk.getBlocklight(localX, ny, localZ);
                    if (newLightLevel > currentLight) {
                        neighborChunk.setBlocklight(localX, ny, localZ, newLightLevel);
                        lightQueue.offer(new LightNode(nx, ny, nz, newLightLevel, BLOCKLIGHT));
                    }
                }
            }
        }
    }
    
    /**
     * Processes the light update queue
     */
    private void processLightQueue() {
        while (!lightQueue.isEmpty()) {
            LightNode node = lightQueue.poll();
            propagateLightFromNode(node);
        }
    }
    
    /**
     * Processes the light removal queue
     */
    private void processRemovalQueue() {
        while (!removalQueue.isEmpty()) {
            LightNode node = removalQueue.poll();
            removeLightFromNode(node);
        }
    }
    
    /**
     * Propagates light from a specific node
     */
    private void propagateLightFromNode(LightNode node) {
        int[] dx = {-1, 1, 0, 0, 0, 0};
        int[] dy = {0, 0, -1, 1, 0, 0};
        int[] dz = {0, 0, 0, 0, -1, 1};
        
        for (int i = 0; i < 6; i++) {
            int nx = node.x + dx[i];
            int ny = node.y + dy[i];
            int nz = node.z + dz[i];
            
            if (ny < 0 || ny >= Chunk.CHUNK_HEIGHT) {
                continue;
            }
            
            Chunk chunk = world.getChunk(nx >> 4, nz >> 4);
            if (chunk == null) {
                continue;
            }
            
            int localX = nx & 15;
            int localZ = nz & 15;
            
            Block.BlockType block = chunk.getBlock(localX, ny, localZ);
            int opacity = getBlockOpacity(block);
            int newLightLevel = node.lightLevel - Math.max(1, opacity);
            
            if (newLightLevel > 0) {
                int currentLight = node.lightType == SUNLIGHT ? 
                    chunk.getSunlight(localX, ny, localZ) : 
                    chunk.getBlocklight(localX, ny, localZ);
                
                if (newLightLevel > currentLight) {
                    if (node.lightType == SUNLIGHT) {
                        chunk.setSunlight(localX, ny, localZ, newLightLevel);
                    } else {
                        chunk.setBlocklight(localX, ny, localZ, newLightLevel);
                    }
                    
                    lightQueue.offer(new LightNode(nx, ny, nz, newLightLevel, node.lightType));
                }
            }
        }
    }
    
    /**
     * Removes light from a specific node
     */
    private void removeLightFromNode(LightNode node) {
        int[] dx = {-1, 1, 0, 0, 0, 0};
        int[] dy = {0, 0, -1, 1, 0, 0};
        int[] dz = {0, 0, 0, 0, -1, 1};
        
        for (int i = 0; i < 6; i++) {
            int nx = node.x + dx[i];
            int ny = node.y + dy[i];
            int nz = node.z + dz[i];
            
            if (ny < 0 || ny >= Chunk.CHUNK_HEIGHT) {
                continue;
            }
            
            Chunk chunk = world.getChunk(nx >> 4, nz >> 4);
            if (chunk == null) {
                continue;
            }
            
            int localX = nx & 15;
            int localZ = nz & 15;
            
            int currentLight = node.lightType == SUNLIGHT ? 
                chunk.getSunlight(localX, ny, localZ) : 
                chunk.getBlocklight(localX, ny, localZ);
            
            if (currentLight > 0 && currentLight < node.lightLevel) {
                if (node.lightType == SUNLIGHT) {
                    chunk.setSunlight(localX, ny, localZ, 0);
                } else {
                    chunk.setBlocklight(localX, ny, localZ, 0);
                }
                
                removalQueue.offer(new LightNode(nx, ny, nz, currentLight, node.lightType));
            }
        }
    }
    
    /**
     * Processes a light update task
     */
    private void processLightUpdate(LightUpdateTask task) {
        Chunk chunk = world.getChunk(task.chunkCoord.x, task.chunkCoord.z);
        if (chunk == null) {
            return;
        }
        
        for (BlockChange change : task.blockChanges) {
            processBlockChange(chunk, change);
        }
    }
    
    /**
     * Processes a single block change for lighting
     */
    private void processBlockChange(Chunk chunk, BlockChange change) {
        int worldX = chunk.getChunkX() * Chunk.CHUNK_SIZE + change.x;
        int worldZ = chunk.getChunkZ() * Chunk.CHUNK_SIZE + change.z;
        
        // Handle light source changes
        int oldEmission = getBlockLightEmission(change.oldBlock);
        int newEmission = getBlockLightEmission(change.newBlock);
        
        if (oldEmission > 0) {
            // Remove old light
            removalQueue.offer(new LightNode(worldX, change.y, worldZ, oldEmission, BLOCKLIGHT));
        }
        
        if (newEmission > 0) {
            // Add new light
            chunk.setBlocklight(change.x, change.y, change.z, newEmission);
            lightQueue.offer(new LightNode(worldX, change.y, worldZ, newEmission, BLOCKLIGHT));
        }
        
        // Recalculate sunlight column if opacity changed
        int oldOpacity = getBlockOpacity(change.oldBlock);
        int newOpacity = getBlockOpacity(change.newBlock);
        
        if (oldOpacity != newOpacity) {
            calculateSunlightColumn(chunk, change.x, change.z);
        }
    }
    
    /**
     * Schedules lighting updates for neighboring chunks
     */
    private void scheduleNeighborUpdates(Chunk chunk) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        
        // Schedule updates for adjacent chunks
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                
                Chunk neighbor = world.getChunk(chunkX + dx, chunkZ + dz);
                if (neighbor != null) {
                    ChunkCoordinate coord = new ChunkCoordinate(chunkX + dx, chunkZ + dz);
                    pendingUpdates.computeIfAbsent(coord, _ -> new LightUpdateTask(coord));
                }
            }
        }
    }
    
    /**
     * Gets the light emission level of a block
     */
    private int getBlockLightEmission(Block.BlockType block) {
        switch (block) {
            case TORCH: return 14;
            case LANTERN: return 15;
            case GLOWSTONE: return 15;
            case LAVA: return 15;
            case FIRE: return 15;
            default: return 0;
        }
    }
    
    /**
     * Gets the opacity (light blocking) level of a block
     */
    private int getBlockOpacity(Block.BlockType block) {
        switch (block) {
            case AIR: return 0;
            case WATER: return 2;
            case LEAVES: return 1;
            case PALM_LEAVES: return 1;
            case SEAWEED: return 1;
            case CORAL: return 1;
            default: return 15; // Solid blocks block all light
        }
    }
    
    /**
     * Represents a coordinate pair for chunks
     */
    private static class ChunkCoordinate {
        final int x, z;
        
        ChunkCoordinate(int x, int z) {
            this.x = x;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ChunkCoordinate)) return false;
            ChunkCoordinate other = (ChunkCoordinate) obj;
            return x == other.x && z == other.z;
        }
        
        @Override
        public int hashCode() {
            return x * 31 + z;
        }
    }
    
    /**
     * Represents a light update task for a chunk
     */
    private static class LightUpdateTask {
        final ChunkCoordinate chunkCoord;
        final Queue<BlockChange> blockChanges;
        
        LightUpdateTask(ChunkCoordinate chunkCoord) {
            this.chunkCoord = chunkCoord;
            this.blockChanges = new ArrayDeque<>();
        }
        
        void addBlockChange(int x, int y, int z, Block.BlockType oldBlock, Block.BlockType newBlock) {
            blockChanges.offer(new BlockChange(x, y, z, oldBlock, newBlock));
        }
    }
    
    /**
     * Represents a block change for lighting updates
     */
    private static class BlockChange {
        final int x, y, z;
        final Block.BlockType oldBlock, newBlock;
        
        BlockChange(int x, int y, int z, Block.BlockType oldBlock, Block.BlockType newBlock) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.oldBlock = oldBlock;
            this.newBlock = newBlock;
        }
    }
    
    /**
     * Represents a light node for propagation
     */
    private static class LightNode {
        final int x, y, z;
        final int lightLevel;
        final int lightType;
        
        LightNode(int x, int y, int z, int lightLevel, int lightType) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.lightLevel = lightLevel;
            this.lightType = lightType;
        }
    }
}