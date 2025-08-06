package com.odyssey.world.chunk;

import org.joml.Vector3f;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Manages seamless LOD transitions and occlusion culling for the chunk LOD system.
 * Prevents "popping" artifacts when chunks change LOD levels and implements
 * hierarchical Z-buffer techniques for efficient culling.
 * 
 * @author Odyssey Team
 * @since 1.0.0
 */
public class LODTransitionManager {
    private static final Logger logger = LoggerFactory.getLogger(LODTransitionManager.class);
    
    /**
     * Represents a LOD transition state for a chunk.
     */
    public static class LODTransition {
        private final ChunkPosition chunkPosition;
        private final ChunkLOD.LODLevel fromLevel;
        private final ChunkLOD.LODLevel toLevel;
        private final long startTime;
        private final long duration;
        private final AtomicBoolean isActive = new AtomicBoolean(true);
        
        /**
         * Creates a new LOD transition.
         * 
         * @param chunkPosition the chunk being transitioned
         * @param fromLevel the starting LOD level
         * @param toLevel the target LOD level
         * @param duration transition duration in milliseconds
         */
        public LODTransition(ChunkPosition chunkPosition, ChunkLOD.LODLevel fromLevel, 
                           ChunkLOD.LODLevel toLevel, long duration) {
            this.chunkPosition = chunkPosition;
            this.fromLevel = fromLevel;
            this.toLevel = toLevel;
            this.startTime = System.currentTimeMillis();
            this.duration = duration;
        }
        
        /**
         * Gets the current transition progress (0.0 to 1.0).
         * 
         * @return transition progress
         */
        public float getProgress() {
            if (!isActive.get()) {
                return 1.0f;
            }
            
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed >= duration) {
                isActive.set(false);
                return 1.0f;
            }
            
            return Math.min(1.0f, (float) elapsed / duration);
        }
        
        /**
         * Gets the interpolated LOD factor for smooth transitions.
         * Uses smooth step function for natural-looking transitions.
         * 
         * @return interpolated LOD factor
         */
        public float getInterpolatedLODFactor() {
            float progress = getProgress();
            
            // Smooth step function for natural transitions
            float smoothProgress = progress * progress * (3.0f - 2.0f * progress);
            
            float fromFactor = fromLevel.getQualityFactor();
            float toFactor = toLevel.getQualityFactor();
            
            return fromFactor + (toFactor - fromFactor) * smoothProgress;
        }
        
        /**
         * Gets the interpolated block reduction factor.
         * 
         * @return interpolated block reduction factor
         */
        public int getInterpolatedBlockReduction() {
            float progress = getProgress();
            float smoothProgress = progress * progress * (3.0f - 2.0f * progress);
            
            int fromReduction = fromLevel.getBlockReductionFactor();
            int toReduction = toLevel.getBlockReductionFactor();
            
            return Math.round(fromReduction + (toReduction - fromReduction) * smoothProgress);
        }
        
        public ChunkPosition getChunkPosition() { return chunkPosition; }
        public ChunkLOD.LODLevel getFromLevel() { return fromLevel; }
        public ChunkLOD.LODLevel getToLevel() { return toLevel; }
        public boolean isActive() { return isActive.get(); }
        public long getStartTime() { return startTime; }
        public long getDuration() { return duration; }
    }
    
    /**
     * Implements hierarchical Z-buffer occlusion culling for LOD chunks.
     */
    public static class HierarchicalOcclusionCuller {
        private final int hierarchicalZBufferWidth;
        private final int hierarchicalZBufferHeight;
        private final int maxMipLevels;
        private final float[][] hierarchicalZBuffer;
        private final Matrix4f viewProjectionMatrix = new Matrix4f();
        
        /**
         * Creates a hierarchical occlusion culler.
         * 
         * @param width Z-buffer width
         * @param height Z-buffer height
         */
        public HierarchicalOcclusionCuller(int width, int height) {
            this.hierarchicalZBufferWidth = width;
            this.hierarchicalZBufferHeight = height;
            this.maxMipLevels = Math.min(
                Integer.numberOfTrailingZeros(Integer.highestOneBit(width)),
                Integer.numberOfTrailingZeros(Integer.highestOneBit(height))
            );
            
            // Initialize hierarchical Z-buffer with multiple mip levels
            this.hierarchicalZBuffer = new float[maxMipLevels][];
            int currentWidth = width;
            int currentHeight = height;
            
            for (int level = 0; level < maxMipLevels; level++) {
                hierarchicalZBuffer[level] = new float[currentWidth * currentHeight];
                // Initialize with far plane distance
                for (int i = 0; i < hierarchicalZBuffer[level].length; i++) {
                    hierarchicalZBuffer[level][i] = 1.0f;
                }
                currentWidth = Math.max(1, currentWidth / 2);
                currentHeight = Math.max(1, currentHeight / 2);
            }
        }
        
        /**
         * Updates the view-projection matrix for culling calculations.
         * 
         * @param viewMatrix the view matrix
         * @param projectionMatrix the projection matrix
         */
        public void updateViewProjectionMatrix(Matrix4f viewMatrix, Matrix4f projectionMatrix) {
            projectionMatrix.mul(viewMatrix, viewProjectionMatrix);
        }
        
        /**
         * Tests if a chunk's bounding box is occluded.
         * 
         * @param chunkPosition the chunk position
         * @param chunkSize the chunk size in world units
         * @param chunkHeight the chunk height
         * @return true if the chunk is occluded and can be culled
         */
        public boolean isChunkOccluded(ChunkPosition chunkPosition, float chunkSize, float chunkHeight) {
            // Calculate chunk bounding box in world space
            float minX = chunkPosition.x * chunkSize;
            float maxX = minX + chunkSize;
            float minY = 0.0f;
            float maxY = chunkHeight;
            float minZ = chunkPosition.z * chunkSize;
            float maxZ = minZ + chunkSize;
            
            // Transform bounding box to screen space
            Vector3f[] corners = {
                new Vector3f(minX, minY, minZ),
                new Vector3f(maxX, minY, minZ),
                new Vector3f(minX, maxY, minZ),
                new Vector3f(maxX, maxY, minZ),
                new Vector3f(minX, minY, maxZ),
                new Vector3f(maxX, minY, maxZ),
                new Vector3f(minX, maxY, maxZ),
                new Vector3f(maxX, maxY, maxZ)
            };
            
            float minScreenX = Float.MAX_VALUE;
            float maxScreenX = Float.MIN_VALUE;
            float minScreenY = Float.MAX_VALUE;
            float maxScreenY = Float.MIN_VALUE;
            float minDepth = Float.MAX_VALUE;
            
            // Transform all corners to screen space
            for (Vector3f corner : corners) {
                Vector3f screenPos = transformToScreenSpace(corner);
                
                // Check if corner is behind camera
                if (screenPos.z < 0) {
                    return false; // Don't cull if any corner is behind camera
                }
                
                minScreenX = Math.min(minScreenX, screenPos.x);
                maxScreenX = Math.max(maxScreenX, screenPos.x);
                minScreenY = Math.min(minScreenY, screenPos.y);
                maxScreenY = Math.max(maxScreenY, screenPos.y);
                minDepth = Math.min(minDepth, screenPos.z);
            }
            
            // Check if bounding box is outside screen
            if (maxScreenX < 0 || minScreenX >= hierarchicalZBufferWidth ||
                maxScreenY < 0 || minScreenY >= hierarchicalZBufferHeight) {
                return true; // Cull if completely outside screen
            }
            
            // Clamp to screen bounds
            int pixelMinX = Math.max(0, (int) Math.floor(minScreenX));
            int pixelMaxX = Math.min(hierarchicalZBufferWidth - 1, (int) Math.ceil(maxScreenX));
            int pixelMinY = Math.max(0, (int) Math.floor(minScreenY));
            int pixelMaxY = Math.min(hierarchicalZBufferHeight - 1, (int) Math.ceil(maxScreenY));
            
            // Determine appropriate mip level based on screen size
            int screenWidth = pixelMaxX - pixelMinX + 1;
            int screenHeight = pixelMaxY - pixelMinY + 1;
            int maxDimension = Math.max(screenWidth, screenHeight);
            int mipLevel = Math.min(maxMipLevels - 1, 
                                   Math.max(0, Integer.numberOfLeadingZeros(maxDimension) - 
                                           Integer.numberOfLeadingZeros(hierarchicalZBufferWidth)));
            
            // Test occlusion at appropriate mip level
            return testOcclusionAtMipLevel(pixelMinX, pixelMinY, pixelMaxX, pixelMaxY, 
                                         minDepth, mipLevel);
        }
        
        /**
         * Transforms a world space position to screen space.
         * 
         * @param worldPos world space position
         * @return screen space position
         */
        private Vector3f transformToScreenSpace(Vector3f worldPos) {
            Vector3f clipPos = new Vector3f();
            viewProjectionMatrix.transformPosition(worldPos, clipPos);
            
            // Perspective divide
            if (Math.abs(clipPos.z) > 0.0001f) {
                clipPos.x /= clipPos.z;
                clipPos.y /= clipPos.z;
            }
            
            // Convert to screen coordinates
            float screenX = (clipPos.x + 1.0f) * 0.5f * hierarchicalZBufferWidth;
            float screenY = (1.0f - clipPos.y) * 0.5f * hierarchicalZBufferHeight;
            
            return new Vector3f(screenX, screenY, clipPos.z);
        }
        
        /**
         * Tests occlusion at a specific mip level.
         * 
         * @param minX minimum X coordinate
         * @param minY minimum Y coordinate
         * @param maxX maximum X coordinate
         * @param maxY maximum Y coordinate
         * @param depth depth to test
         * @param mipLevel mip level to test at
         * @return true if occluded
         */
        private boolean testOcclusionAtMipLevel(int minX, int minY, int maxX, int maxY, 
                                              float depth, int mipLevel) {
            float[] zBuffer = hierarchicalZBuffer[mipLevel];
            int levelWidth = hierarchicalZBufferWidth >> mipLevel;
            int levelHeight = hierarchicalZBufferHeight >> mipLevel;
            
            // Scale coordinates to mip level
            int scaledMinX = Math.max(0, minX >> mipLevel);
            int scaledMaxX = Math.min(levelWidth - 1, maxX >> mipLevel);
            int scaledMinY = Math.max(0, minY >> mipLevel);
            int scaledMaxY = Math.min(levelHeight - 1, maxY >> mipLevel);
            
            // Test all pixels in the region
            for (int y = scaledMinY; y <= scaledMaxY; y++) {
                for (int x = scaledMinX; x <= scaledMaxX; x++) {
                    int index = y * levelWidth + x;
                    if (index < zBuffer.length && depth < zBuffer[index]) {
                        return false; // Not occluded if any pixel is closer
                    }
                }
            }
            
            return true; // Occluded if all pixels are farther
        }
        
        /**
         * Updates the hierarchical Z-buffer with rendered geometry.
         * This should be called after rendering opaque geometry.
         * 
         * @param depthBuffer the depth buffer from rendering
         */
        public void updateHierarchicalZBuffer(float[] depthBuffer) {
            if (depthBuffer.length != hierarchicalZBuffer[0].length) {
                logger.warn("Depth buffer size mismatch: expected {}, got {}", 
                           hierarchicalZBuffer[0].length, depthBuffer.length);
                return;
            }
            
            // Copy depth buffer to level 0
            System.arraycopy(depthBuffer, 0, hierarchicalZBuffer[0], 0, depthBuffer.length);
            
            // Generate mip levels by taking maximum depth in 2x2 regions
            for (int level = 1; level < maxMipLevels; level++) {
                generateMipLevel(level);
            }
        }
        
        /**
         * Generates a mip level by downsampling the previous level.
         * 
         * @param level the mip level to generate
         */
        private void generateMipLevel(int level) {
            float[] prevLevel = hierarchicalZBuffer[level - 1];
            float[] currentLevel = hierarchicalZBuffer[level];
            
            int prevWidth = hierarchicalZBufferWidth >> (level - 1);
            int prevHeight = hierarchicalZBufferHeight >> (level - 1);
            int currentWidth = hierarchicalZBufferWidth >> level;
            int currentHeight = hierarchicalZBufferHeight >> level;
            
            for (int y = 0; y < currentHeight; y++) {
                for (int x = 0; x < currentWidth; x++) {
                    int prevX = x * 2;
                    int prevY = y * 2;
                    
                    // Sample 2x2 region and take maximum depth (farthest)
                    float maxDepth = 0.0f;
                    for (int dy = 0; dy < 2 && prevY + dy < prevHeight; dy++) {
                        for (int dx = 0; dx < 2 && prevX + dx < prevWidth; dx++) {
                            int prevIndex = (prevY + dy) * prevWidth + (prevX + dx);
                            if (prevIndex < prevLevel.length) {
                                maxDepth = Math.max(maxDepth, prevLevel[prevIndex]);
                            }
                        }
                    }
                    
                    int currentIndex = y * currentWidth + x;
                    if (currentIndex < currentLevel.length) {
                        currentLevel[currentIndex] = maxDepth;
                    }
                }
            }
        }
        
        /**
         * Clears the hierarchical Z-buffer.
         */
        public void clear() {
            for (float[] level : hierarchicalZBuffer) {
                for (int i = 0; i < level.length; i++) {
                    level[i] = 1.0f; // Far plane
                }
            }
        }
    }
    
    // Instance fields
    private final Map<ChunkPosition, LODTransition> activeTransitions = new ConcurrentHashMap<>();
    private final HierarchicalOcclusionCuller occlusionCuller;
    private final long defaultTransitionDuration;
    
    /**
     * Creates a new LOD transition manager.
     * 
     * @param screenWidth screen width for occlusion culling
     * @param screenHeight screen height for occlusion culling
     * @param defaultTransitionDuration default transition duration in milliseconds
     */
    public LODTransitionManager(int screenWidth, int screenHeight, long defaultTransitionDuration) {
        this.occlusionCuller = new HierarchicalOcclusionCuller(screenWidth, screenHeight);
        this.defaultTransitionDuration = defaultTransitionDuration;
        
        logger.info("Initialized LOD transition manager with {}x{} occlusion buffer", 
                   screenWidth, screenHeight);
    }
    
    /**
     * Starts a LOD transition for a chunk.
     * 
     * @param chunkPosition the chunk position
     * @param fromLevel the current LOD level
     * @param toLevel the target LOD level
     * @return the created transition
     */
    public LODTransition startTransition(ChunkPosition chunkPosition, 
                                       ChunkLOD.LODLevel fromLevel, 
                                       ChunkLOD.LODLevel toLevel) {
        return startTransition(chunkPosition, fromLevel, toLevel, defaultTransitionDuration);
    }
    
    /**
     * Starts a LOD transition for a chunk with custom duration.
     * 
     * @param chunkPosition the chunk position
     * @param fromLevel the current LOD level
     * @param toLevel the target LOD level
     * @param duration transition duration in milliseconds
     * @return the created transition
     */
    public LODTransition startTransition(ChunkPosition chunkPosition, 
                                       ChunkLOD.LODLevel fromLevel, 
                                       ChunkLOD.LODLevel toLevel, 
                                       long duration) {
        // Cancel any existing transition for this chunk
        LODTransition existingTransition = activeTransitions.get(chunkPosition);
        if (existingTransition != null) {
            existingTransition.isActive.set(false);
        }
        
        LODTransition transition = new LODTransition(chunkPosition, fromLevel, toLevel, duration);
        activeTransitions.put(chunkPosition, transition);
        
        logger.debug("Started LOD transition for chunk {} from {} to {} ({}ms)", 
                    chunkPosition, fromLevel, toLevel, duration);
        
        return transition;
    }
    
    /**
     * Gets the active transition for a chunk.
     * 
     * @param chunkPosition the chunk position
     * @return the active transition or null if none
     */
    public LODTransition getActiveTransition(ChunkPosition chunkPosition) {
        LODTransition transition = activeTransitions.get(chunkPosition);
        if (transition != null && !transition.isActive()) {
            activeTransitions.remove(chunkPosition);
            return null;
        }
        return transition;
    }
    
    /**
     * Updates all active transitions and removes completed ones.
     * 
     * @return number of active transitions
     */
    public int updateTransitions() {
        List<ChunkPosition> completedTransitions = new ArrayList<>();
        
        for (Map.Entry<ChunkPosition, LODTransition> entry : activeTransitions.entrySet()) {
            LODTransition transition = entry.getValue();
            if (!transition.isActive()) {
                completedTransitions.add(entry.getKey());
            }
        }
        
        // Remove completed transitions
        for (ChunkPosition position : completedTransitions) {
            activeTransitions.remove(position);
        }
        
        return activeTransitions.size();
    }
    
    /**
     * Gets all currently active transitions.
     * 
     * @return list of active transitions
     */
    public List<LODTransition> getActiveTransitions() {
        return new ArrayList<>(activeTransitions.values());
    }
    
    /**
     * Updates the view-projection matrix for occlusion culling.
     * 
     * @param viewMatrix the view matrix
     * @param projectionMatrix the projection matrix
     */
    public void updateViewProjectionMatrix(Matrix4f viewMatrix, Matrix4f projectionMatrix) {
        occlusionCuller.updateViewProjectionMatrix(viewMatrix, projectionMatrix);
    }
    
    /**
     * Tests if a chunk is occluded and can be culled.
     * 
     * @param chunkPosition the chunk position
     * @param chunkSize the chunk size in world units
     * @param chunkHeight the chunk height
     * @return true if the chunk is occluded
     */
    public boolean isChunkOccluded(ChunkPosition chunkPosition, float chunkSize, float chunkHeight) {
        return occlusionCuller.isChunkOccluded(chunkPosition, chunkSize, chunkHeight);
    }
    
    /**
     * Updates the hierarchical Z-buffer with rendered depth data.
     * 
     * @param depthBuffer the depth buffer from rendering
     */
    public void updateOcclusionBuffer(float[] depthBuffer) {
        occlusionCuller.updateHierarchicalZBuffer(depthBuffer);
    }
    
    /**
     * Clears the occlusion buffer.
     */
    public void clearOcclusionBuffer() {
        occlusionCuller.clear();
    }
    
    /**
     * Clears all active transitions.
     */
    public void clearTransitions() {
        activeTransitions.clear();
    }
    
    /**
     * Gets the number of active transitions.
     * 
     * @return number of active transitions
     */
    public int getActiveTransitionCount() {
        return activeTransitions.size();
    }
}