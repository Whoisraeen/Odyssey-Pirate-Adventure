package com.odyssey.world.chunk;

import com.odyssey.world.generation.WorldGenerator.BlockType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Represents a vertical column of chunks in the world (32x256x32 blocks).
 * ChunkColumns optimize memory usage and world generation by grouping chunks
 * that share the same x,z coordinates but different y levels.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Vertical chunk organization for better memory locality</li>
 *   <li>Shared biome and height map data across vertical chunks</li>
 *   <li>Optimized block access patterns for vertical operations</li>
 *   <li>Thread-safe operations with minimal locking</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class ChunkColumn {
    private static final Logger logger = LoggerFactory.getLogger(ChunkColumn.class);
    
    /** The width and depth of a chunk column in blocks (32x32). */
    public static final int COLUMN_SIZE = 32;
    
    /** The height of a chunk column in blocks (256). */
    public static final int COLUMN_HEIGHT = 256;
    
    /** Number of chunks per column vertically. */
    public static final int CHUNKS_PER_COLUMN = COLUMN_HEIGHT / Chunk.CHUNK_HEIGHT;
    
    private final ChunkPosition columnPosition;
    private final ConcurrentHashMap<Integer, Chunk> chunks;
    
    // Shared column data
    private final int[][] heightMap;
    private final byte[][] biomeMap;
    
    // Thread safety
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // State tracking
    private final AtomicBoolean isGenerated = new AtomicBoolean(false);
    private final AtomicBoolean isDirty = new AtomicBoolean(false);
    private volatile long lastAccessTime;
    
    /**
     * Creates a new chunk column at the specified position.
     * 
     * @param columnPosition the column position (chunk coordinates)
     */
    public ChunkColumn(ChunkPosition columnPosition) {
        this.columnPosition = columnPosition;
        this.chunks = new ConcurrentHashMap<>();
        this.heightMap = new int[COLUMN_SIZE][COLUMN_SIZE];
        this.biomeMap = new byte[COLUMN_SIZE][COLUMN_SIZE];
        this.lastAccessTime = System.currentTimeMillis();
        
        // Initialize height map to sea level
        for (int x = 0; x < COLUMN_SIZE; x++) {
            for (int z = 0; z < COLUMN_SIZE; z++) {
                heightMap[x][z] = 64; // Sea level
                biomeMap[x][z] = 0; // Default biome
            }
        }
    }
    
    /**
     * Gets the block type at the specified local coordinates.
     * 
     * @param x the local x-coordinate (0-31)
     * @param y the y-coordinate (0-255)
     * @param z the local z-coordinate (0-31)
     * @return the block type
     * @throws IndexOutOfBoundsException if coordinates are invalid
     */
    public BlockType getBlock(int x, int y, int z) {
        validateCoordinates(x, y, z);
        
        int chunkY = y / Chunk.CHUNK_HEIGHT;
        Chunk chunk = chunks.get(chunkY);
        
        if (chunk != null) {
            int localY = y % Chunk.CHUNK_HEIGHT;
            lastAccessTime = System.currentTimeMillis();
            return chunk.getBlock(x, localY, z);
        }
        
        return BlockType.AIR;
    }
    
    /**
     * Sets the block type at the specified local coordinates.
     * 
     * @param x the local x-coordinate (0-31)
     * @param y the y-coordinate (0-255)
     * @param z the local z-coordinate (0-31)
     * @param blockType the new block type
     * @throws IndexOutOfBoundsException if coordinates are invalid
     * @throws IllegalArgumentException if blockType is null
     */
    public void setBlock(int x, int y, int z, BlockType blockType) {
        if (blockType == null) {
            throw new IllegalArgumentException("Block type cannot be null");
        }
        validateCoordinates(x, y, z);
        
        int chunkY = y / Chunk.CHUNK_HEIGHT;
        Chunk chunk = getOrCreateChunk(chunkY);
        
        int localY = y % Chunk.CHUNK_HEIGHT;
        chunk.setBlock(x, localY, z, blockType);
        
        isDirty.set(true);
        lastAccessTime = System.currentTimeMillis();
    }
    
    /**
     * Gets the height at the specified local x,z coordinates.
     * 
     * @param x the local x-coordinate (0-31)
     * @param z the local z-coordinate (0-31)
     * @return the terrain height
     */
    public int getHeight(int x, int z) {
        if (x < 0 || x >= COLUMN_SIZE || z < 0 || z >= COLUMN_SIZE) {
            throw new IndexOutOfBoundsException("Coordinates out of bounds: (" + x + ", " + z + ")");
        }
        
        lock.readLock().lock();
        try {
            return heightMap[x][z];
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Sets the height at the specified local x,z coordinates.
     * 
     * @param x the local x-coordinate (0-31)
     * @param z the local z-coordinate (0-31)
     * @param height the terrain height
     */
    public void setHeight(int x, int z, int height) {
        if (x < 0 || x >= COLUMN_SIZE || z < 0 || z >= COLUMN_SIZE) {
            throw new IndexOutOfBoundsException("Coordinates out of bounds: (" + x + ", " + z + ")");
        }
        
        lock.writeLock().lock();
        try {
            heightMap[x][z] = height;
            isDirty.set(true);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the biome at the specified local x,z coordinates.
     * 
     * @param x the local x-coordinate (0-31)
     * @param z the local z-coordinate (0-31)
     * @return the biome ID
     */
    public byte getBiome(int x, int z) {
        if (x < 0 || x >= COLUMN_SIZE || z < 0 || z >= COLUMN_SIZE) {
            throw new IndexOutOfBoundsException("Coordinates out of bounds: (" + x + ", " + z + ")");
        }
        
        lock.readLock().lock();
        try {
            return biomeMap[x][z];
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Sets the biome at the specified local x,z coordinates.
     * 
     * @param x the local x-coordinate (0-31)
     * @param z the local z-coordinate (0-31)
     * @param biomeId the biome ID
     */
    public void setBiome(int x, int z, byte biomeId) {
        if (x < 0 || x >= COLUMN_SIZE || z < 0 || z >= COLUMN_SIZE) {
            throw new IndexOutOfBoundsException("Coordinates out of bounds: (" + x + ", " + z + ")");
        }
        
        lock.writeLock().lock();
        try {
            biomeMap[x][z] = biomeId;
            isDirty.set(true);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets or creates a chunk at the specified vertical level.
     * 
     * @param chunkY the vertical chunk index (0 = bottom chunk)
     * @return the chunk at the specified level
     */
    public Chunk getOrCreateChunk(int chunkY) {
        if (chunkY < 0 || chunkY >= CHUNKS_PER_COLUMN) {
            throw new IndexOutOfBoundsException("Chunk Y out of bounds: " + chunkY);
        }
        
        return chunks.computeIfAbsent(chunkY, y -> {
            ChunkPosition chunkPos = new ChunkPosition(columnPosition.x, columnPosition.z);
            return new Chunk(chunkPos);
        });
    }
    
    /**
     * Gets a chunk at the specified vertical level.
     * 
     * @param chunkY the vertical chunk index
     * @return the chunk, or null if not loaded
     */
    public Chunk getChunk(int chunkY) {
        if (chunkY < 0 || chunkY >= CHUNKS_PER_COLUMN) {
            return null;
        }
        return chunks.get(chunkY);
    }
    
    /**
     * Unloads a chunk at the specified vertical level.
     * 
     * @param chunkY the vertical chunk index
     * @return true if the chunk was unloaded
     */
    public boolean unloadChunk(int chunkY) {
        Chunk removed = chunks.remove(chunkY);
        if (removed != null) {
            logger.debug("Unloaded chunk at column {} level {}", columnPosition, chunkY);
            return true;
        }
        return false;
    }
    
    /**
     * Gets the column position.
     * 
     * @return the column position
     */
    public ChunkPosition getPosition() {
        return columnPosition;
    }
    
    /**
     * Returns whether this column has been generated.
     * 
     * @return true if the column is generated
     */
    public boolean isGenerated() {
        return isGenerated.get();
    }
    
    /**
     * Marks this column as generated.
     */
    public void markGenerated() {
        isGenerated.set(true);
    }
    
    /**
     * Returns whether this column is dirty.
     * 
     * @return true if the column is dirty
     */
    public boolean isDirty() {
        return isDirty.get();
    }
    
    /**
     * Marks this column as clean.
     */
    public void markClean() {
        isDirty.set(false);
    }
    
    /**
     * Gets the last access time.
     * 
     * @return the last access time in milliseconds
     */
    public long getLastAccessTime() {
        return lastAccessTime;
    }
    
    /**
     * Gets the number of loaded chunks in this column.
     * 
     * @return the number of loaded chunks
     */
    public int getLoadedChunkCount() {
        return chunks.size();
    }
    
    /**
     * Validates that the given coordinates are within column bounds.
     * 
     * @param x the local x-coordinate
     * @param y the y-coordinate
     * @param z the local z-coordinate
     * @throws IndexOutOfBoundsException if coordinates are invalid
     */
    private void validateCoordinates(int x, int y, int z) {
        if (x < 0 || x >= COLUMN_SIZE || y < 0 || y >= COLUMN_HEIGHT || z < 0 || z >= COLUMN_SIZE) {
            throw new IndexOutOfBoundsException(
                String.format("Coordinates out of bounds: (%d, %d, %d). Valid range: (0-31, 0-255, 0-31)", x, y, z)
            );
        }
    }
    
    @Override
    public String toString() {
        return String.format("ChunkColumn{position=%s, chunks=%d, generated=%s, dirty=%s}", 
            columnPosition, chunks.size(), isGenerated.get(), isDirty.get());
    }
}