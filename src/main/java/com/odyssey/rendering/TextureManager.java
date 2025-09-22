package com.odyssey.rendering;

import com.odyssey.util.Logger;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Manages texture loading, caching, and cleanup for the rendering system.
 * Provides efficient texture resource management with automatic cleanup.
 */
public class TextureManager {
    
    private static final Logger logger = Logger.getLogger(TextureManager.class);
    
    // Texture cache
    private final Map<String, Texture> textureCache;
    private final Map<String, Integer> referenceCount;
    
    // Default textures
    private Texture defaultTexture;
    private Texture whiteTexture;
    private Texture blackTexture;
    
    public TextureManager() {
        this.textureCache = new HashMap<>();
        this.referenceCount = new HashMap<>();
        
        // Initialize STB Image
        STBImage.stbi_set_flip_vertically_on_load(true);
        
        createDefaultTextures();
        logger.info("TextureManager initialized");
    }
    
    /**
     * Initialize the texture manager (called by Renderer).
     * This method is separate from the constructor to allow for explicit initialization.
     */
    public void initialize() {
        // Additional initialization if needed
        logger.debug("TextureManager initialize() called");
    }
    
    /**
     * Loads a texture from file path
     */
    public Texture loadTexture(String name, String filePath) {
        // Check cache first
        if (textureCache.containsKey(name)) {
            referenceCount.put(name, referenceCount.get(name) + 1);
            return textureCache.get(name);
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            
            ByteBuffer imageData = STBImage.stbi_load(filePath, width, height, channels, 0);
            if (imageData == null) {
                logger.error("Failed to load texture: {} - {}", filePath, STBImage.stbi_failure_reason());
                return getDefaultTexture();
            }
            
            int format = switch (channels.get(0)) {
                case 1 -> GL_RED;
                case 2 -> GL_RG;
                case 3 -> GL_RGB;
                case 4 -> GL_RGBA;
                default -> GL_RGBA;
            };
            
            Texture texture = Texture.create(name, width.get(0), height.get(0), format, format, GL_UNSIGNED_BYTE);
            
            // Upload texture data
            glBindTexture(GL_TEXTURE_2D, texture.getTextureId());
            glTexImage2D(GL_TEXTURE_2D, 0, format, width.get(0), height.get(0), 0, format, GL_UNSIGNED_BYTE, imageData);
            glGenerateMipmap(GL_TEXTURE_2D);
            glBindTexture(GL_TEXTURE_2D, 0);
            
            STBImage.stbi_image_free(imageData);
            
            // Cache the texture
            textureCache.put(name, texture);
            referenceCount.put(name, 1);
            
            logger.debug("Loaded texture: {} ({}x{})", name, width.get(0), height.get(0));
            return texture;
            
        } catch (Exception e) {
            logger.error("Error loading texture: " + filePath, e);
            return getDefaultTexture();
        }
    }
    
    /**
     * Creates a texture from raw data
     */
    public Texture createTexture(String name, int width, int height, int format, ByteBuffer data) {
        if (textureCache.containsKey(name)) {
            referenceCount.put(name, referenceCount.get(name) + 1);
            return textureCache.get(name);
        }
        
        Texture texture = Texture.create(name, width, height, format, format, GL_UNSIGNED_BYTE);
        
        glBindTexture(GL_TEXTURE_2D, texture.getTextureId());
        glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0, format, GL_UNSIGNED_BYTE, data);
        glGenerateMipmap(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, 0);
        
        textureCache.put(name, texture);
        referenceCount.put(name, 1);
        
        logger.debug("Created texture: {} ({}x{})", name, width, height);
        return texture;
    }
    
    /**
     * Gets a cached texture by name
     */
    public Texture getTexture(String name) {
        Texture texture = textureCache.get(name);
        if (texture != null) {
            referenceCount.put(name, referenceCount.get(name) + 1);
            return texture;
        }
        
        logger.warn("Texture not found: {}", name);
        return getDefaultTexture();
    }
    
    /**
     * Releases a texture reference
     */
    public void releaseTexture(String name) {
        if (!referenceCount.containsKey(name)) {
            return;
        }
        
        int count = referenceCount.get(name) - 1;
        if (count <= 0) {
            // Remove from cache and cleanup
            Texture texture = textureCache.remove(name);
            referenceCount.remove(name);
            
            if (texture != null) {
                texture.cleanup();
                logger.debug("Released texture: {}", name);
            }
        } else {
            referenceCount.put(name, count);
        }
    }
    
    /**
     * Gets the default texture (magenta checkerboard)
     */
    public Texture getDefaultTexture() {
        return defaultTexture;
    }
    
    /**
     * Gets a white texture
     */
    public Texture getWhiteTexture() {
        return whiteTexture;
    }
    
    /**
     * Gets a black texture
     */
    public Texture getBlackTexture() {
        return blackTexture;
    }
    
    /**
     * Creates default textures for fallback use
     */
    private void createDefaultTextures() {
        // Create default magenta checkerboard texture
        ByteBuffer defaultData = ByteBuffer.allocateDirect(4 * 4 * 4); // 4x4 RGBA
        for (int i = 0; i < 16; i++) {
            boolean checker = ((i % 4) / 2 + (i / 8)) % 2 == 0;
            if (checker) {
                defaultData.put((byte) 255).put((byte) 0).put((byte) 255).put((byte) 255); // Magenta
            } else {
                defaultData.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 255); // Black
            }
        }
        defaultData.flip();
        
        defaultTexture = createTexture("default", 4, 4, GL_RGBA, defaultData);
        
        // Create white texture
        ByteBuffer whiteData = ByteBuffer.allocateDirect(4);
        whiteData.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255);
        whiteData.flip();
        
        whiteTexture = createTexture("white", 1, 1, GL_RGBA, whiteData);
        
        // Create black texture
        ByteBuffer blackData = ByteBuffer.allocateDirect(4);
        blackData.put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 255);
        blackData.flip();
        
        blackTexture = createTexture("black", 1, 1, GL_RGBA, blackData);
    }
    
    /**
     * Gets texture cache statistics
     */
    public void printCacheStats() {
        logger.info("Texture cache: {} textures loaded", textureCache.size());
        for (Map.Entry<String, Integer> entry : referenceCount.entrySet()) {
            logger.debug("  {} - {} references", entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Cleanup all textures
     */
    public void cleanup() {
        logger.info("Cleaning up TextureManager...");
        
        for (Texture texture : textureCache.values()) {
            texture.cleanup();
        }
        
        textureCache.clear();
        referenceCount.clear();
        
        logger.info("TextureManager cleanup complete");
    }
}