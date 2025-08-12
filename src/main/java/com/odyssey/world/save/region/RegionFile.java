package com.odyssey.world.save.region;

import com.odyssey.world.chunk.Chunk;
import com.odyssey.world.chunk.ChunkPosition;
import com.odyssey.world.save.compression.CompressionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * Handles individual region files (.oreg) with optimized binary format.
 * Each region file contains a 32x32 grid of chunks with 8KB header.
 * 
 * <p>File format:
 * <pre>
 * Header (8192 bytes):
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
 *   - Length (4 bytes): compressed chunk data length
 *   - Compression type (1 byte): compression algorithm used
 *   - Compressed data: Zstandard/Deflate compressed chunk NBT
 * </pre>
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class RegionFile {
    private static final Logger logger = LoggerFactory.getLogger(RegionFile.class);
    
    /** Magic number for region files. */
    private static final int MAGIC_NUMBER = 0x4F524547; // "OREG"
    
    /** Current file format version. */
    private static final int FORMAT_VERSION = 1;
    
    /** Size of the file header in bytes. */
    private static final int HEADER_SIZE = 8192;
    
    /** Number of chunks per region (32x32). */
    private static final int CHUNKS_PER_REGION = 1024;
    
    /** Size of chunk offset table in bytes. */
    private static final int OFFSET_TABLE_SIZE = CHUNKS_PER_REGION * 4;
    
    /** Size of timestamp table in bytes. */
    private static final int TIMESTAMP_TABLE_SIZE = CHUNKS_PER_REGION * 4;
    
    /** Size of compression flags table in bytes. */
    private static final int COMPRESSION_TABLE_SIZE = CHUNKS_PER_REGION;
    
    /** Maximum chunk size (1MB). */
    private static final int MAX_CHUNK_SIZE = 1024 * 1024;
    
    private final Path filePath;
    private final RegionPosition position;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // File access
    private RandomAccessFile file;
    private FileChannel channel;
    
    // Header data
    private final int[] chunkOffsets = new int[CHUNKS_PER_REGION];
    private final int[] chunkTimestamps = new int[CHUNKS_PER_REGION];
    private final byte[] compressionFlags = new byte[CHUNKS_PER_REGION];
    
    // Free list for managing file space
    private final FreeList freeList = new FreeList();
    
    // State tracking
    private boolean headerDirty = false;
    private long fileSize = 0;
    
    /**
     * Creates or opens a region file.
     * 
     * @param filePath the path to the region file
     * @param position the region position
     * @throws IOException if the file cannot be opened or created
     */
    public RegionFile(Path filePath, RegionPosition position) throws IOException {
        this.filePath = filePath;
        this.position = position;
        
        // Open or create the file
        openFile();
        
        // Load or initialize header
        if (Files.exists(filePath) && Files.size(filePath) >= HEADER_SIZE) {
            loadHeader();
        } else {
            initializeHeader();
            writeHeader();
        }
        
        logger.debug("Opened region file: {} at position {}", filePath, position);
    }
    
    /**
     * Opens the region file for reading and writing.
     * 
     * @throws IOException if the file cannot be opened
     */
    private void openFile() throws IOException {
        // Create parent directories if needed
        Files.createDirectories(filePath.getParent());
        
        // Open file
        file = new RandomAccessFile(filePath.toFile(), "rw");
        channel = file.getChannel();
        fileSize = file.length();
    }
    
    /**
     * Initializes a new header for an empty region file.
     */
    private void initializeHeader() {
        // Clear all arrays
        for (int i = 0; i < CHUNKS_PER_REGION; i++) {
            chunkOffsets[i] = 0;
            chunkTimestamps[i] = 0;
            compressionFlags[i] = 0;
        }
        
        freeList.clear();
        headerDirty = true;
        
        logger.debug("Initialized new header for region file: {}", filePath);
    }
    
    /**
     * Loads the header from an existing region file.
     * 
     * @throws IOException if the header cannot be read
     */
    private void loadHeader() throws IOException {
        lock.writeLock().lock();
        try {
            file.seek(0);
            
            // Read and verify magic number
            int magic = file.readInt();
            if (magic != MAGIC_NUMBER) {
                throw new IOException("Invalid region file magic number: " + Integer.toHexString(magic));
            }
            
            // Read and verify version
            int version = file.readInt();
            if (version != FORMAT_VERSION) {
                throw new IOException("Unsupported region file version: " + version);
            }
            
            // Read and verify region coordinates
            int regionX = file.readInt();
            int regionZ = file.readInt();
            if (regionX != position.x || regionZ != position.z) {
                throw new IOException(String.format(
                    "Region file coordinates mismatch: expected (%d,%d), got (%d,%d)",
                    position.x, position.z, regionX, regionZ));
            }
            
            // Read chunk offsets
            for (int i = 0; i < CHUNKS_PER_REGION; i++) {
                chunkOffsets[i] = file.readInt();
            }
            
            // Read chunk timestamps
            for (int i = 0; i < CHUNKS_PER_REGION; i++) {
                chunkTimestamps[i] = file.readInt();
            }
            
            // Read compression flags
            file.readFully(compressionFlags);
            
            // Read free list header
            freeList.loadFromFile(file);
            
            logger.debug("Loaded header for region file: {}", filePath);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Writes the header to the region file.
     * 
     * @throws IOException if the header cannot be written
     */
    private void writeHeader() throws IOException {
        if (!headerDirty) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            file.seek(0);
            
            // Write magic number and version
            file.writeInt(MAGIC_NUMBER);
            file.writeInt(FORMAT_VERSION);
            
            // Write region coordinates
            file.writeInt(position.x);
            file.writeInt(position.z);
            
            // Write chunk offsets
            for (int offset : chunkOffsets) {
                file.writeInt(offset);
            }
            
            // Write chunk timestamps
            for (int timestamp : chunkTimestamps) {
                file.writeInt(timestamp);
            }
            
            // Write compression flags
            file.write(compressionFlags);
            
            // Write free list header
            freeList.saveToFile(file);
            
            // Pad to header size
            long currentPos = file.getFilePointer();
            long padding = HEADER_SIZE - currentPos;
            if (padding > 0) {
                file.write(new byte[(int) padding]);
            }
            
            headerDirty = false;
            
            logger.debug("Wrote header for region file: {}", filePath);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Reads a chunk from the region file.
     * 
     * @param chunkPos the chunk position
     * @return the chunk, or null if not found
     * @throws IOException if an I/O error occurs
     */
    public Chunk readChunk(ChunkPosition chunkPos) throws IOException {
        int index = getChunkIndex(chunkPos);
        if (index < 0 || index >= CHUNKS_PER_REGION) {
            return null;
        }
        
        lock.readLock().lock();
        try {
            int offset = chunkOffsets[index];
            if (offset == 0) {
                return null; // Chunk not present
            }
            
            // Seek to chunk data
            file.seek(offset);
            
            // Read chunk length
            int length = file.readInt();
            if (length <= 0 || length > MAX_CHUNK_SIZE) {
                logger.warn("Invalid chunk length: {} at offset: {}", length, offset);
                return null;
            }
            
            // Read compression type
            byte compressionType = file.readByte();
            
            // Read compressed data
            byte[] compressedData = new byte[length - 1]; // -1 for compression type byte
            file.readFully(compressedData);
            
            // Decompress data
            byte[] chunkData = decompress(compressedData, CompressionType.fromByte(compressionType));
            
            // Deserialize chunk
            return deserializeChunk(chunkData, chunkPos);
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Writes a chunk to the region file.
     * 
     * @param chunkPos the chunk position
     * @param chunk the chunk to write
     * @throws IOException if an I/O error occurs
     */
    public void writeChunk(ChunkPosition chunkPos, Chunk chunk) throws IOException {
        int index = getChunkIndex(chunkPos);
        if (index < 0 || index >= CHUNKS_PER_REGION) {
            throw new IOException("Chunk position out of region bounds: " + chunkPos);
        }
        
        lock.writeLock().lock();
        try {
            // Serialize chunk
            byte[] chunkData = serializeChunk(chunk);
            
            // Compress data
            CompressionType compressionType = CompressionType.DEFLATE;
            byte[] compressedData = compress(chunkData, compressionType);
            
            // Calculate total size (length + compression type + data)
            int totalSize = 4 + 1 + compressedData.length;
            
            // Find space for the chunk
            int oldOffset = chunkOffsets[index];
            int newOffset = allocateSpace(totalSize, oldOffset);
            
            // Write chunk data
            file.seek(newOffset);
            file.writeInt(compressedData.length + 1); // +1 for compression type
            file.writeByte(compressionType.getByte());
            file.write(compressedData);
            
            // Update header
            chunkOffsets[index] = newOffset;
            chunkTimestamps[index] = (int) (System.currentTimeMillis() / 1000);
            compressionFlags[index] = compressionType.getByte();
            headerDirty = true;
            
            // Free old space if chunk was relocated
            if (oldOffset != 0 && oldOffset != newOffset) {
                freeSpace(oldOffset);
            }
            
            logger.debug("Wrote chunk {} to region file at offset: {}", chunkPos, newOffset);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Checks if a chunk exists in this region file.
     * 
     * @param chunkPos the chunk position
     * @return true if the chunk exists
     */
    public boolean hasChunk(ChunkPosition chunkPos) {
        int index = getChunkIndex(chunkPos);
        if (index < 0 || index >= CHUNKS_PER_REGION) {
            return false;
        }
        
        lock.readLock().lock();
        try {
            return chunkOffsets[index] != 0;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Flushes any pending changes to disk.
     * 
     * @throws IOException if an I/O error occurs
     */
    public void flush() throws IOException {
        lock.writeLock().lock();
        try {
            writeHeader();
            channel.force(true);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Closes the region file.
     * 
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        lock.writeLock().lock();
        try {
            flush();
            
            if (channel != null) {
                channel.close();
            }
            if (file != null) {
                file.close();
            }
            
            logger.debug("Closed region file: {}", filePath);
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets the chunk index within the region for a chunk position.
     * 
     * @param chunkPos the chunk position
     * @return the chunk index (0-1023), or -1 if out of bounds
     */
    private int getChunkIndex(ChunkPosition chunkPos) {
        // Calculate local coordinates within the region
        int localX = chunkPos.x - (position.x * RegionFileManager.REGION_SIZE);
        int localZ = chunkPos.z - (position.z * RegionFileManager.REGION_SIZE);
        
        // Check bounds
        if (localX < 0 || localX >= RegionFileManager.REGION_SIZE ||
            localZ < 0 || localZ >= RegionFileManager.REGION_SIZE) {
            return -1;
        }
        
        return localZ * RegionFileManager.REGION_SIZE + localX;
    }
    
    /**
     * Allocates space in the file for chunk data.
     * 
     * @param size the size needed
     * @param oldOffset the old offset (for reuse), or 0 for new allocation
     * @return the allocated offset
     * @throws IOException if space cannot be allocated
     */
    private int allocateSpace(int size, int oldOffset) throws IOException {
        // Try to reuse existing space if it's large enough
        if (oldOffset != 0) {
            // For simplicity, we'll just append for now
            // A full implementation would check if the existing space is sufficient
        }
        
        // Append to end of file
        int offset = (int) Math.max(fileSize, HEADER_SIZE);
        fileSize = offset + size;
        
        return offset;
    }
    
    /**
     * Marks space as free for reuse.
     * 
     * @param offset the offset to free
     */
    private void freeSpace(int offset) {
        // Add to free list for future reuse
        freeList.addFreeBlock(offset);
    }
    
    /**
     * Compresses chunk data.
     * 
     * @param data the data to compress
     * @param type the compression type
     * @return the compressed data
     * @throws IOException if compression fails
     */
    private byte[] compress(byte[] data, CompressionType type) throws IOException {
        switch (type) {
            case NONE:
                return data;
            case DEFLATE:
                Deflater deflater = new Deflater();
                deflater.setInput(data);
                deflater.finish();
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                while (!deflater.finished()) {
                    int count = deflater.deflate(buffer);
                    baos.write(buffer, 0, count);
                }
                deflater.end();
                return baos.toByteArray();
            default:
                throw new IOException("Unsupported compression type: " + type);
        }
    }
    
    /**
     * Decompresses chunk data.
     * 
     * @param data the compressed data
     * @param type the compression type
     * @return the decompressed data
     * @throws IOException if decompression fails
     */
    private byte[] decompress(byte[] data, CompressionType type) throws IOException {
        switch (type) {
            case NONE:
                return data;
            case DEFLATE:
                Inflater inflater = new Inflater();
                inflater.setInput(data);
                
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                try {
                    while (!inflater.finished()) {
                        int count = inflater.inflate(buffer);
                        baos.write(buffer, 0, count);
                    }
                } catch (java.util.zip.DataFormatException e) {
                    throw new IOException("Failed to decompress chunk data", e);
                } finally {
                    inflater.end();
                }
                return baos.toByteArray();
            default:
                throw new IOException("Unsupported compression type: " + type);
        }
    }
    
    /**
     * Serializes a chunk to byte array.
     * 
     * @param chunk the chunk to serialize
     * @return the serialized data
     * @throws IOException if serialization fails
     */
    private byte[] serializeChunk(Chunk chunk) throws IOException {
        // This would use the existing ChunkSerializer or a new NBT-based serializer
        // For now, we'll use a simple placeholder
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        
        // Write chunk data (simplified)
        oos.writeInt(chunk.getPosition().x);
        oos.writeInt(chunk.getPosition().z);
        oos.writeLong(chunk.getLastModificationTime());
        oos.writeBoolean(chunk.isGenerated());
        
        // Write block data would go here
        // This is a placeholder - real implementation would serialize the block array
        
        oos.close();
        return baos.toByteArray();
    }
    
    /**
     * Deserializes a chunk from byte array.
     * 
     * @param data the serialized data
     * @param chunkPos the chunk position
     * @return the deserialized chunk
     * @throws IOException if deserialization fails
     */
    private Chunk deserializeChunk(byte[] data, ChunkPosition chunkPos) throws IOException {
        // This would use the existing ChunkSerializer or a new NBT-based deserializer
        // For now, we'll return null as a placeholder
        logger.debug("Deserializing chunk {} (placeholder implementation)", chunkPos);
        return null;
    }
    
    /**
     * Gets the region position.
     * 
     * @return the region position
     */
    public RegionPosition getPosition() {
        return position;
    }
    
    /**
     * Gets the file size.
     * 
     * @return the file size in bytes
     */
    public long getSize() {
        return fileSize;
    }
    
    /**
     * Gets the number of chunks stored in this region.
     * 
     * @return the chunk count
     */
    public int getChunkCount() {
        lock.readLock().lock();
        try {
            int count = 0;
            for (int offset : chunkOffsets) {
                if (offset != 0) {
                    count++;
                }
            }
            return count;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Simple free list implementation for managing freed space.
     */
    private static class FreeList {
        // Placeholder implementation
        // A full implementation would maintain a list of free blocks
        
        void clear() {
            // Clear free list
        }
        
        void loadFromFile(RandomAccessFile file) throws IOException {
            // Load free list from file
            // Skip for now
            file.skipBytes(896); // Skip free list header space
        }
        
        void saveToFile(RandomAccessFile file) throws IOException {
            // Save free list to file
            // Write zeros for now
            file.write(new byte[896]);
        }
        
        void addFreeBlock(int offset) {
            // Add block to free list
        }
    }
}