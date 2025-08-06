package com.odyssey.world.chunk;

import com.odyssey.world.generation.WorldGenerator.BlockType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles serialization and deserialization of chunks to/from disk using
 * compressed binary format for optimal storage efficiency and I/O performance.
 * 
 * <p>Key features:
 * <ul>
 *   <li>GZip compression for reduced disk usage (typically 80-95% compression)</li>
 *   <li>Asynchronous I/O operations to prevent main thread blocking</li>
 *   <li>Optimized binary format for fast serialization</li>
 *   <li>Support for both regular and palettized chunks</li>
 *   <li>Automatic directory structure management</li>
 * </ul>
 * 
 * <p>File format:
 * <pre>
 * Header (16 bytes):
 *   - Magic number (4 bytes): "ODCH"
 *   - Version (4 bytes): format version
 *   - Chunk X (4 bytes): chunk x coordinate
 *   - Chunk Z (4 bytes): chunk z coordinate
 * 
 * Chunk Data (compressed):
 *   - Chunk type (1 byte): 0=regular, 1=palettized
 *   - Block data: varies by chunk type
 *   - Metadata: timestamps, flags, etc.
 * </pre>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class ChunkSerializer {
    private static final Logger logger = LoggerFactory.getLogger(ChunkSerializer.class);
    
    /** Magic number for chunk files. */
    private static final int MAGIC_NUMBER = 0x4F444348; // "ODCH"
    
    /** Current file format version. */
    private static final int FORMAT_VERSION = 1;
    
    /** Chunk type identifier for regular chunks. */
    private static final byte CHUNK_TYPE_REGULAR = 0;
    
    /** Chunk type identifier for palettized chunks. */
    private static final byte CHUNK_TYPE_PALETTIZED = 1;
    
    private final Path worldDirectory;
    private final ExecutorService ioExecutor;
    
    /**
     * Creates a new chunk serializer for the specified world directory.
     * 
     * @param worldDirectory the directory to store chunk files
     */
    public ChunkSerializer(Path worldDirectory) {
        this.worldDirectory = worldDirectory;
        this.ioExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "ChunkIO-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        // Create world directory if it doesn't exist
        try {
            Files.createDirectories(worldDirectory);
        } catch (IOException e) {
            logger.error("Failed to create world directory: {}", worldDirectory, e);
        }
    }
    
    /**
     * Creates a new chunk serializer with default world directory.
     */
    public ChunkSerializer() {
        this(Paths.get("world", "chunks"));
    }
    
    /**
     * Saves a chunk to disk asynchronously.
     * 
     * @param chunk the chunk to save
     * @return a CompletableFuture that completes when the save operation finishes
     */
    public CompletableFuture<Void> saveChunkAsync(Chunk chunk) {
        if (chunk == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.runAsync(() -> {
            try {
                saveChunk(chunk);
            } catch (IOException e) {
                logger.error("Failed to save chunk {}", chunk.getPosition(), e);
                throw new RuntimeException(e);
            }
        }, ioExecutor);
    }
    
    /**
     * Saves a chunk to disk synchronously.
     * 
     * @param chunk the chunk to save
     * @throws IOException if an I/O error occurs
     */
    public void saveChunk(Chunk chunk) throws IOException {
        if (chunk == null) {
            return;
        }
        
        Path chunkFile = getChunkFile(chunk.getPosition());
        
        // Create parent directories if needed
        Files.createDirectories(chunkFile.getParent());
        
        try (FileOutputStream fos = new FileOutputStream(chunkFile.toFile());
             GZIPOutputStream gzos = new GZIPOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(gzos)) {
            
            // Write header
            dos.writeInt(MAGIC_NUMBER);
            dos.writeInt(FORMAT_VERSION);
            dos.writeInt(chunk.getPosition().x);
            dos.writeInt(chunk.getPosition().z);
            
            // Write chunk type and data
            if (chunk instanceof PalettizedChunk) {
                dos.writeByte(CHUNK_TYPE_PALETTIZED);
                writePalettizedChunk((PalettizedChunk) chunk, dos);
            } else {
                dos.writeByte(CHUNK_TYPE_REGULAR);
                writeRegularChunk(chunk, dos);
            }
            
            // Write metadata
            dos.writeLong(chunk.getLastAccessTime());
            dos.writeLong(chunk.getLastModificationTime());
            dos.writeBoolean(chunk.isGenerated());
            dos.writeBoolean(chunk.isLoaded());
            
        }
        
        logger.debug("Saved chunk {} to {}", chunk.getPosition(), chunkFile);
    }
    
    /**
     * Loads a chunk from disk asynchronously.
     * 
     * @param position the chunk position
     * @return a CompletableFuture containing the loaded chunk, or null if not found
     */
    public CompletableFuture<Chunk> loadChunkAsync(ChunkPosition position) {
        if (position == null) {
            return CompletableFuture.completedFuture(null);
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadChunk(position);
            } catch (IOException e) {
                logger.error("Failed to load chunk {}", position, e);
                return null;
            }
        }, ioExecutor);
    }
    
    /**
     * Loads a chunk from disk synchronously.
     * 
     * @param position the chunk position
     * @return the loaded chunk, or null if not found
     * @throws IOException if an I/O error occurs
     */
    public Chunk loadChunk(ChunkPosition position) throws IOException {
        if (position == null) {
            return null;
        }
        
        Path chunkFile = getChunkFile(position);
        
        if (!Files.exists(chunkFile)) {
            return null;
        }
        
        try (FileInputStream fis = new FileInputStream(chunkFile.toFile());
             GZIPInputStream gzis = new GZIPInputStream(fis);
             DataInputStream dis = new DataInputStream(gzis)) {
            
            // Read and validate header
            int magic = dis.readInt();
            if (magic != MAGIC_NUMBER) {
                throw new IOException("Invalid chunk file magic number: " + Integer.toHexString(magic));
            }
            
            int version = dis.readInt();
            if (version != FORMAT_VERSION) {
                throw new IOException("Unsupported chunk file version: " + version);
            }
            
            int chunkX = dis.readInt();
            int chunkZ = dis.readInt();
            
            if (chunkX != position.x || chunkZ != position.z) {
                throw new IOException("Chunk position mismatch in file");
            }
            
            // Read chunk type and data
            byte chunkType = dis.readByte();
            Chunk chunk;
            
            switch (chunkType) {
                case CHUNK_TYPE_REGULAR:
                    chunk = readRegularChunk(position, dis);
                    break;
                case CHUNK_TYPE_PALETTIZED:
                    chunk = readPalettizedChunk(position, dis);
                    break;
                default:
                    throw new IOException("Unknown chunk type: " + chunkType);
            }
            
            // Read metadata
            long lastAccessTime = dis.readLong();
            long lastModificationTime = dis.readLong();
            boolean isGenerated = dis.readBoolean();
            boolean isLoaded = dis.readBoolean();
            
            // Apply metadata (using reflection to access private fields)
            chunk.markLoaded();
            if (isGenerated) {
                chunk.markGenerated();
            }
            
            logger.debug("Loaded chunk {} from {}", position, chunkFile);
            return chunk;
        }
    }
    
    /**
     * Checks if a chunk file exists on disk.
     * 
     * @param position the chunk position
     * @return true if the chunk file exists
     */
    public boolean chunkExists(ChunkPosition position) {
        if (position == null) {
            return false;
        }
        return Files.exists(getChunkFile(position));
    }
    
    /**
     * Deletes a chunk file from disk.
     * 
     * @param position the chunk position
     * @return true if the file was deleted
     */
    public boolean deleteChunk(ChunkPosition position) {
        if (position == null) {
            return false;
        }
        
        Path chunkFile = getChunkFile(position);
        try {
            boolean deleted = Files.deleteIfExists(chunkFile);
            if (deleted) {
                logger.debug("Deleted chunk file {}", chunkFile);
            }
            return deleted;
        } catch (IOException e) {
            logger.error("Failed to delete chunk file {}", chunkFile, e);
            return false;
        }
    }
    
    /**
     * Gets the file path for a chunk.
     * 
     * @param position the chunk position
     * @return the chunk file path
     */
    private Path getChunkFile(ChunkPosition position) {
        // Organize chunks into subdirectories to avoid too many files in one directory
        int regionX = position.x >> 5; // 32 chunks per region
        int regionZ = position.z >> 5;
        
        String fileName = String.format("chunk_%d_%d.dat", position.x, position.z);
        return worldDirectory.resolve(String.format("r.%d.%d", regionX, regionZ)).resolve(fileName);
    }
    
    /**
     * Writes a regular chunk to the output stream.
     * 
     * @param chunk the chunk to write
     * @param dos the data output stream
     * @throws IOException if an I/O error occurs
     */
    private void writeRegularChunk(Chunk chunk, DataOutputStream dos) throws IOException {
        // Write block data
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    BlockType blockType = chunk.getBlock(x, y, z);
                    dos.writeInt(blockType.id);
                }
            }
        }
    }
    
    /**
     * Writes a palettized chunk to the output stream.
     * 
     * @param chunk the palettized chunk to write
     * @param dos the data output stream
     * @throws IOException if an I/O error occurs
     */
    private void writePalettizedChunk(PalettizedChunk chunk, DataOutputStream dos) throws IOException {
        BlockPalette palette = chunk.getPalette();
        
        // Write palette
        dos.writeInt(palette.size());
        for (BlockType blockType : palette.getAllBlockTypes()) {
            dos.writeInt(blockType.id);
        }
        
        // Write bits per index
        dos.writeInt(chunk.getBitsPerIndex());
        
        // Write block indices
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    BlockType blockType = chunk.getBlock(x, y, z);
                    int index = palette.getIndex(blockType);
                    dos.writeInt(index);
                }
            }
        }
    }
    
    /**
     * Reads a regular chunk from the input stream.
     * 
     * @param position the chunk position
     * @param dis the data input stream
     * @return the loaded chunk
     * @throws IOException if an I/O error occurs
     */
    private Chunk readRegularChunk(ChunkPosition position, DataInputStream dis) throws IOException {
        Chunk chunk = new Chunk(position);
        
        // Read block data
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    int blockId = dis.readInt();
                    BlockType blockType = BlockType.values()[Math.min(blockId, BlockType.values().length - 1)];
                    chunk.setBlock(x, y, z, blockType);
                }
            }
        }
        
        return chunk;
    }
    
    /**
     * Reads a palettized chunk from the input stream.
     * 
     * @param position the chunk position
     * @param dis the data input stream
     * @return the loaded palettized chunk
     * @throws IOException if an I/O error occurs
     */
    private PalettizedChunk readPalettizedChunk(ChunkPosition position, DataInputStream dis) throws IOException {
        // Read palette
        int paletteSize = dis.readInt();
        BlockType[] paletteBlocks = new BlockType[paletteSize];
        for (int i = 0; i < paletteSize; i++) {
            int blockId = dis.readInt();
            paletteBlocks[i] = BlockType.values()[Math.min(blockId, BlockType.values().length - 1)];
        }
        
        // Read bits per index
        int bitsPerIndex = dis.readInt();
        
        PalettizedChunk chunk = new PalettizedChunk(position);
        
        // Read block indices and set blocks
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    int index = dis.readInt();
                    if (index < paletteBlocks.length) {
                        chunk.setBlock(x, y, z, paletteBlocks[index]);
                    }
                }
            }
        }
        
        return chunk;
    }
    
    /**
     * Shuts down the I/O executor service.
     */
    public void shutdown() {
        ioExecutor.shutdown();
        logger.info("Chunk serializer shut down");
    }
}