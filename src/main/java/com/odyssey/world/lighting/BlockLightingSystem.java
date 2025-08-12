package com.odyssey.world.lighting;

import com.odyssey.world.World;
import com.odyssey.world.chunk.Chunk;
import com.odyssey.world.chunk.ChunkColumn;
import com.odyssey.world.generation.WorldGenerator.BlockType;
import org.joml.Vector3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Block-level lighting system with incremental flood-fill lighting updates.
 * Handles separate sky light and emissive block light channels with queued relight updates.
 */
public class BlockLightingSystem {
    private static final Logger logger = LoggerFactory.getLogger(BlockLightingSystem.class);
    
    // Light level constants
    public static final int MAX_LIGHT_LEVEL = 15;
    public static final int MIN_LIGHT_LEVEL = 0;
    public static final int SKY_LIGHT_LEVEL = 15;
    
    // Light propagation directions (6 cardinal directions)
    private static final Vector3i[] LIGHT_DIRECTIONS = {
        new Vector3i(1, 0, 0),   // East
        new Vector3i(-1, 0, 0),  // West
        new Vector3i(0, 1, 0),   // Up
        new Vector3i(0, -1, 0),  // Down
        new Vector3i(0, 0, 1),   // South
        new Vector3i(0, 0, -1)   // North
    };
    
    private final World world;
    private final ConcurrentLinkedQueue<LightUpdate> lightUpdateQueue;
    private final ConcurrentLinkedQueue<LightUpdate> skylightUpdateQueue;
    private final Map<Vector3i, Integer> pendingBlockLightUpdates;
    private final Map<Vector3i, Integer> pendingSkyLightUpdates;
    
    // Performance tracking
    private int updatesPerTick = 100;
    private long lastUpdateTime = 0;
    
    public BlockLightingSystem(World world) {
        this.world = world;
        this.lightUpdateQueue = new ConcurrentLinkedQueue<>();
        this.skylightUpdateQueue = new ConcurrentLinkedQueue<>();
        this.pendingBlockLightUpdates = new ConcurrentHashMap<>();
        this.pendingSkyLightUpdates = new ConcurrentHashMap<>();
        
        logger.info("Block lighting system initialized");
    }
    
    /**
     * Updates lighting for a limited number of queued updates per tick.
     */
    public void tick() {
        long startTime = System.nanoTime();
        
        // Process block light updates
        processLightUpdates(lightUpdateQueue, pendingBlockLightUpdates, false);
        
        // Process sky light updates
        processLightUpdates(skylightUpdateQueue, pendingSkyLightUpdates, true);
        
        lastUpdateTime = System.nanoTime() - startTime;
    }
    
    /**
     * Processes a batch of light updates from the queue.
     */
    private void processLightUpdates(Queue<LightUpdate> queue, Map<Vector3i, Integer> pending, boolean isSkyLight) {
        int processed = 0;
        
        while (!queue.isEmpty() && processed < updatesPerTick) {
            LightUpdate update = queue.poll();
            if (update != null) {
                if (isSkyLight) {
                    propagateSkyLight(update.position, update.lightLevel);
                } else {
                    propagateBlockLight(update.position, update.lightLevel);
                }
                pending.remove(update.position);
                processed++;
            }
        }
    }
    
    /**
     * Schedules a block light update at the specified position.
     */
    public void scheduleBlockLightUpdate(Vector3i position, int lightLevel) {
        Vector3i pos = new Vector3i(position);
        
        // Avoid duplicate updates
        if (!pendingBlockLightUpdates.containsKey(pos)) {
            pendingBlockLightUpdates.put(pos, lightLevel);
            lightUpdateQueue.offer(new LightUpdate(pos, lightLevel));
        }
    }
    
    /**
     * Schedules a sky light update at the specified position.
     */
    public void scheduleSkyLightUpdate(Vector3i position, int lightLevel) {
        Vector3i pos = new Vector3i(position);
        
        // Avoid duplicate updates
        if (!pendingSkyLightUpdates.containsKey(pos)) {
            pendingSkyLightUpdates.put(pos, lightLevel);
            skylightUpdateQueue.offer(new LightUpdate(pos, lightLevel));
        }
    }
    
    /**
     * Propagates block light from a source position.
     */
    private void propagateBlockLight(Vector3i position, int lightLevel) {
        if (lightLevel <= 0) {
            return;
        }
        
        Chunk chunk = world.getChunkManager().loadChunk(position.x >> 4, position.z >> 4);
        if (chunk == null) {
            return;
        }
        
        int currentLight = chunk.getBlockLight(position.x & 15, position.y, position.z & 15);
        
        // Only update if new light level is higher
        if (lightLevel <= currentLight) {
            return;
        }
        
        chunk.setBlockLight(position.x & 15, position.y, position.z & 15, (byte) lightLevel);
        
        // Propagate to neighboring blocks
        for (Vector3i direction : LIGHT_DIRECTIONS) {
            Vector3i neighborPos = new Vector3i(position).add(direction);
            
            // Check bounds
            if (neighborPos.y < 0 || neighborPos.y >= 256) {
                continue;
            }
            
            Chunk neighborChunk = world.getChunkManager().loadChunk(neighborPos.x >> 4, neighborPos.z >> 4);
            if (neighborChunk == null) {
                continue;
            }
            
            BlockType neighborBlock = neighborChunk.getBlock(
                neighborPos.x & 15, neighborPos.y, neighborPos.z & 15);
            
            // Calculate light reduction based on block opacity
            int lightReduction = getBlockOpacity(neighborBlock);
            int newLightLevel = Math.max(0, lightLevel - lightReduction - 1);
            
            if (newLightLevel > 0) {
                int neighborCurrentLight = neighborChunk.getBlockLight(
                    neighborPos.x & 15, neighborPos.y, neighborPos.z & 15);
                
                if (newLightLevel > neighborCurrentLight) {
                    scheduleBlockLightUpdate(neighborPos, newLightLevel);
                }
            }
        }
    }
    
    /**
     * Propagates sky light from a source position.
     */
    private void propagateSkyLight(Vector3i position, int lightLevel) {
        if (lightLevel <= 0) {
            return;
        }
        
        Chunk chunk = world.getChunkManager().loadChunk(position.x >> 4, position.z >> 4);
        if (chunk == null) {
            return;
        }
        
        int currentLight = chunk.getSkyLight(position.x & 15, position.y, position.z & 15);
        
        // Only update if new light level is higher
        if (lightLevel <= currentLight) {
            return;
        }
        
        chunk.setSkyLight(position.x & 15, position.y, position.z & 15, (byte) lightLevel);
        
        // Propagate to neighboring blocks
        for (Vector3i direction : LIGHT_DIRECTIONS) {
            Vector3i neighborPos = new Vector3i(position).add(direction);
            
            // Check bounds
            if (neighborPos.y < 0 || neighborPos.y >= 256) {
                continue;
            }
            
            Chunk neighborChunk = world.getChunkManager().loadChunk(neighborPos.x >> 4, neighborPos.z >> 4);
            if (neighborChunk == null) {
                continue;
            }
            
            BlockType neighborBlock = neighborChunk.getBlock(
                neighborPos.x & 15, neighborPos.y, neighborPos.z & 15);
            
            // Sky light propagates differently - no reduction for transparent blocks
            int lightReduction = getBlockOpacity(neighborBlock);
            int newLightLevel;
            
            if (direction.y == -1) {
                // Downward propagation - sky light maintains full strength through transparent blocks
                newLightLevel = lightReduction == 0 ? lightLevel : Math.max(0, lightLevel - lightReduction);
            } else {
                // Horizontal and upward propagation
                newLightLevel = Math.max(0, lightLevel - lightReduction - 1);
            }
            
            if (newLightLevel > 0) {
                int neighborCurrentLight = neighborChunk.getSkyLight(
                    neighborPos.x & 15, neighborPos.y, neighborPos.z & 15);
                
                if (newLightLevel > neighborCurrentLight) {
                    scheduleSkyLightUpdate(neighborPos, newLightLevel);
                }
            }
        }
    }
    
    /**
     * Handles block placement - triggers light recalculation.
     */
    public void onBlockPlaced(Vector3i position, BlockType blockType) {
        // Remove light from this position if block is opaque
        if (getBlockOpacity(blockType) > 0) {
            removeLight(position);
        }
        
        // Add light if block is emissive
        int emissionLevel = getBlockEmissionLevel(blockType);
        if (emissionLevel > 0) {
            scheduleBlockLightUpdate(position, emissionLevel);
        }
        
        // Update sky light
        updateSkyLightColumn(position.x, position.z);
    }
    
    /**
     * Handles block removal - triggers light recalculation.
     */
    public void onBlockRemoved(Vector3i position, BlockType oldBlockType) {
        // Recalculate lighting for this position
        recalculateLightAt(position);
        
        // Update sky light column
        updateSkyLightColumn(position.x, position.z);
    }
    
    /**
     * Removes light from a position and propagates the removal.
     */
    private void removeLight(Vector3i position) {
        Chunk chunk = world.getChunkManager().loadChunk(position.x >> 4, position.z >> 4);
        if (chunk == null) {
            return;
        }
        
        int localX = position.x & 15;
        int localZ = position.z & 15;
        
        // Remove block light
        chunk.setBlockLight(localX, position.y, localZ, (byte) 0);
        
        // Remove sky light if not at surface
        if (!isExposedToSky(position)) {
            chunk.setSkyLight(localX, position.y, localZ, (byte) 0);
        }
        
        // Schedule updates for neighboring blocks
        for (Vector3i direction : LIGHT_DIRECTIONS) {
            Vector3i neighborPos = new Vector3i(position).add(direction);
            recalculateLightAt(neighborPos);
        }
    }
    
    /**
     * Recalculates lighting at a specific position.
     */
    private void recalculateLightAt(Vector3i position) {
        if (position.y < 0 || position.y >= 256) {
            return;
        }
        
        Chunk chunk = world.getChunkManager().loadChunk(position.x >> 4, position.z >> 4);
        if (chunk == null) {
            return;
        }
        
        int localX = position.x & 15;
        int localZ = position.z & 15;
        
        BlockType blockType = chunk.getBlock(localX, position.y, localZ);
        
        // Calculate block light from neighbors
        int maxBlockLight = 0;
        for (Vector3i direction : LIGHT_DIRECTIONS) {
            Vector3i neighborPos = new Vector3i(position).add(direction);
            int neighborLight = getBlockLightAt(neighborPos);
            if (neighborLight > 0) {
                int propagatedLight = Math.max(0, neighborLight - getBlockOpacity(blockType) - 1);
                maxBlockLight = Math.max(maxBlockLight, propagatedLight);
            }
        }
        
        // Add emission from this block
        int emission = getBlockEmissionLevel(blockType);
        int finalBlockLight = Math.max(maxBlockLight, emission);
        
        if (finalBlockLight != chunk.getBlockLight(localX, position.y, localZ)) {
            scheduleBlockLightUpdate(position, finalBlockLight);
        }
        
        // Calculate sky light
        int skyLight = calculateSkyLightAt(position);
        if (skyLight != chunk.getSkyLight(localX, position.y, localZ)) {
            scheduleSkyLightUpdate(position, skyLight);
        }
    }
    
    /**
     * Updates sky light for an entire column.
     */
    private void updateSkyLightColumn(int x, int z) {
        ChunkColumn column = world.getChunkManager().getChunkColumn(x >> 4, z >> 4);
        if (column == null) {
            return;
        }
        
        int localX = x & 15;
        int localZ = z & 15;
        
        // Find the highest non-transparent block
        int topY = 255;
        for (int y = 255; y >= 0; y--) {
            Chunk chunk = column.getChunk(y >> 4);
            if (chunk != null) {
                BlockType blockType = chunk.getBlock(localX, y, localZ);
                if (getBlockOpacity(blockType) > 0) {
                    topY = y;
                    break;
                }
            }
        }
        
        // Set sky light for all blocks above the highest opaque block
        for (int y = 255; y > topY; y--) {
            scheduleSkyLightUpdate(new Vector3i(x, y, z), SKY_LIGHT_LEVEL);
        }
        
        // Propagate sky light downward
        for (int y = topY; y >= 0; y--) {
            Vector3i pos = new Vector3i(x, y, z);
            int skyLight = calculateSkyLightAt(pos);
            if (skyLight > 0) {
                scheduleSkyLightUpdate(pos, skyLight);
            }
        }
    }
    
    /**
     * Calculates sky light level at a position.
     */
    private int calculateSkyLightAt(Vector3i position) {
        if (isExposedToSky(position)) {
            return SKY_LIGHT_LEVEL;
        }
        
        // Calculate from neighbors
        int maxSkyLight = 0;
        for (Vector3i direction : LIGHT_DIRECTIONS) {
            Vector3i neighborPos = new Vector3i(position).add(direction);
            int neighborSkyLight = getSkyLightAt(neighborPos);
            
            if (neighborSkyLight > 0) {
                Chunk chunk = world.getChunkManager().loadChunk(position.x >> 4, position.z >> 4);
                if (chunk != null) {
                    BlockType blockType = chunk.getBlock(position.x & 15, position.y, position.z & 15);
                    int opacity = getBlockOpacity(blockType);
                    
                    int propagatedLight;
                    if (direction.y == -1) {
                        // Downward propagation
                        propagatedLight = opacity == 0 ? neighborSkyLight : Math.max(0, neighborSkyLight - opacity);
                    } else {
                        // Horizontal and upward propagation
                        propagatedLight = Math.max(0, neighborSkyLight - opacity - 1);
                    }
                    
                    maxSkyLight = Math.max(maxSkyLight, propagatedLight);
                }
            }
        }
        
        return maxSkyLight;
    }
    
    /**
     * Checks if a position is exposed to sky.
     */
    private boolean isExposedToSky(Vector3i position) {
        for (int y = position.y + 1; y <= 255; y++) {
            Chunk chunk = world.getChunkManager().loadChunk(position.x >> 4, position.z >> 4);
            if (chunk != null) {
                BlockType blockType = chunk.getBlock(position.x & 15, y, position.z & 15);
                if (getBlockOpacity(blockType) > 0) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Gets block light level at a position.
     */
    public int getBlockLightAt(Vector3i position) {
        if (position.y < 0 || position.y >= 256) {
            return 0;
        }
        
        Chunk chunk = world.getChunkManager().loadChunk(position.x >> 4, position.z >> 4);
        if (chunk == null) {
            return 0;
        }
        
        return chunk.getBlockLight(position.x & 15, position.y, position.z & 15);
    }
    
    /**
     * Gets sky light level at a position.
     */
    public int getSkyLightAt(Vector3i position) {
        if (position.y < 0 || position.y >= 256) {
            return 0;
        }
        
        Chunk chunk = world.getChunkManager().loadChunk(position.x >> 4, position.z >> 4);
        if (chunk == null) {
            return 0;
        }
        
        return chunk.getSkyLight(position.x & 15, position.y, position.z & 15);
    }
    
    /**
     * Gets combined light level (max of block and sky light).
     */
    public int getCombinedLightAt(Vector3i position) {
        return Math.max(getBlockLightAt(position), getSkyLightAt(position));
    }
    
    /**
     * Gets the opacity level of a block type.
     */
    private int getBlockOpacity(BlockType blockType) {
        if (blockType == null) {
            return 0;
        }
        return blockType.opacity;
    }
    
    /**
     * Gets the light emission level of a block type.
     */
    private int getBlockEmissionLevel(BlockType blockType) {
        if (blockType == null) {
            return 0;
        }
        return blockType.emission;
    }
    
    /**
     * Gets performance statistics.
     */
    public LightingStats getStats() {
        return new LightingStats(
            lightUpdateQueue.size(),
            skylightUpdateQueue.size(),
            pendingBlockLightUpdates.size(),
            pendingSkyLightUpdates.size(),
            lastUpdateTime / 1_000_000.0 // Convert to milliseconds
        );
    }
    
    /**
     * Sets the number of light updates to process per tick.
     */
    public void setUpdatesPerTick(int updatesPerTick) {
        this.updatesPerTick = Math.max(1, updatesPerTick);
    }
    
    /**
     * Light update data structure.
     */
    private static class LightUpdate {
        final Vector3i position;
        final int lightLevel;
        
        LightUpdate(Vector3i position, int lightLevel) {
            this.position = position;
            this.lightLevel = lightLevel;
        }
    }
    
    /**
     * Lighting performance statistics.
     */
    public static class LightingStats {
        public final int blockLightQueueSize;
        public final int skyLightQueueSize;
        public final int pendingBlockUpdates;
        public final int pendingSkyUpdates;
        public final double lastUpdateTimeMs;
        
        public LightingStats(int blockLightQueueSize, int skyLightQueueSize,
                           int pendingBlockUpdates, int pendingSkyUpdates,
                           double lastUpdateTimeMs) {
            this.blockLightQueueSize = blockLightQueueSize;
            this.skyLightQueueSize = skyLightQueueSize;
            this.pendingBlockUpdates = pendingBlockUpdates;
            this.pendingSkyUpdates = pendingSkyUpdates;
            this.lastUpdateTimeMs = lastUpdateTimeMs;
        }
    }
}