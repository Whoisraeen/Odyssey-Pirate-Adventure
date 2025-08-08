package com.odyssey.world.save.compression;

/**
 * Defines compression algorithms used for chunk data storage.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public enum CompressionType {
    /** No compression. */
    NONE((byte) 0),
    
    /** Deflate compression (zlib). */
    DEFLATE((byte) 1),
    
    /** Zstandard compression (future). */
    ZSTD((byte) 2),
    
    /** LZ4 compression (future). */
    LZ4((byte) 3);
    
    private final byte value;
    
    CompressionType(byte value) {
        this.value = value;
    }
    
    /**
     * Gets the byte value for this compression type.
     * 
     * @return the byte value
     */
    public byte getByte() {
        return value;
    }
    
    /**
     * Gets the compression type from a byte value.
     * 
     * @param value the byte value
     * @return the compression type
     * @throws IllegalArgumentException if the value is invalid
     */
    public static CompressionType fromByte(byte value) {
        for (CompressionType type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown compression type: " + value);
    }
}