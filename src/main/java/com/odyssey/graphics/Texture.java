package com.odyssey.graphics;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/**
 * Texture class for The Odyssey with compression support.
 * Handles texture loading, compression, and GPU upload with memory optimization.
 */
public class Texture {
    private static final Logger logger = LoggerFactory.getLogger(Texture.class);
    
    private final int textureId;
    private final int width;
    private final int height;
    private final TextureCompression.CompressionFormat format;
    private final boolean compressed;
    private final int memoryUsage;
    
    /**
     * Texture filtering modes.
     */
    public enum FilterMode {
        NEAREST(GL11.GL_NEAREST),
        LINEAR(GL11.GL_LINEAR),
        NEAREST_MIPMAP_NEAREST(GL11.GL_NEAREST_MIPMAP_NEAREST),
        LINEAR_MIPMAP_NEAREST(GL11.GL_LINEAR_MIPMAP_NEAREST),
        NEAREST_MIPMAP_LINEAR(GL11.GL_NEAREST_MIPMAP_LINEAR),
        LINEAR_MIPMAP_LINEAR(GL11.GL_LINEAR_MIPMAP_LINEAR);
        
        private final int glValue;
        
        FilterMode(int glValue) {
            this.glValue = glValue;
        }
        
        public int getGLValue() {
            return glValue;
        }
    }
    
    /**
     * Texture wrap modes.
     */
    public enum WrapMode {
        REPEAT(GL11.GL_REPEAT),
        CLAMP_TO_EDGE(GL12.GL_CLAMP_TO_EDGE),
        CLAMP_TO_BORDER(GL13.GL_CLAMP_TO_BORDER),
        MIRRORED_REPEAT(GL14.GL_MIRRORED_REPEAT);
        
        private final int glValue;
        
        WrapMode(int glValue) {
            this.glValue = glValue;
        }
        
        public int getGLValue() {
            return glValue;
        }
    }
    
    /**
     * Creates a texture from compressed texture data.
     *
     * @param compressedTexture Compressed texture data
     * @param generateMipmaps Whether to generate mipmaps
     */
    public Texture(TextureCompression.CompressedTexture compressedTexture, boolean generateMipmaps) {
        this.textureId = GL11.glGenTextures();
        this.width = compressedTexture.getWidth();
        this.height = compressedTexture.getHeight();
        this.format = compressedTexture.getFormat();
        this.compressed = (format != TextureCompression.CompressionFormat.RGB && 
                          format != TextureCompression.CompressionFormat.RGBA);
        this.memoryUsage = compressedTexture.getCompressedSize();
        
        // Upload to GPU
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        
        TextureCompression.uploadCompressedTexture(compressedTexture, textureId, 0);
        
        if (generateMipmaps) {
            if (compressed) {
                // For compressed textures, we need to generate mipmaps manually
                // or use GL_GENERATE_MIPMAP if supported
                logger.warn("Mipmap generation for compressed textures not fully implemented");
            } else {
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
            }
        }
        
        // Set default filtering
        setFilterMode(generateMipmaps ? FilterMode.LINEAR_MIPMAP_LINEAR : FilterMode.LINEAR);
        setWrapMode(WrapMode.REPEAT);
        
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        
        logger.debug("Created texture {}x{} ({}KB, format: {}, compressed: {})", 
                    width, height, memoryUsage / 1024, format, compressed);
    }
    
    /**
     * Loads a texture from file with automatic compression.
     *
     * @param filePath Path to the texture file
     * @param quality Compression quality (0.0 = lowest, 1.0 = highest)
     * @param generateMipmaps Whether to generate mipmaps
     * @return Loaded texture
     */
    public static Texture loadFromFile(String filePath, float quality, boolean generateMipmaps) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer widthBuffer = stack.mallocInt(1);
            IntBuffer heightBuffer = stack.mallocInt(1);
            IntBuffer channelsBuffer = stack.mallocInt(1);
            
            // Load image data
            ByteBuffer imageData = STBImage.stbi_load(filePath, widthBuffer, heightBuffer, 
                                                          channelsBuffer, 0);
            
            if (imageData == null) {
                throw new RuntimeException("Failed to load texture: " + filePath + 
                                         " - " + STBImage.stbi_failure_reason());
            }
            
            int width = widthBuffer.get(0);
            int height = heightBuffer.get(0);
            int channels = channelsBuffer.get(0);
            
            logger.debug("Loaded image {}x{} with {} channels from {}", width, height, channels, filePath);
            
            // Select compression format
            boolean hasAlpha = channels == 4;
            TextureCompression.CompressionFormat compressionFormat = 
                TextureCompression.selectBestFormat(hasAlpha, quality);
            
            // Compress texture
            TextureCompression.CompressedTexture compressedTexture = 
                TextureCompression.compressTexture(imageData, width, height, compressionFormat);
            
            // Free original image data
            STBImage.stbi_image_free(imageData);
            
            // Create texture
            Texture texture = new Texture(compressedTexture, generateMipmaps);
            
            logger.info("Loaded texture: {} ({})", filePath, 
                       TextureCompression.calculateMemorySavings(width * height * 4, 
                                                               compressedTexture.getCompressedSize()));
            
            return texture;
        }
    }
    
    /**
     * Creates a texture from raw RGBA data.
     *
     * @param data RGBA pixel data
     * @param width Texture width
     * @param height Texture height
     * @param quality Compression quality
     * @param generateMipmaps Whether to generate mipmaps
     * @return Created texture
     */
    public static Texture createFromData(ByteBuffer data, int width, int height, 
                                        float quality, boolean generateMipmaps) {
        // Select compression format (assume RGBA input)
        TextureCompression.CompressionFormat compressionFormat = 
            TextureCompression.selectBestFormat(true, quality);
        
        // Compress texture
        TextureCompression.CompressedTexture compressedTexture = 
            TextureCompression.compressTexture(data, width, height, compressionFormat);
        
        return new Texture(compressedTexture, generateMipmaps);
    }
    
    /**
     * Binds this texture for rendering.
     *
     * @param textureUnit Texture unit to bind to (0-31)
     */
    public void bind(int textureUnit) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }
    
    /**
     * Binds this texture to texture unit 0.
     */
    public void bind() {
        bind(0);
    }
    
    /**
     * Unbinds any texture from the specified unit.
     *
     * @param textureUnit Texture unit to unbind
     */
    public static void unbind(int textureUnit) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0 + textureUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
    
    /**
     * Unbinds any texture from unit 0.
     */
    public static void unbind() {
        unbind(0);
    }
    
    /**
     * Sets the texture filtering mode.
     *
     * @param filterMode Filtering mode to use
     */
    public void setFilterMode(FilterMode filterMode) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filterMode.getGLValue());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, 
                           filterMode == FilterMode.NEAREST ? GL11.GL_NEAREST : GL11.GL_LINEAR);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
    
    /**
     * Sets the texture wrap mode.
     *
     * @param wrapMode Wrap mode to use
     */
    public void setWrapMode(WrapMode wrapMode) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrapMode.getGLValue());
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrapMode.getGLValue());
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
    
    /**
     * Sets anisotropic filtering level.
     *
     * @param level Anisotropic filtering level (1.0 = disabled, higher = more filtering)
     */
    public void setAnisotropicFiltering(float level) {
        // Check if anisotropic filtering is supported
        String extensions = GL11.glGetString(GL11.GL_EXTENSIONS);
        if (extensions != null && extensions.contains("GL_EXT_texture_filter_anisotropic")) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glTexParameterf(GL11.GL_TEXTURE_2D, 0x84FE, level); // GL_TEXTURE_MAX_ANISOTROPY_EXT
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        }
    }
    
    /**
     * Disposes of this texture and frees GPU memory.
     */
    public void dispose() {
        GL11.glDeleteTextures(textureId);
        logger.debug("Disposed texture {}x{} ({}KB)", width, height, memoryUsage / 1024);
    }
    
    // Getters
    
    public int getTextureId() {
        return textureId;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public TextureCompression.CompressionFormat getFormat() {
        return format;
    }
    
    public boolean isCompressed() {
        return compressed;
    }
    
    public int getMemoryUsage() {
        return memoryUsage;
    }
    
    /**
     * Gets texture information as a string.
     *
     * @return Texture information
     */
    public String getInfo() {
        return String.format("Texture[%dx%d, %s, %s, %dKB]", 
                           width, height, format, compressed ? "compressed" : "uncompressed", 
                           memoryUsage / 1024);
    }
}