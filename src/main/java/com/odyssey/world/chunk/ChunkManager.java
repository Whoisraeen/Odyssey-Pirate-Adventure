package com.odyssey.world.chunk;

import com.odyssey.core.GameConfig;
import com.odyssey.graphics.Renderer;
import com.odyssey.world.chunk.LODTextureAtlasManager.LODLevel;
import com.odyssey.world.generation.WorldGenerator;
import com.odyssey.world.generation.WorldGenerator.BlockType;
import com.odyssey.world.biome.BiomeManager;
import com.odyssey.core.jobs.JobSystem;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * Advanced chunk management system for The Odyssey with enhanced voxel engine architecture.
 * 
 * <p>Features:
 * <ul>
 *   <li>Memory-efficient palettized chunks with automatic optimization</li>
 *   <li>LRU caching with configurable memory limits</li>
 *   <li>Asynchronous chunk loading/unloading with disk serialization</li>
 *   <li>Optimized mesh generation with face culling and greedy meshing</li>
 *   <li>Chunk columns for efficient vertical world management</li>
 *   <li>Thread-safe operations with minimal blocking</li>
 *   <li>Automatic chunk streaming based on player position</li>
 * </ul>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class ChunkManager {
    private static final Logger logger = LoggerFactory.getLogger(ChunkManager.class);
    
    /** Default render distance in chunks. */
    private static final int DEFAULT_RENDER_DISTANCE = 8;
    
    /** Default cache size in chunks. */
    private static final int DEFAULT_CACHE_SIZE = 1024;
    
    /** Default memory limit for chunk cache (256 MB). */
    private static final long DEFAULT_MEMORY_LIMIT = 256L * 1024 * 1024;
    
    /** Chunk update interval in milliseconds. */
    private static final long CHUNK_UPDATE_INTERVAL = 100;
    
    private final GameConfig config;
    private final WorldGenerator worldGenerator;
    private final BiomeManager biomeManager;
    private final JobSystem jobSystem;
    
    // Core components
    private final ChunkCache chunkCache;
    private final ChunkSerializer chunkSerializer;
    private final ChunkMeshGenerator meshGenerator;
    private final ChunkLOD lodManager;
    
    // Thread-safe chunk storage
    private final ConcurrentHashMap<ChunkPosition, Chunk> loadedChunks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ChunkPosition, ChunkColumn> chunkColumns = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<ChunkPosition, ChunkMeshGenerator.ChunkMesh> chunkMeshes = new ConcurrentHashMap<>();
    
    // Async processing
    private final ExecutorService chunkExecutor;
    private final AtomicLong lastUpdateTime = new AtomicLong(0);
    
    // Player tracking
    private volatile Vector3f lastPlayerPosition = new Vector3f();
    private volatile int renderDistance = DEFAULT_RENDER_DISTANCE;
    
    // Statistics
    private final AtomicLong chunksLoaded = new AtomicLong(0);
    private final AtomicLong chunksUnloaded = new AtomicLong(0);
    private final AtomicLong chunksSaved = new AtomicLong(0);
    private final AtomicLong meshesGenerated = new AtomicLong(0);
    
    /**
     * Creates a new chunk manager with default job system.
     * 
     * @param config the game configuration
     * @param worldGenerator the world generator for chunk data
     * @param biomeManager the biome manager for terrain generation
     */
    public ChunkManager(GameConfig config, WorldGenerator worldGenerator, BiomeManager biomeManager) {
        this(config, worldGenerator, biomeManager, null);
    }
    
    /**
     * Creates a new chunk manager with specified job system.
     * 
     * @param config the game configuration
     * @param worldGenerator the world generator for chunk data
     * @param biomeManager the biome manager for terrain generation
     * @param jobSystem the job system for async operations (can be null)
     */
    public ChunkManager(GameConfig config, WorldGenerator worldGenerator, BiomeManager biomeManager, JobSystem jobSystem) {
        this.config = config;
        this.worldGenerator = worldGenerator;
        this.biomeManager = biomeManager;
        this.jobSystem = jobSystem;
        
        // Initialize render distance from config
        this.renderDistance = config != null ? (int) config.getRenderDistance() : DEFAULT_RENDER_DISTANCE;
        
        // Initialize core components
        this.chunkSerializer = new ChunkSerializer(Paths.get("world", "chunks"));
        this.chunkCache = new ChunkCache(DEFAULT_CACHE_SIZE, DEFAULT_MEMORY_LIMIT, chunkSerializer);
        this.meshGenerator = new ChunkMeshGenerator(true, true); // Enable all optimizations
        this.lodManager = new ChunkLOD(); // Initialize LOD system with default configuration
        
        // Initialize thread pool for chunk operations
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        this.chunkExecutor = Executors.newFixedThreadPool(threadCount, r -> {
            Thread t = new Thread(r, "ChunkManager-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        logger.info("Initialized chunk manager with {} threads, cache size: {}, memory limit: {} MB",
                threadCount, DEFAULT_CACHE_SIZE, DEFAULT_MEMORY_LIMIT / (1024 * 1024));
    }
    
    /**
     * Initializes the chunk manager and starts background services.
     */
    public void initialize() {
        logger.info("Initializing chunk manager with render distance: {}", renderDistance);
        
        // Start background chunk streaming
        startChunkStreaming();
        
        // Initialize cache statistics
        chunkCache.getStatistics(); // Warm up statistics
        
        logger.info("Chunk manager initialized successfully");
    }
    
    /**
     * Updates chunks based on player position and render distance.
     * 
     * @param playerPosition the current player position
     * @param renderDistance the render distance in chunks
     */
    public void updateChunks(Vector3f playerPosition, float renderDistance) {
        if (playerPosition == null) {
            return;
        }
        
        this.lastPlayerPosition.set(playerPosition);
        this.renderDistance = Math.max(1, (int) renderDistance);
        
        // Update LOD system with new player position
        lodManager.updatePlayerPosition(playerPosition);
        
        // Throttle updates to avoid excessive processing
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime.get() < CHUNK_UPDATE_INTERVAL) {
            return;
        }
        lastUpdateTime.set(currentTime);
        
        // Async chunk streaming
        CompletableFuture.runAsync(() -> {
            streamChunks(playerPosition, this.renderDistance);
        }, chunkExecutor);
    }
    
    /**
     * Updates the chunk manager with delta time and player position.
     * 
     * @param deltaTime the time elapsed since last update
     * @param playerX the player's x coordinate
     * @param playerZ the player's z coordinate
     */
    public void update(double deltaTime, float playerX, float playerZ) {
        Vector3f playerPos = new Vector3f(playerX, 0, playerZ);
        updateChunks(playerPos, renderDistance);
        
        // Update cache and cleanup old chunks
        chunkCache.cleanup();
        
        // Update mesh generation for dirty chunks
        updateMeshes();
    }
    
    /**
     * Renders visible chunks using the provided renderer.
     * 
     * @param renderer the renderer to use for chunk rendering
     */
    public void render(Renderer renderer) {
        if (renderer == null) {
            return;
        }
        
        Vector3f playerPos = lastPlayerPosition;
        int playerChunkX = (int) Math.floor(playerPos.x / ChunkPosition.CHUNK_SIZE);
        int playerChunkZ = (int) Math.floor(playerPos.z / ChunkPosition.CHUNK_SIZE);
        
        // Render chunks within render distance
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                
                // Check if chunk is within circular render distance
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > renderDistance) {
                    continue;
                }
                
                ChunkPosition position = new ChunkPosition(chunkX, chunkZ);
                
                // Determine LOD level for this chunk
                LODLevel lodLevel = lodManager.determineLODLevel(position, playerPos);
                
                ChunkMeshGenerator.ChunkMesh mesh = chunkMeshes.get(position);
                
                if (mesh != null && !mesh.isEmpty()) {
                    renderChunkMeshWithLOD(renderer, mesh, lodLevel);
                }
            }
        }
    }
    
    /**
     * Gets the block type at the specified world coordinates.
     * Uses ChunkColumn for efficient vertical access patterns.
     * 
     * @param x the world x-coordinate
     * @param y the world y-coordinate
     * @param z the world z-coordinate
     * @return the block type, or AIR if the chunk is not loaded
     */
    public BlockType getBlock(int x, int y, int z) {
        ChunkPosition columnPos = ChunkPosition.fromWorldPosition(x, z);
        
        // Try chunk column first for better vertical access
        ChunkColumn column = chunkColumns.get(columnPos);
        if (column != null) {
            int localX = columnPos.toLocalX(x);
            int localZ = columnPos.toLocalZ(z);
            return column.getBlock(localX, y, localZ);
        }
        
        // Fallback to individual chunk lookup
        Chunk chunk = loadedChunks.get(columnPos);
        if (chunk != null) {
            int localX = columnPos.toLocalX(x);
            int localZ = columnPos.toLocalZ(z);
            return chunk.getBlock(localX, y, localZ);
        }
        
        // Try cache
        chunk = chunkCache.get(columnPos);
        if (chunk != null) {
            int localX = columnPos.toLocalX(x);
            int localZ = columnPos.toLocalZ(z);
            return chunk.getBlock(localX, y, localZ);
        }
        
        // Generate block on-demand if world generator is available
        if (worldGenerator != null) {
            return worldGenerator.getBlockAt(x, y, z);
        }
        
        return BlockType.AIR;
    }
    
    /**
     * Sets the block type at the specified world coordinates.
     * Uses ChunkColumn for efficient vertical access and automatic column management.
     * 
     * @param x the world x-coordinate
     * @param y the world y-coordinate
     * @param z the world z-coordinate
     * @param blockType the new block type
     * @return true if the block was set successfully
     */
    public boolean setBlock(int x, int y, int z, BlockType blockType) {
        if (blockType == null) {
            blockType = BlockType.AIR;
        }
        
        ChunkPosition columnPos = ChunkPosition.fromWorldPosition(x, z);
        
        // Get or create chunk column
        ChunkColumn column = getOrCreateChunkColumn(columnPos);
        if (column == null) {
            logger.warn("Failed to create chunk column for block at ({}, {}, {})", x, y, z);
            return false;
        }
        
        int localX = columnPos.toLocalX(x);
        int localZ = columnPos.toLocalZ(z);
        
        // Get old block type for change detection
        BlockType oldBlockType = column.getBlock(localX, y, localZ);
        
        // Set the new block through column
        column.setBlock(localX, y, localZ, blockType);
        
        // Schedule mesh regeneration if block changed
        if (oldBlockType != blockType) {
            // Schedule mesh regeneration for affected chunk
            scheduleChunkMeshUpdate(columnPos);
            
            // Also update neighboring chunks if this is a boundary block
            updateNeighborChunks(x, y, z, localX, localZ);
        }
        
        logger.debug("Set block at ({}, {}, {}) to type {} (was {})", x, y, z, blockType, oldBlockType);
        return true;
    }

    public int getBlockAt(int x, int y, int z) {
        BlockType blockType = getBlock(x, y, z);
        return blockType != null ? blockType.id : 0;
    }
    
    public void setBlockAt(int x, int y, int z, int blockType) {
        // Convert int blockType to BlockType enum
        BlockType type = BlockType.values()[Math.min(blockType, BlockType.values().length - 1)];
        setBlock(x, y, z, type);
    }
    
    /**
     * Checks if a chunk is loaded at the specified chunk coordinates.
     * 
     * @param chunkX the chunk x-coordinate
     * @param chunkZ the chunk z-coordinate
     * @return true if the chunk is loaded
     */
    public boolean isChunkLoaded(int chunkX, int chunkZ) {
        return loadedChunks.containsKey(new ChunkPosition(chunkX, chunkZ));
    }
    
    /**
     * Loads a chunk at the specified chunk coordinates.
     * First tries to load from disk, then generates if not found.
     * 
     * @param chunkX the chunk x-coordinate
     * @param chunkZ the chunk z-coordinate
     * @return the loaded chunk
     */
    public Chunk loadChunk(int chunkX, int chunkZ) {
        ChunkPosition position = new ChunkPosition(chunkX, chunkZ);
        return getOrLoadChunk(position);
    }
    
    /**
     * Loads a chunk asynchronously.
     * 
     * @param chunkX the chunk x-coordinate
     * @param chunkZ the chunk z-coordinate
     * @return a CompletableFuture containing the loaded chunk
     */
    public CompletableFuture<Chunk> loadChunkAsync(int chunkX, int chunkZ) {
        ChunkPosition position = new ChunkPosition(chunkX, chunkZ);
        
        return CompletableFuture.supplyAsync(() -> {
            return getOrLoadChunk(position);
        }, chunkExecutor);
    }
    
    /**
     * Unloads a chunk at the specified chunk coordinates.
     * Saves the chunk to disk if it's dirty before unloading.
     * 
     * @param chunkX the chunk x-coordinate
     * @param chunkZ the chunk z-coordinate
     * @return true if the chunk was unloaded, false if it wasn't loaded
     */
    public boolean unloadChunk(int chunkX, int chunkZ) {
        ChunkPosition position = new ChunkPosition(chunkX, chunkZ);
        return unloadChunk(position);
    }
    
    /**
     * Unloads a chunk at the specified position.
     * 
     * @param position the chunk position
     * @return true if the chunk was unloaded
     */
    public boolean unloadChunk(ChunkPosition position) {
        Chunk chunk = loadedChunks.remove(position);
        
        if (chunk != null) {
            // Save chunk if dirty
            if (chunk.isDirty()) {
                saveChunkAsync(chunk);
            }
            
            // Move to cache
            chunkCache.put(position, chunk);
            
            // Remove mesh
            chunkMeshes.remove(position);
            
            chunksUnloaded.incrementAndGet();
            logger.debug("Unloaded chunk at {}", position);
            return true;
        }
        
        return false;
    }
    
    /**
     * Unloads a chunk asynchronously.
     * 
     * @param chunkX the chunk x-coordinate
     * @param chunkZ the chunk z-coordinate
     * @return a CompletableFuture that completes when unloading is done
     */
    public CompletableFuture<Boolean> unloadChunkAsync(int chunkX, int chunkZ) {
        ChunkPosition position = new ChunkPosition(chunkX, chunkZ);
        
        return CompletableFuture.supplyAsync(() -> {
            return unloadChunk(position);
        }, chunkExecutor);
    }
    
    /**
     * Gets the number of currently loaded chunks.
     * 
     * @return the number of loaded chunks
     */
    public int getLoadedChunkCount() {
        return loadedChunks.size();
    }
    
    /**
     * Generates the block data for a chunk using the world generator.
     * 
     * @param chunk the chunk to generate data for
     */
    private void generateChunkData(Chunk chunk) {
        ChunkPosition pos = chunk.getPosition();
        int worldX = pos.getWorldX();
        int worldZ = pos.getWorldZ();
        
        // Generate blocks for the entire chunk
        for (int x = 0; x < ChunkPosition.CHUNK_SIZE; x++) {
            for (int z = 0; z < ChunkPosition.CHUNK_SIZE; z++) {
                for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                    BlockType blockType = worldGenerator.getBlockAt(worldX + x, y, worldZ + z);
                    chunk.setBlock(x, y, z, blockType);
                }
            }
        }
    }

    public float getHeightAt(float x, float z) {
        // Use world generator to get terrain height
        if (worldGenerator != null) {
            return worldGenerator.getHeightAt(x, z);
        }
        return 64.0f; // Sea level
    }
    
    /**
     * Gets or creates a chunk column for the specified position.
     * 
     * @param position the column position
     * @return the chunk column, or null if creation failed
     */
    private ChunkColumn getOrCreateChunkColumn(ChunkPosition position) {
        // Check if column already exists
        ChunkColumn column = chunkColumns.get(position);
        if (column != null) {
            return column;
        }
        
        // Create new column
        column = new ChunkColumn(position);
        chunkColumns.put(position, column);
        
        // Load any existing chunks for this column
        loadChunksForColumn(column, position);
        
        logger.debug("Created chunk column at {}", position);
        return column;
    }
    
    /**
     * Loads existing chunks for a column from cache or disk.
     * 
     * @param column the chunk column
     * @param position the column position
     */
    private void loadChunksForColumn(ChunkColumn column, ChunkPosition position) {
        // For now, we'll load chunks on-demand when accessed
        // In a full implementation, you might preload common Y levels
        // This is a placeholder for future optimization
    }
    
    /**
     * Gets or loads a chunk, trying cache and disk before generating.
     * 
     * @param position the chunk position
     * @return the chunk, or null if loading failed
     */
    private Chunk getOrLoadChunk(ChunkPosition position) {
        // Check if already loaded
        Chunk chunk = loadedChunks.get(position);
        if (chunk != null) {
            return chunk;
        }
        
        // Try to load from cache
        chunk = chunkCache.get(position);
        if (chunk != null) {
            loadedChunks.put(position, chunk);
            chunksLoaded.incrementAndGet();
            return chunk;
        }
        
        // Try to load from disk
        try {
            chunk = chunkSerializer.loadChunk(position);
            if (chunk != null) {
                loadedChunks.put(position, chunk);
                chunksLoaded.incrementAndGet();
                logger.debug("Loaded chunk {} from disk", position);
                return chunk;
            }
        } catch (Exception e) {
            logger.warn("Failed to load chunk {} from disk: {}", position, e.getMessage());
        }
        
        // Generate new chunk
        chunk = createOptimizedChunk(position);
        if (chunk != null) {
            generateChunkData(chunk);
            loadedChunks.put(position, chunk);
            chunksLoaded.incrementAndGet();
            logger.debug("Generated new chunk {}", position);
        }
        
        return chunk;
    }
    
    /**
     * Creates an optimized chunk (palettized if beneficial).
     * 
     * @param position the chunk position
     * @return the created chunk
     */
    private Chunk createOptimizedChunk(ChunkPosition position) {
        // For now, always create palettized chunks for better memory efficiency
        // In a full implementation, you might analyze the chunk content first
        return new PalettizedChunk(position);
    }
    
    /**
     * Saves a chunk to disk asynchronously.
     * 
     * @param chunk the chunk to save
     * @return a CompletableFuture that completes when saving is done
     */
    private CompletableFuture<Void> saveChunkAsync(Chunk chunk) {
        if (chunk == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return chunkSerializer.saveChunkAsync(chunk).thenRun(() -> {
            chunksSaved.incrementAndGet();
            chunk.clearDirty(); // Mark as clean after successful save
        });
    }
    
    /**
     * Starts background chunk streaming based on player position.
     */
    private void startChunkStreaming() {
        // This would typically be a scheduled task that runs periodically
        // For now, it's called from the update methods
        logger.debug("Chunk streaming started");
    }
    
    /**
     * Streams chunks around the player position.
     * 
     * @param playerPosition the player position
     * @param renderDistance the render distance in chunks
     */
    private void streamChunks(Vector3f playerPosition, int renderDistance) {
        int playerChunkX = (int) Math.floor(playerPosition.x / ChunkPosition.CHUNK_SIZE);
        int playerChunkZ = (int) Math.floor(playerPosition.z / ChunkPosition.CHUNK_SIZE);
        
        Set<ChunkPosition> requiredChunks = new HashSet<>();
        Set<ChunkPosition> chunksToUnload = new HashSet<>();
        
        // Determine required chunks
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance <= renderDistance) {
                    ChunkPosition pos = new ChunkPosition(playerChunkX + dx, playerChunkZ + dz);
                    requiredChunks.add(pos);
                }
            }
        }
        
        // Find chunks to unload (outside render distance + buffer)
        int unloadDistance = renderDistance + 2;
        for (ChunkPosition loadedPos : loadedChunks.keySet()) {
            double distance = Math.sqrt(
                Math.pow(loadedPos.x - playerChunkX, 2) +
                Math.pow(loadedPos.z - playerChunkZ, 2)
            );
            
            if (distance > unloadDistance) {
                chunksToUnload.add(loadedPos);
            }
        }
        
        // Load required chunks
        for (ChunkPosition pos : requiredChunks) {
            if (!loadedChunks.containsKey(pos)) {
                CompletableFuture.runAsync(() -> {
                    getOrLoadChunk(pos);
                }, chunkExecutor);
            }
        }
        
        // Unload distant chunks
        for (ChunkPosition pos : chunksToUnload) {
            CompletableFuture.runAsync(() -> {
                unloadChunk(pos);
            }, chunkExecutor);
        }
    }
    
    /**
     * Updates meshes for dirty chunks.
     */
    private void updateMeshes() {
        List<ChunkPosition> dirtyChunks = new ArrayList<>();
        
        // Find dirty chunks
        for (Map.Entry<ChunkPosition, Chunk> entry : loadedChunks.entrySet()) {
            if (entry.getValue().isDirty()) {
                dirtyChunks.add(entry.getKey());
            }
        }
        
        // Generate meshes for dirty chunks
        for (ChunkPosition pos : dirtyChunks) {
            scheduleChunkMeshUpdate(pos);
        }
    }
    
    /**
     * Schedules a mesh update for the specified chunk.
     * 
     * @param position the chunk position
     */
    private void scheduleChunkMeshUpdate(ChunkPosition position) {
        Chunk chunk = loadedChunks.get(position);
        if (chunk == null) {
            return;
        }
        
        // Determine LOD level for mesh generation
        LODLevel lodLevel = lodManager.determineLODLevel(position, lastPlayerPosition);
        
        // Get neighboring chunks for face culling
        Chunk[] neighbors = getNeighborChunks(position);
        
        // Generate mesh asynchronously with LOD considerations
        generateMeshWithLOD(chunk, neighbors, lodLevel).thenAccept(mesh -> {
            if (mesh != null) {
                chunkMeshes.put(position, mesh);
                meshesGenerated.incrementAndGet();
                chunk.clearDirty(); // Mark as clean after mesh generation
            }
        });
    }
    
    /**
     * Generates a mesh for a chunk with specified LOD level.
     * 
     * @param chunk the chunk to generate mesh for
     * @param neighbors neighboring chunks for face culling
     * @param lodLevel the level of detail to use
     * @return CompletableFuture containing the generated mesh
     */
    private CompletableFuture<ChunkMeshGenerator.ChunkMesh> generateMeshWithLOD(
            Chunk chunk, Chunk[] neighbors, LODLevel lodLevel) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Configure mesh generation based on LOD level
                boolean useGreedyMeshing = !ChunkLOD.shouldUseSimplifiedMeshing(lodLevel);
                boolean includeSmallDetails = !ChunkLOD.shouldSkipSmallDetails(lodLevel);
                
                // For now, use the existing mesh generator
                // In a full implementation, you'd pass LOD parameters to the generator
                return meshGenerator.generateMesh(chunk, neighbors);
                
            } catch (Exception e) {
                logger.error("Error generating mesh for chunk {} with LOD {}", 
                        chunk.getPosition(), lodLevel, e);
                return null;
            }
        }, chunkExecutor);
    }
    
    /**
     * Gets neighboring chunks for face culling.
     * 
     * @param position the center chunk position
     * @return array of neighboring chunks (some may be null)
     */
    private Chunk[] getNeighborChunks(ChunkPosition position) {
        Chunk[] neighbors = new Chunk[6]; // North, South, East, West, Up, Down
        
        neighbors[0] = loadedChunks.get(new ChunkPosition(position.x, position.z + 1)); // North
        neighbors[1] = loadedChunks.get(new ChunkPosition(position.x, position.z - 1)); // South
        neighbors[2] = loadedChunks.get(new ChunkPosition(position.x + 1, position.z)); // East
        neighbors[3] = loadedChunks.get(new ChunkPosition(position.x - 1, position.z)); // West
        // Up and Down would be for 3D chunk columns (not implemented in this basic version)
        
        return neighbors;
    }
    
    /**
     * Updates neighboring chunks when a boundary block changes.
     * 
     * @param worldX the world x coordinate
     * @param worldY the world y coordinate
     * @param worldZ the world z coordinate
     * @param localX the local x coordinate within chunk
     * @param localZ the local z coordinate within chunk
     */
    private void updateNeighborChunks(int worldX, int worldY, int worldZ, int localX, int localZ) {
        // Check if this is a boundary block that affects neighboring chunks
        boolean isXBoundary = localX == 0 || localX == ChunkPosition.CHUNK_SIZE - 1;
        boolean isZBoundary = localZ == 0 || localZ == ChunkPosition.CHUNK_SIZE - 1;
        
        if (isXBoundary || isZBoundary) {
            // Schedule mesh updates for affected neighboring chunks
            if (localX == 0) {
                ChunkPosition westPos = new ChunkPosition(
                    (worldX - 1) / ChunkPosition.CHUNK_SIZE,
                    worldZ / ChunkPosition.CHUNK_SIZE
                );
                scheduleChunkMeshUpdate(westPos);
            } else if (localX == ChunkPosition.CHUNK_SIZE - 1) {
                ChunkPosition eastPos = new ChunkPosition(
                    (worldX + 1) / ChunkPosition.CHUNK_SIZE,
                    worldZ / ChunkPosition.CHUNK_SIZE
                );
                scheduleChunkMeshUpdate(eastPos);
            }
            
            if (localZ == 0) {
                ChunkPosition southPos = new ChunkPosition(
                    worldX / ChunkPosition.CHUNK_SIZE,
                    (worldZ - 1) / ChunkPosition.CHUNK_SIZE
                );
                scheduleChunkMeshUpdate(southPos);
            } else if (localZ == ChunkPosition.CHUNK_SIZE - 1) {
                ChunkPosition northPos = new ChunkPosition(
                    worldX / ChunkPosition.CHUNK_SIZE,
                    (worldZ + 1) / ChunkPosition.CHUNK_SIZE
                );
                scheduleChunkMeshUpdate(northPos);
            }
        }
    }
    
    /**
     * Renders a chunk mesh using the provided renderer.
     * 
     * @param renderer the renderer
     * @param mesh the chunk mesh to render
     */
    private void renderChunkMesh(Renderer renderer, ChunkMeshGenerator.ChunkMesh mesh) {
        renderChunkMeshWithLOD(renderer, mesh, LODLevel.ULTRA);
    }
    
    /**
     * Renders a chunk mesh with specified level of detail.
     * 
     * @param renderer the renderer
     * @param mesh the chunk mesh to render
     * @param lodLevel the level of detail to use
     */
    private void renderChunkMeshWithLOD(Renderer renderer, ChunkMeshGenerator.ChunkMesh mesh, LODLevel lodLevel) {
        if (mesh == null || renderer == null) {
            return;
        }
        
        // Calculate rendering parameters based on LOD level
        float complexityFactor = ChunkLOD.getMeshComplexityFactor(lodLevel);
        int trianglesToRender = (int) (mesh.getTriangleCount() * complexityFactor);
        
        // Skip rendering if LOD level is too low and chunk is very distant
        if (lodLevel == LODLevel.MINIMAL && trianglesToRender < 10) {
            return;
        }
        
        // This is a placeholder - in a real implementation you'd:
        // 1. Upload vertex data to GPU buffers (potentially subsampled based on LOD)
        // 2. Set up shader uniforms (world matrix, textures, LOD parameters)
        // 3. Issue draw calls with appropriate vertex count
        // 4. Use different shaders or rendering techniques based on LOD level
        
        // For different LOD levels, you might:
        // - FULL: Render all geometry with full detail
        // - HIGH: Skip some small details, use full textures
        // - MEDIUM: Use simplified geometry, lower resolution textures
        // - LOW: Use impostor rendering or very simplified geometry
        // - MINIMAL: Render as simple colored blocks or skip entirely
        
        if (trianglesToRender > 0) {
            logger.trace("Rendering chunk {} with {} triangles (LOD: {}, factor: {:.2f})", 
                    mesh.getPosition(), trianglesToRender, lodLevel.getSuffix(), complexityFactor);
        }
    }
    
    /**
     * Gets chunk manager statistics.
     * 
     * @return a map of statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("loadedChunks", loadedChunks.size());
        stats.put("cachedChunks", chunkCache.size());
        stats.put("generatedMeshes", chunkMeshes.size());
        stats.put("chunksLoaded", chunksLoaded.get());
        stats.put("chunksUnloaded", chunksUnloaded.get());
        stats.put("chunksSaved", chunksSaved.get());
        stats.put("meshesGenerated", meshesGenerated.get());
        stats.put("renderDistance", renderDistance);
        stats.put("cacheStatistics", chunkCache.getStatistics());
        stats.put("lodCacheSize", lodManager.getCacheSize());
        stats.put("chunkColumns", chunkColumns.size());
        return stats;
    }
    
    /**
     * Sets the render distance.
     * 
     * @param renderDistance the new render distance in chunks
     */
    public void setRenderDistance(int renderDistance) {
        this.renderDistance = Math.max(1, renderDistance);
        logger.info("Render distance set to {} chunks", this.renderDistance);
    }
    
    /**
     * Gets the current render distance.
     * 
     * @return the render distance in chunks
     */
    public int getRenderDistance() {
        return renderDistance;
    }
    
    /**
     * Gets a chunk column at the specified chunk coordinates.
     * 
     * @param chunkX the chunk x-coordinate
     * @param chunkZ the chunk z-coordinate
     * @return the chunk column, or null if not loaded
     */
    public ChunkColumn getChunkColumn(int chunkX, int chunkZ) {
        ChunkPosition position = new ChunkPosition(chunkX, chunkZ);
        return chunkColumns.get(position);
    }
    
    /**
     * Gets the LOD manager for advanced configuration.
     * 
     * @return the chunk LOD manager
     */
    public ChunkLOD getLODManager() {
        return lodManager;
    }
    
    /**
     * Sets the LOD configuration.
     * 
     * @param config the new LOD configuration
     */
    public void setLODConfig(ChunkLOD.LODConfig config) {
        lodManager.setConfig(config);
        logger.info("Updated chunk LOD configuration");
    }
    
    /**
     * Forces a save of all dirty chunks.
     * 
     * @return a CompletableFuture that completes when all saves are done
     */
    public CompletableFuture<Void> saveAllChunks() {
        List<CompletableFuture<Void>> saveTasks = new ArrayList<>();
        
        for (Chunk chunk : loadedChunks.values()) {
            if (chunk.isDirty()) {
                saveTasks.add(saveChunkAsync(chunk));
            }
        }
        
        return CompletableFuture.allOf(saveTasks.toArray(new CompletableFuture[0]));
    }
    
    /**
     * Cleans up the chunk manager and shuts down all services.
     */
    public void cleanup() {
        logger.info("Cleaning up chunk manager - {} loaded chunks, {} cached chunks", 
                loadedChunks.size(), chunkCache.size());
        
        // Save all dirty chunks before shutdown
        try {
            saveAllChunks().get(); // Wait for all saves to complete
        } catch (Exception e) {
            logger.error("Error saving chunks during cleanup", e);
        }
        
        // Shutdown services
        chunkExecutor.shutdown();
        meshGenerator.shutdown();
        chunkSerializer.shutdown();
        
        // Clear data structures
        loadedChunks.clear();
        chunkColumns.clear();
        chunkMeshes.clear();
        chunkCache.clear(true); // Save dirty chunks to disk before clearing
        
        logger.info("Chunk manager cleanup completed");
    }
}