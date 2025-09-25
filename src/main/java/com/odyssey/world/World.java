package com.odyssey.world;

import com.odyssey.core.GameConfig;
import com.odyssey.util.Logger;
import com.odyssey.rendering.Camera;
import com.odyssey.rendering.RenderCommand;
import com.odyssey.player.PlayerManager;
import com.odyssey.player.Player;
import org.joml.Vector2i;
import org.joml.Vector2f;
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
            r -> new Thread(r, "ChunkLoader-" + Thread.currentThread().threadId()));
        this.meshBuildingExecutor = Executors.newFixedThreadPool(threadCount,
            r -> new Thread(r, "MeshBuilder-" + Thread.currentThread().threadId()));
        
        Logger.world("Created world '{}' with seed {} (render distance: {}, threads: {})", 
                   worldName, worldSeed, renderDistance, threadCount);
    }
    
    /**
     * Updates the world, handling chunk loading/unloading and mesh building
     */
    public void update(Camera camera) {
        // Update player position from PlayerManager if available, otherwise use camera
        PlayerManager playerManager = PlayerManager.getInstance();
        Player player = playerManager.getCurrentPlayer();
        
        if (player != null) {
            // Use actual player position
            playerPosition.set(player.getPosition());
        } else {
            // Fallback to camera position for backwards compatibility
            Vector3f cameraPos = camera.getPosition();
            playerPosition.set(cameraPos);
        }
        
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
    public void loadChunk(Vector2i chunkPos) {
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
                
                // Convert WorldChunk data to Chunk format
                convertWorldChunkToChunk(worldChunk, chunk);
                
                // Calculate lighting on the generated chunk
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
    public void unloadChunk(Vector2i chunkPos) {
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
        
        // Use player position for distance calculations if available, otherwise camera
        Vector3f referencePos;
        PlayerManager playerManager = PlayerManager.getInstance();
        Player player = playerManager.getCurrentPlayer();
        
        if (player != null) {
            referencePos = player.getPosition();
        } else {
            referencePos = camera.getPosition();
        }
        
        float renderDistanceSquared = renderDistance * Chunk.CHUNK_SIZE * renderDistance * Chunk.CHUNK_SIZE;
        
        for (Chunk chunk : loadedChunks.values()) {
            if (chunk.isEmpty()) {
                continue;
            }
            
            // Distance culling
            Vector3i chunkWorldPos = chunk.getWorldPosition();
            float distanceSquared = referencePos.distanceSquared(
                chunkWorldPos.x + Chunk.CHUNK_SIZE / 2.0f,
                referencePos.y,
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
     * Gets all loaded chunks as a map for saving/serialization
     */
    public Map<String, Object> getLoadedChunks() {
        Map<String, Object> chunkData = new HashMap<>();
        for (Map.Entry<Vector2i, Chunk> entry : loadedChunks.entrySet()) {
            Vector2i pos = entry.getKey();
            Chunk chunk = entry.getValue();
            String key = pos.x + "," + pos.y;
            chunkData.put(key, chunk);
        }
        return chunkData;
    }
    
    /**
     * Gets all loaded chunks for rendering
     */
    public java.util.Collection<Chunk> getChunks() {
        return loadedChunks.values();
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
    
    /**
     * Converts WorldChunk terrain and biome data to Chunk block format
     */
    private void convertWorldChunkToChunk(WorldChunk worldChunk, Chunk chunk) {
        int chunkSize = worldChunk.getChunkSize();
        
        // Convert terrain data to blocks
        for (int x = 0; x < chunkSize && x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < chunkSize && z < Chunk.CHUNK_SIZE; z++) {
                float height = worldChunk.getHeightAt(x, z);
                BiomeType biome = worldChunk.getBiomeAt(x, z);
                
                // Generate terrain column
                generateTerrainColumn(chunk, x, z, height, biome);
            }
        }
        
        // Add islands from WorldChunk
        for (Island island : worldChunk.getIslands()) {
            generateIslandBlocks(chunk, island, worldChunk);
        }
        
        Logger.world("Converted WorldChunk to Chunk format for chunk ({}, {})", 
                    worldChunk.getChunkX(), worldChunk.getChunkZ());
    }
    
    /**
     * Generates a terrain column based on height and biome
     */
    private void generateTerrainColumn(Chunk chunk, int x, int z, float height, BiomeType biome) {
        int terrainHeight = (int) Math.floor(height);
        int seaLevel = 64; // Standard sea level
        
        // Generate bedrock layer
        chunk.setBlock(x, 0, z, Block.BlockType.STONE);
        
        // Generate stone layers
        for (int y = 1; y < Math.max(terrainHeight - 3, 1); y++) {
            chunk.setBlock(x, y, z, Block.BlockType.STONE);
        }
        
        // Generate subsurface layers based on biome
        Block.BlockType subsurfaceBlock = getSubsurfaceBlockForBiome(biome);
        for (int y = Math.max(terrainHeight - 3, 1); y < terrainHeight; y++) {
            if (y > 0 && y < Chunk.CHUNK_HEIGHT) {
                chunk.setBlock(x, y, z, subsurfaceBlock);
            }
        }
        
        // Generate surface block
        if (terrainHeight > 0 && terrainHeight < Chunk.CHUNK_HEIGHT) {
            Block.BlockType surfaceBlock = getSurfaceBlockForBiome(biome, terrainHeight, seaLevel);
            chunk.setBlock(x, terrainHeight, z, surfaceBlock);
        }
        
        // Fill with water if below sea level
        for (int y = terrainHeight + 1; y <= seaLevel && y < Chunk.CHUNK_HEIGHT; y++) {
            chunk.setBlock(x, y, z, Block.BlockType.WATER);
        }
        
        // Generate vegetation on surface
        if (terrainHeight > seaLevel && terrainHeight + 1 < Chunk.CHUNK_HEIGHT) {
            generateVegetation(chunk, x, terrainHeight + 1, z, biome);
        }
    }
    
    /**
     * Gets the appropriate surface block for a biome
     */
    private Block.BlockType getSurfaceBlockForBiome(BiomeType biome, int height, int seaLevel) {
        switch (biome) {
            case DESERT:
                return Block.BlockType.SAND;
            case TROPICAL_FOREST:
            case JUNGLE:
            case FOREST:
                return Block.BlockType.GRASS;
            case OCEAN:
                return height <= seaLevel ? Block.BlockType.SAND : Block.BlockType.GRASS;
            case TUNDRA:
                return Block.BlockType.STONE;
            case PLAINS:
            case GRASSLAND:
            case SAVANNA:
            default:
                return Block.BlockType.GRASS;
        }
    }
    
    /**
     * Gets the appropriate subsurface block for a biome
     */
    private Block.BlockType getSubsurfaceBlockForBiome(BiomeType biome) {
        switch (biome) {
            case DESERT:
                return Block.BlockType.SAND;
            case OCEAN:
                return Block.BlockType.SAND;
            default:
                return Block.BlockType.DIRT;
        }
    }
    
    /**
     * Generates vegetation based on biome
     */
    private void generateVegetation(Chunk chunk, int x, int y, int z, BiomeType biome) {
        if (y >= Chunk.CHUNK_HEIGHT) return;
        
        // Simple vegetation generation based on biome
        switch (biome) {
            case TROPICAL_FOREST:
            case JUNGLE:
                if (Math.random() < 0.1) { // 10% chance for palm trees
                    chunk.setBlock(x, y, z, Block.BlockType.PALM_WOOD);
                    if (y + 1 < Chunk.CHUNK_HEIGHT) {
                        chunk.setBlock(x, y + 1, z, Block.BlockType.PALM_LEAVES);
                    }
                }
                break;
            case FOREST:
                if (Math.random() < 0.05) { // 5% chance for regular trees
                    chunk.setBlock(x, y, z, Block.BlockType.WOOD);
                    if (y + 1 < Chunk.CHUNK_HEIGHT) {
                        chunk.setBlock(x, y + 1, z, Block.BlockType.LEAVES);
                    }
                }
                break;
            case PLAINS:
            case GRASSLAND:
                // No additional vegetation for now
                break;
        }
    }
    
    /**
     * Generates blocks for islands within the chunk
     */
    private void generateIslandBlocks(Chunk chunk, Island island, WorldChunk worldChunk) {
        // Convert island world coordinates to local chunk coordinates
        Vector2f islandCenter = new Vector2f(island.getCenterX(), island.getCenterZ());
        Vector2f chunkWorldPos = worldChunk.localToWorld(0, 0);
        
        float localX = islandCenter.x - chunkWorldPos.x;
        float localZ = islandCenter.y - chunkWorldPos.y;
        
        // Only generate if island center is within this chunk
        if (localX >= 0 && localX < worldChunk.getChunkSize() && 
            localZ >= 0 && localZ < worldChunk.getChunkSize()) {
            
            int centerX = (int) Math.floor(localX);
            int centerZ = (int) Math.floor(localZ);
            float radius = island.getSize();
            
            // Generate island terrain in a circular pattern
            for (int x = Math.max(0, centerX - (int)radius); 
                 x <= Math.min(Chunk.CHUNK_SIZE - 1, centerX + (int)radius); x++) {
                for (int z = Math.max(0, centerZ - (int)radius); 
                     z <= Math.min(Chunk.CHUNK_SIZE - 1, centerZ + (int)radius); z++) {
                    
                    float distance = (float) Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));
                    
                    if (distance <= radius) {
                        float worldX = worldChunk.localToWorld(x, z).x;
                        float worldZ = worldChunk.localToWorld(x, z).y;
                        
                        // Get height and biome from island
                        float baseHeight = island.getHeightAt(worldX, worldZ);
                        BiomeType biome = island.getBiomeAt(worldX, worldZ);
                        
                        // Generate island terrain
                        generateIslandTerrain(chunk, x, z, (int)baseHeight, biome);
                    }
                }
            }
        }
    }
    
    /**
     * Generates terrain for a specific island location
     */
    private void generateIslandTerrain(Chunk chunk, int x, int z, int height, BiomeType biome) {
        int seaLevel = 64;
        int baseHeight = Math.max(seaLevel, height);
        
        // Generate island blocks from sea level up
        for (int y = seaLevel; y <= baseHeight && y < Chunk.CHUNK_HEIGHT; y++) {
            if (y == baseHeight) {
                // Surface block
                chunk.setBlock(x, y, z, getSurfaceBlockForBiome(biome, y, seaLevel));
            } else if (y >= baseHeight - 2) {
                // Subsurface
                chunk.setBlock(x, y, z, getSubsurfaceBlockForBiome(biome));
            } else {
                // Core
                chunk.setBlock(x, y, z, Block.BlockType.STONE);
            }
        }
        
        // Add vegetation on top
        if (baseHeight + 1 < Chunk.CHUNK_HEIGHT) {
            generateVegetation(chunk, x, baseHeight + 1, z, biome);
        }
    }
}