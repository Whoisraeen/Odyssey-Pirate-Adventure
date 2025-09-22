package com.odyssey.world;

import com.odyssey.util.Logger;
import com.odyssey.rendering.Mesh;
import com.odyssey.rendering.RenderCommand;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Represents a chunk of the world - a 16x16x256 section of blocks.
 * Handles block storage, mesh generation, and rendering optimization.
 */
public class Chunk {
    
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 256;
    public static final int BLOCKS_PER_CHUNK = CHUNK_SIZE * CHUNK_SIZE * CHUNK_HEIGHT;
    
    // Chunk coordinates (in chunk space, not world space)
    private final int chunkX;
    private final int chunkZ;
    
    // Block storage - using short array for memory efficiency
    private final short[] blocks;
    private final byte[] metadata;
    
    // Mesh data for rendering
    private Mesh solidMesh;
    private Mesh transparentMesh;
    private Mesh waterMesh;
    
    // State tracking
    private final AtomicBoolean needsRebuild = new AtomicBoolean(true);
    private final AtomicBoolean isGenerating = new AtomicBoolean(false);
    private final AtomicBoolean isBuilding = new AtomicBoolean(false);
    private boolean isEmpty = false;
    private boolean hasTransparency = false;
    private boolean hasWater = false;
    
    // Neighbor chunks for mesh generation
    private Chunk northChunk, southChunk, eastChunk, westChunk;
    
    // Lighting data
    private final byte[] lightLevels;
    
    // Statistics
    private int solidBlockCount = 0;
    private int transparentBlockCount = 0;
    private int waterBlockCount = 0;
    
    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blocks = new short[BLOCKS_PER_CHUNK];
        this.metadata = new byte[BLOCKS_PER_CHUNK];
        this.lightLevels = new byte[BLOCKS_PER_CHUNK];
        
        // Initialize with air blocks
        for (int i = 0; i < BLOCKS_PER_CHUNK; i++) {
            blocks[i] = (short) Block.BlockType.AIR.getId();
        }
        
        Logger.world("Created chunk at ({}, {})", chunkX, chunkZ);
    }
    
    /**
     * Gets the block at the specified local coordinates (0-15, 0-255, 0-15)
     */
    public Block.BlockType getBlock(int x, int y, int z) {
        if (!isValidLocalCoordinate(x, y, z)) {
            return Block.BlockType.AIR;
        }
        
        int index = getBlockIndex(x, y, z);
        return Block.BlockType.fromId(blocks[index]);
    }
    
    /**
     * Sets the block at the specified local coordinates
     */
    public void setBlock(int x, int y, int z, Block.BlockType blockType) {
        setBlock(x, y, z, blockType, (byte) 0);
    }
    
    /**
     * Sets the block at the specified local coordinates with metadata
     */
    public void setBlock(int x, int y, int z, Block.BlockType blockType, byte meta) {
        if (!isValidLocalCoordinate(x, y, z)) {
            return;
        }
        
        int index = getBlockIndex(x, y, z);
        Block.BlockType oldType = Block.BlockType.fromId(blocks[index]);
        
        // Update block data
        blocks[index] = (short) blockType.getId();
        metadata[index] = meta;
        
        // Update statistics
        updateBlockCounts(oldType, blockType);
        
        // Mark for rebuild
        markForRebuild();
        
        // Update neighboring chunks if on edge
        if (x == 0 && westChunk != null) westChunk.markForRebuild();
        if (x == CHUNK_SIZE - 1 && eastChunk != null) eastChunk.markForRebuild();
        if (z == 0 && northChunk != null) northChunk.markForRebuild();
        if (z == CHUNK_SIZE - 1 && southChunk != null) southChunk.markForRebuild();
    }
    
    /**
     * Gets the metadata for the block at the specified coordinates
     */
    public byte getMetadata(int x, int y, int z) {
        if (!isValidLocalCoordinate(x, y, z)) {
            return 0;
        }
        return metadata[getBlockIndex(x, y, z)];
    }
    
    /**
     * Gets the light level at the specified coordinates
     */
    public int getLightLevel(int x, int y, int z) {
        if (!isValidLocalCoordinate(x, y, z)) {
            return 15; // Full light outside chunk
        }
        return lightLevels[getBlockIndex(x, y, z)] & 0xFF;
    }
    
    /**
     * Sets the light level at the specified coordinates
     */
    public void setLightLevel(int x, int y, int z, int level) {
        if (!isValidLocalCoordinate(x, y, z)) {
            return;
        }
        lightLevels[getBlockIndex(x, y, z)] = (byte) Math.max(0, Math.min(15, level));
    }
    
    /**
     * Gets the sunlight level at the specified coordinates
     */
    public int getSunlight(int x, int y, int z) {
        if (!isValidLocalCoordinate(x, y, z)) {
            return 0;
        }
        // For now, sunlight is stored in the upper 4 bits of lightLevels
        return (lightLevels[getBlockIndex(x, y, z)] >> 4) & 0xF;
    }
    
    /**
     * Sets the sunlight level at the specified coordinates
     */
    public void setSunlight(int x, int y, int z, int level) {
        if (!isValidLocalCoordinate(x, y, z)) {
            return;
        }
        int index = getBlockIndex(x, y, z);
        int blocklight = lightLevels[index] & 0xF; // Preserve blocklight
        lightLevels[index] = (byte) ((Math.max(0, Math.min(15, level)) << 4) | blocklight);
    }
    
    /**
     * Gets the block light level at the specified coordinates
     */
    public int getBlocklight(int x, int y, int z) {
        if (!isValidLocalCoordinate(x, y, z)) {
            return 0;
        }
        // Block light is stored in the lower 4 bits of lightLevels
        return lightLevels[getBlockIndex(x, y, z)] & 0xF;
    }
    
    /**
     * Sets the block light level at the specified coordinates
     */
    public void setBlocklight(int x, int y, int z, int level) {
        if (!isValidLocalCoordinate(x, y, z)) {
            return;
        }
        int index = getBlockIndex(x, y, z);
        int sunlight = (lightLevels[index] >> 4) & 0xF; // Preserve sunlight
        lightLevels[index] = (byte) ((sunlight << 4) | Math.max(0, Math.min(15, level)));
    }
    
    /**
     * Converts local coordinates to array index
     */
    private int getBlockIndex(int x, int y, int z) {
        return y * CHUNK_SIZE * CHUNK_SIZE + z * CHUNK_SIZE + x;
    }
    
    /**
     * Validates local coordinates
     */
    private boolean isValidLocalCoordinate(int x, int y, int z) {
        return x >= 0 && x < CHUNK_SIZE && 
               y >= 0 && y < CHUNK_HEIGHT && 
               z >= 0 && z < CHUNK_SIZE;
    }
    
    /**
     * Updates block count statistics
     */
    private void updateBlockCounts(Block.BlockType oldType, Block.BlockType newType) {
        // Decrement old type count
        if (oldType.isSolid()) solidBlockCount--;
        else if (oldType.isTransparent() && oldType != Block.BlockType.AIR) transparentBlockCount--;
        if (oldType == Block.BlockType.WATER) waterBlockCount--;
        
        // Increment new type count
        if (newType.isSolid()) solidBlockCount++;
        else if (newType.isTransparent() && newType != Block.BlockType.AIR) transparentBlockCount++;
        if (newType == Block.BlockType.WATER) waterBlockCount++;
        
        // Update flags
        isEmpty = (solidBlockCount + transparentBlockCount + waterBlockCount) == 0;
        hasTransparency = transparentBlockCount > 0;
        hasWater = waterBlockCount > 0;
    }
    
    /**
     * Marks this chunk for mesh rebuild
     */
    public void markForRebuild() {
        needsRebuild.set(true);
    }
    
    /**
     * Checks if this chunk needs a mesh rebuild
     */
    public boolean needsRebuild() {
        return needsRebuild.get();
    }
    
    /**
     * Builds the chunk mesh for rendering
     */
    public void buildMesh() {
        if (!needsRebuild.get() || isBuilding.get()) {
            return;
        }
        
        isBuilding.set(true);
        
        try {
            // Clear existing meshes
            if (solidMesh != null) solidMesh.cleanup();
            if (transparentMesh != null) transparentMesh.cleanup();
            if (waterMesh != null) waterMesh.cleanup();
            
            // Generate new meshes
            ChunkMeshBuilder builder = new ChunkMeshBuilder(this);
            ChunkMeshBuilder.MeshData meshData = builder.generateMesh();
            
            solidMesh = meshData.solidMesh;
            transparentMesh = meshData.transparentMesh;
            waterMesh = meshData.waterMesh;
            
            needsRebuild.set(false);
            
            Logger.logInfo("Built mesh for chunk ({}, {}) - Solid: {}, Transparent: {}, Water: {}", 
                       chunkX, chunkZ, 
                       solidMesh != null ? solidMesh.getVertexCount() : 0,
                       transparentMesh != null ? transparentMesh.getVertexCount() : 0,
                       waterMesh != null ? waterMesh.getVertexCount() : 0);
            
        } catch (Exception e) {
            Logger.logError("Failed to build mesh for chunk ({}, {}): {}", chunkX, chunkZ, e.getMessage());
        } finally {
            isBuilding.set(false);
        }
    }
    
    /**
     * Gets render commands for this chunk
     */
    public List<RenderCommand> getRenderCommands() {
        List<RenderCommand> commands = new ArrayList<>();
        
        Vector3f chunkWorldPos = new Vector3f(chunkX * CHUNK_SIZE, 0, chunkZ * CHUNK_SIZE);
        
        // Solid geometry
        if (solidMesh != null && solidMesh.getVertexCount() > 0) {
            RenderCommand solidCommand = new RenderCommand.Builder()
                .mesh(solidMesh)
                .position(chunkWorldPos)
                .queue(RenderCommand.RenderQueue.OPAQUE)
                .build();
            commands.add(solidCommand);
        }
        
        // Transparent geometry
        if (transparentMesh != null && transparentMesh.getVertexCount() > 0) {
            RenderCommand transparentCommand = new RenderCommand.Builder()
                .mesh(transparentMesh)
                .position(chunkWorldPos)
                .queue(RenderCommand.RenderQueue.TRANSPARENT)
                .build();
            commands.add(transparentCommand);
        }
        
        // Water geometry
        if (waterMesh != null && waterMesh.getVertexCount() > 0) {
            RenderCommand waterCommand = new RenderCommand.Builder()
                .mesh(waterMesh)
                .position(chunkWorldPos)
                .queue(RenderCommand.RenderQueue.TRANSPARENT)
                .build();
            commands.add(waterCommand);
        }
        
        return commands;
    }
    
    /**
     * Sets neighbor chunks for mesh generation
     */
    public void setNeighbors(Chunk north, Chunk south, Chunk east, Chunk west) {
        this.northChunk = north;
        this.southChunk = south;
        this.eastChunk = east;
        this.westChunk = west;
    }
    
    /**
     * Gets a neighbor block (may be from adjacent chunk)
     */
    public Block.BlockType getNeighborBlock(int x, int y, int z) {
        if (isValidLocalCoordinate(x, y, z)) {
            return getBlock(x, y, z);
        }
        
        // Check adjacent chunks
        if (x < 0 && westChunk != null) {
            return westChunk.getBlock(CHUNK_SIZE - 1, y, z);
        }
        if (x >= CHUNK_SIZE && eastChunk != null) {
            return eastChunk.getBlock(0, y, z);
        }
        if (z < 0 && northChunk != null) {
            return northChunk.getBlock(x, y, CHUNK_SIZE - 1);
        }
        if (z >= CHUNK_SIZE && southChunk != null) {
            return southChunk.getBlock(x, y, 0);
        }
        
        return Block.BlockType.AIR;
    }
    
    /**
     * Calculates lighting for this chunk
     */
    public void calculateLighting() {
        // Reset lighting
        for (int i = 0; i < lightLevels.length; i++) {
            lightLevels[i] = 0;
        }
        
        // Calculate sunlight (top-down)
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                int lightLevel = 15; // Full sunlight at top
                
                for (int y = CHUNK_HEIGHT - 1; y >= 0; y--) {
                    Block.BlockType block = getBlock(x, y, z);
                    
                    if (block == Block.BlockType.AIR) {
                        setLightLevel(x, y, z, lightLevel);
                    } else {
                        lightLevel = Math.max(0, lightLevel - block.getLightAbsorption());
                        setLightLevel(x, y, z, Math.max(lightLevel, block.getLightLevel()));
                    }
                }
            }
        }
        
        // TODO: Implement block light propagation
        markForRebuild();
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        if (solidMesh != null) {
            solidMesh.cleanup();
            solidMesh = null;
        }
        if (transparentMesh != null) {
            transparentMesh.cleanup();
            transparentMesh = null;
        }
        if (waterMesh != null) {
            waterMesh.cleanup();
            waterMesh = null;
        }
        
        Logger.world("Cleaned up chunk ({}, {})", chunkX, chunkZ);
    }
    
    // Getters
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public boolean isEmpty() { return isEmpty; }
    public boolean hasTransparency() { return hasTransparency; }
    public boolean hasWater() { return hasWater; }
    public boolean isGenerating() { return isGenerating.get(); }
    public boolean isBuilding() { return isBuilding.get(); }
    
    public int getSolidBlockCount() { return solidBlockCount; }
    public int getTransparentBlockCount() { return transparentBlockCount; }
    public int getWaterBlockCount() { return waterBlockCount; }
    
    /**
     * Gets the solid mesh for rendering
     */
    public Mesh getMesh() {
        return solidMesh;
    }
    
    /**
     * Gets the model matrix for this chunk's world position
     */
    public org.joml.Matrix4f getModelMatrix() {
        org.joml.Matrix4f matrix = new org.joml.Matrix4f();
        matrix.translation(chunkX * CHUNK_SIZE, 0, chunkZ * CHUNK_SIZE);
        return matrix;
    }
    
    /**
     * Gets world coordinates for this chunk
     */
    public Vector3i getWorldPosition() {
        return new Vector3i(chunkX * CHUNK_SIZE, 0, chunkZ * CHUNK_SIZE);
    }
    
    /**
     * Converts world coordinates to local chunk coordinates
     */
    public static Vector3i worldToLocal(int worldX, int worldZ) {
        int chunkX = Math.floorDiv(worldX, CHUNK_SIZE);
        int chunkZ = Math.floorDiv(worldZ, CHUNK_SIZE);
        int localX = worldX - chunkX * CHUNK_SIZE;
        int localZ = worldZ - chunkZ * CHUNK_SIZE;
        return new Vector3i(localX, 0, localZ);
    }
    
    /**
     * Gets chunk coordinates from world coordinates
     */
    public static Vector3i getChunkCoords(int worldX, int worldZ) {
        return new Vector3i(Math.floorDiv(worldX, CHUNK_SIZE), 0, Math.floorDiv(worldZ, CHUNK_SIZE));
    }
    
    @Override
    public String toString() {
        return String.format("Chunk{pos=(%d,%d), blocks=%d, needsRebuild=%s}", 
                           chunkX, chunkZ, solidBlockCount + transparentBlockCount + waterBlockCount, needsRebuild.get());
    }
}