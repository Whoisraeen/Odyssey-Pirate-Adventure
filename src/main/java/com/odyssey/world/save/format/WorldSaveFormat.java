package com.odyssey.world.save.format;

/**
 * Defines the file format specifications and constants for the Odyssey world save system.
 * This format is designed to be similar to Minecraft's world save format while being
 * optimized for maritime gameplay and large ocean worlds.
 * 
 * <p>World Save Structure:
 * <pre>
 * world_name/
 * ├── level.json                    # World metadata and settings
 * ├── session.lock                  # Session lock file
 * ├── playerdata/                   # Player-specific data
 * │   ├── player_uuid.json          # Individual player data
 * │   └── ...
 * ├── region/                       # Region files for chunks
 * │   ├── r.0.0.oreg               # Region file (32x32 chunks)
 * │   ├── r.0.1.oreg
 * │   └── ...
 * ├── journal/                      # Write-ahead journal for crash recovery
 * │   ├── journal-001.wal
 * │   ├── journal-002.wal
 * │   └── ...
 * └── backups/                      # Automated backups
 *     ├── 2024-01-15_14-30-00.zip
 *     └── ...
 * </pre>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public final class WorldSaveFormat {
    
    // Version information
    public static final int WORLD_FORMAT_VERSION = 1;
    public static final String WORLD_FORMAT_NAME = "Odyssey World Format";
    
    // File extensions
    public static final String REGION_FILE_EXTENSION = ".oreg";
    public static final String JOURNAL_FILE_EXTENSION = ".wal";
    public static final String BACKUP_FILE_EXTENSION = ".zip";
    public static final String PLAYER_DATA_EXTENSION = ".json";
    public static final String LEVEL_DATA_EXTENSION = ".json";
    
    // Directory names
    public static final String REGION_DIR = "region";
    public static final String PLAYER_DATA_DIR = "playerdata";
    public static final String JOURNAL_DIR = "journal";
    public static final String BACKUP_DIR = "backups";
    public static final String ENTITIES_DIR = "entities";
    public static final String POI_DIR = "poi";
    public static final String GENERATED_DIR = "generated";
    public static final String STATS_DIR = "stats";
    public static final String ADVANCEMENTS_DIR = "advancements";
    public static final String DATA_DIR = "data";
    public static final String DATAPACKS_DIR = "datapacks";
    
    // Dimension directories
    public static final String DIM_NETHER = "DIM-1";
    public static final String DIM_END = "DIM1";
    
    // File names
    public static final String LEVEL_DATA_FILE = "level.json";
    public static final String LEVEL_DATA_OLD_FILE = "level.dat_old";
    public static final String SESSION_LOCK_FILE = "session.lock";
    public static final String WORLD_ICON_FILE = "icon.png";
    public static final String PACK_MCMETA_FILE = "pack.mcmeta";
    
    // Region file format
    public static final int REGION_SIZE = 32; // 32x32 chunks per region
    public static final int REGION_HEADER_SIZE = 8192; // 8KB header
    public static final int REGION_CHUNK_COUNT = REGION_SIZE * REGION_SIZE; // 1024 chunks
    public static final int REGION_OFFSET_TABLE_SIZE = REGION_CHUNK_COUNT * 4; // 4 bytes per offset
    public static final int REGION_TIMESTAMP_TABLE_SIZE = REGION_CHUNK_COUNT * 4; // 4 bytes per timestamp
    public static final int REGION_FLAGS_TABLE_SIZE = REGION_CHUNK_COUNT * 1; // 1 byte per flags
    
    // Region file magic numbers
    public static final int REGION_MAGIC_NUMBER = 0x4F524547; // "OREG" in ASCII
    public static final int REGION_VERSION = 1;
    
    // Chunk data format
    public static final int CHUNK_DATA_VERSION = 1;
    public static final int MAX_CHUNK_SIZE = 1024 * 1024; // 1MB max per chunk
    
    // Compression types (stored in region flags)
    public static final byte COMPRESSION_NONE = 0;
    public static final byte COMPRESSION_DEFLATE = 1;
    public static final byte COMPRESSION_ZSTD = 2;
    public static final byte COMPRESSION_LZ4 = 3;
    
    // Journal format
    public static final int JOURNAL_MAGIC_NUMBER = 0x4F4A524E; // "OJRN" in ASCII
    public static final int JOURNAL_VERSION = 1;
    public static final int JOURNAL_HEADER_SIZE = 64; // 64 bytes
    public static final int MAX_JOURNAL_ENTRY_SIZE = 64 * 1024; // 64KB max per entry
    public static final int JOURNAL_FILE_SIZE_LIMIT = 16 * 1024 * 1024; // 16MB per journal file
    
    // Session lock format
    public static final int SESSION_LOCK_VERSION = 1;
    public static final long SESSION_HEARTBEAT_INTERVAL_MS = 5000; // 5 seconds
    public static final long SESSION_STALE_TIMEOUT_MS = 30000; // 30 seconds
    
    // Player data format
    public static final int PLAYER_DATA_VERSION = 1;
    public static final int MAX_PLAYER_DATA_SIZE = 1024 * 1024; // 1MB max per player
    
    // Level data format
    public static final int LEVEL_DATA_VERSION = 1;
    
    // Current world save format version
    public static final int CURRENT_VERSION = 1;
    
    // Backup format
    public static final String BACKUP_DATE_FORMAT = "yyyy-MM-dd_HH-mm-ss";
    public static final int DEFAULT_BACKUP_COMPRESSION_LEVEL = 6;
    
    // World generation constants
    public static final long DEFAULT_WORLD_SEED = 0L;
    public static final String DEFAULT_GENERATOR_TYPE = "odyssey:ocean_world";
    
    // Maritime-specific constants
    public static final int TIDAL_CYCLE_MINUTES = 20;
    public static final double DEFAULT_SEA_LEVEL = 64.0;
    public static final double DEFAULT_WAVE_HEIGHT = 2.0;
    public static final double DEFAULT_WIND_SPEED = 5.0;
    
    // Performance constants
    public static final int CHUNK_CACHE_SIZE = 256; // Number of chunks to keep in memory
    public static final int REGION_CACHE_SIZE = 64; // Number of region files to keep open
    public static final int ASYNC_SAVE_QUEUE_SIZE = 1000; // Max pending save operations
    
    // Error codes
    public static final int ERROR_NONE = 0;
    public static final int ERROR_INVALID_FORMAT = 1;
    public static final int ERROR_VERSION_MISMATCH = 2;
    public static final int ERROR_CORRUPTED_DATA = 3;
    public static final int ERROR_IO_FAILURE = 4;
    public static final int ERROR_LOCK_FAILED = 5;
    public static final int ERROR_INSUFFICIENT_SPACE = 6;
    
    /**
     * Gets the region file name for the given region coordinates.
     * 
     * @param regionX the region X coordinate
     * @param regionZ the region Z coordinate
     * @return the region file name
     */
    public static String getRegionFileName(int regionX, int regionZ) {
        return String.format("r.%d.%d%s", regionX, regionZ, REGION_FILE_EXTENSION);
    }
    
    /**
     * Gets the journal file name for the given sequence number.
     * 
     * @param sequenceNumber the journal sequence number
     * @return the journal file name
     */
    public static String getJournalFileName(int sequenceNumber) {
        return String.format("journal-%03d%s", sequenceNumber, JOURNAL_FILE_EXTENSION);
    }
    
    /**
     * Gets the player data file name for the given player UUID.
     * 
     * @param playerUuid the player UUID
     * @return the player data file name
     */
    public static String getPlayerDataFileName(String playerUuid) {
        return playerUuid + PLAYER_DATA_EXTENSION;
    }
    
    /**
     * Gets the backup file name for the given timestamp.
     * 
     * @param timestamp the backup timestamp
     * @return the backup file name
     */
    public static String getBackupFileName(String timestamp) {
        return timestamp + BACKUP_FILE_EXTENSION;
    }
    
    /**
     * Converts chunk coordinates to region coordinates.
     * 
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the region coordinates as [regionX, regionZ]
     */
    public static int[] chunkToRegion(int chunkX, int chunkZ) {
        return new int[] {
            Math.floorDiv(chunkX, REGION_SIZE),
            Math.floorDiv(chunkZ, REGION_SIZE)
        };
    }
    
    /**
     * Converts chunk coordinates to local coordinates within a region.
     * 
     * @param chunkX the chunk X coordinate
     * @param chunkZ the chunk Z coordinate
     * @return the local coordinates as [localX, localZ]
     */
    public static int[] chunkToLocal(int chunkX, int chunkZ) {
        return new int[] {
            Math.floorMod(chunkX, REGION_SIZE),
            Math.floorMod(chunkZ, REGION_SIZE)
        };
    }
    
    /**
     * Converts local region coordinates to a chunk index.
     * 
     * @param localX the local X coordinate (0-31)
     * @param localZ the local Z coordinate (0-31)
     * @return the chunk index (0-1023)
     */
    public static int localToChunkIndex(int localX, int localZ) {
        return localZ * REGION_SIZE + localX;
    }
    
    /**
     * Converts a chunk index to local region coordinates.
     * 
     * @param chunkIndex the chunk index (0-1023)
     * @return the local coordinates as [localX, localZ]
     */
    public static int[] chunkIndexToLocal(int chunkIndex) {
        return new int[] {
            chunkIndex % REGION_SIZE,
            chunkIndex / REGION_SIZE
        };
    }
    
    /**
     * Validates a region file magic number.
     * 
     * @param magic the magic number to validate
     * @return true if valid
     */
    public static boolean isValidRegionMagic(int magic) {
        return magic == REGION_MAGIC_NUMBER;
    }
    
    /**
     * Validates a journal file magic number.
     * 
     * @param magic the magic number to validate
     * @return true if valid
     */
    public static boolean isValidJournalMagic(int magic) {
        return magic == JOURNAL_MAGIC_NUMBER;
    }
    
    /**
     * Validates a compression type.
     * 
     * @param compressionType the compression type to validate
     * @return true if valid
     */
    public static boolean isValidCompressionType(byte compressionType) {
        return compressionType >= COMPRESSION_NONE && compressionType <= COMPRESSION_LZ4;
    }
    
    /**
     * Gets the default compression type.
     * 
     * @return the default compression type
     */
    public static byte getDefaultCompressionType() {
        return COMPRESSION_ZSTD; // Use Zstandard by default for best performance
    }
    
    /**
     * Checks if a chunk size is valid.
     * 
     * @param size the chunk size to validate
     * @return true if valid
     */
    public static boolean isValidChunkSize(int size) {
        return size > 0 && size <= MAX_CHUNK_SIZE;
    }
    
    /**
     * Checks if a journal entry size is valid.
     * 
     * @param size the entry size to validate
     * @return true if valid
     */
    public static boolean isValidJournalEntrySize(int size) {
        return size > 0 && size <= MAX_JOURNAL_ENTRY_SIZE;
    }
    
    /**
     * Gets the estimated disk space required for a world with the given parameters.
     * 
     * @param chunkCount the estimated number of chunks
     * @param avgChunkSize the average chunk size in bytes
     * @param playerCount the number of players
     * @return the estimated disk space in bytes
     */
    public static long estimateWorldSize(int chunkCount, int avgChunkSize, int playerCount) {
        long chunkData = (long) chunkCount * avgChunkSize;
        long playerData = (long) playerCount * (MAX_PLAYER_DATA_SIZE / 4); // Assume 25% of max
        long metadata = 1024 * 1024; // 1MB for metadata
        long journalOverhead = JOURNAL_FILE_SIZE_LIMIT * 2; // Keep 2 journal files
        
        return chunkData + playerData + metadata + journalOverhead;
    }
    
    // Private constructor to prevent instantiation
    private WorldSaveFormat() {
        throw new UnsupportedOperationException("Utility class");
    }
}