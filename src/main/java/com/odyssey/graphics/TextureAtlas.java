package com.odyssey.graphics;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Dynamic texture atlas system for efficient texture management.
 * Automatically packs multiple textures into a single atlas texture
 * to reduce draw calls and improve rendering performance.
 */
public class TextureAtlas {
    private static final Logger logger = Logger.getLogger(TextureAtlas.class.getName());
    
    /**
     * Represents a region within the texture atlas.
     */
    public static class AtlasRegion {
        private final Vector4f uvBounds; // x, y, width, height in UV coordinates
        private final Vector2f size; // Original texture size
        private final String name;
        
        public AtlasRegion(String name, Vector4f uvBounds, Vector2f size) {
            this.name = name;
            this.uvBounds = new Vector4f(uvBounds);
            this.size = new Vector2f(size);
        }
        
        /**
         * Gets the UV bounds of this region.
         * @return Vector4f containing (u, v, width, height) in UV coordinates
         */
        public Vector4f getUVBounds() {
            return new Vector4f(uvBounds);
        }
        
        /**
         * Gets the original size of the texture.
         * @return Vector2f containing (width, height) in pixels
         */
        public Vector2f getSize() {
            return new Vector2f(size);
        }
        
        /**
         * Gets the name of this region.
         * @return The region name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Checks if this region is animated.
         * @return False for basic regions (override in AnimatedAtlasRegion)
         */
        public boolean isAnimated() {
            return false;
        }
        
        /**
         * Gets the current frame UV bounds for animated textures.
         * @param time Current time in seconds
         * @return UV bounds for the current frame
         */
        public Vector4f getCurrentFrameUVBounds(float time) {
            return getUVBounds();
        }
    }
    
    /**
     * Represents an animated region within the texture atlas with multiple frames.
     */
    public static class AnimatedAtlasRegion extends AtlasRegion {
        private final List<Vector4f> frameUVBounds;
        private final float frameDuration;
        private final boolean looping;
        
        /**
         * Creates an animated atlas region.
         * @param name The region name
         * @param frameUVBounds List of UV bounds for each frame
         * @param frameSize The size of each frame
         * @param frameDuration Duration of each frame in seconds
         * @param looping Whether the animation should loop
         */
        public AnimatedAtlasRegion(String name, List<Vector4f> frameUVBounds, Vector2f frameSize, 
                                 float frameDuration, boolean looping) {
            super(name, frameUVBounds.get(0), frameSize);
            this.frameUVBounds = new ArrayList<>(frameUVBounds);
            this.frameDuration = frameDuration;
            this.looping = looping;
        }
        
        @Override
        public boolean isAnimated() {
            return true;
        }
        
        @Override
        public Vector4f getCurrentFrameUVBounds(float time) {
            if (frameUVBounds.isEmpty()) {
                return getUVBounds();
            }
            
            float totalDuration = frameUVBounds.size() * frameDuration;
            float animationTime = looping ? time % totalDuration : Math.min(time, totalDuration);
            
            int frameIndex = (int) (animationTime / frameDuration);
            frameIndex = Math.min(frameIndex, frameUVBounds.size() - 1);
            
            return new Vector4f(frameUVBounds.get(frameIndex));
        }
        
        /**
         * Gets the number of frames in this animation.
         * @return The frame count
         */
        public int getFrameCount() {
            return frameUVBounds.size();
        }
        
        /**
         * Gets the duration of each frame.
         * @return Frame duration in seconds
         */
        public float getFrameDuration() {
            return frameDuration;
        }
        
        /**
         * Gets the total animation duration.
         * @return Total duration in seconds
         */
        public float getTotalDuration() {
            return frameUVBounds.size() * frameDuration;
        }
        
        /**
         * Checks if the animation loops.
         * @return True if looping, false otherwise
         */
        public boolean isLooping() {
            return looping;
        }
        
        /**
         * Gets UV bounds for a specific frame.
         * @param frameIndex The frame index
         * @return UV bounds for the specified frame
         */
        public Vector4f getFrameUVBounds(int frameIndex) {
            if (frameIndex < 0 || frameIndex >= frameUVBounds.size()) {
                return getUVBounds();
            }
            return new Vector4f(frameUVBounds.get(frameIndex));
        }
    }
    
    /**
     * Rectangle representation for bin packing algorithms.
     */
    private static class Rectangle {
        int x, y, width, height;
        boolean used;
        
        Rectangle(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.used = false;
        }
        
        /**
         * Checks if this rectangle can fit another rectangle.
         */
        boolean canFit(int w, int h) {
            return !used && width >= w && height >= h;
        }
        
        /**
         * Splits this rectangle to accommodate a new rectangle.
         */
        Rectangle[] split(int w, int h) {
            if (!canFit(w, h)) return null;
            
            used = true;
            Rectangle[] result = new Rectangle[2];
            
            // Decide split direction based on remaining space
            int remainingHorizontal = width - w;
            int remainingVertical = height - h;
            
            if (remainingHorizontal > remainingVertical) {
                // Split horizontally
                result[0] = new Rectangle(x + w, y, remainingHorizontal, height);
                result[1] = new Rectangle(x, y + h, w, remainingVertical);
            } else {
                // Split vertically
                result[0] = new Rectangle(x, y + h, width, remainingVertical);
                result[1] = new Rectangle(x + w, y, remainingHorizontal, h);
            }
            
            return result;
        }
    }
    
    /**
     * Bin packing algorithm types.
     */
    public enum PackingAlgorithm {
        SHELF,      // Shelf-based packing (good for similar-sized textures)
        GUILLOTINE, // Guillotine packing (better space utilization)
        MAXRECTS    // MaxRects algorithm (best space utilization)
    }
    
    /**
     * Shelf for shelf-based packing algorithm.
     */
    private class Shelf {
        int y, height, usedWidth;
        
        Shelf(int y, int height) {
            this.y = y;
            this.height = height;
            this.usedWidth = 0;
        }
        
        boolean canFit(int width, int height) {
            return height <= this.height && usedWidth + width <= atlasSize;
        }
        
        Rectangle allocate(int width, int height) {
            if (!canFit(width, height)) return null;
            
            Rectangle rect = new Rectangle(usedWidth, y, width, height);
            usedWidth += width;
            return rect;
        }
    }
    
    private final int atlasSize;
    private final int textureId;
    private final Map<String, AtlasRegion> regions;
    private final PackingAlgorithm packingAlgorithm;
    
    // UV coordinate caching
    private final Map<String, float[]> uvCache;
    private final Map<String, float[]> normalizedUVCache;
    
    // Guillotine algorithm data
    private final java.util.List<Rectangle> freeRectangles;
    
    // Shelf algorithm data
    private final java.util.List<Shelf> shelves;
    
    // Simple row-based packing (fallback)
    private int currentX;
    private int currentY;
    private int currentRowHeight;
    
    /**
     * Creates a new texture atlas with the specified size using GUILLOTINE packing.
     * @param atlasSize The size of the atlas texture (width and height)
     */
    public TextureAtlas(int atlasSize) {
        this(atlasSize, PackingAlgorithm.GUILLOTINE);
    }
    
    /**
     * Creates a new texture atlas with the specified size and packing algorithm.
     * @param atlasSize The size of the atlas texture (width and height)
     * @param packingAlgorithm The bin packing algorithm to use
     */
    public TextureAtlas(int atlasSize, PackingAlgorithm packingAlgorithm) {
        this.atlasSize = atlasSize;
        this.regions = new HashMap<>();
        this.packingAlgorithm = packingAlgorithm;
        this.uvCache = new HashMap<>();
        this.normalizedUVCache = new HashMap<>();
        this.freeRectangles = new java.util.ArrayList<>();
        this.shelves = new java.util.ArrayList<>();
        this.currentX = 0;
        this.currentY = 0;
        this.currentRowHeight = 0;
        
        // Initialize packing algorithm
        initializePackingAlgorithm();
        
        // Create OpenGL texture
        this.textureId = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        
        // Set texture parameters
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        
        // Initialize with empty texture
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, atlasSize, atlasSize, 
                         0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        
        logger.info("Created texture atlas with size: " + atlasSize + "x" + atlasSize);
    }
    
    /**
     * Initializes the selected packing algorithm.
     */
    private void initializePackingAlgorithm() {
        switch (packingAlgorithm) {
            case GUILLOTINE:
            case MAXRECTS:
                freeRectangles.add(new Rectangle(0, 0, atlasSize, atlasSize));
                break;
            case SHELF:
                // Shelves are created dynamically as needed
                break;
        }
    }
    
    /**
     * Finds the best position for a texture using the guillotine algorithm.
     */
    private Rectangle findPositionGuillotine(int width, int height) {
        Rectangle bestRect = null;
        int bestShortSide = Integer.MAX_VALUE;
        int bestLongSide = Integer.MAX_VALUE;
        
        for (Rectangle rect : freeRectangles) {
            if (rect.canFit(width, height)) {
                int leftoverHorizontal = rect.width - width;
                int leftoverVertical = rect.height - height;
                int shortSide = Math.min(leftoverHorizontal, leftoverVertical);
                int longSide = Math.max(leftoverHorizontal, leftoverVertical);
                
                if (shortSide < bestShortSide || (shortSide == bestShortSide && longSide < bestLongSide)) {
                    bestRect = rect;
                    bestShortSide = shortSide;
                    bestLongSide = longSide;
                }
            }
        }
        
        if (bestRect != null) {
            // Split the rectangle and add new free rectangles
            Rectangle[] newRects = bestRect.split(width, height);
            freeRectangles.remove(bestRect);
            
            if (newRects != null) {
                for (Rectangle newRect : newRects) {
                    if (newRect.width > 0 && newRect.height > 0) {
                        freeRectangles.add(newRect);
                    }
                }
            }
            
            return new Rectangle(bestRect.x, bestRect.y, width, height);
        }
        
        return null;
    }
    
    /**
     * Finds the best position for a texture using the shelf algorithm.
     */
    private Rectangle findPositionShelf(int width, int height) {
        // Try to fit in existing shelves
        for (Shelf shelf : shelves) {
            if (shelf.canFit(width, height)) {
                return shelf.allocate(width, height);
            }
        }
        
        // Create a new shelf if possible
        int newShelfY = shelves.isEmpty() ? 0 : 
            shelves.get(shelves.size() - 1).y + shelves.get(shelves.size() - 1).height;
        
        if (newShelfY + height <= atlasSize && width <= atlasSize) {
            Shelf newShelf = new Shelf(newShelfY, height);
            shelves.add(newShelf);
            return newShelf.allocate(width, height);
        }
        
        return null;
    }
    
    /**
     * Adds a texture to the atlas.
     * @param name The name identifier for the texture
     * @param textureData The texture data as ByteBuffer
     * @param width The width of the texture
     * @param height The height of the texture
     * @return The atlas region for the added texture, or null if it doesn't fit
     */
    public AtlasRegion addTexture(String name, ByteBuffer textureData, int width, int height) {
        if (regions.containsKey(name)) {
            logger.warning("Texture with name '" + name + "' already exists in atlas");
            return regions.get(name);
        }
        
        // Find position for the texture using the selected packing algorithm
        Rectangle position = findPosition(width, height);
        if (position == null) {
            logger.fine("Cannot fit texture '" + name + "' (" + width + "x" + height + ") in " + 
                       atlasSize + "x" + atlasSize + " atlas (utilization: " + 
                       String.format("%.1f%%", getSpaceUtilization() * 100) + ")");
            return null;
        }
        
        // Upload texture data to the atlas
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
        GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, position.x, position.y, 
                            width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, textureData);
        
        // Calculate UV coordinates
        float u = (float) position.x / atlasSize;
        float v = (float) position.y / atlasSize;
        float uWidth = (float) width / atlasSize;
        float vHeight = (float) height / atlasSize;
        
        Vector4f uvBounds = new Vector4f(u, v, uWidth, vHeight);
        Vector2f size = new Vector2f(width, height);
        AtlasRegion region = new AtlasRegion(name, uvBounds, size);
        
        regions.put(name, region);
        
        // Cache UV coordinates for quick access
        cacheUVCoordinates(name, region);
        
        // Regenerate mipmaps
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        
        logger.info("Added texture '" + name + "' to atlas at position (" + 
                   position.x + ", " + position.y + ") with size (" + width + "x" + height + ")");
        
        return region;
    }
    
    /**
     * Adds an animated texture to the atlas.
     * @param name The name identifier for the animated texture
     * @param frameData List of texture data for each frame
     * @param frameWidth The width of each frame
     * @param frameHeight The height of each frame
     * @param frameDuration Duration of each frame in seconds
     * @param looping Whether the animation should loop
     * @return The animated atlas region for the added texture, or null if it doesn't fit
     */
    public AnimatedAtlasRegion addAnimatedTexture(String name, List<ByteBuffer> frameData, 
                                                int frameWidth, int frameHeight, 
                                                float frameDuration, boolean looping) {
        if (regions.containsKey(name)) {
            logger.warning("Texture with name '" + name + "' already exists in atlas");
            AtlasRegion existing = regions.get(name);
            if (existing instanceof AnimatedAtlasRegion) {
                return (AnimatedAtlasRegion) existing;
            }
            return null;
        }
        
        List<Vector4f> frameUVBounds = new ArrayList<>();
        
        // Add each frame to the atlas
        for (int i = 0; i < frameData.size(); i++) {
            String frameName = name + "_frame_" + i;
            Rectangle position = findPosition(frameWidth, frameHeight);
            
            if (position == null) {
                logger.warning("Cannot fit frame " + i + " of animated texture '" + name + 
                             "' (" + frameWidth + "x" + frameHeight + ") in atlas");
                
                // Clean up any frames that were already added
                for (int j = 0; j < i; j++) {
                    String cleanupFrameName = name + "_frame_" + j;
                    regions.remove(cleanupFrameName);
                }
                return null;
            }
            
            // Upload frame data to the atlas
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
            GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, position.x, position.y, 
                                frameWidth, frameHeight, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, frameData.get(i));
            
            // Calculate UV coordinates for this frame
            float u = (float) position.x / atlasSize;
            float v = (float) position.y / atlasSize;
            float uWidth = (float) frameWidth / atlasSize;
            float vHeight = (float) frameHeight / atlasSize;
            
            Vector4f frameUV = new Vector4f(u, v, uWidth, vHeight);
            frameUVBounds.add(frameUV);
            
            // Create a temporary region for this frame (for internal tracking)
            Vector2f frameSize = new Vector2f(frameWidth, frameHeight);
            AtlasRegion frameRegion = new AtlasRegion(frameName, frameUV, frameSize);
            regions.put(frameName, frameRegion);
        }
        
        // Create the animated region
        Vector2f frameSize = new Vector2f(frameWidth, frameHeight);
        AnimatedAtlasRegion animatedRegion = new AnimatedAtlasRegion(name, frameUVBounds, frameSize, frameDuration, looping);
        regions.put(name, animatedRegion);
        
        // Cache UV coordinates for quick access
        cacheUVCoordinates(name, animatedRegion);
        
        // Regenerate mipmaps
        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        
        logger.info("Added animated texture '" + name + "' with " + frameData.size() + 
                   " frames (" + frameWidth + "x" + frameHeight + " each) to atlas");
        
        return animatedRegion;
    }
    
    /**
     * Caches UV coordinates for a region to improve performance.
     * @param name The region name
     * @param region The atlas region
     */
    private void cacheUVCoordinates(String name, AtlasRegion region) {
        Vector4f uvBounds = region.getUVBounds();
        
        // Cache standard UV coordinates [minU, minV, maxU, maxV]
        float[] uvCoords = new float[4];
        uvCoords[0] = uvBounds.x; // minU
        uvCoords[1] = uvBounds.y; // minV
        uvCoords[2] = uvBounds.x + uvBounds.z; // maxU
        uvCoords[3] = uvBounds.y + uvBounds.w; // maxV
        uvCache.put(name, uvCoords);
        
        // Cache normalized UV coordinates for different texture coordinate systems
        float[] normalizedCoords = new float[8];
        normalizedCoords[0] = uvBounds.x; // bottom-left U
        normalizedCoords[1] = uvBounds.y; // bottom-left V
        normalizedCoords[2] = uvBounds.x + uvBounds.z; // bottom-right U
        normalizedCoords[3] = uvBounds.y; // bottom-right V
        normalizedCoords[4] = uvBounds.x + uvBounds.z; // top-right U
        normalizedCoords[5] = uvBounds.y + uvBounds.w; // top-right V
        normalizedCoords[6] = uvBounds.x; // top-left U
        normalizedCoords[7] = uvBounds.y + uvBounds.w; // top-left V
        normalizedUVCache.put(name, normalizedCoords);
    }
    
    /**
     * Gets cached UV coordinates for a region.
     * @param name The region name
     * @return UV coordinates as [minU, minV, maxU, maxV], or null if not found
     */
    public float[] getCachedUVCoordinates(String name) {
        float[] cached = uvCache.get(name);
        return cached != null ? cached.clone() : null;
    }
    
    /**
     * Gets cached normalized UV coordinates for a region (quad vertices).
     * @param name The region name
     * @return UV coordinates as [BL_U, BL_V, BR_U, BR_V, TR_U, TR_V, TL_U, TL_V], or null if not found
     */
    public float[] getCachedNormalizedUVCoordinates(String name) {
        float[] cached = normalizedUVCache.get(name);
        return cached != null ? cached.clone() : null;
    }
    
    /**
     * Gets UV coordinates for a region with time-based animation support.
     * @param name The region name
     * @param time Current time in seconds (for animated textures)
     * @return UV coordinates as [minU, minV, maxU, maxV], or null if not found
     */
    public float[] getUVCoordinatesWithTime(String name, float time) {
        AtlasRegion region = regions.get(name);
        if (region == null) {
            return null;
        }
        
        Vector4f uvBounds = region.getCurrentFrameUVBounds(time);
        return new float[] {
            uvBounds.x, // minU
            uvBounds.y, // minV
            uvBounds.x + uvBounds.z, // maxU
            uvBounds.y + uvBounds.w  // maxV
        };
    }
    
    /**
     * Invalidates UV coordinate cache for a specific region.
     * @param name The region name
     */
    public void invalidateUVCache(String name) {
        uvCache.remove(name);
        normalizedUVCache.remove(name);
    }
    
    /**
     * Clears all UV coordinate caches.
     */
    public void clearUVCache() {
        uvCache.clear();
        normalizedUVCache.clear();
    }
    
    /**
     * Finds a position for a texture using the selected packing algorithm.
     * @param width The width of the texture to place
     * @param height The height of the texture to place
     * @return A Rectangle representing the position, or null if it doesn't fit
     */
    private Rectangle findPosition(int width, int height) {
        switch (packingAlgorithm) {
            case GUILLOTINE:
            case MAXRECTS:
                return findPositionGuillotine(width, height);
            case SHELF:
                return findPositionShelf(width, height);
            default:
                return findPositionSimple(width, height);
        }
    }
    
    /**
     * Finds a position for a texture using simple row-based packing (fallback).
     * @param width The width of the texture to place
     * @param height The height of the texture to place
     * @return A Rectangle representing the position, or null if it doesn't fit
     */
    private Rectangle findPositionSimple(int width, int height) {
        // Check if texture fits in current row
        if (currentX + width <= atlasSize && currentY + height <= atlasSize) {
            Rectangle position = new Rectangle(currentX, currentY, width, height);
            currentX += width;
            currentRowHeight = Math.max(currentRowHeight, height);
            return position;
        }
        
        // Move to next row
        currentX = 0;
        currentY += currentRowHeight;
        currentRowHeight = 0;
        
        // Check if texture fits in new row
        if (currentX + width <= atlasSize && currentY + height <= atlasSize) {
            Rectangle position = new Rectangle(currentX, currentY, width, height);
            currentX += width;
            currentRowHeight = Math.max(currentRowHeight, height);
            return position;
        }
        
        // Doesn't fit
        return null;
    }
    
    /**
     * Gets a texture region by name.
     * @param name The name of the texture region
     * @return The atlas region, or null if not found
     */
    public AtlasRegion getRegion(String name) {
        return regions.get(name);
    }
    
    /**
     * Checks if a texture region exists.
     * @param name The name of the texture region
     * @return True if the region exists, false otherwise
     */
    public boolean hasRegion(String name) {
        return regions.containsKey(name);
    }
    
    /**
     * Gets the OpenGL texture ID of the atlas.
     * @return The texture ID
     */
    public int getTextureId() {
        return textureId;
    }
    
    /**
     * Gets the size of the atlas.
     * @return The atlas size in pixels
     */
    public int getAtlasSize() {
        return atlasSize;
    }
    
    /**
     * Gets the number of textures in the atlas.
     * @return The number of texture regions
     */
    public int getRegionCount() {
        return regions.size();
    }
    
    /**
     * Gets the number of textures in the atlas.
     * Alias for getRegionCount() for compatibility.
     * @return The number of texture regions
     */
    public int getTextureCount() {
        return getRegionCount();
    }
    
    /**
     * Binds the atlas texture for rendering.
     */
    public void bind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);
    }
    
    /**
     * Unbinds the atlas texture.
     */
    public void unbind() {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
    
    /**
     * Gets the space utilization percentage of the atlas.
     * @return The percentage of space used (0.0 to 1.0)
     */
    public float getSpaceUtilization() {
        int usedPixels = 0;
        for (AtlasRegion region : regions.values()) {
            usedPixels += (int)(region.getSize().x * region.getSize().y);
        }
        return (float) usedPixels / (atlasSize * atlasSize);
    }
    
    /**
     * Gets the number of free rectangles (for guillotine algorithm).
     * @return The number of free rectangles
     */
    public int getFreeRectangleCount() {
        return freeRectangles.size();
    }
    
    /**
     * Gets the number of shelves (for shelf algorithm).
     * @return The number of shelves
     */
    public int getShelfCount() {
        return shelves.size();
    }
    
    /**
     * Gets the packing algorithm being used.
     * @return The packing algorithm
     */
    public PackingAlgorithm getPackingAlgorithm() {
        return packingAlgorithm;
    }
    
    /**
     * Gets the memory usage of this atlas in bytes.
     * @return The memory usage in bytes
     */
    public long getMemoryUsage() {
        // Calculate memory usage based on atlas size and format
        // Assuming RGBA format (4 bytes per pixel)
        return (long) atlasSize * atlasSize * 4;
    }
    
    /**
     * Gets detailed atlas statistics.
     * @return A string containing atlas statistics
     */
    public String getStatistics() {
        StringBuilder stats = new StringBuilder();
        stats.append("Texture Atlas Statistics:\n");
        stats.append("  Size: ").append(atlasSize).append("x").append(atlasSize).append("\n");
        stats.append("  Algorithm: ").append(packingAlgorithm).append("\n");
        stats.append("  Regions: ").append(regions.size()).append("\n");
        stats.append("  Space Utilization: ").append(String.format("%.2f%%", getSpaceUtilization() * 100)).append("\n");
        
        switch (packingAlgorithm) {
            case GUILLOTINE:
            case MAXRECTS:
                stats.append("  Free Rectangles: ").append(freeRectangles.size()).append("\n");
                break;
            case SHELF:
                stats.append("  Shelves: ").append(shelves.size()).append("\n");
                break;
        }
        
        return stats.toString();
    }
    
    /**
     * Cleans up OpenGL resources.
     */
    public void cleanup() {
        if (textureId != 0) {
            GL11.glDeleteTextures(textureId);
            logger.info("Cleaned up texture atlas");
        }
    }
}