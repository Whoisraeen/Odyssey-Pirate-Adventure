package com.odyssey.core;

import com.odyssey.util.Logger;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Resource management system for The Odyssey.
 * 
 * This class handles loading, caching, and lifecycle management of game assets
 * including textures, models, shaders, audio files, and configuration data.
 * It supports both synchronous and asynchronous loading with automatic caching.
 */
public class ResourceManager {
    
    private static final Logger logger = Logger.getLogger(ResourceManager.class);
    
    // Singleton instance
    private static ResourceManager instance;
    
    // Resource caches
    private final ConcurrentHashMap<String, Object> resourceCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletableFuture<Object>> loadingFutures = new ConcurrentHashMap<>();
    
    // Resource loaders
    private final ConcurrentHashMap<String, Function<String, Object>> resourceLoaders = new ConcurrentHashMap<>();
    
    // Thread pool for async loading
    private final ExecutorService loadingExecutor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "ResourceLoader");
        thread.setDaemon(true);
        return thread;
    });
    
    // Resource paths
    private Path resourceRoot;
    private Path texturesPath;
    private Path modelsPath;
    private Path shadersPath;
    private Path audioPath;
    private Path configPath;
    private Path fontsPath;
    
    // Statistics
    private volatile long totalResourcesLoaded = 0;
    private volatile long totalBytesLoaded = 0;
    private volatile long cacheHits = 0;
    private volatile long cacheMisses = 0;
    
    /**
     * Private constructor for singleton pattern.
     */
    private ResourceManager() {
        initializeResourcePaths();
        registerDefaultLoaders();
    }
    
    /**
     * Get the singleton instance of ResourceManager.
     */
    public static synchronized ResourceManager getInstance() {
        if (instance == null) {
            instance = new ResourceManager();
        }
        return instance;
    }
    
    /**
     * Initialize resource directory paths.
     */
    private void initializeResourcePaths() {
        try {
            // Try to find resources directory relative to the working directory
            resourceRoot = Paths.get("src/main/resources").toAbsolutePath();
            
            if (!Files.exists(resourceRoot)) {
                // Fallback to resources in the classpath
                resourceRoot = Paths.get("resources").toAbsolutePath();
            }
            
            if (!Files.exists(resourceRoot)) {
                // Create resources directory if it doesn't exist
                Files.createDirectories(resourceRoot);
                logger.info("Created resources directory: {}", resourceRoot);
            }
            
            // Initialize subdirectories
            texturesPath = resourceRoot.resolve("textures");
            modelsPath = resourceRoot.resolve("models");
            shadersPath = resourceRoot.resolve("shaders");
            audioPath = resourceRoot.resolve("audio");
            configPath = resourceRoot.resolve("config");
            fontsPath = resourceRoot.resolve("fonts");
            
            // Create subdirectories if they don't exist
            createDirectoryIfNotExists(texturesPath);
            createDirectoryIfNotExists(modelsPath);
            createDirectoryIfNotExists(shadersPath);
            createDirectoryIfNotExists(audioPath);
            createDirectoryIfNotExists(configPath);
            createDirectoryIfNotExists(fontsPath);
            
            logger.info("Resource paths initialized - Root: {}", resourceRoot);
            
        } catch (IOException e) {
            logger.error("Failed to initialize resource paths", e);
            throw new RuntimeException("Failed to initialize resource manager", e);
        }
    }
    
    /**
     * Create a directory if it doesn't exist.
     */
    private void createDirectoryIfNotExists(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
            logger.debug("Created resource directory: {}", path);
        }
    }
    
    /**
     * Register default resource loaders.
     */
    private void registerDefaultLoaders() {
        // Text file loader
        registerLoader("txt", this::loadTextFile);
        registerLoader("properties", this::loadTextFile);
        registerLoader("cfg", this::loadTextFile);
        
        // Shader loaders
        registerLoader("vert", this::loadTextFile);
        registerLoader("frag", this::loadTextFile);
        registerLoader("geom", this::loadTextFile);
        registerLoader("comp", this::loadTextFile);
        registerLoader("glsl", this::loadTextFile);
        
        // Binary file loader
        registerLoader("bin", this::loadBinaryFile);
        registerLoader("dat", this::loadBinaryFile);
        
        // JSON loader (as text for now)
        registerLoader("json", this::loadTextFile);
        
        logger.info("Registered {} default resource loaders", resourceLoaders.size());
    }
    
    /**
     * Register a custom resource loader for a file extension.
     */
    public void registerLoader(String extension, Function<String, Object> loader) {
        resourceLoaders.put(extension.toLowerCase(), loader);
        logger.debug("Registered loader for extension: {}", extension);
    }
    
    /**
     * Load a resource synchronously.
     */
    @SuppressWarnings("unchecked")
    public <T> T loadResource(String resourcePath) {
        // Check cache first
        Object cached = resourceCache.get(resourcePath);
        if (cached != null) {
            cacheHits++;
            logger.trace("Cache hit for resource: {}", resourcePath);
            return (T) cached;
        }
        
        cacheMisses++;
        logger.debug("Loading resource: {}", resourcePath);
        
        try {
            // Determine file extension
            String extension = getFileExtension(resourcePath);
            Function<String, Object> loader = resourceLoaders.get(extension);
            
            if (loader == null) {
                logger.warn("No loader registered for extension: {}", extension);
                return null;
            }
            
            // Load the resource
            Object resource = loader.apply(resourcePath);
            
            if (resource != null) {
                // Cache the resource
                resourceCache.put(resourcePath, resource);
                totalResourcesLoaded++;
                logger.debug("Successfully loaded and cached resource: {}", resourcePath);
            } else {
                logger.warn("Failed to load resource: {}", resourcePath);
            }
            
            return (T) resource;
            
        } catch (Exception e) {
            logger.error("Error loading resource: {}", resourcePath, e);
            return null;
        }
    }
    
    /**
     * Load a resource asynchronously.
     */
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<T> loadResourceAsync(String resourcePath) {
        // Check cache first
        Object cached = resourceCache.get(resourcePath);
        if (cached != null) {
            cacheHits++;
            logger.trace("Cache hit for async resource: {}", resourcePath);
            return CompletableFuture.completedFuture((T) cached);
        }
        
        // Check if already loading
        CompletableFuture<Object> existingFuture = loadingFutures.get(resourcePath);
        if (existingFuture != null) {
            logger.trace("Resource already loading: {}", resourcePath);
            return existingFuture.thenApply(obj -> (T) obj);
        }
        
        cacheMisses++;
        logger.debug("Loading resource asynchronously: {}", resourcePath);
        
        // Create loading future
        CompletableFuture<Object> future = CompletableFuture.supplyAsync(() -> {
            try {
                String extension = getFileExtension(resourcePath);
                Function<String, Object> loader = resourceLoaders.get(extension);
                
                if (loader == null) {
                    logger.warn("No loader registered for extension: {}", extension);
                    return null;
                }
                
                Object resource = loader.apply(resourcePath);
                
                if (resource != null) {
                    resourceCache.put(resourcePath, resource);
                    totalResourcesLoaded++;
                    logger.debug("Successfully loaded and cached async resource: {}", resourcePath);
                } else {
                    logger.warn("Failed to load async resource: {}", resourcePath);
                }
                
                return resource;
                
            } catch (Exception e) {
                logger.error("Error loading async resource: {}", resourcePath, e);
                return null;
            }
        }, loadingExecutor);
        
        // Store the future and clean up when done
        loadingFutures.put(resourcePath, future);
        future.whenComplete((result, throwable) -> loadingFutures.remove(resourcePath));
        
        return future.thenApply(obj -> (T) obj);
    }
    
    /**
     * Check if a resource is cached.
     */
    public boolean isResourceCached(String resourcePath) {
        return resourceCache.containsKey(resourcePath);
    }
    
    /**
     * Remove a resource from cache.
     */
    public void unloadResource(String resourcePath) {
        Object removed = resourceCache.remove(resourcePath);
        if (removed != null) {
            logger.debug("Unloaded resource from cache: {}", resourcePath);
            
            // If the resource implements AutoCloseable, close it
            if (removed instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) removed).close();
                } catch (Exception e) {
                    logger.warn("Error closing resource: {}", resourcePath, e);
                }
            }
        }
    }
    
    /**
     * Clear all cached resources.
     */
    public void clearCache() {
        logger.info("Clearing resource cache ({} resources)", resourceCache.size());
        
        // Close all AutoCloseable resources
        resourceCache.values().forEach(resource -> {
            if (resource instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) resource).close();
                } catch (Exception e) {
                    logger.warn("Error closing resource during cache clear", e);
                }
            }
        });
        
        resourceCache.clear();
        logger.info("Resource cache cleared");
    }
    
    /**
     * Get resource statistics.
     */
    public ResourceStats getStats() {
        return new ResourceStats(
            totalResourcesLoaded,
            totalBytesLoaded,
            cacheHits,
            cacheMisses,
            resourceCache.size(),
            loadingFutures.size()
        );
    }
    
    /**
     * Load a text file.
     */
    private String loadTextFile(String resourcePath) {
        try {
            Path fullPath = resolveResourcePath(resourcePath);
            if (!Files.exists(fullPath)) {
                logger.warn("Text file not found: {}", fullPath);
                return null;
            }
            
            byte[] bytes = Files.readAllBytes(fullPath);
            totalBytesLoaded += bytes.length;
            
            return new String(bytes, "UTF-8");
            
        } catch (IOException e) {
            logger.error("Error loading text file: {}", resourcePath, e);
            return null;
        }
    }
    
    /**
     * Load a binary file.
     */
    private ByteBuffer loadBinaryFile(String resourcePath) {
        try {
            Path fullPath = resolveResourcePath(resourcePath);
            if (!Files.exists(fullPath)) {
                logger.warn("Binary file not found: {}", fullPath);
                return null;
            }
            
            try (FileChannel channel = FileChannel.open(fullPath, StandardOpenOption.READ)) {
                ByteBuffer buffer = ByteBuffer.allocateDirect((int) channel.size());
                channel.read(buffer);
                buffer.flip();
                
                totalBytesLoaded += buffer.remaining();
                return buffer;
            }
            
        } catch (IOException e) {
            logger.error("Error loading binary file: {}", resourcePath, e);
            return null;
        }
    }
    
    /**
     * Resolve a resource path to a full file system path.
     */
    private Path resolveResourcePath(String resourcePath) {
        // Remove leading slash if present
        if (resourcePath.startsWith("/")) {
            resourcePath = resourcePath.substring(1);
        }
        
        return resourceRoot.resolve(resourcePath);
    }
    
    /**
     * Get the file extension from a path.
     */
    private String getFileExtension(String path) {
        int lastDot = path.lastIndexOf('.');
        if (lastDot == -1 || lastDot == path.length() - 1) {
            return "";
        }
        return path.substring(lastDot + 1).toLowerCase();
    }
    
    /**
     * Get the textures directory path.
     */
    public Path getTexturesPath() {
        return texturesPath;
    }
    
    /**
     * Get the models directory path.
     */
    public Path getModelsPath() {
        return modelsPath;
    }
    
    /**
     * Get the shaders directory path.
     */
    public Path getShadersPath() {
        return shadersPath;
    }
    
    /**
     * Get the audio directory path.
     */
    public Path getAudioPath() {
        return audioPath;
    }
    
    /**
     * Get the config directory path.
     */
    public Path getConfigPath() {
        return configPath;
    }
    
    /**
     * Get the fonts directory path.
     */
    public Path getFontsPath() {
        return fontsPath;
    }
    
    /**
     * Get the root resources directory path.
     */
    public Path getResourceRoot() {
        return resourceRoot;
    }
    
    /**
     * Shutdown the resource manager.
     */
    public void shutdown() {
        logger.info("Shutting down ResourceManager");
        
        // Clear cache
        clearCache();
        
        // Shutdown executor
        loadingExecutor.shutdown();
        
        logger.info("ResourceManager shutdown complete");
    }
    
    /**
     * Resource statistics data class.
     */
    public static class ResourceStats {
        public final long totalResourcesLoaded;
        public final long totalBytesLoaded;
        public final long cacheHits;
        public final long cacheMisses;
        public final int cachedResources;
        public final int loadingResources;
        
        public ResourceStats(long totalResourcesLoaded, long totalBytesLoaded, 
                           long cacheHits, long cacheMisses, 
                           int cachedResources, int loadingResources) {
            this.totalResourcesLoaded = totalResourcesLoaded;
            this.totalBytesLoaded = totalBytesLoaded;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.cachedResources = cachedResources;
            this.loadingResources = loadingResources;
        }
        
        public double getCacheHitRatio() {
            long total = cacheHits + cacheMisses;
            return total > 0 ? (double) cacheHits / total : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "ResourceStats{loaded=%d, bytes=%dKB, cached=%d, loading=%d, hitRatio=%.2f%%}",
                totalResourcesLoaded, totalBytesLoaded / 1024, cachedResources, 
                loadingResources, getCacheHitRatio() * 100
            );
        }
    }
}