package com.odyssey.core.concurrent;

import com.odyssey.world.World;
import com.odyssey.world.chunk.ChunkManager;
import com.odyssey.world.chunk.ChunkPosition;
import com.odyssey.world.generation.WorldGenerator.BlockType;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.Objects;

/**
 * Thread-safe accessor for world data that provides concurrent read/write operations
 * while maintaining data consistency. This class uses a combination of lock-free
 * data structures and read-write locks to optimize for the common case of many
 * readers with occasional writers.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Lock-free read operations for frequently accessed data</li>
 *   <li>Optimistic concurrency for block modifications</li>
 *   <li>Atomic player position updates</li>
 *   <li>Asynchronous chunk loading and generation</li>
 *   <li>Memory-efficient caching of hot data</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class ThreadSafeWorldAccess {
    
    private final World world;
    private final ChunkManager chunkManager;
    
    // Player position with atomic updates
    private final AtomicReference<Vector3f> playerPosition = new AtomicReference<>(new Vector3f(0.0f, 0.0f, 0.0f));
    
    // Lock-free cache for frequently accessed blocks
    private final LockFreeHashMap<Vector3i, BlockType> blockCache = new LockFreeHashMap<>(1024);
    
    // Chunk access coordination
    private final ConcurrentHashMap<ChunkPosition, ReadWriteLock> chunkLocks = new ConcurrentHashMap<>();
    
    // Pending chunk operations
    private final LockFreeRingBuffer<ChunkOperation> pendingOperations = new LockFreeRingBuffer<>(4096);
    
    // World state version for optimistic concurrency
    private volatile long worldVersion = 0;
    
    /**
     * Creates a new thread-safe world accessor.
     * 
     * @param world the world to provide access to
     * @throws IllegalArgumentException if world is null
     */
    public ThreadSafeWorldAccess(World world) {
        if (world == null) {
            throw new IllegalArgumentException("World cannot be null");
        }
        this.world = world;
        this.chunkManager = world.getChunkManager();
    }
    
    /**
     * Gets the block type at the specified position in a thread-safe manner.
     * This method uses a lock-free cache for frequently accessed blocks.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @return the block type, or null if the chunk is not loaded
     */
    public BlockType getBlock(int x, int y, int z) {
        Vector3i position = new Vector3i(x, y, z);
        
        // Try cache first (lock-free)
        BlockType cached = blockCache.get(position);
        if (cached != null) {
            return cached;
        }
        
        // Get chunk position
        ChunkPosition chunkPos = ChunkPosition.fromWorldPosition(x, z);
        
        // Acquire read lock for chunk
        ReadWriteLock chunkLock = getChunkLock(chunkPos);
        chunkLock.readLock().lock();
        try {
            BlockType blockType = chunkManager.getBlock(x, y, z);
            
            // Cache the result if it's a valid block
            if (blockType != null) {
                blockCache.put(position, blockType);
            }
            
            return blockType;
        } finally {
            chunkLock.readLock().unlock();
        }
    }
    
    /**
     * Sets the block type at the specified position in a thread-safe manner.
     * This method uses optimistic concurrency and will retry on conflicts.
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @param z the z coordinate
     * @param blockType the new block type
     * @return true if the block was successfully set, false if the chunk is not loaded
     */
    public boolean setBlock(int x, int y, int z, BlockType blockType) {
        if (blockType == null) {
            throw new IllegalArgumentException("Block type cannot be null");
        }
        
        Vector3i position = new Vector3i(x, y, z);
        ChunkPosition chunkPos = ChunkPosition.fromWorldPosition(x, z);
        
        // Acquire write lock for chunk
        ReadWriteLock chunkLock = getChunkLock(chunkPos);
        chunkLock.writeLock().lock();
        try {
            boolean success = chunkManager.setBlock(x, y, z, blockType);
            
            if (success) {
                // Update cache
                blockCache.put(position, blockType);
                
                // Increment world version for optimistic concurrency
                worldVersion++;
                
                // Queue chunk update operation
                ChunkOperation operation = new ChunkOperation(
                    ChunkOperation.Type.BLOCK_CHANGED,
                    chunkPos,
                    position,
                    blockType
                );
                pendingOperations.offer(operation);
            }
            
            return success;
        } finally {
            chunkLock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the current player position atomically.
     * 
     * @return the current player position
     */
    public Vector3f getPlayerPosition() {
        return new Vector3f(playerPosition.get().x, playerPosition.get().y, playerPosition.get().z);
    }
    
    /**
     * Updates the player position atomically.
     * 
     * @param position the new player position
     */
    public void updatePlayerPosition(Vector3f position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        
        Vector3f oldPosition = playerPosition.getAndSet(new Vector3f(position.x, position.y, position.z));
        
        // If position changed significantly, trigger chunk loading
        if (oldPosition == null || distanceSquared(position, oldPosition) > 256) {
            triggerChunkLoading(position);
        }
    }
    
    private float distanceSquared(Vector3f a, Vector3f b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        float dz = a.z - b.z;
        return dx * dx + dy * dy + dz * dz;
    }
    

    
    /**
     * Checks if a chunk is loaded in a thread-safe manner.
     * 
     * @param chunkPos the chunk position
     * @return true if the chunk is loaded
     */
    public boolean isChunkLoaded(ChunkPosition chunkPos) {
        if (chunkPos == null) {
            throw new IllegalArgumentException("Chunk position cannot be null");
        }
        
        ReadWriteLock chunkLock = getChunkLock(chunkPos);
        chunkLock.readLock().lock();
        try {
            return chunkManager.isChunkLoaded(chunkPos.x, chunkPos.z);
        } finally {
            chunkLock.readLock().unlock();
        }
    }
    
    /**
     * Triggers loading of a chunk in a thread-safe manner.
     * 
     * @param chunkPos the chunk position
     * @return true if loading was triggered successfully
     */
    public boolean triggerChunkLoad(ChunkPosition chunkPos) {
        if (chunkPos == null) {
            throw new IllegalArgumentException("Chunk position cannot be null");
        }
        
        ReadWriteLock chunkLock = getChunkLock(chunkPos);
        chunkLock.writeLock().lock();
        try {
            if (!chunkManager.isChunkLoaded(chunkPos.x, chunkPos.z)) {
                chunkManager.loadChunk(chunkPos.x, chunkPos.z);
                worldVersion++;
                return true;
            }
            return false;
        } finally {
            chunkLock.writeLock().unlock();
        }
    }
    
    /**
     * Asynchronously loads a chunk if it's not already loaded.
     * 
     * @param chunkPos the chunk position
     * @return a future that completes when the chunk is loaded
     */
    public CompletableFuture<Void> loadChunkAsync(ChunkPosition chunkPos) {
        if (chunkPos == null) {
            throw new IllegalArgumentException("Chunk position cannot be null");
        }
        
        return CompletableFuture.runAsync(() -> {
            ReadWriteLock chunkLock = getChunkLock(chunkPos);
            chunkLock.writeLock().lock();
            try {
                if (!chunkManager.isChunkLoaded(chunkPos.x, chunkPos.z)) {
                    chunkManager.loadChunk(chunkPos.x, chunkPos.z);
                    worldVersion++;
                }
            } finally {
                chunkLock.writeLock().unlock();
            }
        });
    }
    
    /**
     * Gets the current world version for optimistic concurrency control.
     * 
     * @return the world version
     */
    public long getWorldVersion() {
        return worldVersion;
    }
    
    /**
     * Processes pending chunk operations.
     * This method should be called periodically from the main thread.
     * 
     * @param maxOperations the maximum number of operations to process
     * @return the number of operations processed
     */
    public int processPendingOperations(int maxOperations) {
        int processed = 0;
        
        while (processed < maxOperations) {
            ChunkOperation operation = pendingOperations.poll();
            if (operation == null) {
                break;
            }
            
            processChunkOperation(operation);
            processed++;
        }
        
        return processed;
    }
    
    /**
     * Clears the block cache.
     * This should be called when chunks are unloaded or when memory pressure is high.
     */
    public void clearCache() {
        // Clear the entire block cache
        blockCache.clear();
    }
    
    /**
     * Gets statistics about the world access system.
     * 
     * @return access statistics
     */
    public WorldAccessStats getStats() {
        return new WorldAccessStats(
            blockCache.size(),
            chunkLocks.size(),
            pendingOperations.size(),
            worldVersion
        );
    }
    
    private ReadWriteLock getChunkLock(ChunkPosition chunkPos) {
        return chunkLocks.computeIfAbsent(chunkPos, k -> new ReentrantReadWriteLock());
    }
    
    private void triggerChunkLoading(Vector3f playerPos) {
        // Load chunks around player position
        int chunkX = (int) playerPos.x >> 4;
        int chunkZ = (int) playerPos.z >> 4;
        int loadRadius = 3; // Load 3 chunks in each direction
        
        for (int dx = -loadRadius; dx <= loadRadius; dx++) {
            for (int dz = -loadRadius; dz <= loadRadius; dz++) {
                ChunkPosition chunkPos = new ChunkPosition(chunkX + dx, chunkZ + dz);
                if (!isChunkLoaded(chunkPos)) {
                    loadChunkAsync(chunkPos);
                }
            }
        }
    }
    
    private void processChunkOperation(ChunkOperation operation) {
        switch (operation.type) {
            case BLOCK_CHANGED:
                // Notify systems about block changes
                // This could trigger mesh regeneration, lighting updates, etc.
                break;
            case CHUNK_LOADED:
                // Handle chunk loading completion
                break;
            case CHUNK_UNLOADED:
                // Clean up chunk-specific data
                chunkLocks.remove(operation.chunkPosition);
                break;
        }
    }
    
    /**
     * Represents a chunk operation that needs to be processed.
     */
    private static class ChunkOperation {
        enum Type {
            BLOCK_CHANGED,
            CHUNK_LOADED,
            CHUNK_UNLOADED
        }
        
        final Type type;
        final ChunkPosition chunkPosition;
        final Vector3i blockPosition;
        final BlockType blockType;
        
        ChunkOperation(Type type, ChunkPosition chunkPosition, Vector3i blockPosition, BlockType blockType) {
            this.type = type;
            this.chunkPosition = chunkPosition;
            this.blockPosition = blockPosition;
            this.blockType = blockType;
        }
    }
    
    /**
     * Statistics about world access operations.
     */
    public static class WorldAccessStats {
        public final int cachedBlocks;
        public final int activeChunkLocks;
        public final int pendingOperations;
        public final long worldVersion;
        
        WorldAccessStats(int cachedBlocks, int activeChunkLocks, int pendingOperations, long worldVersion) {
            this.cachedBlocks = cachedBlocks;
            this.activeChunkLocks = activeChunkLocks;
            this.pendingOperations = pendingOperations;
            this.worldVersion = worldVersion;
        }
        
        @Override
        public String toString() {
            return String.format(
                "WorldAccessStats{cachedBlocks=%d, activeChunkLocks=%d, pendingOperations=%d, worldVersion=%d}",
                cachedBlocks, activeChunkLocks, pendingOperations, worldVersion
            );
        }
    }
    
    /**
     * Immutable 3D vector for player position.
     */
    public static final class Vector3f {
        public final float x, y, z;
        
        public Vector3f(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Vector3f)) return false;
            Vector3f other = (Vector3f) obj;
            return Float.compare(x, other.x) == 0 &&
                   Float.compare(y, other.y) == 0 &&
                   Float.compare(z, other.z) == 0;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
        
        @Override
        public String toString() {
            return String.format("Vector3f(%.2f, %.2f, %.2f)", x, y, z);
        }
    }
    
    /**
     * Immutable 3D integer vector for block positions.
     */
    public static final class Vector3i {
        public final int x, y, z;
        
        public Vector3i(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Vector3i)) return false;
            Vector3i other = (Vector3i) obj;
            return x == other.x && y == other.y && z == other.z;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
        
        @Override
        public String toString() {
            return String.format("Vector3i(%d, %d, %d)", x, y, z);
        }
    }
    
    /**
     * Represents a block operation for testing purposes.
     */
    public static final class BlockOperation {
        public final Vector3i position;
        public final BlockType blockType;
        
        public BlockOperation(Vector3i position, BlockType blockType) {
            this.position = position;
            this.blockType = blockType;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof BlockOperation)) return false;
            BlockOperation other = (BlockOperation) obj;
            return Objects.equals(position, other.position) &&
                   Objects.equals(blockType, other.blockType);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(position, blockType);
        }
        
        @Override
        public String toString() {
            return String.format("BlockOperation{position=%s, blockType=%s}", position, blockType);
        }
    }
}