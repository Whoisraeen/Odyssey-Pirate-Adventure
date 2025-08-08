package com.odyssey.world.save.region;

import com.odyssey.world.chunk.Chunk;
import com.odyssey.world.chunk.ChunkPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages region files (.oreg) for efficient chunk storage.
 * Each region file contains a 32x32 grid of chunks with optimized binary format.
 * 
 * <p>Region file format (.oreg):
 * <pre>
 * Header (8 KB):
 *   - Magic number (4 bytes): "OREG"
 *   - Version (4 bytes): format version
 *   - Region X (4 bytes): region x coordinate
 *   - Region Z (4 bytes): region z coordinate
 *   - Chunk offsets (4096 bytes): 1024 entries × 4 bytes each
 *   - Chunk timestamps (4096 bytes): 1024 entries × 4 bytes each
 *   - Compression flags (1024 bytes): 1024 entries × 1 byte each
 *   - Free list header (896 bytes): sparse free-list metadata
 * 
 * Chunk Data:
 *   - Variable-length Zstandard-compressed NBT/CBOR payloads
 *   - Sparse free-list to reduce fragmentation
 * </pre>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class RegionFileManager {
    private static final Logger logger = LoggerFactory.getLogger(RegionFileManager.class);
    
    /** Size of a region in chunks (32x32). */
    public static final int REGION_SIZE = 32;
    
    /** Total number of chunks per region. */
    public static final int CHUNKS_PER_REGION = REGION_SIZE * REGION_SIZE;
    
    private final Path regionDirectory;
    private final ConcurrentHashMap<RegionPosition, RegionFile> openRegions;
    private final ExecutorService ioExecutor;
    
    /**
     * Creates a new region file manager.
     * 
     * @param regionDirectory the directory to store region files
     * @throws IOException if the directory cannot be created
     */
    public RegionFileManager(Path regionDirectory) throws IOException {
        this.regionDirectory = regionDirectory;
        this.openRegions = new ConcurrentHashMap<>();
        this.ioExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "RegionIO-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        // Create region directory
        Files.createDirectories(regionDirectory);
        
        logger.info("Initialized region file manager at: {}", regionDirectory);
    }
    
    /**
     * Saves a chunk to its region file asynchronously.
     * 
     * @param chunk the chunk to save
     * @return a CompletableFuture that completes when the save is finished
     */
    public CompletableFuture<Void> saveChunkAsync(Chunk chunk) {
        if (chunk == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                saveChunk(chunk);
            } catch (IOException e) {
                logger.error("Failed to save chunk {} to region file", chunk.getPosition(), e);
                throw new RuntimeException(e);
            }
        }, ioExecutor);
    }
    
    /**
     * Saves a chunk to its region file synchronously.
     * 
     * @param chunk the chunk to save
     * @throws IOException if an I/O error occurs
     */
    public void saveChunk(Chunk chunk) throws IOException {
        if (chunk == null) {
            return;
        }
        
        ChunkPosition chunkPos = chunk.getPosition();
        RegionPosition regionPos = getRegionPosition(chunkPos);
        
        RegionFile regionFile = getOrCreateRegionFile(regionPos);
        regionFile.writeChunk(chunkPos, chunk);
        
        logger.debug("Saved chunk {} to region {}", chunkPos, regionPos);
    }
    
    /**
     * Loads a chunk from its region file asynchronously.
     * 
     * @param chunkPosition the chunk position
     * @return a CompletableFuture containing the loaded chunk, or null if not found
     */
    public CompletableFuture<Chunk> loadChunkAsync(ChunkPosition chunkPosition) {
        if (chunkPosition == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadChunk(chunkPosition);
            } catch (IOException e) {
                logger.error("Failed to load chunk {} from region file", chunkPosition, e);
                return null;
            }
        }, ioExecutor);
    }
    
    /**
     * Loads a chunk from its region file synchronously.
     * 
     * @param chunkPosition the chunk position
     * @return the loaded chunk, or null if not found
     * @throws IOException if an I/O error occurs
     */
    public Chunk loadChunk(ChunkPosition chunkPosition) throws IOException {
        if (chunkPosition == null) {
            return null;
        }
        
        RegionPosition regionPos = getRegionPosition(chunkPosition);
        
        // Check if region file exists
        if (!regionFileExists(regionPos)) {
            return null;
        }
        
        RegionFile regionFile = getOrCreateRegionFile(regionPos);
        Chunk chunk = regionFile.readChunk(chunkPosition);
        
        if (chunk != null) {
            logger.debug("Loaded chunk {} from region {}", chunkPosition, regionPos);
        }
        
        return chunk;
    }
    
    /**
     * Checks if a chunk exists in storage.
     * 
     * @param chunkPosition the chunk position
     * @return true if the chunk exists
     */
    public boolean chunkExists(ChunkPosition chunkPosition) {
        if (chunkPosition == null) {
            return false;
        }
        
        RegionPosition regionPos = getRegionPosition(chunkPosition);
        
        if (!regionFileExists(regionPos)) {
            return false;
        }
        
        try {
            RegionFile regionFile = getOrCreateRegionFile(regionPos);
            return regionFile.hasChunk(chunkPosition);
        } catch (IOException e) {
            logger.error("Failed to check chunk existence: {}", chunkPosition, e);
            return false;
        }
    }
    
    /**
     * Saves all dirty chunks in all open region files.
     */
    public void saveAllDirtyChunks() {
        logger.info("Saving all dirty chunks in {} open regions", openRegions.size());
        
        for (RegionFile regionFile : openRegions.values()) {
            try {
                regionFile.flush();
            } catch (IOException e) {
                logger.error("Failed to flush region file: {}", regionFile.getPosition(), e);
            }
        }
        
        logger.info("Completed saving all dirty chunks");
    }
    
    /**
     * Gets or creates a region file for the specified position.
     * 
     * @param regionPos the region position
     * @return the region file
     * @throws IOException if the region file cannot be created or opened
     */
    private RegionFile getOrCreateRegionFile(RegionPosition regionPos) throws IOException {
        return openRegions.computeIfAbsent(regionPos, pos -> {
            try {
                Path regionFilePath = getRegionFilePath(pos);
                return new RegionFile(regionFilePath, pos);
            } catch (IOException e) {
                logger.error("Failed to create region file for position: {}", pos, e);
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Gets the region position for a chunk position.
     * 
     * @param chunkPos the chunk position
     * @return the region position
     */
    private RegionPosition getRegionPosition(ChunkPosition chunkPos) {
        int regionX = Math.floorDiv(chunkPos.x, REGION_SIZE);
        int regionZ = Math.floorDiv(chunkPos.z, REGION_SIZE);
        return new RegionPosition(regionX, regionZ);
    }
    
    /**
     * Gets the file path for a region file.
     * 
     * @param regionPos the region position
     * @return the region file path
     */
    private Path getRegionFilePath(RegionPosition regionPos) {
        String fileName = String.format("r.%d.%d.oreg", regionPos.x, regionPos.z);
        return regionDirectory.resolve(fileName);
    }
    
    /**
     * Checks if a region file exists on disk.
     * 
     * @param regionPos the region position
     * @return true if the region file exists
     */
    private boolean regionFileExists(RegionPosition regionPos) {
        return Files.exists(getRegionFilePath(regionPos));
    }
    
    /**
     * Closes all open region files and shuts down the manager.
     */
    public void close() {
        logger.info("Closing region file manager");
        
        // Close all open region files
        for (RegionFile regionFile : openRegions.values()) {
            try {
                regionFile.close();
            } catch (IOException e) {
                logger.error("Failed to close region file: {}", regionFile.getPosition(), e);
            }
        }
        
        openRegions.clear();
        
        // Shutdown executor
        ioExecutor.shutdown();
        
        logger.info("Region file manager closed");
    }
    
    /**
     * Gets statistics about the region file manager.
     * 
     * @return region manager statistics
     */
    public RegionManagerStats getStats() {
        return new RegionManagerStats(
            openRegions.size(),
            openRegions.values().stream().mapToLong(RegionFile::getSize).sum(),
            openRegions.values().stream().mapToInt(RegionFile::getChunkCount).sum()
        );
    }
    
    /**
     * Statistics for the region file manager.
     */
    public static class RegionManagerStats {
        public final int openRegions;
        public final long totalSize;
        public final int totalChunks;
        
        public RegionManagerStats(int openRegions, long totalSize, int totalChunks) {
            this.openRegions = openRegions;
            this.totalSize = totalSize;
            this.totalChunks = totalChunks;
        }
        
        @Override
        public String toString() {
            return String.format("RegionManagerStats{openRegions=%d, totalSize=%d, totalChunks=%d}",
                                openRegions, totalSize, totalChunks);
        }
    }
}