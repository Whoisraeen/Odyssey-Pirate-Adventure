package com.odyssey.world.chunk;

import com.odyssey.world.generation.WorldGenerator.BlockType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a 16x16x256 chunk of the world containing block data.
 * This class is thread-safe and optimized for concurrent read/write operations.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Thread-safe block access using read-write locks</li>
 *   <li>Memory-efficient storage using 3D arrays</li>
 *   <li>Dirty tracking for rendering optimization</li>
 *   <li>Atomic state management for loading/generation</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class Chunk {
    
    /** The height of a chunk in blocks. */
    public static final int CHUNK_HEIGHT = 256;
    
    /** The size of a chunk in blocks (16x16). */
    public static final int CHUNK_SIZE = ChunkPosition.CHUNK_SIZE;
    
    private final ChunkPosition position;
    private final BlockType[][][] blocks;
    
    // Thread safety
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // State tracking
    protected final AtomicBoolean isDirty = new AtomicBoolean(false);
    protected final AtomicBoolean isGenerated = new AtomicBoolean(false);
    protected final AtomicBoolean isLoaded = new AtomicBoolean(false);
    
    // Metadata
    protected volatile long lastAccessTime;
    protected volatile long lastModificationTime;
    
    /**
     * Creates a new chunk at the specified position.
     * 
     * @param position the chunk position
     */
    public Chunk(ChunkPosition position) {
        this.position = position;
        this.blocks = new BlockType[CHUNK_SIZE][CHUNK_HEIGHT][CHUNK_SIZE];
        this.lastAccessTime = System.currentTimeMillis();
        this.lastModificationTime = System.currentTimeMillis();
        
        // Initialize with air blocks
        initializeWithAir();
    }
    
    /**
     * Gets the block type at the specified local coordinates.
     * 
     * @param x the local x-coordinate (0-15)
     * @param y the y-coordinate (0-255)
     * @param z the local z-coordinate (0-15)
     * @return the block type
     * @throws IndexOutOfBoundsException if coordinates are invalid
     */
    public BlockType getBlock(int x, int y, int z) {
        validateCoordinates(x, y, z);
        
        lock.readLock().lock();
        try {
            lastAccessTime = System.currentTimeMillis();
            return blocks[x][y][z];
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Sets the block type at the specified local coordinates.
     * 
     * @param x the local x-coordinate (0-15)
     * @param y the y-coordinate (0-255)
     * @param z the local z-coordinate (0-15)
     * @param blockType the new block type
     * @throws IndexOutOfBoundsException if coordinates are invalid
     * @throws IllegalArgumentException if blockType is null
     */
    public void setBlock(int x, int y, int z, BlockType blockType) {
        if (blockType == null) {
            throw new IllegalArgumentException("Block type cannot be null");
        }
        validateCoordinates(x, y, z);
        
        lock.writeLock().lock();
        try {
            BlockType oldBlock = blocks[x][y][z];
            if (oldBlock != blockType) {
                blocks[x][y][z] = blockType;
                isDirty.set(true);
                lastModificationTime = System.currentTimeMillis();
                lastAccessTime = lastModificationTime;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the chunk position.
     * 
     * @return the chunk position
     */
    public ChunkPosition getPosition() {
        return position;
    }
    
    /**
     * Returns whether this chunk has been modified since the last render.
     * 
     * @return true if the chunk is dirty
     */
    public boolean isDirty() {
        return isDirty.get();
    }
    
    /**
     * Marks this chunk as clean (not dirty).
     * This should be called after the chunk has been rendered.
     */
    public void markClean() {
        isDirty.set(false);
    }
    
    /**
     * Clears the dirty flag for this chunk.
     * Alias for markClean() for consistency with other systems.
     */
    public void clearDirty() {
        isDirty.set(false);
    }
    
    /**
     * Returns whether this chunk has been generated.
     * 
     * @return true if the chunk is generated
     */
    public boolean isGenerated() {
        return isGenerated.get();
    }
    
    /**
     * Marks this chunk as generated.
     */
    public void markGenerated() {
        isGenerated.set(true);
    }
    
    /**
     * Returns whether this chunk is loaded.
     * 
     * @return true if the chunk is loaded
     */
    public boolean isLoaded() {
        return isLoaded.get();
    }
    
    /**
     * Marks this chunk as loaded.
     */
    public void markLoaded() {
        isLoaded.set(true);
    }
    
    /**
     * Marks this chunk as unloaded.
     */
    public void markUnloaded() {
        isLoaded.set(false);
    }
    
    /**
     * Gets the last access time in milliseconds.
     * 
     * @return the last access time
     */
    public long getLastAccessTime() {
        return lastAccessTime;
    }
    
    /**
     * Gets the last modification time in milliseconds.
     * 
     * @return the last modification time
     */
    public long getLastModificationTime() {
        return lastModificationTime;
    }
    
    /**
     * Checks if this chunk is empty (contains only air blocks).
     * This method acquires a read lock and may be expensive for large chunks.
     * 
     * @return true if the chunk contains only air blocks
     */
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int y = 0; y < CHUNK_HEIGHT; y++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        if (blocks[x][y][z] != BlockType.AIR) {
                            return false;
                        }
                    }
                }
            }
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets the highest non-air block at the specified local x,z coordinates.
     * 
     * @param x the local x-coordinate (0-15)
     * @param z the local z-coordinate (0-15)
     * @return the y-coordinate of the highest non-air block, or -1 if none found
     */
    public int getHighestBlock(int x, int z) {
        if (x < 0 || x >= CHUNK_SIZE || z < 0 || z >= CHUNK_SIZE) {
            throw new IndexOutOfBoundsException("Coordinates out of bounds: (" + x + ", " + z + ")");
        }
        
        lock.readLock().lock();
        try {
            for (int y = CHUNK_HEIGHT - 1; y >= 0; y--) {
                if (blocks[x][y][z] != BlockType.AIR) {
                    return y;
                }
            }
            return -1;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Fills a region of the chunk with the specified block type.
     * 
     * @param startX the starting local x-coordinate
     * @param startY the starting y-coordinate
     * @param startZ the starting local z-coordinate
     * @param endX the ending local x-coordinate (exclusive)
     * @param endY the ending y-coordinate (exclusive)
     * @param endZ the ending local z-coordinate (exclusive)
     * @param blockType the block type to fill with
     */
    public void fill(int startX, int startY, int startZ, int endX, int endY, int endZ, BlockType blockType) {
        if (blockType == null) {
            throw new IllegalArgumentException("Block type cannot be null");
        }
        
        // Clamp coordinates to chunk bounds
        startX = Math.max(0, Math.min(startX, CHUNK_SIZE));
        startY = Math.max(0, Math.min(startY, CHUNK_HEIGHT));
        startZ = Math.max(0, Math.min(startZ, CHUNK_SIZE));
        endX = Math.max(0, Math.min(endX, CHUNK_SIZE));
        endY = Math.max(0, Math.min(endY, CHUNK_HEIGHT));
        endZ = Math.max(0, Math.min(endZ, CHUNK_SIZE));
        
        lock.writeLock().lock();
        try {
            boolean modified = false;
            for (int x = startX; x < endX; x++) {
                for (int y = startY; y < endY; y++) {
                    for (int z = startZ; z < endZ; z++) {
                        if (blocks[x][y][z] != blockType) {
                            blocks[x][y][z] = blockType;
                            modified = true;
                        }
                    }
                }
            }
            
            if (modified) {
                isDirty.set(true);
                lastModificationTime = System.currentTimeMillis();
                lastAccessTime = lastModificationTime;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Creates a copy of this chunk's block data.
     * This method is expensive and should be used sparingly.
     * 
     * @return a copy of the block data array
     */
    public BlockType[][][] copyBlocks() {
        lock.readLock().lock();
        try {
            BlockType[][][] copy = new BlockType[CHUNK_SIZE][CHUNK_HEIGHT][CHUNK_SIZE];
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int y = 0; y < CHUNK_HEIGHT; y++) {
                    System.arraycopy(blocks[x][y], 0, copy[x][y], 0, CHUNK_SIZE);
                }
            }
            return copy;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Initializes the chunk with air blocks.
     */
    private void initializeWithAir() {
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_HEIGHT; y++) {
                for (int z = 0; z < CHUNK_SIZE; z++) {
                    blocks[x][y][z] = BlockType.AIR;
                }
            }
        }
    }
    
    /**
     * Validates that the given coordinates are within chunk bounds.
     * 
     * @param x the local x-coordinate
     * @param y the y-coordinate
     * @param z the local z-coordinate
     * @throws IndexOutOfBoundsException if coordinates are invalid
     */
    protected void validateCoordinates(int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_HEIGHT || z < 0 || z >= CHUNK_SIZE) {
            throw new IndexOutOfBoundsException(
                String.format("Coordinates out of bounds: (%d, %d, %d). Valid range: (0-15, 0-255, 0-15)", x, y, z)
            );
        }
    }
    
    @Override
    public String toString() {
        return String.format("Chunk{position=%s, generated=%s, loaded=%s, dirty=%s}", 
            position, isGenerated.get(), isLoaded.get(), isDirty.get());
    }
    
    /**
     * Gets detailed statistics about this chunk.
     * 
     * @return chunk statistics
     */
    public ChunkStats getStats() {
        lock.readLock().lock();
        try {
            int airBlocks = 0;
            int solidBlocks = 0;
            
            for (int x = 0; x < CHUNK_SIZE; x++) {
                for (int y = 0; y < CHUNK_HEIGHT; y++) {
                    for (int z = 0; z < CHUNK_SIZE; z++) {
                        if (blocks[x][y][z] == BlockType.AIR) {
                            airBlocks++;
                        } else {
                            solidBlocks++;
                        }
                    }
                }
            }
            
            return new ChunkStats(position, airBlocks, solidBlocks, lastAccessTime, lastModificationTime);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Statistics about a chunk.
     */
    public static class ChunkStats {
        public final ChunkPosition position;
        public final int airBlocks;
        public final int solidBlocks;
        public final long lastAccessTime;
        public final long lastModificationTime;
        
        ChunkStats(ChunkPosition position, int airBlocks, int solidBlocks, long lastAccessTime, long lastModificationTime) {
            this.position = position;
            this.airBlocks = airBlocks;
            this.solidBlocks = solidBlocks;
            this.lastAccessTime = lastAccessTime;
            this.lastModificationTime = lastModificationTime;
        }
        
        public int getTotalBlocks() {
            return airBlocks + solidBlocks;
        }
        
        public double getSolidBlockPercentage() {
            return (double) solidBlocks / getTotalBlocks() * 100.0;
        }
        
        @Override
        public String toString() {
            return String.format("ChunkStats{position=%s, solid=%d (%.1f%%), air=%d}", 
                position, solidBlocks, getSolidBlockPercentage(), airBlocks);
        }
    }
}