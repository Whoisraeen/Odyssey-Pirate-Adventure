package com.odyssey.rendering;

import com.odyssey.util.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages framebuffer creation, caching, and cleanup for the rendering system.
 * Provides efficient framebuffer resource management with automatic cleanup.
 */
public class FramebufferManager {
    
    private static final Logger logger = Logger.getLogger(FramebufferManager.class);
    
    // Framebuffer cache
    private final Map<String, Framebuffer> framebufferCache;
    private final Map<String, Integer> referenceCount;
    
    // Common framebuffers
    private Framebuffer shadowMapFramebuffer;
    private Framebuffer gBufferFramebuffer;
    private Framebuffer postProcessFramebuffer;
    
    public FramebufferManager() {
        this.framebufferCache = new HashMap<>();
        this.referenceCount = new HashMap<>();
        
        logger.info("FramebufferManager initialized");
    }
    
    /**
     * Initialize the framebuffer manager (called by Renderer).
     * This method is separate from the constructor to allow for explicit initialization.
     */
    public void initialize() {
        // Additional initialization if needed
        logger.debug("FramebufferManager initialize() called");
    }
    
    /**
     * Creates a basic color framebuffer with normal attachment
     */
    public Framebuffer createColorFramebuffer(String name, int width, int height) {
        if (framebufferCache.containsKey(name)) {
            referenceCount.put(name, referenceCount.get(name) + 1);
            return framebufferCache.get(name);
        }
        
        Framebuffer framebuffer = new Framebuffer.Builder()
            .size(width, height)
            .addColorRGBA8()    // Color attachment 0 - main color
            .addColorRGBA16F()  // Color attachment 1 - normals
            .depthTexture()
            .build();
            
        framebufferCache.put(name, framebuffer);
        referenceCount.put(name, 1);
        
        logger.debug("Created color framebuffer: {} ({}x{})", name, width, height);
        return framebuffer;
    }
    
    /**
     * Creates a HDR color framebuffer
     */
    public Framebuffer createHDRFramebuffer(String name, int width, int height) {
        if (framebufferCache.containsKey(name)) {
            referenceCount.put(name, referenceCount.get(name) + 1);
            return framebufferCache.get(name);
        }
        
        Framebuffer framebuffer = new Framebuffer.Builder()
            .size(width, height)
            .addColorRGBA16F()
            .depthRbo()
            .build();
            
        framebufferCache.put(name, framebuffer);
        referenceCount.put(name, 1);
        
        logger.debug("Created HDR framebuffer: {} ({}x{})", name, width, height);
        return framebuffer;
    }
    
    /**
     * Creates a depth-only framebuffer (for shadow mapping)
     */
    public Framebuffer createDepthFramebuffer(String name, int width, int height) {
        if (framebufferCache.containsKey(name)) {
            referenceCount.put(name, referenceCount.get(name) + 1);
            return framebufferCache.get(name);
        }
        
        Framebuffer framebuffer = new Framebuffer.Builder()
            .size(width, height)
            .depthTexture()
            .build();
            
        framebufferCache.put(name, framebuffer);
        referenceCount.put(name, 1);
        
        logger.debug("Created depth framebuffer: {} ({}x{})", name, width, height);
        return framebuffer;
    }
    
    /**
     * Creates a G-Buffer for deferred rendering
     */
    public Framebuffer createGBuffer(String name, int width, int height) {
        if (framebufferCache.containsKey(name)) {
            referenceCount.put(name, referenceCount.get(name) + 1);
            return framebufferCache.get(name);
        }
        
        Framebuffer framebuffer = new Framebuffer.Builder()
            .size(width, height)
            .addColorRGBA8()    // Albedo + Metallic
            .addColorRGBA16F()  // Normal + Roughness
            .addColorRGBA8()    // Motion + AO
            .depthTexture()
            .build();
            
        framebufferCache.put(name, framebuffer);
        referenceCount.put(name, 1);
        
        logger.debug("Created G-Buffer: {} ({}x{})", name, width, height);
        return framebuffer;
    }
    
    /**
     * Creates a multisampled framebuffer for anti-aliasing
     */
    public Framebuffer createMSAAFramebuffer(String name, int width, int height, int samples) {
        if (framebufferCache.containsKey(name)) {
            referenceCount.put(name, referenceCount.get(name) + 1);
            return framebufferCache.get(name);
        }
        
        Framebuffer framebuffer = new Framebuffer.Builder()
            .size(width, height)
            .multisampled(samples)
            .addColorRGBA8()
            .depthRbo()
            .build();
            
        framebufferCache.put(name, framebuffer);
        referenceCount.put(name, 1);
        
        logger.debug("Created MSAA framebuffer: {} ({}x{}, {}x samples)", 
                    name, width, height, samples);
        return framebuffer;
    }
    
    /**
     * Creates a custom framebuffer with specified attachments
     */
    public Framebuffer createCustomFramebuffer(String name, Framebuffer.Builder builder) {
        if (framebufferCache.containsKey(name)) {
            referenceCount.put(name, referenceCount.get(name) + 1);
            return framebufferCache.get(name);
        }
        
        Framebuffer framebuffer = builder.build();
        
        framebufferCache.put(name, framebuffer);
        referenceCount.put(name, 1);
        
        logger.debug("Created custom framebuffer: {}", name);
        return framebuffer;
    }
    
    /**
     * Gets a cached framebuffer by name
     */
    public Framebuffer getFramebuffer(String name) {
        Framebuffer framebuffer = framebufferCache.get(name);
        if (framebuffer != null) {
            referenceCount.put(name, referenceCount.get(name) + 1);
            return framebuffer;
        }
        
        logger.warn("Framebuffer not found: {}", name);
        return null;
    }
    
    /**
     * Releases a framebuffer reference
     */
    public void releaseFramebuffer(String name) {
        if (!referenceCount.containsKey(name)) {
            return;
        }
        
        int count = referenceCount.get(name) - 1;
        if (count <= 0) {
            // Remove from cache and cleanup
            Framebuffer framebuffer = framebufferCache.remove(name);
            referenceCount.remove(name);
            
            if (framebuffer != null) {
                framebuffer.cleanup();
                logger.debug("Released framebuffer: {}", name);
            }
        } else {
            referenceCount.put(name, count);
        }
    }
    
    /**
     * Resizes a framebuffer
     */
    public void resizeFramebuffer(String name, int newWidth, int newHeight) {
        Framebuffer framebuffer = framebufferCache.get(name);
        if (framebuffer != null) {
            framebuffer.resize(newWidth, newHeight);
            logger.debug("Resized framebuffer: {} to {}x{}", name, newWidth, newHeight);
        } else {
            logger.warn("Cannot resize framebuffer - not found: {}", name);
        }
    }
    
    /**
     * Gets or creates a shadow map framebuffer
     */
    public Framebuffer getShadowMapFramebuffer(int size) {
        String name = "shadowMap_" + size;
        if (!framebufferCache.containsKey(name)) {
            createDepthFramebuffer(name, size, size);
        }
        return getFramebuffer(name);
    }
    
    /**
     * Gets or creates a G-Buffer framebuffer
     */
    public Framebuffer getGBufferFramebuffer(int width, int height) {
        String name = "gBuffer_" + width + "x" + height;
        if (!framebufferCache.containsKey(name)) {
            createGBuffer(name, width, height);
        }
        return getFramebuffer(name);
    }
    
    /**
     * Gets or creates a post-process framebuffer
     */
    public Framebuffer getPostProcessFramebuffer(int width, int height) {
        String name = "postProcess_" + width + "x" + height;
        if (!framebufferCache.containsKey(name)) {
            createHDRFramebuffer(name, width, height);
        }
        return getFramebuffer(name);
    }
    
    /**
     * Creates common framebuffers for the rendering pipeline
     */
    public void createCommonFramebuffers(int width, int height) {
        // Shadow map (1024x1024 is common for shadows)
        shadowMapFramebuffer = createDepthFramebuffer("shadowMap", 1024, 1024);
        
        // G-Buffer for deferred rendering
        gBufferFramebuffer = createGBuffer("gBuffer", width, height);
        
        // Post-process framebuffer
        postProcessFramebuffer = createHDRFramebuffer("postProcess", width, height);
        
        logger.info("Created common framebuffers for {}x{}", width, height);
    }
    
    /**
     * Resizes common framebuffers (typically called on window resize)
     */
    public void resizeCommonFramebuffers(int width, int height) {
        resizeFramebuffer("gBuffer", width, height);
        resizeFramebuffer("postProcess", width, height);
        
        logger.info("Resized common framebuffers to {}x{}", width, height);
    }
    
    /**
     * Handle window resize by updating all relevant framebuffers.
     * This method is called by the Renderer when the window is resized.
     */
    public void onWindowResize(int width, int height) {
        resizeCommonFramebuffers(width, height);
        logger.debug("FramebufferManager handled window resize to {}x{}", width, height);
    }
    
    /**
     * Gets framebuffer cache statistics
     */
    public void printCacheStats() {
        logger.info("Framebuffer cache: {} framebuffers loaded", framebufferCache.size());
        for (Map.Entry<String, Integer> entry : referenceCount.entrySet()) {
            logger.debug("  {} - {} references", entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Cleanup all framebuffers
     */
    public void cleanup() {
        logger.info("Cleaning up FramebufferManager...");
        
        for (Framebuffer framebuffer : framebufferCache.values()) {
            framebuffer.cleanup();
        }
        
        framebufferCache.clear();
        referenceCount.clear();
        
        logger.info("FramebufferManager cleanup complete");
    }
}