package com.odyssey.rendering;

import com.odyssey.util.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL14.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Represents an OpenGL texture for The Odyssey rendering system.
 * 
 * Handles texture loading, binding, and management with support for
 * various texture types and formats.
 */
public class Texture {
    
    private static final Logger logger = Logger.getLogger(Texture.class);
    
    // Texture properties
    private int textureId;
    private int width;
    private int height;
    private int channels;
    private int target;
    private int internalFormat;
    private int format;
    private int type;
    private String name;
    private String filePath;
    
    // Texture parameters
    private int minFilter = GL_LINEAR_MIPMAP_LINEAR;
    private int magFilter = GL_LINEAR;
    private int wrapS = GL_REPEAT;
    private int wrapT = GL_REPEAT;
    private int wrapR = GL_REPEAT;
    
    // Texture types
    public static final int TEXTURE_2D = GL_TEXTURE_2D;
    public static final int TEXTURE_CUBE_MAP = GL_TEXTURE_CUBE_MAP;
    public static final int TEXTURE_2D_ARRAY = GL_TEXTURE_2D_ARRAY;
    
    // Common formats
    public static final int FORMAT_RGB = GL_RGB;
    public static final int FORMAT_RGBA = GL_RGBA;
    public static final int FORMAT_DEPTH = GL_DEPTH_COMPONENT;
    public static final int FORMAT_DEPTH_STENCIL = GL_DEPTH_STENCIL;
    
    /**
     * Create empty texture.
     */
    private Texture(String name) {
        this.name = name;
        this.textureId = glGenTextures();
        this.target = GL_TEXTURE_2D;
    }
    
    /**
     * Create texture from file.
     */
    public static Texture loadFromFile(String name, String filePath) {
        return loadFromFile(name, filePath, true);
    }
    
    /**
     * Create texture from file with optional mipmap generation.
     */
    public static Texture loadFromFile(String name, String filePath, boolean generateMipmaps) {
        Texture texture = new Texture(name);
        texture.filePath = filePath;
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer widthBuffer = stack.mallocInt(1);
            IntBuffer heightBuffer = stack.mallocInt(1);
            IntBuffer channelsBuffer = stack.mallocInt(1);
            
            // Load image data
            STBImage.stbi_set_flip_vertically_on_load(true);
            ByteBuffer imageData = STBImage.stbi_load(filePath, widthBuffer, heightBuffer, channelsBuffer, 0);
            
            if (imageData == null) {
                logger.error("Failed to load texture '{}': {}", name, STBImage.stbi_failure_reason());
                texture.cleanup();
                return null;
            }
            
            texture.width = widthBuffer.get(0);
            texture.height = heightBuffer.get(0);
            texture.channels = channelsBuffer.get(0);
            
            // Determine format
            switch (texture.channels) {
                case 1:
                    texture.internalFormat = GL_R8;
                    texture.format = GL_RED;
                    break;
                case 2:
                    texture.internalFormat = GL_RG8;
                    texture.format = GL_RG;
                    break;
                case 3:
                    texture.internalFormat = GL_RGB8;
                    texture.format = GL_RGB;
                    break;
                case 4:
                    texture.internalFormat = GL_RGBA8;
                    texture.format = GL_RGBA;
                    break;
                default:
                    logger.error("Unsupported channel count for texture '{}': {}", name, texture.channels);
                    STBImage.stbi_image_free(imageData);
                    texture.cleanup();
                    return null;
            }
            
            texture.type = GL_UNSIGNED_BYTE;
            
            // Upload texture data
            glBindTexture(GL_TEXTURE_2D, texture.textureId);
            glTexImage2D(GL_TEXTURE_2D, 0, texture.internalFormat, texture.width, texture.height, 
                        0, texture.format, texture.type, imageData);
            
            // Set default parameters
            texture.applyParameters();
            
            // Generate mipmaps if requested
            if (generateMipmaps) {
                glGenerateMipmap(GL_TEXTURE_2D);
            }
            
            glBindTexture(GL_TEXTURE_2D, 0);
            
            // Free image data
            STBImage.stbi_image_free(imageData);
            
            logger.info("Loaded texture '{}' ({}x{}, {} channels) from: {}", 
                       name, texture.width, texture.height, texture.channels, filePath);
            
            return texture;
            
        } catch (Exception e) {
            logger.error("Failed to load texture '{}': {}", name, e.getMessage());
            texture.cleanup();
            return null;
        }
    }
    
    /**
     * Create empty texture with specified dimensions and format.
     */
    public static Texture create(String name, int width, int height, int internalFormat, int format, int type) {
        return create(name, width, height, internalFormat, format, type, GL_TEXTURE_2D);
    }
    
    /**
     * Create empty texture with specified dimensions, format, and target.
     */
    public static Texture create(String name, int width, int height, int internalFormat, int format, int type, int target) {
        Texture texture = new Texture(name);
        texture.width = width;
        texture.height = height;
        texture.internalFormat = internalFormat;
        texture.format = format;
        texture.type = type;
        texture.target = target;
        
        glBindTexture(target, texture.textureId);
        
        if (target == GL_TEXTURE_2D) {
            glTexImage2D(target, 0, internalFormat, width, height, 0, format, type, (ByteBuffer) null);
        } else if (target == GL_TEXTURE_CUBE_MAP) {
            // Create all 6 faces for cube map
            for (int i = 0; i < 6; i++) {
                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, internalFormat, width, height, 0, format, type, (ByteBuffer) null);
            }
        }
        
        texture.applyParameters();
        glBindTexture(target, 0);
        
        logger.debug("Created empty texture '{}' ({}x{}, format: {}, target: {})", 
                    name, width, height, internalFormat, target);
        
        return texture;
    }
    
    /**
     * Create depth texture for shadow mapping or depth testing.
     */
    public static Texture createDepthTexture(String name, int width, int height) {
        Texture texture = create(name, width, height, GL_DEPTH_COMPONENT24, GL_DEPTH_COMPONENT, GL_FLOAT);
        
        // Set appropriate parameters for depth textures
        texture.setMinFilter(GL_LINEAR);
        texture.setMagFilter(GL_LINEAR);
        texture.setWrapS(GL_CLAMP_TO_BORDER);
        texture.setWrapT(GL_CLAMP_TO_BORDER);
        
        // Set border color to white (far plane)
        float[] borderColor = {1.0f, 1.0f, 1.0f, 1.0f};
        glBindTexture(GL_TEXTURE_2D, texture.textureId);
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);
        glBindTexture(GL_TEXTURE_2D, 0);
        
        return texture;
    }
    
    /**
     * Create cube map texture from 6 face images.
     */
    public static Texture createCubeMap(String name, String[] facePaths) {
        Texture texture = new Texture(name);
        texture.target = GL_TEXTURE_CUBE_MAP;
        
        glBindTexture(GL_TEXTURE_CUBE_MAP, texture.textureId);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer widthBuffer = stack.mallocInt(1);
            IntBuffer heightBuffer = stack.mallocInt(1);
            IntBuffer channelsBuffer = stack.mallocInt(1);
            
            STBImage.stbi_set_flip_vertically_on_load(false); // Cube maps don't need flipping
            
            for (int i = 0; i < 6; i++) {
                ByteBuffer imageData = STBImage.stbi_load(facePaths[i], widthBuffer, heightBuffer, channelsBuffer, 0);
                
                if (imageData == null) {
                    logger.error("Failed to load cube map face {}: {}", i, STBImage.stbi_failure_reason());
                    glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
                    texture.cleanup();
                    return null;
                }
                
                int width = widthBuffer.get(0);
                int height = heightBuffer.get(0);
                int channels = channelsBuffer.get(0);
                
                if (i == 0) {
                    texture.width = width;
                    texture.height = height;
                    texture.channels = channels;
                    
                    // Determine format
                    switch (channels) {
                        case 3:
                            texture.internalFormat = GL_RGB8;
                            texture.format = GL_RGB;
                            break;
                        case 4:
                            texture.internalFormat = GL_RGBA8;
                            texture.format = GL_RGBA;
                            break;
                        default:
                            logger.error("Unsupported channel count for cube map: {}", channels);
                            STBImage.stbi_image_free(imageData);
                            glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
                            texture.cleanup();
                            return null;
                    }
                    
                    texture.type = GL_UNSIGNED_BYTE;
                }
                
                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, texture.internalFormat, 
                           width, height, 0, texture.format, texture.type, imageData);
                
                STBImage.stbi_image_free(imageData);
            }
            
            // Set cube map parameters
            texture.setMinFilter(GL_LINEAR);
            texture.setMagFilter(GL_LINEAR);
            texture.setWrapS(GL_CLAMP_TO_EDGE);
            texture.setWrapT(GL_CLAMP_TO_EDGE);
            texture.setWrapR(GL_CLAMP_TO_EDGE);
            
            glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
            
            logger.info("Created cube map texture '{}' ({}x{})", name, texture.width, texture.height);
            return texture;
            
        } catch (Exception e) {
            logger.error("Failed to create cube map texture '{}': {}", name, e.getMessage());
            glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
            texture.cleanup();
            return null;
        }
    }
    
    /**
     * Bind texture to specified texture unit.
     */
    public void bind(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(target, textureId);
    }
    
    /**
     * Bind texture to texture unit 0.
     */
    public void bind() {
        bind(0);
    }
    
    /**
     * Unbind texture from specified texture unit.
     */
    public void unbind(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(target, 0);
    }
    
    /**
     * Unbind texture from texture unit 0.
     */
    public void unbind() {
        unbind(0);
    }
    
    /**
     * Update texture data.
     */
    public void updateData(ByteBuffer data) {
        updateData(data, 0, 0, width, height);
    }
    
    /**
     * Update partial texture data.
     */
    public void updateData(ByteBuffer data, int x, int y, int width, int height) {
        glBindTexture(target, textureId);
        glTexSubImage2D(target, 0, x, y, width, height, format, type, data);
        glBindTexture(target, 0);
    }
    
    /**
     * Generate mipmaps for this texture.
     */
    public void generateMipmaps() {
        glBindTexture(target, textureId);
        glGenerateMipmap(target);
        glBindTexture(target, 0);
    }
    
    /**
     * Set texture filtering parameters.
     */
    public void setFilter(int minFilter, int magFilter) {
        this.minFilter = minFilter;
        this.magFilter = magFilter;
        
        glBindTexture(target, textureId);
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, minFilter);
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, magFilter);
        glBindTexture(target, 0);
    }
    
    /**
     * Set texture wrap parameters.
     */
    public void setWrap(int wrapS, int wrapT) {
        this.wrapS = wrapS;
        this.wrapT = wrapT;
        
        glBindTexture(target, textureId);
        glTexParameteri(target, GL_TEXTURE_WRAP_S, wrapS);
        glTexParameteri(target, GL_TEXTURE_WRAP_T, wrapT);
        glBindTexture(target, 0);
    }
    
    /**
     * Set texture wrap parameters for 3D textures.
     */
    public void setWrap(int wrapS, int wrapT, int wrapR) {
        this.wrapS = wrapS;
        this.wrapT = wrapT;
        this.wrapR = wrapR;
        
        glBindTexture(target, textureId);
        glTexParameteri(target, GL_TEXTURE_WRAP_S, wrapS);
        glTexParameteri(target, GL_TEXTURE_WRAP_T, wrapT);
        glTexParameteri(target, GL_TEXTURE_WRAP_R, wrapR);
        glBindTexture(target, 0);
    }
    
    /**
     * Apply all texture parameters.
     */
    private void applyParameters() {
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, minFilter);
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, magFilter);
        glTexParameteri(target, GL_TEXTURE_WRAP_S, wrapS);
        glTexParameteri(target, GL_TEXTURE_WRAP_T, wrapT);
        
        if (target == GL_TEXTURE_CUBE_MAP || target == GL_TEXTURE_2D_ARRAY) {
            glTexParameteri(target, GL_TEXTURE_WRAP_R, wrapR);
        }
    }
    
    // Getters and setters
    
    public int getTextureId() {
        return textureId;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getChannels() {
        return channels;
    }
    
    public int getTarget() {
        return target;
    }
    
    public int getInternalFormat() {
        return internalFormat;
    }
    
    public int getFormat() {
        return format;
    }
    
    public int getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public int getMinFilter() {
        return minFilter;
    }
    
    public void setMinFilter(int minFilter) {
        this.minFilter = minFilter;
        glBindTexture(target, textureId);
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, minFilter);
        glBindTexture(target, 0);
    }
    
    public int getMagFilter() {
        return magFilter;
    }
    
    public void setMagFilter(int magFilter) {
        this.magFilter = magFilter;
        glBindTexture(target, textureId);
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, magFilter);
        glBindTexture(target, 0);
    }
    
    public int getWrapS() {
        return wrapS;
    }
    
    public void setWrapS(int wrapS) {
        this.wrapS = wrapS;
        glBindTexture(target, textureId);
        glTexParameteri(target, GL_TEXTURE_WRAP_S, wrapS);
        glBindTexture(target, 0);
    }
    
    public int getWrapT() {
        return wrapT;
    }
    
    public void setWrapT(int wrapT) {
        this.wrapT = wrapT;
        glBindTexture(target, textureId);
        glTexParameteri(target, GL_TEXTURE_WRAP_T, wrapT);
        glBindTexture(target, 0);
    }
    
    public int getWrapR() {
        return wrapR;
    }
    
    public void setWrapR(int wrapR) {
        this.wrapR = wrapR;
        glBindTexture(target, textureId);
        glTexParameteri(target, GL_TEXTURE_WRAP_R, wrapR);
        glBindTexture(target, 0);
    }
    
    /**
     * Check if texture is valid.
     */
    public boolean isValid() {
        return textureId != 0 && glIsTexture(textureId);
    }
    
    /**
     * Get texture memory usage in bytes.
     */
    public long getMemoryUsage() {
        int bytesPerPixel = 0;
        
        switch (internalFormat) {
            case GL_R8:
                bytesPerPixel = 1;
                break;
            case GL_RG8:
                bytesPerPixel = 2;
                break;
            case GL_RGB8:
                bytesPerPixel = 3;
                break;
            case GL_RGBA8:
                bytesPerPixel = 4;
                break;
            case GL_DEPTH_COMPONENT24:
                bytesPerPixel = 3;
                break;
            case GL_DEPTH_COMPONENT32F:
                bytesPerPixel = 4;
                break;
            default:
                bytesPerPixel = 4; // Assume 4 bytes per pixel
                break;
        }
        
        long baseSize = (long) width * height * bytesPerPixel;
        
        // Account for cube maps (6 faces)
        if (target == GL_TEXTURE_CUBE_MAP) {
            baseSize *= 6;
        }
        
        // Account for mipmaps (approximately 1.33x more memory)
        if (minFilter == GL_LINEAR_MIPMAP_LINEAR || minFilter == GL_NEAREST_MIPMAP_NEAREST ||
            minFilter == GL_LINEAR_MIPMAP_NEAREST || minFilter == GL_NEAREST_MIPMAP_LINEAR) {
            baseSize = (long) (baseSize * 1.33);
        }
        
        return baseSize;
    }
    
    /**
     * Cleanup texture resources.
     */
    public void cleanup() {
        if (textureId != 0) {
            glDeleteTextures(textureId);
            textureId = 0;
            logger.debug("Cleaned up texture: {}", name);
        }
    }
    
    @Override
    public String toString() {
        return String.format("Texture{name='%s', id=%d, size=%dx%d, channels=%d, target=%d}", 
                           name, textureId, width, height, channels, target);
    }
}