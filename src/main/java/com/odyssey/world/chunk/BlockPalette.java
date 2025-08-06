package com.odyssey.world.chunk;

import com.odyssey.world.generation.WorldGenerator.BlockType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A memory-efficient block palette that maps block types to indices.
 * This system dramatically reduces memory usage for chunks by storing
 * only unique block types and referencing them by small indices.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Automatic palette expansion when new block types are added</li>
 *   <li>Compact storage using the smallest possible index size</li>
 *   <li>Thread-safe operations for concurrent access</li>
 *   <li>Optimized for chunks with few unique block types</li>
 * </ul>
 * 
 * <p>Memory savings example:
 * - Without palette: 65,536 blocks × 4 bytes = 262,144 bytes
 * - With palette (8 unique types): 65,536 blocks × 1 byte + 8 types × 4 bytes = 65,568 bytes
 * - Memory reduction: ~75%
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class BlockPalette {
    
    /** Maximum number of block types that can be stored in this palette. */
    public static final int MAX_PALETTE_SIZE = 256;
    
    private final List<BlockType> palette;
    private final Map<BlockType, Integer> blockToIndex;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    /**
     * Creates a new block palette with AIR as the default block type.
     */
    public BlockPalette() {
        this.palette = new ArrayList<>();
        this.blockToIndex = new HashMap<>();
        
        // Always start with AIR at index 0
        palette.add(BlockType.AIR);
        blockToIndex.put(BlockType.AIR, 0);
    }
    
    /**
     * Creates a new block palette with the specified initial block types.
     * 
     * @param initialBlocks the initial block types to add to the palette
     * @throws IllegalArgumentException if initialBlocks is null or contains null values
     */
    public BlockPalette(List<BlockType> initialBlocks) {
        if (initialBlocks == null) {
            throw new IllegalArgumentException("Initial blocks cannot be null");
        }
        
        this.palette = new ArrayList<>();
        this.blockToIndex = new HashMap<>();
        
        // Always start with AIR at index 0
        if (!initialBlocks.contains(BlockType.AIR)) {
            palette.add(BlockType.AIR);
            blockToIndex.put(BlockType.AIR, 0);
        }
        
        // Add initial blocks
        for (BlockType blockType : initialBlocks) {
            if (blockType == null) {
                throw new IllegalArgumentException("Block type cannot be null");
            }
            addBlockType(blockType);
        }
    }
    
    /**
     * Gets the index for the specified block type, adding it to the palette if necessary.
     * 
     * @param blockType the block type to get the index for
     * @return the index of the block type in the palette
     * @throws IllegalArgumentException if blockType is null
     * @throws IllegalStateException if the palette is full and cannot accommodate new block types
     */
    public int getIndex(BlockType blockType) {
        if (blockType == null) {
            throw new IllegalArgumentException("Block type cannot be null");
        }
        
        lock.readLock().lock();
        try {
            Integer index = blockToIndex.get(blockType);
            if (index != null) {
                return index;
            }
        } finally {
            lock.readLock().unlock();
        }
        
        // Block type not found, need to add it
        lock.writeLock().lock();
        try {
            // Double-check in case another thread added it
            Integer index = blockToIndex.get(blockType);
            if (index != null) {
                return index;
            }
            
            // Check if palette is full
            if (palette.size() >= MAX_PALETTE_SIZE) {
                throw new IllegalStateException("Palette is full, cannot add more block types");
            }
            
            // Add new block type
            int newIndex = palette.size();
            palette.add(blockType);
            blockToIndex.put(blockType, newIndex);
            return newIndex;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the block type for the specified index.
     * 
     * @param index the index in the palette
     * @return the block type at the specified index
     * @throws IndexOutOfBoundsException if the index is invalid
     */
    public BlockType getBlockType(int index) {
        lock.readLock().lock();
        try {
            if (index < 0 || index >= palette.size()) {
                throw new IndexOutOfBoundsException("Index out of bounds: " + index);
            }
            return palette.get(index);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Adds a block type to the palette if it's not already present.
     * 
     * @param blockType the block type to add
     * @return the index of the block type in the palette
     * @throws IllegalArgumentException if blockType is null
     * @throws IllegalStateException if the palette is full
     */
    public int addBlockType(BlockType blockType) {
        return getIndex(blockType); // getIndex handles adding if not present
    }
    
    /**
     * Gets the number of unique block types in this palette.
     * 
     * @return the palette size
     */
    public int size() {
        lock.readLock().lock();
        try {
            return palette.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Checks if the palette contains the specified block type.
     * 
     * @param blockType the block type to check for
     * @return true if the palette contains the block type
     */
    public boolean contains(BlockType blockType) {
        lock.readLock().lock();
        try {
            return blockToIndex.containsKey(blockType);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets the number of bits required to represent all indices in this palette.
     * This is useful for determining the optimal storage format.
     * 
     * @return the number of bits required (1, 2, 4, or 8)
     */
    public int getBitsPerIndex() {
        int size = size();
        if (size <= 2) return 1;
        if (size <= 4) return 2;
        if (size <= 16) return 4;
        return 8;
    }
    
    /**
     * Creates a copy of this palette.
     * 
     * @return a new palette with the same block types
     */
    public BlockPalette copy() {
        lock.readLock().lock();
        try {
            return new BlockPalette(new ArrayList<>(palette));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets all block types in this palette.
     * 
     * @return a list of all block types in palette order
     */
    public List<BlockType> getAllBlockTypes() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(palette);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Optimizes the palette by removing unused block types.
     * This method should be called with a usage map indicating which indices are actually used.
     * 
     * @param usedIndices a boolean array indicating which palette indices are used
     * @return a mapping from old indices to new indices
     */
    public Map<Integer, Integer> optimize(boolean[] usedIndices) {
        if (usedIndices.length != size()) {
            throw new IllegalArgumentException("Usage array size must match palette size");
        }
        
        lock.writeLock().lock();
        try {
            List<BlockType> newPalette = new ArrayList<>();
            Map<BlockType, Integer> newBlockToIndex = new HashMap<>();
            Map<Integer, Integer> indexMapping = new HashMap<>();
            
            int newIndex = 0;
            for (int oldIndex = 0; oldIndex < palette.size(); oldIndex++) {
                if (usedIndices[oldIndex]) {
                    BlockType blockType = palette.get(oldIndex);
                    newPalette.add(blockType);
                    newBlockToIndex.put(blockType, newIndex);
                    indexMapping.put(oldIndex, newIndex);
                    newIndex++;
                }
            }
            
            // Update palette
            palette.clear();
            palette.addAll(newPalette);
            blockToIndex.clear();
            blockToIndex.putAll(newBlockToIndex);
            
            return indexMapping;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Calculates the memory usage of this palette in bytes.
     * 
     * @return the estimated memory usage in bytes
     */
    public long getMemoryUsage() {
        lock.readLock().lock();
        try {
            // Palette list: each BlockType reference = 8 bytes
            long paletteMemory = palette.size() * 8L;
            
            // Map: each entry = key (8 bytes) + value (4 bytes) + overhead (~32 bytes)
            long mapMemory = blockToIndex.size() * 44L;
            
            return paletteMemory + mapMemory;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return String.format("BlockPalette{size=%d, bitsPerIndex=%d, types=%s}", 
                size(), getBitsPerIndex(), palette);
        } finally {
            lock.readLock().unlock();
        }
    }
}