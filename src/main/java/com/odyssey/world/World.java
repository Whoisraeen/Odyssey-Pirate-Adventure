package com.odyssey.world;

import com.odyssey.core.GameConfig;
import com.odyssey.util.Logger;
import com.odyssey.rendering.Camera;
import com.odyssey.rendering.RenderCommand;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.concurrent.*;

/**
 * Manages the entire voxel world, including chunk loading, unloading, and generation.
 * Handles world streaming around the player and provides block access methods.
 */
public class World {
    
    private final Map<Vector2i, Chunk> loadedChunks = new ConcurrentHashMap<>();
    private final Set<Vector2i> chunksToLoad = ConcurrentHashMap.newKeySet();
    private final Set<Vector2i> chunksToUnload = ConcurrentHashMap.newKeySet();
    private final Set<Vector2i> chunksToRebuild = ConcurrentHashMap.newKeySet();
    
    // World generation
    private final WorldGenerator worldGenerator;
    
    // Threading
    private final ExecutorService chunkLoadingExecutor;
    private final ExecutorService meshBuildingExecutor;
    
    // World settings
    private final int renderDistance;
    private final int loadDistance;
    private final int maxChunksPerFrame;
    
    // Player tracking
    private Vector3f playerPosition = new Vector3f(0, 64, 0);
    private Vector2i currentChunk = new Vector2i(0, 0);
    private Vector2i lastChunk = new Vector2i(Integer.MAX_VALUE, Integer.MAX_VALUE);
    
    // Statistics
    private int totalChunksLoaded = 0;
    private int chunksLoadedThisFrame = 0;
    private int chunksUnloadedThisFrame = 0;
    private int meshesBuiltThisFrame = 0;
    
    // World properties
    private final String worldName;
    private final long worldSeed;
    
    public World(String worldName, long worldSeed) {
        this.worldName = worldName;
        this.worldSeed = worldSeed;
        
        // Load settings from config
        GameConfig config = GameConfig.getInstance();
        this.renderDistance = config.getInt("world.render_distance", 8);
        this.loadDistance = renderDistance + 2;
        this.maxChunksPerFrame = config.getInt("world.max_chunks_per_frame", 4);
        
        // Initialize world generator
        this.worldGenerator = new WorldGenerator(worldSeed);
        
        // Initialize thread pools
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        this.chunkLoadingExecutor = Executors.newFixedThreadPool(threadCount, 
            r -> new Thread(r, "ChunkLoader-" + Thread.currentThread().getId()));
        this.meshBuildingExecutor = Executors.newFixedThreadPool(threadCount,
            r -> new Thread(r, "MeshBuilder-" + Thread.currentThread().getId()));
        
        Logger.world("Created world '{}' with seed {} (render distance: {}, threads: {})", 
                   worldName, worldSeed, renderDistance, threadCount);
    }
    
    /**
     * Updates the world, handling chunk loading/unloading and mesh building
     */
    public void update(Camera camera) {
        // Update player position
        Vector3f cameraPos = camera.getPosition();
        playerPosition.set(cameraPos);
        
        // Calculate current chunk
        Vector2i newChunk = new Vector2i(
            Math.floorDiv((int) playerPosition.x, Chunk.CHUNK_SIZE),
            Math.floorDiv((int) playerPosition.z, Chunk.CHUNK_SIZE)
        );
        
        // Check if player moved to a different chunk
        if (!newChunk.equals(currentChunk)) {
            currentChunk.set(newChunk);
            updateChunkLoading();
        }
        
        // Reset frame counters
        chunksLoadedThisFrame = 0;
        chunksUnloadedThisFrame = 0;
        meshesBuiltThisFrame = 0;
        
        // Process chunk operations
        processChunkUnloading();
        processChunkLoading();
        processMeshBuilding();
        
        // Update statistics
        totalChunksLoaded = loadedChunks.size();
    }
    
    /**
     * Updates which chunks should be loaded/unloaded
     */
    private void updateChunkLoading() {
        Set<Vector2i> shouldBeLoaded = new HashSet<>();
        
        // Calculate chunks that should be loaded
        for (int x = currentChunk.x - loadDistance; x <= currentChunk.x + loadDistance; x++) {
            for (int z = currentChunk.y - loadDistance; z <= currentChunk.y + loadDistance; z++) {
                Vector2i chunkPos = new Vector2i(x, z);
                double distance = chunkPos.distance(currentChunk);
                
                if (distance <= loadDistance) {
                    shouldBeLoaded.add(new Vector2i(chunkPos));
                }
            }
        }
        
        // Find chunks to load
        chunksToLoad.clear();
        for (Vector2i chunkPos : shouldBeLoaded) {
            if (!loadedChunks.containsKey(chunkPos)) {
                chunksToLoad.add(chunkPos);
            }
        }
        
        // Find chunks to unload
        chunksToUnload.clear();
        for (Vector2i chunkPos : loadedChunks.keySet()) {
            if (!shouldBeLoaded.contains(chunkPos)) {
                chunksToUnload.add(chunkPos);
            }
        }
        
        Logger.world("Chunk update: {} to load, {} to unload, {} loaded", 
                   chunksToLoad.size(), chunksToUnload.size(), loadedChunks.size());
    }
    
    /**
     * Processes chunk loading
     */
    private void processChunkLoading() {
        if (chunksToLoad.isEmpty() || chunksLoadedThisFrame >= maxChunksPerFrame) {
            return;
        }
        
        // Sort chunks by distance to player
        List<Vector2i> sortedChunks = new ArrayList<>(chunksToLoad);
        sortedChunks.sort((a, b) -> {
            double distA = a.distance(currentChunk);
            double distB = b.distance(currentChunk);
            return Double.compare(distA, distB);
        });
        
        // Load closest chunks first
        for (Vector2i chunkPos : sortedChunks) {
            if (chunksLoadedThisFrame >= maxChunksPerFrame) {
                break;
            }
            
            loadChunk(chunkPos);
            chunksToLoad.remove(chunkPos);
            chunksLoadedThisFrame++;
        }
    }
    
    /**
     * Processes chunk unloading
     */
    private void processChunkUnloading() {
        for (Vector2i chunkPos : chunksToUnload) {
            unloadChunk(chunkPos);
            chunksUnloadedThisFrame++;
        }
        chunksToUnload.clear();
    }
    
    /**
     * Processes mesh building for chunks that need it
     */
    private void processMeshBuilding() {
        for (Chunk chunk : loadedChunks.values()) {
            if (chunk.needsRebuild() && !chunk.isBuilding() && meshesBuiltThisFrame < maxChunksPerFrame) {
                buildChunkMesh(chunk);
                meshesBuiltThisFrame++;
            }
        }
    }
    
    /**
     * Loads a chunk at the specified position
     */
    private void loadChunk(Vector2i chunkPos) {
        if (loadedChunks.containsKey(chunkPos)) {
            return;
        }
        
        Chunk chunk = new Chunk(chunkPos.x, chunkPos.y);
        loadedChunks.put(new Vector2i(chunkPos), chunk);
        
        // Generate chunk data asynchronously
        chunkLoadingExecutor.submit(() -> {
            try {
                // Get the world chunk from the generator
                WorldChunk worldChunk = worldGenerator.getChunk(chunkPos.x, chunkPos.y);
                
                // TODO: Convert WorldChunk data to Chunk format
                // For now, just calculate lighting on the empty chunk
                chunk.calculateLighting();
                
                // Set up neighbors
                updateChunkNeighbors(chunk);
                
                Logger.world("Generated chunk ({}, {})", chunkPos.x, chunkPos.y);
            } catch (Exception e) {
                Logger.logError("Failed to generate chunk ({}, {}): {}", chunkPos.x, chunkPos.y, e.getMessage());
            }
        });
    }
    
    /**
     * Unloads a chunk at the specified position
     */
    private void unloadChunk(Vector2i chunkPos) {
        Chunk chunk = loadedChunks.remove(chunkPos);
        if (chunk != null) {
            chunk.cleanup();
            Logger.world("Unloaded chunk ({}, {})", chunkPos.x, chunkPos.y);
        }
    }
    
    /**
     * Builds mesh for a chunk asynchronously
     */
    private void buildChunkMesh(Chunk chunk) {
        meshBuildingExecutor.submit(() -> {
            try {
                chunk.buildMesh();
            } catch (Exception e) {
                Logger.logError("Failed to build mesh for chunk ({}, {}): {}", 
                           chunk.getChunkX(), chunk.getChunkZ(), e.getMessage());
            }
        });
    }
    
    /**
     * Updates neighbor references for a chunk
     */
    private void updateChunkNeighbors(Chunk chunk) {
        int x = chunk.getChunkX();
        int z = chunk.getChunkZ();
        
        Chunk north = loadedChunks.get(new Vector2i(x, z - 1));
        Chunk south = loadedChunks.get(new Vector2i(x, z + 1));
        Chunk east = loadedChunks.get(new Vector2i(x + 1, z));
        Chunk west = loadedChunks.get(new Vector2i(x - 1, z));
        
        chunk.setNeighbors(north, south, east, west);
        
        // Update neighbors to reference this chunk
        if (north != null) {
            north.setNeighbors(
                loadedChunks.get(new Vector2i(x, z - 2)),
                chunk,
                loadedChunks.get(new Vector2i(x + 1, z - 1)),
                loadedChunks.get(new Vector2i(x - 1, z - 1))
            );
        }
        if (south != null) {
            south.setNeighbors(
                chunk,
                loadedChunks.get(new Vector2i(x, z + 2)),
                loadedChunks.get(new Vector2i(x + 1, z + 1)),
                loadedChunks.get(new Vector2i(x - 1, z + 1))
            );
        }
        if (east != null) {
            east.setNeighbors(
                loadedChunks.get(new Vector2i(x + 1, z - 1)),
                loadedChunks.get(new Vector2i(x + 1, z + 1)),
                loadedChunks.get(new Vector2i(x + 2, z)),
                chunk
            );
        }
        if (west != null) {
            west.setNeighbors(
                loadedChunks.get(new Vector2i(x - 1, z - 1)),
                loadedChunks.get(new Vector2i(x - 1, z + 1)),
                chunk,
                loadedChunks.get(new Vector2i(x - 2, z))
            );
        }
    }
    
    /**
     * Gets a block at world coordinates
     */
    public Block.BlockType getBlock(int x, int y, int z) {
        Vector2i chunkPos = new Vector2i(
            Math.floorDiv(x, Chunk.CHUNK_SIZE),
            Math.floorDiv(z, Chunk.CHUNK_SIZE)
        );
        
        Chunk chunk = loadedChunks.get(chunkPos);
        if (chunk == null) {
            return Block.BlockType.AIR;
        }
        
        int localX = x - chunkPos.x * Chunk.CHUNK_SIZE;
        int localZ = z - chunkPos.y * Chunk.CHUNK_SIZE;
        
        return chunk.getBlock(localX, y, localZ);
    }
    
    /**
     * Sets a block at world coordinates
     */
    public void setBlock(int x, int y, int z, Block.BlockType blockType) {
        Vector2i chunkPos = new Vector2i(
            Math.floorDiv(x, Chunk.CHUNK_SIZE),
            Math.floorDiv(z, Chunk.CHUNK_SIZE)
        );
        
        Chunk chunk = loadedChunks.get(chunkPos);
        if (chunk == null) {
            return;
        }
        
        int localX = x - chunkPos.x * Chunk.CHUNK_SIZE;
        int localZ = z - chunkPos.y * Chunk.CHUNK_SIZE;
        
        chunk.setBlock(localX, y, localZ, blockType);
    }
    
    /**
     * Gets render commands for all visible chunks
     */
    public List<RenderCommand> getRenderCommands(Camera camera) {
        List<RenderCommand> commands = new ArrayList<>();
        
        Vector3f cameraPos = camera.getPosition();
        float renderDistanceSquared = renderDistance * Chunk.CHUNK_SIZE * renderDistance * Chunk.CHUNK_SIZE;
        
        for (Chunk chunk : loadedChunks.values()) {
            if (chunk.isEmpty()) {
                continue;
            }
            
            // Distance culling
            Vector3i chunkWorldPos = chunk.getWorldPosition();
            float distanceSquared = cameraPos.distanceSquared(
                chunkWorldPos.x + Chunk.CHUNK_SIZE / 2.0f,
                cameraPos.y,
                chunkWorldPos.z + Chunk.CHUNK_SIZE / 2.0f
            );
            
            if (distanceSquared <= renderDistanceSquared) {
                commands.addAll(chunk.getRenderCommands());
            }
        }
        
        return commands;
    }
    
    /**
     * Performs raycast to find the first solid block hit
     */
    public RaycastResult raycast(Vector3f origin, Vector3f direction, float maxDistance) {
        Vector3f current = new Vector3f(origin);
        Vector3f step = new Vector3f(direction).normalize().mul(0.1f);
        
        for (float distance = 0; distance < maxDistance; distance += 0.1f) {
            int x = (int) Math.floor(current.x);
            int y = (int) Math.floor(current.y);
            int z = (int) Math.floor(current.z);
            
            Block.BlockType block = getBlock(x, y, z);
            if (block.isSolid()) {
                return new RaycastResult(true, new Vector3i(x, y, z), block, distance, current);
            }
            
            current.add(step);
        }
        
        return new RaycastResult(false, null, null, maxDistance, current);
    }
    
    /**
     * Result of a raycast operation
     */
    public static class RaycastResult {
        public final boolean hit;
        public final Vector3i blockPosition;
        public final Block.BlockType blockType;
        public final float distance;
        public final Vector3f hitPosition;
        
        public RaycastResult(boolean hit, Vector3i blockPosition, Block.BlockType blockType, 
                           float distance, Vector3f hitPosition) {
            this.hit = hit;
            this.blockPosition = blockPosition;
            this.blockType = blockType;
            this.distance = distance;
            this.hitPosition = new Vector3f(hitPosition);
        }
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        Logger.world("Shutting down world '{}'...", worldName);
        
        // Shutdown executors
        chunkLoadingExecutor.shutdown();
        meshBuildingExecutor.shutdown();
        
        try {
            if (!chunkLoadingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                chunkLoadingExecutor.shutdownNow();
            }
            if (!meshBuildingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                meshBuildingExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            chunkLoadingExecutor.shutdownNow();
            meshBuildingExecutor.shutdownNow();
        }
        
        // Cleanup all chunks
        for (Chunk chunk : loadedChunks.values()) {
            chunk.cleanup();
        }
        loadedChunks.clear();
        
        Logger.world("World '{}' shut down successfully", worldName);
    }
    
    // Getters
    public String getWorldName() { return worldName; }
    public long getWorldSeed() { return worldSeed; }
    public int getTotalChunksLoaded() { return totalChunksLoaded; }
    public int getChunksLoadedThisFrame() { return chunksLoadedThisFrame; }
    public int getChunksUnloadedThisFrame() { return chunksUnloadedThisFrame; }
    public int getMeshesBuiltThisFrame() { return meshesBuiltThisFrame; }
    public Vector3f getPlayerPosition() { return new Vector3f(playerPosition); }
    public Vector2i getCurrentChunk() { return new Vector2i(currentChunk); }
    public int getRenderDistance() { return renderDistance; }
    
    /**
     * Gets a chunk by its coordinates
     */
    public Chunk getChunk(int chunkX, int chunkZ) {
        Vector2i chunkPos = new Vector2i(chunkX, chunkZ);
        return loadedChunks.get(chunkPos);
    }
    
    /**
     * Gets world statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("worldName", worldName);
        stats.put("worldSeed", worldSeed);
        stats.put("chunksLoaded", totalChunksLoaded);
        stats.put("chunksToLoad", chunksToLoad.size());
        stats.put("chunksToUnload", chunksToUnload.size());
        stats.put("renderDistance", renderDistance);
        stats.put("playerChunk", currentChunk.toString());
        return stats;
    }
}