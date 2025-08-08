package com.odyssey.world.save;

import com.odyssey.core.GameConfig;
import com.odyssey.world.save.region.RegionFileManager;
import com.odyssey.world.save.player.PlayerDataManager;
import com.odyssey.world.save.level.LevelMetadata;
import com.odyssey.world.save.journal.WriteAheadJournal;
import com.odyssey.world.save.backup.SnapshotManager;
import com.odyssey.world.save.compression.CompressionManager;
import com.odyssey.world.save.format.WorldSaveFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main coordinator for the world save architecture system.
 * Manages the complete Minecraft-style file system with full parity including:
 * - Region files for chunk storage
 * - Player data files
 * - Level metadata
 * - Write-ahead journaling
 * - Snapshot/backup system
 * - Compression and encryption
 * 
 * <p>Directory structure:
 * <pre>
 * /saves/&lt;WorldName&gt;/
 *   ├── region/           - chunk/region binaries (.oreg files)
 *   ├── playerdata/       - per-UUID player files (.odp files)
 *   ├── data/             - global maps, structures, world events
 *   │   ├── maps/         - PNG + palette per explored chart
 *   │   └── advancements.dat - compressed JSON toast progress
 *   ├── dimension/        - alternate realms
 *   │   └── &lt;DimName&gt;/region/
 *   ├── snapshots/        - zstd-tarballs for backups
 *   ├── level.meta        - world metadata (JSON + binary mirror)
 *   ├── session.lock      - session lock with PID tracking
 *   └── journal.wal       - write-ahead log
 * </pre>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class WorldSaveManager {
    private static final Logger logger = LoggerFactory.getLogger(WorldSaveManager.class);
    
    private final String worldName;
    private final Path worldDirectory;
    private final Path savesDirectory;
    
    // Core managers
    private final RegionFileManager regionManager;
    private final PlayerDataManager playerDataManager;
    private final LevelMetadata levelMetadata;
    private final WriteAheadJournal journal;
    private final SnapshotManager snapshotManager;
    private final CompressionManager compressionManager;
    
    // Session management
    private final SessionLock sessionLock;
    private final ReentrantLock saveLock = new ReentrantLock();
    
    // Async operations
    private final ExecutorService saveExecutor;
    
    /**
     * Creates a new world save manager for the specified world.
     * 
     * @param worldName the name of the world
     * @param config the game configuration
     * @throws IOException if the world directory cannot be created or accessed
     */
    public WorldSaveManager(String worldName, GameConfig config) throws IOException {
        this.worldName = worldName;
        this.savesDirectory = Paths.get("saves");
        this.worldDirectory = savesDirectory.resolve(worldName);
        
        // Create directory structure
        createDirectoryStructure();
        
        // Initialize session lock first
        this.sessionLock = new SessionLock(worldDirectory);
        
        // Initialize managers
        this.regionManager = new RegionFileManager(worldDirectory.resolve("region"));
        this.playerDataManager = new PlayerDataManager(worldDirectory.resolve("playerdata"));
        this.levelMetadata = new LevelMetadata(worldDirectory.resolve("level.meta"));
        this.journal = new WriteAheadJournal(worldDirectory.resolve("journal.wal"));
        this.snapshotManager = new SnapshotManager(worldDirectory.resolve("snapshots"), config);
        this.compressionManager = new CompressionManager(config);
        
        // Initialize executor service
        this.saveExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "WorldSave-" + worldName + "-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        logger.info("Initialized world save manager for world: {}", worldName);
    }
    
    /**
     * Creates the complete directory structure for the world save system.
     * 
     * @throws IOException if directories cannot be created
     */
    private void createDirectoryStructure() throws IOException {
        // Main world directory
        Files.createDirectories(worldDirectory);
        
        // Core directories
        Files.createDirectories(worldDirectory.resolve(WorldSaveFormat.REGION_DIR));
        Files.createDirectories(worldDirectory.resolve(WorldSaveFormat.PLAYER_DATA_DIR));
        Files.createDirectories(worldDirectory.resolve(WorldSaveFormat.JOURNAL_DIR));
        Files.createDirectories(worldDirectory.resolve(WorldSaveFormat.BACKUP_DIR));
        Files.createDirectories(worldDirectory.resolve(WorldSaveFormat.ENTITIES_DIR));
        Files.createDirectories(worldDirectory.resolve(WorldSaveFormat.POI_DIR));
        Files.createDirectories(worldDirectory.resolve(WorldSaveFormat.GENERATED_DIR));
        Files.createDirectories(worldDirectory.resolve(WorldSaveFormat.STATS_DIR));
        Files.createDirectories(worldDirectory.resolve(WorldSaveFormat.ADVANCEMENTS_DIR));
        Files.createDirectories(worldDirectory.resolve(WorldSaveFormat.DATA_DIR));
        Files.createDirectories(worldDirectory.resolve(WorldSaveFormat.DATAPACKS_DIR));
        
        // Data subdirectories
        Files.createDirectories(worldDirectory.resolve(WorldSaveFormat.DATA_DIR).resolve("maps"));
        Files.createDirectories(worldDirectory.resolve(WorldSaveFormat.DATA_DIR).resolve("structures"));
        
        // Dimension directories with entity and POI subdirectories
        createDimensionDirectories("");  // Overworld
        createDimensionDirectories(WorldSaveFormat.DIM_NETHER);
        createDimensionDirectories(WorldSaveFormat.DIM_END);
        
        logger.debug("Created complete directory structure for world: {}", worldName);
    }
    
    /**
     * Creates directory structure for a specific dimension.
     * 
     * @param dimensionPath the dimension path (empty for overworld)
     * @throws IOException if directories cannot be created
     */
    private void createDimensionDirectories(String dimensionPath) throws IOException {
        Path dimPath = dimensionPath.isEmpty() ? worldDirectory : worldDirectory.resolve(dimensionPath);
        
        // Create dimension-specific directories
        Files.createDirectories(dimPath.resolve(WorldSaveFormat.REGION_DIR));
        Files.createDirectories(dimPath.resolve(WorldSaveFormat.ENTITIES_DIR));
        Files.createDirectories(dimPath.resolve(WorldSaveFormat.POI_DIR));
    }
    
    /**
     * Acquires the session lock for this world.
     * 
     * @throws IOException if the lock cannot be acquired
     */
    public void acquireSessionLock() throws IOException {
        sessionLock.acquire();
        logger.info("Acquired session lock for world: {}", worldName);
    }
    
    /**
     * Releases the session lock for this world.
     */
    public void releaseSessionLock() {
        sessionLock.release();
        logger.info("Released session lock for world: {}", worldName);
    }
    
    /**
     * Gets the region file manager.
     * 
     * @return the region file manager
     */
    public RegionFileManager getRegionManager() {
        return regionManager;
    }
    
    /**
     * Gets the player data manager.
     * 
     * @return the player data manager
     */
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    /**
     * Gets the level metadata manager.
     * 
     * @return the level metadata manager
     */
    public LevelMetadata getLevelMetadata() {
        return levelMetadata;
    }
    
    /**
     * Gets the write-ahead journal.
     * 
     * @return the write-ahead journal
     */
    public WriteAheadJournal getJournal() {
        return journal;
    }
    
    /**
     * Gets the snapshot manager.
     * 
     * @return the snapshot manager
     */
    public SnapshotManager getSnapshotManager() {
        return snapshotManager;
    }
    
    /**
     * Performs a complete world save operation.
     * 
     * @return a CompletableFuture that completes when the save is finished
     */
    public CompletableFuture<Void> saveWorld() {
        return CompletableFuture.runAsync(() -> {
            saveLock.lock();
            try {
                logger.info("Starting world save for: {}", worldName);
                
                // Save level metadata
                levelMetadata.save();
                
                // Flush journal
                journal.flush();
                
                // Save all dirty chunks through region manager
                regionManager.saveAllDirtyChunks();
                
                // Save all player data
                playerDataManager.saveAllPlayers();
                
                logger.info("Completed world save for: {}", worldName);
                
            } catch (Exception e) {
                logger.error("Failed to save world: {}", worldName, e);
                throw new RuntimeException("World save failed", e);
            } finally {
                saveLock.unlock();
            }
        }, saveExecutor);
    }
    
    /**
     * Creates a snapshot backup of the world.
     * 
     * @return a CompletableFuture that completes when the snapshot is created
     */
    public CompletableFuture<Path> createSnapshot() {
        return snapshotManager.createSnapshot();
    }
    
    /**
     * Shuts down the world save manager and releases all resources.
     */
    public void shutdown() {
        logger.info("Shutting down world save manager for: {}", worldName);
        
        try {
            // Save everything before shutdown
            saveWorld().join();
            
            // Close all managers
            regionManager.close();
            playerDataManager.close();
            journal.close();
            snapshotManager.close();
            
            // Release session lock
            releaseSessionLock();
            
            // Shutdown executor
            saveExecutor.shutdown();
            
        } catch (Exception e) {
            logger.error("Error during world save manager shutdown", e);
        }
        
        logger.info("World save manager shutdown complete for: {}", worldName);
    }
    
    /**
     * Gets the world directory path.
     * 
     * @return the world directory path
     */
    public Path getWorldDirectory() {
        return worldDirectory;
    }
    
    /**
     * Gets the world name.
     * 
     * @return the world name
     */
    public String getWorldName() {
        return worldName;
    }
}