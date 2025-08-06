package com.odyssey.graphics;

import com.odyssey.core.jobs.JobSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Streaming Texture Manager for The Odyssey.
 * Combines asset streaming pipeline with texture compression for efficient
 * progressive loading of distant world content with optimal GPU memory usage.
 */
public class StreamingTextureManager {
    private static final Logger logger = LoggerFactory.getLogger(StreamingTextureManager.class);
    
    private final AssetManager assetManager;
    private final ConcurrentHashMap<String, Texture> loadedTextures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<Texture>> loadingTextures = new ConcurrentHashMap<>();
    
    // Configuration
    private final float defaultQuality;
    private final boolean generateMipmaps;
    private final int maxTextureMemoryMB;
    
    // Statistics
    private final AtomicLong totalTextureMemory = new AtomicLong(0);
    private final AtomicLong texturesLoaded = new AtomicLong(0);
    private final AtomicLong texturesEvicted = new AtomicLong(0);
    
    /**
     * Texture loading configuration.
     */
    public static class TextureConfig {
        private float quality = 0.8f;
        private boolean generateMipmaps = true;
        private AssetManager.AssetPriority priority = AssetManager.AssetPriority.MEDIUM;
        private boolean enableAnisotropicFiltering = true;
        private float anisotropicLevel = 4.0f;
        
        public TextureConfig quality(float quality) {
            this.quality = Math.max(0.0f, Math.min(1.0f, quality));
            return this;
        }
        
        public TextureConfig generateMipmaps(boolean generateMipmaps) {
            this.generateMipmaps = generateMipmaps;
            return this;
        }
        
        public TextureConfig priority(AssetManager.AssetPriority priority) {
            this.priority = priority;
            return this;
        }
        
        public TextureConfig anisotropicFiltering(boolean enable, float level) {
            this.enableAnisotropicFiltering = enable;
            this.anisotropicLevel = level;
            return this;
        }
        
        public float getQuality() { return quality; }
        public boolean shouldGenerateMipmaps() { return generateMipmaps; }
        public AssetManager.AssetPriority getPriority() { return priority; }
        public boolean isAnisotropicFilteringEnabled() { return enableAnisotropicFiltering; }
        public float getAnisotropicLevel() { return anisotropicLevel; }
    }
    
    /**
     * Creates a new StreamingTextureManager.
     *
     * @param jobSystem Job system for async operations
     * @param maxAssetCacheMB Maximum asset cache size in MB
     * @param maxTextureMemoryMB Maximum texture memory in MB
     * @param defaultQuality Default compression quality
     * @param generateMipmaps Default mipmap generation setting
     */
    public StreamingTextureManager(JobSystem jobSystem, int maxAssetCacheMB, 
                                  int maxTextureMemoryMB, float defaultQuality, 
                                  boolean generateMipmaps) {
        this.assetManager = new AssetManager(maxAssetCacheMB, 4, jobSystem);
        this.maxTextureMemoryMB = maxTextureMemoryMB;
        this.defaultQuality = defaultQuality;
        this.generateMipmaps = generateMipmaps;
        
        // Initialize texture compression
        TextureCompression.initialize();
        
        logger.info("StreamingTextureManager initialized with {}MB asset cache, {}MB texture memory", 
                   maxAssetCacheMB, maxTextureMemoryMB);
        logger.info("Texture compression capabilities: {}", 
                   TextureCompression.getCapabilities().getSupportedFormats());
    }
    
    /**
     * Loads a texture asynchronously with default configuration.
     *
     * @param texturePath Path to the texture file
     * @param callback Callback when texture is loaded (optional)
     * @return Future for the loaded texture
     */
    public CompletableFuture<Texture> loadTextureAsync(String texturePath, Consumer<Texture> callback) {
        return loadTextureAsync(texturePath, new TextureConfig(), callback);
    }
    
    /**
     * Loads a texture asynchronously with custom configuration.
     *
     * @param texturePath Path to the texture file
     * @param config Texture loading configuration
     * @param callback Callback when texture is loaded (optional)
     * @return Future for the loaded texture
     */
    public CompletableFuture<Texture> loadTextureAsync(String texturePath, TextureConfig config, 
                                                      Consumer<Texture> callback) {
        // Check if already loaded
        Texture existingTexture = loadedTextures.get(texturePath);
        if (existingTexture != null) {
            if (callback != null) {
                callback.accept(existingTexture);
            }
            return CompletableFuture.completedFuture(existingTexture);
        }
        
        // Check if already loading
        CompletableFuture<Texture> existingLoad = loadingTextures.get(texturePath);
        if (existingLoad != null) {
            if (callback != null) {
                existingLoad.thenAccept(callback);
            }
            return existingLoad;
        }
        
        // Start new loading operation
        CompletableFuture<Texture> loadFuture = assetManager.loadAssetAsync(
            texturePath, config.getPriority(), null
        ).thenApply(assetData -> {
            try {
                // Create texture from loaded asset data
                Texture texture = Texture.loadFromFile(texturePath, config.getQuality(), 
                                                      config.shouldGenerateMipmaps());
                
                // Apply additional settings
                if (config.isAnisotropicFilteringEnabled()) {
                    texture.setAnisotropicFiltering(config.getAnisotropicLevel());
                }
                
                // Check memory limits and evict if necessary
                long newMemoryUsage = totalTextureMemory.addAndGet(texture.getMemoryUsage());
                if (newMemoryUsage > maxTextureMemoryMB * 1024L * 1024L) {
                    evictTextures(maxTextureMemoryMB * 1024L * 1024L * 3 / 4); // Evict to 75% capacity
                }
                
                // Cache the texture
                loadedTextures.put(texturePath, texture);
                loadingTextures.remove(texturePath);
                texturesLoaded.incrementAndGet();
                
                logger.debug("Loaded texture: {} ({})", texturePath, texture.getInfo());
                
                return texture;
            } catch (Exception e) {
                loadingTextures.remove(texturePath);
                logger.error("Failed to create texture from asset: {}", texturePath, e);
                throw new RuntimeException("Texture creation failed: " + texturePath, e);
            }
        });
        
        loadingTextures.put(texturePath, loadFuture);
        
        if (callback != null) {
            loadFuture.thenAccept(callback);
        }
        
        return loadFuture;
    }
    
    /**
     * Loads a texture synchronously.
     *
     * @param texturePath Path to the texture file
     * @param config Texture loading configuration
     * @return Loaded texture
     */
    public Texture loadTextureSync(String texturePath, TextureConfig config) {
        // Check if already loaded
        Texture existingTexture = loadedTextures.get(texturePath);
        if (existingTexture != null) {
            return existingTexture;
        }
        
        try {
            // Load asset synchronously
            byte[] assetData = assetManager.loadAssetSync(texturePath, config.getPriority());
            
            // Create texture
            Texture texture = Texture.loadFromFile(texturePath, config.getQuality(), 
                                                  config.shouldGenerateMipmaps());
            
            // Apply additional settings
            if (config.isAnisotropicFilteringEnabled()) {
                texture.setAnisotropicFiltering(config.getAnisotropicLevel());
            }
            
            // Check memory limits and evict if necessary
            long newMemoryUsage = totalTextureMemory.addAndGet(texture.getMemoryUsage());
            if (newMemoryUsage > maxTextureMemoryMB * 1024L * 1024L) {
                evictTextures(maxTextureMemoryMB * 1024L * 1024L * 3 / 4);
            }
            
            // Cache the texture
            loadedTextures.put(texturePath, texture);
            texturesLoaded.incrementAndGet();
            
            logger.debug("Loaded texture sync: {} ({})", texturePath, texture.getInfo());
            
            return texture;
        } catch (Exception e) {
            logger.error("Failed to load texture synchronously: {}", texturePath, e);
            throw new RuntimeException("Texture loading failed: " + texturePath, e);
        }
    }
    
    /**
     * Preloads textures in the background.
     *
     * @param texturePaths Array of texture paths to preload
     * @param config Configuration for preloading
     */
    public void preloadTextures(String[] texturePaths, TextureConfig config) {
        for (String texturePath : texturePaths) {
            if (!loadedTextures.containsKey(texturePath) && !loadingTextures.containsKey(texturePath)) {
                loadTextureAsync(texturePath, config, null);
            }
        }
        
        logger.info("Preloading {} textures in background", texturePaths.length);
    }
    
    /**
     * Gets a loaded texture by path.
     *
     * @param texturePath Path to the texture
     * @return Loaded texture or null if not loaded
     */
    public Texture getTexture(String texturePath) {
        return loadedTextures.get(texturePath);
    }
    
    /**
     * Checks if a texture is loaded.
     *
     * @param texturePath Path to the texture
     * @return true if texture is loaded
     */
    public boolean isTextureLoaded(String texturePath) {
        return loadedTextures.containsKey(texturePath);
    }
    
    /**
     * Checks if a texture is currently loading.
     *
     * @param texturePath Path to the texture
     * @return true if texture is loading
     */
    public boolean isTextureLoading(String texturePath) {
        return loadingTextures.containsKey(texturePath);
    }
    
    /**
     * Evicts textures to free memory.
     *
     * @param targetMemory Target memory usage after eviction
     */
    public void evictTextures(long targetMemory) {
        if (totalTextureMemory.get() <= targetMemory) {
            return;
        }
        
        // Sort textures by usage and evict least recently used
        loadedTextures.entrySet().stream()
                .sorted((e1, e2) -> {
                    // Simple LRU based on memory usage (in a real implementation,
                    // you'd track actual access times)
                    return Integer.compare(e1.getValue().getMemoryUsage(), 
                                         e2.getValue().getMemoryUsage());
                })
                .forEach(entry -> {
                    if (totalTextureMemory.get() > targetMemory) {
                        String path = entry.getKey();
                        Texture texture = entry.getValue();
                        
                        loadedTextures.remove(path);
                        totalTextureMemory.addAndGet(-texture.getMemoryUsage());
                        texture.dispose();
                        texturesEvicted.incrementAndGet();
                        
                        logger.debug("Evicted texture: {} ({})", path, texture.getInfo());
                    }
                });
        
        logger.info("Texture eviction completed. Memory usage: {}MB", 
                   totalTextureMemory.get() / (1024 * 1024));
    }
    
    /**
     * Gets streaming statistics.
     *
     * @return Statistics string
     */
    public String getStatistics() {
        long memoryMB = totalTextureMemory.get() / (1024 * 1024);
        
        return String.format(
            "Streaming Textures: %d loaded, %d evicted, %dMB/%dMB memory (%.1f%%) | %s",
            texturesLoaded.get(), texturesEvicted.get(), memoryMB, maxTextureMemoryMB,
            (double) memoryMB / maxTextureMemoryMB * 100,
            assetManager.getCacheStats()
        );
    }
    
    /**
     * Clears all loaded textures.
     */
    public void clearTextures() {
        loadedTextures.values().forEach(Texture::dispose);
        loadedTextures.clear();
        loadingTextures.clear();
        totalTextureMemory.set(0);
        
        logger.info("All textures cleared");
    }
    
    /**
     * Shuts down the streaming texture manager.
     */
    public void shutdown() {
        clearTextures();
        assetManager.shutdown();
        
        logger.info("StreamingTextureManager shutdown completed");
    }
}