package com.odyssey.rendering;

import com.odyssey.util.Logger;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.stb.STBImageWrite.*;

/**
 * Texture Atlas system for The Odyssey.
 * Manages multiple textures in a single atlas for improved rendering performance
 * and reduced texture binding calls in the voxel world.
 * 
 * @author The Odyssey Team
 * @version 1.0.0
 */
public class TextureAtlas {
    
    private static final Logger logger = Logger.getLogger(TextureAtlas.class);
    
    /**
     * Represents a texture region within the atlas.
     */
    public static class TextureRegion {
        private final String name;
        private final Vector2f minUV;
        private final Vector2f maxUV;
        private final Vector2f size;
        private final int width;
        private final int height;
        
        public TextureRegion(String name, Vector2f minUV, Vector2f maxUV, int width, int height) {
            this.name = name;
            this.minUV = new Vector2f(minUV);
            this.maxUV = new Vector2f(maxUV);
            this.size = new Vector2f(maxUV).sub(minUV);
            this.width = width;
            this.height = height;
        }
        
        public String getName() { return name; }
        public Vector2f getMinUV() { return new Vector2f(minUV); }
        public Vector2f getMaxUV() { return new Vector2f(maxUV); }
        public Vector2f getSize() { return new Vector2f(size); }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        
        /**
         * Gets UV coordinates for a specific corner of the texture region.
         * 
         * @param u U coordinate (0.0 to 1.0)
         * @param v V coordinate (0.0 to 1.0)
         * @return UV coordinates within the atlas
         */
        public Vector2f getUV(float u, float v) {
            return new Vector2f(
                minUV.x + u * size.x,
                minUV.y + v * size.y
            );
        }
        
        /**
         * Gets UV coordinates for texture animation frames.
         * 
         * @param frame Frame index
         * @param totalFrames Total number of frames
         * @param u U coordinate (0.0 to 1.0)
         * @param v V coordinate (0.0 to 1.0)
         * @return Animated UV coordinates
         */
        public Vector2f getAnimatedUV(int frame, int totalFrames, float u, float v) {
            float frameHeight = size.y / totalFrames;
            float frameV = (frame % totalFrames) * frameHeight;
            
            return new Vector2f(
                minUV.x + u * size.x,
                minUV.y + frameV + v * frameHeight
            );
        }
    }
    
    /**
     * Represents a texture to be packed into the atlas.
     */
    private static class TextureData {
        String name;
        ByteBuffer data;
        int width;
        int height;
        int channels;
        
        TextureData(String name, ByteBuffer data, int width, int height, int channels) {
            this.name = name;
            this.data = data;
            this.width = width;
            this.height = height;
            this.channels = channels;
        }
    }
    
    /**
     * Simple rectangle packing node for atlas generation.
     */
    private static class PackingNode {
        int x, y, width, height;
        PackingNode left, right;
        boolean occupied;
        
        PackingNode(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.occupied = false;
        }
        
        PackingNode insert(int w, int h) {
            if (left != null || right != null) {
                // Not a leaf node
                PackingNode newNode = left != null ? left.insert(w, h) : null;
                if (newNode != null) return newNode;
                return right != null ? right.insert(w, h) : null;
            }
            
            if (occupied || w > width || h > height) {
                return null; // No space
            }
            
            if (w == width && h == height) {
                occupied = true;
                return this; // Perfect fit
            }
            
            // Split the node
            int dw = width - w;
            int dh = height - h;
            
            if (dw > dh) {
                left = new PackingNode(x, y, w, height);
                right = new PackingNode(x + w, y, width - w, height);
            } else {
                left = new PackingNode(x, y, width, h);
                right = new PackingNode(x, y + h, width, height - h);
            }
            
            return left.insert(w, h);
        }
    }
    
    // Atlas properties
    private int atlasWidth;
    private int atlasHeight;
    private int textureId;
    private boolean generated;
    
    // Texture management
    private Map<String, TextureRegion> textureRegions;
    private List<TextureData> texturesToPack;
    private PackingNode rootNode;
    
    // Settings
    private int maxAtlasSize = 4096;
    private int padding = 1; // Padding between textures to prevent bleeding
    private boolean generateMipmaps = true;
    private int filterMode = GL_LINEAR;
    private int wrapMode = GL_CLAMP_TO_EDGE;
    
    /**
     * Creates a new TextureAtlas.
     */
    public TextureAtlas() {
        this.textureRegions = new HashMap<>();
        this.texturesToPack = new ArrayList<>();
        this.generated = false;
    }
    
    /**
     * Creates a new TextureAtlas with specified maximum size.
     * 
     * @param maxSize Maximum atlas size (width and height)
     */
    public TextureAtlas(int maxSize) {
        this();
        this.maxAtlasSize = maxSize;
    }
    
    /**
     * Adds a texture to be packed into the atlas.
     * 
     * @param name Texture name/identifier
     * @param filePath Path to the texture file
     * @return True if texture was added successfully
     */
    public boolean addTexture(String name, String filePath) {
        if (generated) {
            logger.warn("Cannot add texture '{}' - atlas already generated", name);
            return false;
        }
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            
            // Load image data
            ByteBuffer imageData = stbi_load(filePath, width, height, channels, 0);
            if (imageData == null) {
                logger.error("Failed to load texture: {} - {}", filePath, stbi_failure_reason());
                return false;
            }
            
            // Create a copy of the image data
            ByteBuffer dataCopy = BufferUtils.createByteBuffer(imageData.remaining());
            dataCopy.put(imageData);
            dataCopy.flip();
            
            // Free original image data
            stbi_image_free(imageData);
            
            // Add to packing list
            texturesToPack.add(new TextureData(name, dataCopy, width.get(0), height.get(0), channels.get(0)));
            
            logger.debug("Added texture '{}' ({}x{}) to atlas", name, width.get(0), height.get(0));
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to add texture '{}' from '{}'", name, filePath, e);
            return false;
        }
    }
    
    /**
     * Adds a texture from raw data.
     * 
     * @param name Texture name/identifier
     * @param data Raw texture data
     * @param width Texture width
     * @param height Texture height
     * @param channels Number of channels (3 for RGB, 4 for RGBA)
     * @return True if texture was added successfully
     */
    public boolean addTexture(String name, ByteBuffer data, int width, int height, int channels) {
        if (generated) {
            logger.warn("Cannot add texture '{}' - atlas already generated", name);
            return false;
        }
        
        // Create a copy of the data
        ByteBuffer dataCopy = BufferUtils.createByteBuffer(data.remaining());
        dataCopy.put(data);
        dataCopy.flip();
        
        texturesToPack.add(new TextureData(name, dataCopy, width, height, channels));
        logger.debug("Added raw texture '{}' ({}x{}) to atlas", name, width, height);
        return true;
    }
    
    /**
     * Generates the texture atlas from all added textures.
     * 
     * @return True if atlas was generated successfully
     */
    public boolean generateAtlas() {
        if (generated) {
            logger.warn("Atlas already generated");
            return true;
        }
        
        if (texturesToPack.isEmpty()) {
            logger.warn("No textures to pack into atlas");
            return false;
        }
        
        logger.info("Generating texture atlas with {} textures...", texturesToPack.size());
        
        // Sort textures by area (largest first) for better packing
        texturesToPack.sort((a, b) -> Integer.compare(b.width * b.height, a.width * a.height));
        
        // Find optimal atlas size
        if (!findOptimalAtlasSize()) {
            logger.error("Failed to find suitable atlas size for all textures");
            return false;
        }
        
        // Pack textures
        if (!packTextures()) {
            logger.error("Failed to pack textures into atlas");
            return false;
        }
        
        // Generate OpenGL texture
        if (!generateOpenGLTexture()) {
            logger.error("Failed to generate OpenGL texture");
            return false;
        }
        
        // Clean up texture data
        for (TextureData textureData : texturesToPack) {
            // ByteBuffer cleanup is handled by GC
        }
        texturesToPack.clear();
        
        generated = true;
        logger.info("Texture atlas generated successfully ({}x{})", atlasWidth, atlasHeight);
        return true;
    }
    
    /**
     * Finds the optimal atlas size to fit all textures.
     */
    private boolean findOptimalAtlasSize() {
        // Calculate total area needed
        int totalArea = 0;
        int maxWidth = 0;
        int maxHeight = 0;
        
        for (TextureData texture : texturesToPack) {
            totalArea += (texture.width + padding * 2) * (texture.height + padding * 2);
            maxWidth = Math.max(maxWidth, texture.width + padding * 2);
            maxHeight = Math.max(maxHeight, texture.height + padding * 2);
        }
        
        // Start with minimum size that can fit the largest texture
        int size = Math.max(maxWidth, maxHeight);
        size = nextPowerOfTwo(size);
        
        // Try different sizes until we find one that works
        while (size <= maxAtlasSize) {
            if (size * size >= totalArea && testPacking(size, size)) {
                atlasWidth = size;
                atlasHeight = size;
                return true;
            }
            size *= 2;
        }
        
        // Try rectangular atlases
        for (int width = nextPowerOfTwo(maxWidth); width <= maxAtlasSize; width *= 2) {
            for (int height = nextPowerOfTwo(maxHeight); height <= maxAtlasSize; height *= 2) {
                if (width * height >= totalArea && testPacking(width, height)) {
                    atlasWidth = width;
                    atlasHeight = height;
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Tests if all textures can be packed into the given dimensions.
     */
    private boolean testPacking(int width, int height) {
        PackingNode testRoot = new PackingNode(0, 0, width, height);
        
        for (TextureData texture : texturesToPack) {
            int w = texture.width + padding * 2;
            int h = texture.height + padding * 2;
            
            if (testRoot.insert(w, h) == null) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Packs all textures into the atlas.
     */
    private boolean packTextures() {
        rootNode = new PackingNode(0, 0, atlasWidth, atlasHeight);
        
        for (TextureData texture : texturesToPack) {
            int w = texture.width + padding * 2;
            int h = texture.height + padding * 2;
            
            PackingNode node = rootNode.insert(w, h);
            if (node == null) {
                logger.error("Failed to pack texture '{}'", texture.name);
                return false;
            }
            
            // Calculate UV coordinates
            float minU = (float) (node.x + padding) / atlasWidth;
            float minV = (float) (node.y + padding) / atlasHeight;
            float maxU = (float) (node.x + padding + texture.width) / atlasWidth;
            float maxV = (float) (node.y + padding + texture.height) / atlasHeight;
            
            // Create texture region
            TextureRegion region = new TextureRegion(
                texture.name,
                new Vector2f(minU, minV),
                new Vector2f(maxU, maxV),
                texture.width,
                texture.height
            );
            
            textureRegions.put(texture.name, region);
            
            logger.debug("Packed texture '{}' at ({}, {}) with UV ({}, {}) to ({}, {})",
                        texture.name, node.x + padding, node.y + padding, minU, minV, maxU, maxV);
        }
        
        return true;
    }
    
    /**
     * Generates the OpenGL texture from packed data.
     */
    private boolean generateOpenGLTexture() {
        // Create atlas buffer
        int channels = 4; // Always use RGBA for consistency
        ByteBuffer atlasData = BufferUtils.createByteBuffer(atlasWidth * atlasHeight * channels);
        
        // Clear atlas to transparent
        for (int i = 0; i < atlasWidth * atlasHeight * channels; i += channels) {
            atlasData.put((byte) 0); // R
            atlasData.put((byte) 0); // G
            atlasData.put((byte) 0); // B
            atlasData.put((byte) 0); // A
        }
        
        // Copy texture data into atlas
        for (TextureData texture : texturesToPack) {
            TextureRegion region = textureRegions.get(texture.name);
            if (region == null) continue;
            
            // Calculate position in atlas
            int atlasX = (int) (region.getMinUV().x * atlasWidth);
            int atlasY = (int) (region.getMinUV().y * atlasHeight);
            
            // Copy pixel data
            copyTextureData(atlasData, texture, atlasX, atlasY);
        }
        
        atlasData.flip();
        
        // Generate OpenGL texture
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        // Upload texture data
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, atlasWidth, atlasHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasData);
        
        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, generateMipmaps ? GL_LINEAR_MIPMAP_LINEAR : filterMode);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filterMode);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, wrapMode);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, wrapMode);
        
        // Generate mipmaps if enabled
        if (generateMipmaps) {
            glGenerateMipmap(GL_TEXTURE_2D);
        }
        
        glBindTexture(GL_TEXTURE_2D, 0);
        
        return true;
    }
    
    /**
     * Copies texture data into the atlas buffer.
     */
    private void copyTextureData(ByteBuffer atlasData, TextureData texture, int atlasX, int atlasY) {
        ByteBuffer sourceData = texture.data;
        sourceData.rewind();
        
        for (int y = 0; y < texture.height; y++) {
            for (int x = 0; x < texture.width; x++) {
                int sourceIndex = (y * texture.width + x) * texture.channels;
                int atlasIndex = ((atlasY + y) * atlasWidth + (atlasX + x)) * 4;
                
                // Copy RGB channels
                for (int c = 0; c < Math.min(3, texture.channels); c++) {
                    atlasData.put(atlasIndex + c, sourceData.get(sourceIndex + c));
                }
                
                // Set alpha channel
                if (texture.channels >= 4) {
                    atlasData.put(atlasIndex + 3, sourceData.get(sourceIndex + 3));
                } else {
                    atlasData.put(atlasIndex + 3, (byte) 255); // Opaque
                }
            }
        }
    }
    
    /**
     * Gets the next power of two greater than or equal to the given value.
     */
    private int nextPowerOfTwo(int value) {
        int power = 1;
        while (power < value) {
            power *= 2;
        }
        return power;
    }
    
    /**
     * Binds the atlas texture for rendering.
     */
    public void bind() {
        if (!generated) {
            logger.warn("Cannot bind atlas - not generated yet");
            return;
        }
        
        glBindTexture(GL_TEXTURE_2D, textureId);
    }
    
    /**
     * Unbinds the atlas texture.
     */
    public void unbind() {
        glBindTexture(GL_TEXTURE_2D, 0);
    }
    
    /**
     * Gets a texture region by name.
     * 
     * @param name Texture name
     * @return TextureRegion or null if not found
     */
    public TextureRegion getTextureRegion(String name) {
        return textureRegions.get(name);
    }
    
    /**
     * Gets UV coordinates for a texture.
     * 
     * @param textureName Texture name
     * @param u U coordinate (0.0 to 1.0)
     * @param v V coordinate (0.0 to 1.0)
     * @return UV coordinates in atlas space, or null if texture not found
     */
    public Vector2f getUV(String textureName, float u, float v) {
        TextureRegion region = textureRegions.get(textureName);
        return region != null ? region.getUV(u, v) : null;
    }
    
    /**
     * Gets UV bounds for a texture (min and max UV coordinates).
     * 
     * @param textureName Texture name
     * @return Vector4f containing (minU, minV, maxU, maxV), or null if texture not found
     */
    public Vector4f getUVBounds(String textureName) {
        TextureRegion region = textureRegions.get(textureName);
        if (region == null) return null;
        
        return new Vector4f(
            region.getMinUV().x,
            region.getMinUV().y,
            region.getMaxUV().x,
            region.getMaxUV().y
        );
    }
    
    /**
     * Checks if a texture exists in the atlas.
     * 
     * @param name Texture name
     * @return True if texture exists
     */
    public boolean hasTexture(String name) {
        return textureRegions.containsKey(name);
    }
    
    /**
     * Gets all texture names in the atlas.
     * 
     * @return Set of texture names
     */
    public java.util.Set<String> getTextureNames() {
        return textureRegions.keySet();
    }
    
    // Getters and setters
    
    public int getAtlasWidth() { return atlasWidth; }
    public int getAtlasHeight() { return atlasHeight; }
    public int getTextureId() { return textureId; }
    public boolean isGenerated() { return generated; }
    
    public void setMaxAtlasSize(int maxSize) {
        if (!generated) {
            this.maxAtlasSize = maxSize;
        }
    }
    
    public void setPadding(int padding) {
        if (!generated) {
            this.padding = padding;
        }
    }
    
    public void setGenerateMipmaps(boolean generate) {
        if (!generated) {
            this.generateMipmaps = generate;
        }
    }
    
    public void setFilterMode(int filterMode) {
        if (!generated) {
            this.filterMode = filterMode;
        }
    }
    
    public void setWrapMode(int wrapMode) {
        if (!generated) {
            this.wrapMode = wrapMode;
        }
    }
    
    /**
     * Saves the atlas as a PNG file for debugging.
     * 
     * @param filePath Output file path
     * @return True if saved successfully
     */
    public boolean saveAtlas(String filePath) {
        if (!generated) {
            logger.warn("Cannot save atlas - not generated yet");
            return false;
        }
        
        // Read texture data from GPU
        ByteBuffer atlasData = BufferUtils.createByteBuffer(atlasWidth * atlasHeight * 4);
        glBindTexture(GL_TEXTURE_2D, textureId);
        glGetTexImage(GL_TEXTURE_2D, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlasData);
        glBindTexture(GL_TEXTURE_2D, 0);
        
        // Save using STB
        boolean result = stbi_write_png(filePath, atlasWidth, atlasHeight, 4, atlasData, atlasWidth * 4);
        
        if (result) {
            logger.info("Atlas saved to: {}", filePath);
            return true;
        } else {
            logger.error("Failed to save atlas to: {}", filePath);
            return false;
        }
    }
    
    /**
     * Cleanup atlas resources.
     */
    public void cleanup() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
        }
        
        textureRegions.clear();
        texturesToPack.clear();
        generated = false;
        
        logger.info("Texture atlas cleaned up");
    }
}